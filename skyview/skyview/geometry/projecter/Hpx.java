package skyview.geometry.projecter;

import static java.lang.Math.PI;

import skyview.geometry.Projecter;
import skyview.geometry.Deprojecter;
import skyview.geometry.Transformer;


/**
 *  This class treats the HealPix projection as being
 *  skewed by 45 degrees so that the squares are oriented with
 *  the coordinate axes.  We rearrange the 12 base squares into the
 *  following orientation
 *  <code>
 *     3       
 *     40
 *     851
 *      962
 *       A7
 *        B
 *  </code>
 * Thus the 12 data squares can be enclosed in a 6x4 array including another
 * 12 unused squares.
 * <p>
 * An alternative arrangement might be.
 * <code>
 *     40  
 *     851 
 *      962
 *       A73
 *        B
 * </code>
 * where the data squares can be enclosed in a 5x5 array.
 * <p>
 * The actual transformations to and from the coordinate plane
 * are carried out using the static methods proj and deproj which
 * are called by the relevant method of Hpx an HpxDeproj.  Note
 * that HpxDeproj is included as a static class.
 * <p>
 * The transformation does not depend upon the input dimension.
 * This is used in ancillary functions (notably cvtPixel) which are
 * used when individual pixels are to be considered rather than the
 * geometric transformation between sphere and plane.
 * 
 */

public class Hpx extends Projecter {
    
    private int[] tileNum  = {
       -1, -1, -1, 11,
       -1, -1, 10,  7,
       -1,  9,  6,  2,
	8,  5,  1, -1,
	4,  0, -1, -1,
	3, -1, -1, -1,
    };
    
    private static final double[][] tileOffsets= {
	  {2*Math.PI, 0}
    };
    
    /** The coordinates of the corners of squares 0 to 11. */   
    private double[] botLeftX={-1,  0,  1, -2, -2, -1,  0,  1, -2, -1,  0,  1};
    private double[] botLeftY={ 1,  0, -1,  2,  1,  0, -1, -2,  0, -1, -2, -3};
    
    private static final double[] error= {Double.NaN, Double.NaN, Double.NaN};
    
    /* The offsets are chosen with respect to an origin at the corner
     *  shared by squares 5 and 6.  This is at the position (3 pi/4, 0)
     *  in the nominal HEALPIX x,y plane.
     */
    private double[] zp = {0.75*PI, 0};
    
    // Recall sin(45)=cos(45) = 1/sqrt(2)
    private double isqrt2 = 1./Math.sqrt(2);
    
    private int nSide;
    private int nPixel;
    private int nSq;
    private int dim;
    
    private double sqDelta;
    
    // The nominal HEALPix projection has the squares being
    // of length pi*sqrt(2)/4
    private final double squareLength = PI*isqrt2/2;
    
    /** Default to the 512x512 squares */
    public Hpx() {
	this(9);
    }
    
    
    /** @param dim The power of two giving the number of pixels
     *             along an edge of a square.  The total number
     *             of pixels in the projection is 12 * Math.pow(2, 2*dim)
     */
    public Hpx(int dim) {
	setDimension(dim);
    }
    
    public void setDimension(int dim) {
	this.dim = dim;
	
	nSide   = (int) Math.pow(2, dim);
	nSq     = nSide * nSide;
	nPixel  = 12*nSq;
	sqDelta = 1./nSide;
    }
    
    
    public String getName() {
	return "Hpx";
    }
    
    public String getDescription() {
	return "HEALPix projection";
    }
    
    public Deprojecter inverse() {
	return new HpxDeproj();
    }
    
    
    public boolean isInverse(Transformer t) {
	return false;
// We'd like this to cancel out with HPXDeproj
// but then the scaler can send data outside the bounds of
// the underlying image.
//	return t instanceof HpxDeproj;
    }
    
    public boolean validPosition(double[] plane) {
	return super.validPosition(plane) &&
	  Math.abs(plane[1]) <= Math.PI/2 - Math.abs(Math.PI/4 - (Math.abs(plane[0])% Math.PI/2));
    }
	  
    /** Get the lower left corner in the oblique projection */
    public double[] getOblCorner(int pix) {
	
	if (pix < 0 || pix >= nPixel) {
	    return new double[]{Double.NaN, Double.NaN};
	}
	
	int square = pix/nSq;
	
	int    rem   = pix % nSq;
	double delta = 0.5;
	int    div   = nSide/2;
	
	double x = botLeftX[square];
	double y = botLeftY[square];
	
	while (div > 0) {
	    int test = rem/div;
	    int nrem = rem%div;
	    
	    x += (rem%2)*delta;
	    y += (rem/2)*delta;
	    
	    delta /= 2;
	    div   /= 2;
	    rem    = nrem;
	}
	return new double[]{x,y};
    }
	    
    /** Get the corners of the pixel in the nominal orientation */
    public double[][] getCorners(int pix) {
	
	double[][] z = new double[4][2];
	
	z[0] = getOblCorner(pix);
	z[1][0] = z[0][0] + sqDelta;
	z[1][1] = z[0][1];
	z[2][0] = z[1][0];
	z[2][1] = z[0][1] + sqDelta;
	z[3][0] = z[0][0];
	z[3][1] = z[2][1];
	
	for (int i=0; i<z.length; i += 1) {
	    double[] t = z[i];
	    
	    double   u = t[0];
	    double   v = t[1];
	    
	    t[0] = squareLength*(isqrt2*u - isqrt2*v) + zp[0]; 
	    if (t[0] > 2*PI) {
		t[0] -= 2*PI;
	    } else if (t[0] < 0) {
		t[0] += 2*PI;
	    }
	    
	    t[1] = squareLength*(isqrt2*u + isqrt2*v) + zp[1];
	}
	return z;
    }

    /** Given an X-Y in the nominal HEALPix projection, return
     *  the unit vector on the sphere.
     */
    public static void deproj(double[] in, double[] unit) {
	
	double x = in[0];
	double y = in[1];
	if (x < 0) {
	    x += 2*PI;
	}
	if (x > 2*PI) {
	    x -= 2*PI;
	}
	
	// Check that we are in the valid region.
	if (Math.abs(y) > 0.5*PI) {
	    System.arraycopy(error, 0, unit, 0, error.length);
	    return;
	}
	if (Math.abs(y) > 0.25*PI) {
	    double posit = (x/PI) % 0.5; 
	    double yt    = Math.abs(y)/PI;
	    
	    // Add a small delta to allow for roundoff.
	    if (yt > (0.5-Math.abs(posit-0.25))+1.e-13 ) {
		System.arraycopy(error, 0, unit, 0, error.length);
	        return;
	    }
	}
	
	double ra, sdec;
	if (Math.abs(y) < PI/4) {
	    ra   = x;
	    sdec = (8*y/(3*PI));
	    
	} else {
	    
	    double yabs = Math.abs(y);
	    double xt   = x % (PI/2);
	    ra  = x - (yabs - PI/4)/(yabs-PI/2) * (xt - PI/4);
	    if (Double.isNaN(ra)) {
		ra = 0;
	    }
	    sdec = (1 - (2-4*yabs/PI)*(2-4*yabs/PI)/3 ) * y / yabs;
	    if (sdec > 1) {
		sdec =  1;
	    } else if (sdec < -1) {
		sdec = -1;
	    }
	}
	double cdec = Math.sqrt(1-sdec*sdec);
	unit[0] = Math.cos(ra)*cdec;
	unit[1] = Math.sin(ra)*cdec;
	unit[2] = sdec;
	return;
    }
	    

    public static void proj(double[] unit, double[] proj) {
	
	if (Math.abs(unit[2]) < 2./3) {
	    proj[0] = Math.atan2(unit[1], unit[0]);
	    if (proj[0] < 0) {
		proj[0] += 2*PI;
	    }
	    proj[1] = 3*PI/8 * unit[2];
	    
	} else {
	    double phi = Math.atan2(unit[1], unit[0]);
	    if (phi < 0) {
		phi += 2*PI;
	    }
	    
	    double phit = phi % (PI/2);
	    
	    double z    = unit[2];
	    double sign = 1;
	    if (z < 0) {
		z    = -z;
		sign = -1;
	    }
	    
	    double sigma = sign*(2-Math.sqrt(3*(1-z)));
	    proj[0] = phi - (Math.abs(sigma)-1)*(phit-PI/4);
	    proj[1] = PI*sigma/4;
	}
	double x = proj[0]/Math.PI;
	double y = proj[1]/Math.PI;
	// We move the right half of tile 4 and all of tile 3 back by 2 PI
        // so that the standard region is appropriate for the 4x6 region in the
	// rotated coordinates.
	if (x > 1.5 && y > 1.75-x)  {
	    proj[0] -= 2*Math.PI;
	}
    }
    
    public void transform(double[] sphere, double[] plane) {
	proj(sphere, plane);
    }
    
    /** Find the pixel that includes the given position.
     * 
     * @param pos The position in the nominal HEALPix projection plane */
    
    public int getPixel(double[] pos) {
	return getPixel(pos[0], pos[1]);
    }
    
    /** Find the pixel that includes the given position.
     *  Generally if a position is exactly on a pixel border the pixel
     *  with the larger coordinate value will be returned.
     * 
     * @param x,y The coordinates of point for which the pixel number is desired.
     */
    
    public int getPixel(double x, double y) {
	// First convert this to the oblique projection plane.
	
	if (x < 0) {
	    x += 2*PI;
	} else if (x > 2*PI) {
	    x -= 2*PI;
	}
	
	
	double u,v;
	// Move to standard rotation center
	x = x - zp[0];
	y = y - zp[1];
	
	// Now rotate to the oblique plan.
	u = ( x*isqrt2 + y*isqrt2)/squareLength;
	v = (-x*isqrt2 + y*isqrt2)/squareLength;
	
	return getObliquePixel(u,v);
    }
    
    public int getObliquePixel(double u, double v) {
	
	
	double xSq  = Math.floor(u);
        double ySq  = Math.floor(v);
	
	
	// Find out which tile we are in.
	
	if (xSq < -2 || xSq >= 2) {
	    // The sequence of tiles repeats every
	    // four columns but ySq=5 for xSq=0
	    // corresponds to ySq=1 for xSq=4
	    int ix = (int) xSq + 2;
	    
	    ySq = ySq + 4*(ix/4);
	    xSq = ix%4;
	    if (xSq < 0) {
		xSq += 4;
	    }
	    xSq -= 2;
	}
	    
	int td   = (int) (xSq+2 + 4*(ySq+3));
	if (ySq < -3 || ySq >= 3) {
	    td = -1;
	}
	
	int tile = -1;
	
	if (td >= 0 && td < tileNum.length) {
	    tile = tileNum[td];
	}
	
	if (tile == -1) {
	    return -1;
	}
	
	int pix = nSq*tile;
	
	double delta = 0.5;
	double dx    = u - xSq;
	double dy    = v - ySq;
	
	int npix = nSide;
	while (npix > 1) {
	    
	    int ipix = 0;   // Will be 0-3 at the end.
	    
	    if (dy  >= delta) {
		ipix = 2;
		dy  -= delta;
	    }
	    
	    if (dx   >= delta) {
		ipix += 1;
		dx   -= delta;
	    }
	    
	    npix   /= 2;
	    delta  /= 2;
	    
	    pix += ipix*npix*npix;
	    
	}
	
	return pix;
    }
    
    /** This method converts a pixel number based on the assumption
     *  that we have a simple two-d image map, into the nested HEALPix
     *  pixel number. This routine assumes that the input pixel
     *  numbers are associated with a (4 nSide)x(6 nSide) virtual
     *  image.  Note that this is assumed to be in the oblique frame.
     */
    public int cvtPixel(int pixel) {
	
	int    ix = pixel % (4 * nSide);
	int    iy = pixel / (4 * nSide);
	
	
	double px = (double)(ix)/nSide;
	double py = (double)(iy)/nSide;
	
	int result = getObliquePixel(px-2, py-3);
	
//	System.err.println("Pixel: "+pixel+"   ->   "+result);
	
	return result;
    }
	
    
    public static void main(String[] args) {
	
	Hpx ob = new Hpx(Integer.parseInt(args[0]));
	
	int       pix = Integer.parseInt(args[1]);
	double[]   x1 = ob.getOblCorner(pix);
	double[][] corners = ob.getCorners(pix);
	
	System.out.println("Oblique corner:"+x1[0]+","+x1[1]);
	
	
	for (int i=0; i<corners.length; i += 1) {
	    System.out.println("Nominal Corners "+i+": "+corners[i][0]/PI+" "+corners[i][1]/PI);
	}
	
	double x = Double.parseDouble(args[2]);
	double y = Double.parseDouble(args[3]);
	double[] unit = new double[3];
	double[] proj = new double[2];
	double xr = Math.toRadians(x);
	double yr = Math.toRadians(y);
	unit[0] = Math.cos(xr)*Math.cos(yr);
	unit[1] = Math.sin(xr)*Math.cos(yr);
	unit[2] = Math.sin(yr);
	
        ob.proj(unit, proj);
	
	ob.deproj(proj, unit);
	System.out.println("  deproj back to:"+Math.toDegrees(Math.atan2(unit[1], unit[0]))+" "+
			                       Math.toDegrees(Math.asin(unit[2])));
	
	System.out.println("Pixel is:"+ob.getPixel(proj));
    }
    
    public static class HpxDeproj extends Deprojecter {
	
	public String getName() {
	    return "HpxDeproj";
	}
	
	public String getDescription() {
	    return "HEALPix deprojector";
	}
	
	public Transformer inverse() {
	    return new Hpx();
	}
	
	public boolean isInverse(Transformer t) {
	    return false;
//  Would like to do the following but seems to cause things
//  to run outside proper bounds
//	    return t instanceof Hpx;
	}
	
	public void transform(double[] plane, double[] sphere) {
	    deproj(plane, sphere);
        }
    }
}
