package skyview.geometry.projecter;

/** This class implements the tangent (gnomonic)
 *  projection.  Note that the tangent point
 *  is assumed to be at the north pole.
 *  This class assumes preallocated arrays for
 *  maximum efficiency.
 */

import skyview.geometry.Projecter;
import skyview.geometry.Deprojecter;
import skyview.geometry.Transformer;
import static org.apache.commons.math3.util.FastMath.*;

public class Tan extends Projecter {
    
    /** Get the name of the compontent */
    public String getName() {
	return "Tan";
    }
    /** Get a description of the component */
    public String getDescription() {
	return "Project to a tangent plane touching the sphere";
    }
    
    /** The entire projection plane is valid */
    public boolean allValid() {
	return true;
    }
	       
    /** Project a point from the sphere to the plane.
     *  @param sphere a double[3] unit vector
     *  @param plane  a double[2] preallocated vector.
     */
    public final void transform(double[] sphere, double[] plane) {
	
	if (Double.isNaN(sphere[2]) || sphere[2] < 0) {
	    plane[0] = Double.NaN;
	    plane[1] = Double.NaN;
	} else {
	    double fac = 1/sphere[2];
	    plane[0] = fac*sphere[0];
	    plane[1] = fac*sphere[1];
	}
    }
    
    /** Get the inverse transformation */
    public Deprojecter inverse() {
	return new Tan.TanDeproj();
    }
    
    /** Is this an inverse of some other transformation? */
    public boolean isInverse(Transformer t) {
	return t.getName().equals("TanDeproj");
    }


    /** Get the Tissot parameters. */
    public double[] tissot (double x, double y) {
        double r = sqrt(1 + x*x + y*y);
        return new double[]{r*r, r, atan2(x,y)};
    }

    public class TanDeproj extends Deprojecter {
	
	/** Get the name of the component */
	public String getName() {
	    return "TanDeproj";
	}
	
	/** Get a description of the component */
	public String getDescription() {
	    return "Transform from the tangent plane to the sphere";
	}
	
	/** Get the inverse transformation */
	public Projecter inverse() {
	    return Tan.this;
	}
        /** Is this an inverse of some other transformation? */
        public boolean isInverse(Transformer t) {
	    return t.getName().equals("Tan");
        }
    
        /** Deproject a point from the plane to the sphere.
         *  @param plane a double[2] vector in the tangent plane.
         *  @param spehre a preallocated double[3] vector.
         */
        public final void  transform(double[] plane, double[] sphere) {
	
	    if (Double.isNaN(plane[0])) {
	        sphere[0] = Double.NaN;
	        sphere[1] = Double.NaN;
	        sphere[2] = Double.NaN;
	    
	    } else {
	    
	        double factor = 1 / sqrt(plane[0]*plane[0] + plane[1]*plane[1]+1);
	        sphere[0] = factor*plane[0];
	        sphere[1] = factor*plane[1];
	        sphere[2] = factor;
	    }
        }
    }
}
