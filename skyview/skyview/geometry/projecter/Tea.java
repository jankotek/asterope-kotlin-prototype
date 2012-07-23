package skyview.geometry.projecter;

import skyview.executive.Settings;
import skyview.geometry.Transformer;
import skyview.geometry.Deprojecter;
import skyview.geometry.Util;

import static org.apache.commons.math3.util.FastMath.*;

/** This class provides for the
 *  translation between coordinates and the TEA Equal Area projection.
 *  <p>
 *  The projection is centered at the north pole.
 *  The south pole is projected to the four corners at (+/-1, +/-1).
 *  The equator projects to the diagonals running between the
 *  points (-1,0)->(0,1), (0,1)->(1,0), (1,0)->(0,-1), (-1,0)->(0,-1).
 *  These diagonals divide the four unit squares at the center of the coordinate
 *  grid into 8 right isoceles triangles.
 */  
public class Tea extends skyview.geometry.Projecter {
    
    private static final double sqrt2 = sqrt(2);
    private static final double rat   = sqrt(PI);
    
    private Straddle myStraddler = new OctaStraddle(rat, this);
    
    public String getName() {
	return "Tea";
    }
    
    public String getDescription() {
	return "Equal Areal Projection based with TOAST topology";
    }

    public boolean isInverse(Transformer obj) {
	return obj instanceof TeaDeproj;
    }
    
    public Deprojecter inverse() {
	return new TeaDeproj();
    }
    
    
    private double[] copy = new double[3];
    public void transform(double[] unit, double[] plane) {
	
	
	boolean dir = true;
	double  delta = 1;
	double[][] vectors;
	
	System.arraycopy(unit, 0, copy, 0, 3);
	
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
	//  z = sin(theta)
	double t   = sqrt((1-copy[2])/2);
	double lat = atan2(copy[1], copy[0]);
	double u   = 4*t*lat/PI;
	
	double x = t*sqrt2 - u/sqrt2;
	double y = u/sqrt(2);
	
	if (flipped) {
	    u = 1-x;
	    t = 1-y;
	    
	    x = t;
	    y = u;
	}
	
	x *= signx;
	y *= signy;
	
//	double[] coo = Util.coord(unit);
//	System.err.printf("Project: %.3f,%.3f  ->  %.3f %.3f\n",
//			   toDegrees(coo[0]), toDegrees(coo[1]),
//			   plane[0], plane[1]);
	plane[0] = rat*x;
	plane[1] = rat*y;
	
    }
    
    
    
    /** Deproject from the plane back to the unit sphere */
    public class TeaDeproj extends skyview.geometry.Deprojecter {
	
	public String getName() {
	    return "TeaDeproj";
	}
	public String getDescription() {
	    return "Deproject from an equal area TOAST style plane to the unit sphere";
	}
	
	public boolean isInverse(Transformer obj) {
	    return obj instanceof Tea;
	}
	
	public Transformer inverse() {
	    return Tea.this;
	}
	
        public void transform(double[] plane, double[] sphere) {
	    
	    double xflip = 1;
	    double yflip = 1;
	    double zflip = 1;
	    
	    double x = plane[0]/rat;
	    double y = plane[1]/rat;
	    
	    // We actually only calculate the transform for
	    // the northern hemisphere, first quadrant in the prime square.
	    // We need to transform everything else to that point
	    // 
	    // First transform to the -1..1,-1,..1 prime square.
	    while (x > 1) {
		xflip *= -1;
		x     -= 2;
	    }
	    while (x < -1) {
		xflip *= -1;
		x     += 2;
	    }
	    
	    while (y > 1) {
		yflip *= -1;
	        y     -= 2;
	    }
	    
	    while (y < -1) {
		yflip *= -1;
		y     += 2;
	    }
	    
	    // Find the quadrant we are in within the square.
	    if (x < 0 ) {
		xflip *= -1;
		x      = -x;
	    }
	    if (y < 0) {
		yflip *= -1;
		y      = -y;
	    }
	    
	    // Northern or southern hemisphere (y=1-x is divider)
	    if (x > 1-y) {
		zflip    = -1;
		double t =  x;
		x        = 1-y;
		y        = 1-t;
	    }
	    
	    double t = (x+y)/sqrt2;
	    double u = sqrt2*y;
	    
	    double z = 0;
	    if (t > 0) {
	        double l = PI/4 * u/t;
		z = 1-2*t*t;
		x = cos(l)*z;
		y = sin(l)*z;
	    } else {
		z = 1;
		x = 0;
		y = 0;
	    }
	    x = x*xflip;
	    y = y*yflip;
	    z = z*zflip;
	    
	    sphere[0] = x;
	    sphere[1] = y;
	    sphere[2] = z;
//	    double[] coo = Util.coord(sphere);
//	    System.err.printf("DeProject: %.3f %.3f  ->  %.3f %.3f\n",
//			   plane[0], plane[1],
//			   toDegrees(coo[0]), toDegrees(coo[1])
//			   );
        }
	
    }
    
    public static void main(String[] args) throws Exception {
	double x = Double.parseDouble(args[0]);
	double y = Double.parseDouble(args[1]);
	double[] pos = new double[]{x,y};
	Transformer  forward  = new Tea();
	Transformer  back     = forward.inverse();
	
	double[] unit = new double[3];
	double[] npos = new double[2];
	
	back.transform(pos, unit);
	forward.transform(unit, npos);
	double[] coords = Util.coord(unit);
	
	System.out.println(
	    "Original map coordinates: "+pos[0]+" "+pos[1]+"\n"+
	    "Transform to vector:      "+unit[0]+" "+unit[1]+" "+unit[2]+"\n"+
	    "At sphericalccoordinates: "+toDegrees(coords[0])+
			                 " "+toDegrees(coords[1])+"\n"+
	    "Back to map coordinates:  "+npos[0]+" "+npos[1]);
    }
    
    public boolean straddleable() {
	return true;
    }
    
    public boolean straddle(double[][] vertices) {
	return myStraddler.straddle(vertices);
    }
    
    public double[][][] straddleComponents(double[][] vertices) {
	return myStraddler.straddleComponents(vertices);
    }
}
