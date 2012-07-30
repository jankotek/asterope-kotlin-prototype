package skyview.geometry.projecter;

import skyview.executive.Key;
import skyview.executive.Settings;
import skyview.geometry.Transformer;
import skyview.geometry.Deprojecter;

import static org.apache.commons.math3.util.FastMath.*;

/** This class provides for the
 *  translation between coordinates and
 *  an HTM-based projection.
 *  <p>
 *  The projection is centered at the north pole.
 *  The south pole is projected to the four corners at (+/-1, +/-1).
 *  The equator projects to the diagonals running between the
 *  points (-1,0)->(0,1), (0,1)->(1,0), (1,0)->(0,-1), (-1,0)->(0,-1).
 *  These diagonals divide the four unit squares at the center of the coordinate
 *  grid into 8 right isoceles triangles.
 *  <p>
 *  The position 0,0 projects to 0,1 while the position 90,0 projects
 *  to 1,0.  Note that in an astronomical projection the coordinates
 *  will normally have the X axis inverted, so that 0,0 will
 *  project to -1,0.
 *  <p>
 *  The 8 triangles correspond to the 8 initial triangles
 *  of the HTM decomposition of the sphere.  The projection
 *  is defined such that the subtriangles in the HTM decomposition
 *  are mapped to isoceles right-triangles in the projection plane.
  *  <p>
 *  This call may be called in the same fashion as standard projections
 *  or it may be called to create a tile space.  In this case
 *  the object immediately computes the coordinates corresponding
 *  to all pixels in a tile and later returns these as requested.
 *  <p>
 *  The tiles are numbered with tile 0,0 as the top, left tile.
 *  The vertical orientation of the tiles is therefore the opposite
 *  of the orientation of the pixel values within the tiles and within
 *  the FITS files generated.  Note that when SkyView generates quicklook
 *  images it flips the Y-axis to accommodate the difference between
 *  the astronomical and imaging conventions.
 */  
public class Toa extends skyview.geometry.Projecter {
    
    /** The spatial resolution requested in the projection. */
    private final static double MIN_DELTA = 1.e-10;
    private              double minDelta  = MIN_DELTA;
    
    /** A number deemed to be indistinguishable from zero.
     *  When we wish to check if a unit vector is inside
     *  a triangle (on the sphere) we take the dot product

     *  of the unit vector with the cross-product of 
     *  each pair of unit vectors to the vertices.  We traverse the triangle in
     *  order that the dit-product should always be >=0
     *  if the point is inside the triangle.  However roundoff
     *  can cause errors.  Note that we are typically dealing
     *  with the square of very small angles, so if we want
     *  the angle to be accurate to 10^-10, we need the
     *  epsilon to be less than 10^-20.
     */
    private final static double gEPSILON  = 1.0E-20;
    private              double epsilon   = gEPSILON;
    
    private Straddle myStraddler = new OctaStraddle(RSCALE, this);
    
    /** This factor converts the grid from [-1,1] to [-pi/2,pi/2].
     *  This corresponds to having a radian scaling at the origin.
     */
    private final static double RSCALE  = PI/2;
 
    /** Rotation vectors for the longitude quadrants.
     *  We only deal with the first quadrant.
     */
    private static double[][][] quadrantRotation = {
	  { { 1, 0}, { 0, 1} },
	  { { 0,-1}, { 1, 0} },
	  { {-1, 0}, { 0,-1} },
	  { { 0, 1}, {-1, 0} } 
    };
    
    
    /** The offsets of the triangles.
     *  The square is divided into a positive
     *  and negative triangle.
     *  
     * <pre>
     *  +----
     *  ++---
     *  +++--
     *  ++++-
     * </pre>
     * 
     * These triangles will be divided into
     * four subtriangles:
     * <pre>
     *  0             100
     *  13             30
     *  112   or        2
     * <pre>
     * 
     *  These vectors gives the minimum offsets for
     *  each of the subtriangles.  Note that the four
     *  triangles are the same size
     *  (in the plane). The figures above just show
     *  how we count the triangles.
     */
    private static double[][] posTriOffsets = {
	  {0,1}, {0,0}, {1,0}, {0,0}
    };
    private static double[][] negTriOffsets = {
	  {1,1}, {0,1}, {1,0}, {1,1}
    };
    
    /** The requested grid level.  A gridLevel >= 0 implies
     *  that we are using a grid.
     */
    private int gridLevel = -1;
    /** The X value of the bounding pixel. */
    private int gridX     = 0;
    /** The Y value of the bounding pixel. */
    private int gridY     = 0;
    /** The number of levels of subdivisions to divide
     *  the bounding pixel into.
     */
     private int gridSub   = 0;
    
    /** The delta associated with the grid */
    private double gridDelta;
    
    /** The offsets associated with the grid */
    private double gridOffX;
    private double gridOffY;
    
    /** The size of the grid */
    private double nPix;
    
    /** The precomputed grid of unit vector values */
    private double[][][] gridValues;
    
    /** Temporaries */
    private double[] vec  = new double[2];
    private double[] tpos = new double[2];

    /** Do we have sinister or dexter diagonals. */
    private boolean diagonal;
    
    public Toa() {
	if (Settings.has(Key.ToastGrid)) {
	    
	    String[] params = Settings.getArray(Key.ToastGrid);
	    gridLevel = Integer.parseInt(params[0]);
	    gridX     = Integer.parseInt(params[1]);
	    gridY     = Integer.parseInt(params[2]);
	    if (params.length < 4) {
		gridSub = 8;
	    } else {
		gridSub = Integer.parseInt(params[3]);
	    }
	    double nTile    = pow(2, gridLevel);
	
            gridOffX  = (2*gridX-nTile)/nTile;
	    gridOffY  = (2*gridY-nTile)/nTile;
	    nPix      = pow(2,gridSub)+1;
	    gridDelta = 2./pow(2,gridLevel+gridSub);
	    
	    gridValues = tile(gridLevel, gridX, gridY, gridSub);
	}
    }
    
    public boolean validPosition(double[] plane) {
	return super.validPosition(plane) &&
	  abs(plane[0]) <= RSCALE && abs(plane[1]) <= RSCALE;
    }
			       
    /** Specify precision used in non-grid project/deproject calculations */
    public void setPrecision(double epsilon, double minDelta) {
	this.epsilon  = epsilon;
	this.minDelta = minDelta;
    }
    
    /** Calculate the x,y corresponding to a given lon/lat
     *  @param lon  The longitude in radians.
     *  @param lat  The latitude in radians.
     *  @return A pair of coordinates in the projection plane where
     *          there coordinates are in the range [-1,1].
     */
    
    public double[] project(double lon, double lat) {
	
	// Fix the Latitude.

	// We interpret 0,91 as being equivalent to 180,89.	
	// More outre example: 0,365 -> 180,-185 -> 0,5

	while (abs(lat) > PI/2) {
	    if (lat > PI/2) {
	        lat = PI-lat;
	    } if (lat < -PI/2) {
	        lat = -PI-lat;
	    }
	    lon = lon + PI;
	}
	
	// Find the starting square.
	while (lon < 0) {
	    lon += 2*PI;
	}
	while (lon >= 2*PI) {
	    lon -= 2*PI;
	}
	
	int    square = (int)(2*lon/PI) % 4;
	double offset = PI*square/2;
	
	lon           = lon-offset;
	
	// In the first quadrant we can be in either
	// the lower left or upper right.
	
	boolean dir  = lat >= 0;
	
	if (!dir) {
	    // Flip to the other triangle.
	    lat = abs(lat);
	}
	    
	double[] pos = find(lon, lat);
	if (!dir) {
	    // Move to the upper triangle x'=1-y, y'=1-x
	    double x = 1-pos[1];
	    double y = 1-pos[0];
	    pos[0] = x;
	    pos[1] = y;
	}
	
	double[][] qr = quadrantRotation[square];
	tpos[0]  = pos[0]*qr[0][0]+pos[1]*qr[0][1];
	tpos[1]  = pos[0]*qr[1][0]+pos[1]*qr[1][1];
	return tpos;
    }
    
    
    private double[] result = new double[2];
    
    public double[] find(double lon, double lat) {
	
	double[] unit = {
	    cos(lon)*cos(lat),
	    sin(lon)*cos(lat),
	    sin(lat)
	};
        transform(unit, result);
	return result;
    }
    
    public String getName() {
	return "Toa";
    }
    
    public String getDescription() {
	return "Projection based on HTM pixelization of sky";
    }

    public boolean isInverse(Transformer obj) {
	return obj instanceof ToaDeproj;
    }
    
    public Deprojecter inverse() {
	return new ToaDeproj();
    }
    
    double[] copy = new double[3];
    public void transform(double[] unit, double[] plane) {
	
	System.arraycopy(unit, 0, copy, 0, 3);
	
	boolean dir = true;
	double delta = 1;
	double[][] vectors;
	
	double signx = 1;
	double signy = 1;
	
	if (copy[0] < 0) {
	    copy[0] = -copy[0];
	    signx   = -1;
	}
	if (copy[1] < 0) {
	    copy[1] = -copy[1];
	    signy   = -1;
	}
	
	boolean flipped = false;
	if (copy[2] < 0) {
	    // Flip along the 1,0 -> 0,1 diagonal.
	    copy[2] = -copy[2];
	    flipped = true;
	}
	
	// Output offset
	plane[0] = 0;
	plane[1] = 0;
	
	if (dir) {
	    vectors = new double[][]{
		  {0,1,0}, {0,0,1}, {1,0,0} 
	    };
	} else {
	    vectors = new double[][]{
		  {0,0,-1}, {0,1,0},{1,0,0}
	    };
	}
	int loop = 0;
	
	while (delta > minDelta) {
	    
	    double[][] midpoints = new double[3][3];
	    delta /= 2;
	    
	    int fragment = findTriangle(copy, vectors, midpoints);
	    if (dir) {
		plane[0] += delta*posTriOffsets[fragment][0];
		plane[1] += delta*posTriOffsets[fragment][1];
	    } else {
		plane[0] += delta*negTriOffsets[fragment][0];
		plane[1] += delta*negTriOffsets[fragment][1];
	    }
	    if (fragment == 0) {
		vectors[1] = midpoints[0];
		vectors[2] = midpoints[2];
	    } else if (fragment == 1) {
		vectors[0] = midpoints[0];
		vectors[2] = midpoints[1];
	    } else if (fragment == 2)  {
		vectors[0] = midpoints[2];
		vectors[1] = midpoints[1];
	    } else {
		if (dir) {
		    vectors[0] = midpoints[2];
		    vectors[1] = midpoints[0];
		    vectors[2] = midpoints[1];
		} else {
		    vectors = midpoints;
		}
		dir = !dir;
	    }
//	    if (loop < 10) {
//		System.out.println("Loop:"+fragment+" "+plane[0]+" "+plane[1]);
//		show("  v0:", vectors[0]);
//		show("  v1:", vectors[1]);
//		show("  v2:", vectors[2]);
//	    }
//	    loop += 1;
	}
	if (flipped) {
	    double tmp = plane[0];
	    plane[0]   = 1 - plane[1];
	    plane[1]   = 1 - tmp;
	}
	    
	plane[0] *= RSCALE*signx;
	plane[1] =  RSCALE*(plane[1])*signy;
//	System.out.printf("%.6f,%.6f -> %.6f,%.6f,%.6f -> %.6f,%.6f\n",toDegrees(atan2(unit[1], unit[0])), toDegrees(asin(unit[2])),unit[0],unit[1],unit[2],plane[0],plane[1]);
    }
    
    int findTriangle(double[] unit, double[][] vectors, double[][] midpoints) {
	
	midpoint(vectors[0],vectors[1], midpoints[0]);
	midpoint(vectors[1],vectors[2], midpoints[1]);
	midpoint(vectors[2],vectors[0], midpoints[2]);
	
	if (inside(unit, vectors[0], midpoints[0],midpoints[2])) {
	    return 0;
	} 
	if (inside(unit, midpoints[0], vectors[1],midpoints[1])) {
	    return 1;
	} 
	if (inside(unit, midpoints[2], midpoints[1], vectors[2])) {
	    return 2;
	    
	} else {
	    return 3;
	}
    }
    
    /**
     * for a given vector p is it contained in the triangle whose corners are
     *  given by the vectors v1, v2,v3.
     */
    boolean inside(double[] p, double[] v1, double[] v2, double[] v3) {
	 
        double crossp[] = new double[3];
	    
	crossp[0] = v1[1] * v2[2] - v2[1] * v1[2];
	crossp[1] = v1[2] * v2[0] - v2[2] * v1[0];
	crossp[2] = v1[0] * v2[1] - v2[0] * v1[1];
	  
	if (p[0] * crossp[0] + p[1] * crossp[1] + p[2] * crossp[2] < -epsilon)
	    return false;

	crossp[0] = v2[1] * v3[2] - v3[1] * v2[2];
	crossp[1] = v2[2] * v3[0] - v3[2] * v2[0];
	crossp[2] = v2[0] * v3[1] - v3[0] * v2[1];
	if (p[0] * crossp[0] + p[1] * crossp[1] + p[2] * crossp[2] < -epsilon)
	    return false;


	crossp[0] = v3[1] * v1[2] - v1[1] * v3[2];
	crossp[1] = v3[2] * v1[0] - v1[2] * v3[0];
	crossp[2] = v3[0] * v1[1] - v1[0] * v3[1];

	if (p[0] * crossp[0] + p[1] * crossp[1] + p[2] * crossp[2] < -epsilon)
	    return false;

	return true;
    }

    /* Calculate midpoint fo vectors v1 and v2 the answer is put in
       the provided w vector.  Allow v1,v2,w vectors to be aliased.
     */
    void midpoint(double[] v1, double[] v2, double[] w) {
	double x, y, z;
	x = v1[0] + v2[0];
	y = v1[1] + v2[1];
	z = v1[2] + v2[2];
	     
	
	double tmp = sqrt(x*x + y*y + z*z);
	w[0] = x/tmp;
	w[1] = y/tmp;
	w[2] = z/tmp;
    }
    
    public double[][][] tile(int level, int ix, int iy, int subdiv) {
	
	int dim = (int) pow(2, subdiv);
	int d2  = dim/2;
	double[][][] coords = new double[dim+1][dim+1][3];
	
	// First get the bounding corners for the tile.
	if (level > 0) {
	    
	    double[][] bounds = bounds(level, ix, iy);
	    coords[dim][dim]  = bounds[0];
	    coords[0][dim]    = bounds[1];
	    coords[0][0]      = bounds[2];
	    coords[dim][0]    = bounds[3];
	    fill(coords, dim, 0, 0, dim, diagonal);
	    
	} else {
	    
	    // Can't start at level 0 since all four corners
	    // are the south pole.  So we fill in the first
	    // level and then begin do each subtile.
	    coords[0][0]      = new double[]{ 0, 0,-1};
	    coords[0][dim]    = new double[]{ 0, 0,-1};
	    coords[dim][0]    = new double[]{ 0, 0,-1};
	    coords[dim][dim]  = new double[]{ 0, 0,-1};
	    
	    coords[d2][0]     = new double[]{ 0,-1, 0};
	    coords[0][d2]     = new double[]{-1, 0, 0};
	    coords[d2][dim]   = new double[]{ 0, 1, 0};
	    coords[dim][d2]   = new double[]{ 1, 0, 0};
	    coords[d2][d2]    = new double[]{ 0, 0, 1};
	    
	    fill(coords, d2, 0,  0,  d2, true);
	    fill(coords, d2, 0,  d2, d2, false);
	    fill(coords, d2, d2, 0,  d2, false);
	    fill(coords, d2, d2, d2, d2, true);
	}
	return coords;
    }
    
    
    double[][] bounds(int level, int ix, int iy) {
	
	int pow = (int)pow(2,level-1);
	int tx  = ix/pow;
	int ty  = iy/pow;
	double[][] coords;
	if (tx == 0 && ty == 1) {
	    coords = new double[][]{ { 0, 0, 1}, {-1, 0, 0}, { 0, 0,-1}, {0,-1, 0} };
	} else if (tx == 1 && ty == 1) {
	    coords = new double[][]{ { 1, 0, 0}, { 0, 0, 1}, { 0,-1, 0}, {0, 0,-1} };
	} else if (tx == 0 && ty == 0) {
	    coords = new double[][]{ { 0, 1, 0}, { 0, 0,-1}, {-1, 0, 0}, {0, 0, 1} };
	} else {
	    coords = new double[][]{ { 0, 0,-1}, { 0, 1, 0}, { 0, 0, 1}, {1, 0, 0} };
	}
	diagonal = (tx != ty);
	return bounds(coords, level-1, ix%pow, iy%pow);
    }
    
    double[][] temp    = new double[4][3];
    double[]   diagTem = new double[3]; 
    
    double[][] bounds(double[][] coords, int level, int x, int y) {
	
	if (level == 0) {
	    return coords;
	}
	int pow = (int)pow(2, level-1);
	
	int tx = x/pow;
	int ty = y/pow;
	int ind = 0;
	if (tx == 1 && ty == 0) {
	    ind = 0;
	} else if (tx == 0 && ty == 0) {
	    ind = 1;
	} else if (tx == 0 && ty == 1) {
	    ind = 2;
	} else {
	    ind = 3;
	}
	
	// First handle diagonal corner.
	int diag = (ind+2)%4;
	if (diagonal) {
	    midpoint(coords[1],coords[3],diagTem);
	} else {
	    midpoint(coords[0],coords[2],diagTem);
	}
	
	// Now handle the two adjacent corners.
	for (int i=0; i<4; i += 1) {
	    // Don't process the selected corner or its
	    // diagonal partner (i.e., 0,2 or 1,3)
	    if (i != ind  && (i+ind) % 2 != 0) {
	       midpoint(coords[ind], coords[i], coords[i]);
	    }
	}
	
	// Copy the diagonal in...
	System.arraycopy(diagTem, 0, coords[diag], 0, diagTem.length);
	return bounds(coords, level-1, x%pow, y%pow);
    }
	    
    void fill(double[][][] coords, int len, int x0, int y0, int mx, boolean sinister) {
	int dim = len;
	while (dim > 1) {
	    
	    int dim2 = dim/2;
	    for (int i=y0+dim2; i<y0+mx; i += dim) {
		int ym = i-dim2;
		int yp = i+dim2;
		
		for (int j=x0+dim2; j<x0+mx; j += dim) {
		    int xm = j-dim2;
		    int xp = j+dim2;
		    midpoint(coords[ym][xm], coords[ym][xp], coords[ym][j]);
		    midpoint(coords[ym][xm], coords[yp][xm], coords[i][xm]);
		    midpoint(coords[yp][xm], coords[yp][xp], coords[yp][j]);
		    midpoint(coords[ym][xp], coords[yp][xp], coords[i][xp]);
		    if (sinister) {
			midpoint(coords[yp][xm], coords[ym][xp], coords[i][j]);
		    } else {
			midpoint(coords[ym][xm], coords[yp][xp], coords[i][j]);
		    }
		}
	    }
	    dim = dim2;
	}
    }
    
    /** Deproject from the ToastPlane back to the unit sphere */
    public class ToaDeproj extends skyview.geometry.Deprojecter {
	
	public String getName() {
	    return "ToaDeproj";
	}
	public String getDescription() {
	    return "Deproject from the TOAST plane to the unit sphere";
	}
	
	public boolean isInverse(Transformer obj) {
	    return obj instanceof Toa;
	}
	
	public Transformer inverse() {
	    return Toa.this;
	}
	
        public void transform(double[] plane, double[] sphere) {
	    double[] res = deproject(plane[0]/RSCALE, plane[1]/RSCALE);
//	    show(" Deproject:", res);
	    System.arraycopy(res,0, sphere, 0, res.length);
        }
    
	double[] result = new double[3];
	
        /** Deproject from the plane to the sky.  Note that
	 *  deproject uses the [-1,1] range of plane coordinates
	 *  while transform uses [-pi/2,pi/2].
         */
	
        public double[] deproject(double x, double y) {
	    
	    if (gridLevel >= 0) {
		
		x = x-gridOffX;
		y = y-gridOffY;
		
		double tx = floor(x/gridDelta + 0.5);
	        double ty = floor(y/gridDelta + 0.5);
		
		
		if (tx >= 0 && tx <= nPix && ty >= 0 && ty <= nPix) {
		    return gridValues[(int)tx][(int)ty];
		}
	    }
	    // Translate to the nominal region.
	    x = (x+1) % 2 - 1;
	    y = (y+1) % 2 - 1;
	
	    // We'll want to deal with everything in quadrant 0
	    double signx = 1;
	    double signy = 1;
	    if (x < 0) {
		x     = -x;
		signx = -1;
	    }
	    
	    if (y < 0) {
		y      = -y;
	        signy  = -1;
	    }
	
	    double[][] vectors;
	    boolean dir;
            if (x + y > 1) {  // Dec < 0
	        vectors = new double[][]{{0,0,-1}, {0,1,0}, {1,0,0}};
	        dir = false;
	    } else {
	        vectors = new double[][]{{0,1,0},  {0,0,1}, {1,0,0}};
	        dir = true;
	    }
	    double delta  = 1;
	    double factor = 1;
	
	    while (delta > minDelta) {
	    
	        delta  = delta/2;
	        factor = factor*2;
	        x *= 2;
	        y *= 2;
	    
	        double[][] midpoints = new double[3][3];
	    
	        midpoint(vectors[0],vectors[1], midpoints[0]);
	        midpoint(vectors[1],vectors[2], midpoints[1]);
	        midpoint(vectors[2],vectors[0], midpoints[2]);
	
	        if (dir) {
		    if (y > 1) {
		        vectors[1] = midpoints[0];
		        vectors[2] = midpoints[2];
		        y -= 1;
		    } else if (x > 1) {
		        vectors[0] = midpoints[2];
		        vectors[1] = midpoints[1];
		        x -= 1;
		    } else if (x+y < 1) {
		        vectors[0] = midpoints[0];
		        vectors[2] = midpoints[1];
		    } else {
		        vectors[0] = midpoints[2];
		        vectors[1] = midpoints[0];
		        vectors[2] = midpoints[1];
		        dir    = !dir;
		    }
	        } else {
		    if (x < 1) {
		        vectors[0] = midpoints[0];
		        vectors[2] = midpoints[1];
		        y -= 1;
		    } else if (y < 1) {
		        vectors[0] = midpoints[2];
		        vectors[1] = midpoints[1];
		        x -= 1;
		    } else {
		        x -= 1;
		        y -= 1;
		        if (x + y < 1) {
			    vectors = midpoints;
			    dir = !dir;
		        } else {
			    vectors[1] = midpoints[0];
			    vectors[2] = midpoints[2];
		        }
		    }
	       }
	    }
	
	    double xs = vectors[0][0] + vectors[1][0] + vectors[2][0];
	    double ys = vectors[0][1] + vectors[1][1] + vectors[2][1];
            double zs = vectors[0][2] + vectors[1][2] + vectors[2][2];
	    double norm = sqrt(xs*xs + ys*ys + zs*zs);
	
 	    double[] unit = {xs/norm, ys/norm, zs/norm};
	    unit[0] *= signx;
	    unit[1] *= signy;
	    return unit;
        }
    }
	
    public static void show(String prefix, double[] vector) {
	
	double[] coords = skyview.geometry.Util.coord(vector);
	System.out.printf("%s: %12.5f %12.5f (%9.5f %9.5f %9.5f)\n",
			  prefix, toDegrees(coords[0]),
			  toDegrees(coords[1]),
			  vector[0], vector[1], vector[2]);
    }
    
    public static void main(String[] args) throws Exception {
	double[] unit = new double[3];
	double[] plane = new double[2];
	
	double ra  = toRadians(Double.parseDouble(args[0]));
	double dec = toRadians(Double.parseDouble(args[1]));
	
	unit[2] = sin(dec);
	unit[0] = cos(ra)*cos(dec);
	unit[1] = sin(ra)*cos(dec);
	
	Toa tp = new Toa();
	tp.transform(unit,plane);
	System.out.printf("%.6f,%.6f\n", plane[0]/RSCALE, plane[1]/RSCALE);
    }
    
    public boolean straddleable() {
	return true;
    }
    
    public double[] shadowPoint(double x, double y) {
	if (abs(x) == abs(y)) {
	    return new double[]{x,y};
	}
	
	else if (abs(y) > abs(x)) {
	    if (y > 0) {
		return new double[]{x,2*RSCALE-y};
	    } else {
		return new double[]{x,-2*RSCALE-y};
	    }
	}
	
	else if (x > 0) {
	    return new double[]{2*RSCALE-x, y};
	} else {
	    return new double[]{-2*RSCALE-x, y};
	}
    }
    
    public boolean straddle(double[][] vertices) {
	return myStraddler.straddle(vertices);
    }
    
    public double[][][] straddleComponents(double[][] vertices) {
	return myStraddler.straddleComponents(vertices);
    }
	
}
