package skyview.geometry.projecter;

import static org.apache.commons.math3.util.FastMath.*;

import skyview.geometry.Projecter;
import skyview.geometry.Deprojecter;
import skyview.geometry.Transformer;

import skyview.geometry.sampler.Clip;

import static org.apache.commons.math3.util.FastMath.*;

/** This class implements the <a href="http://www.quadibloc.com/maps/meq0801.htm"> AIT (Hammer-Aitoff) </a>
 *  projection.  This version uses only the sqrt
 *  function without any calls to trigonometric functions.
 *  <p/>
 *  The Hammer-Aitoff or Aitoff (Ait):  The Aitoff projection is an equal-area projection that transforms the sky into a elliptical region.
 *  The Aitoff projection is usually used for all sky projections around the origin.  Maximum distortions are much less severe than for the
 *  Cartesian projection.
 */

public class Ait extends Projecter {
    
    private Straddle myStraddler = new AitStraddle(this);
    
    /** The name of the Component */
    public String getName() {
	return "Ait";
    }
    
    /** A description of the component */
    public String getDescription() {
	return "Project to an Hammer-Aitoff projection (often used for all sky data)";
    }
    
    /** Get the associated deprojecter */
    public Deprojecter inverse() {
	return new Ait.AitDeproj();
    }
    
    /** Is this the inverse of another transformation? */
    public boolean isInverse(Transformer trans) {
	return trans.getName().equals("AitDeproj");
    }
    
    public final void transform(double[] sphere, double[] plane) {
	if ( Double.isNaN(sphere[2]) ) {
	    plane[0] = Double.NaN;
	    plane[1] = Double.NaN;
	} else {
	    forwardTransform(sphere, plane);
	}
    }
    
    public static void forwardTransform(double[] sphere, double[] plane) {
	
	// Sphere[2] is just sin_b.
	double cos_b = sqrt(1-sphere[2]*sphere[2]);
	double cos_l = 0;
	
	if (1 - abs(sphere[2]) > 1.e-10) {
	    // Not at a pole
	    cos_l = sphere[0]/cos_b;
	}
	
	// Use half angle formulae to get cos(l/2), sin(l/2)
	// Be careful of roundoff errors.
	
	double cos_l2 = (0.5*(1+cos_l));
	if (cos_l2 > 0) {
	    cos_l2 = sqrt(cos_l2);
	} else {
	    cos_l2 = 0;
	}
	
	double sin_l2 = (0.5*(1-cos_l));
	if (sin_l2 > 0) {
	    sin_l2 = sqrt(sin_l2);
	} else {
	    sin_l2 = 0;
	}
	
	// Need to be careful to handle the sign of the
	// half angle formulae.  We're treating this as a projection
	// around 0,0.  So we're really looking not at 0 - 2PI for the
	// range of L, but -PI to PI.  So if we have a negative
	// Y value we want to use a negative value for sin(L/2)
	// In this interval cos(L/2) is guaranteed to be positive.
	
	if (sphere[1] < 0) {
	    sin_l2 = -sin_l2;
	}
	
	// Now use Calabretta and Griesen formulae.
	double gamma = sqrt( 2 / (1 + cos_b*cos_l2));
	plane[0] = 2*gamma*cos_b*sin_l2;
	plane[1] = gamma*sphere[2];
    }
    
    public static void reverseTransform(double[] plane, double[] sphere) {
	    
	// Use Calabretta and Greisen fomulae
        double z = (1 - plane[0]*plane[0]/16 - plane[1]*plane[1]/4 );
	if (z > 0) {
	    z = sqrt(z);
	} else {
	    z = 0;
	}
	
	sphere[2]  = plane[1]*z;
	double cos_b = sqrt(1-sphere[2]*sphere[2]);
	
	if (abs(cos_b) > 1.e-12) {
		
	    // Use the double able formula to get form sin(l/2) to sin(l)
	    // C&G don't actually gives these values for the
	    // sin(l/2) and cos(l/2).  Rather they give
	    // L = 2*arg(2*z*z-1, z*x/2)
	    // This gives the sin(l/2), cos(l/2) to within
	    // a factor.  Empirically that seems to be 1/cos(B)
	    // Using the double angle formulae we only need to
	    // compute two square roots for the transformation.
	
	    double sl2 = z*plane[0]/(2*cos_b);
	    double cl2 = (2*z*z-1)/cos_b;
	
            // Double angle formulae
	    double cl = 2*cl2*cl2-1;
	    double sl = 2*sl2*cl2;
		
	    sphere[0] = cl*cos_b;
	    sphere[1] = sl*cos_b;
		
	} else {
	    sphere[0] = 0;
	    sphere[1] = 0;
	}
       
    }
    
    public boolean validPosition(double[] plane) {
        return super.validPosition(plane) &&
	       plane[0]*plane[0]/8 + plane[1]*plane[1]/2 <= 1;
    }
    
    public boolean straddleable() {
	return true;
    }
    
    /** Does this figure straddle the boundary.
     */
    public boolean straddle(double[][] pnts) {
	return myStraddler.straddle(pnts);
    }
    
    /** Find the shadow point for the given element.
     */
    public double[] shadowPoint(double x, double y) {
	
	double[] xx  = new double[]{x,y};
	double[] pnt = new double[3];
	
	reverseTransform(xx, pnt);
	
	double slat = pnt[2];
	double clat = 1-slat*slat;
	if (clat < 0) {
	    clat = 0;
	}
	
	clat = sqrt(clat);
	double lon = atan2(pnt[1],pnt[0]);
	if (lon <= 0) {
	    lon += 2*PI;
	} else {
	    lon -= 2*PI;
	}
	
        double gamma = (1+clat*cos(lon/2));
	if (gamma > 0) {
	    gamma = sqrt(2/gamma);
	} else {
	    gamma = 0;
	}
	double[] res = new double[] {2*gamma*clat*sin(lon/2), gamma*slat};
	
	// This can sometimes happen if we are on the edge.
	if ((x > 0 && res[0] > 0) || (x < 0 && res[0] < 0) ) {
	    res[0] = -res[0];
	}
	
	return res;
    }
    
    /** Get the straddle regions from a given set. 
     *  We don't truncate the at the boundaries of the ellipse.
     *  We can probably calulate this (i.e., by looking to see if
     *  each component crosses the ellipse boundary) but invalid pixels
     *  should be handled by the validPosition check.  It seems unlikely
     *  that anyone is handling this boundary perfectly...
     */
    public double[][][] straddleComponents(double[][] input) {
	return myStraddler.straddleComponents(input);
    }
  
    
    public class AitDeproj extends Deprojecter {
	
	/** Name of component */
	public String getName() {
	    return "AitDeproj";
	}
	
	/** Description of component */
	public String getDescription() {
	    return "Deproject from a Hammer-Aitoff ellipse back to the sphere.";
	}
	
	/** Get the inverse transformation */
	public Projecter inverse() {
	    return Ait.this;
	}
    
        /** Is this the inverse of another transformation? */
        public boolean isInverse(Transformer trans) {
	    return trans.getName().equals("Ait");
        }
	
        /** Deproject a point from the plane to the sphere.
         *  @param plane  The input position in the projection  plane.
         *  @param sphere A preallocated 3-vector to hold the unit vector result.
         */
        public final void transform(double[] plane, double[] sphere) {
	    if (!validPosition(plane)) {
	        sphere[0] = Double.NaN;
	        sphere[1] = Double.NaN;
	        sphere[2] = Double.NaN;
	    } else {
	        reverseTransform(plane, sphere);
	    }
	}
    }
    
    public static void main(String[] args) {
	Ait p = new Ait();
	
	double ra = Double.parseDouble(args[0]);
	double dec = Double.parseDouble(args[1]);
	
	double rra = toRadians(ra);
	double rdec = toRadians(dec);
	double[] unit = skyview.geometry.Util.unit(rra, dec);
	double[] pnt = new double[2];
	p.transform(unit,pnt);
	
	double[] shadow = p.shadowPoint(pnt[0], pnt[1]);
	System.err.printf("Decimal degrees: %10.5f %10.5f\n", ra, dec);
	System.err.printf("Radians:         %10.5f %10.5f\n", rra, rdec);
	System.err.printf("Unit vector:     %10.5f %10.5f %10.5f\n",unit[0],unit[1],unit[2]);
	System.err.printf("Map position:    %10.5f %10.5f\n", pnt[0], pnt[1]);
	System.err.printf("Shadow position: %10.5f %10.5f\n", shadow[0], shadow[1]);
    }
}
