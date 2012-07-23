package skyview.geometry.projecter;

/** This class implements the stereoscopic (STG)
 *  projection.  Note that the tangent point
 *  is assumed to be at the north pole.
 *  The STG projection projects circles into circles.
 */

import skyview.geometry.Projecter;
import skyview.geometry.Deprojecter;
import skyview.geometry.Transformer;

public class Stg extends Projecter {
    
    /** Get the name of the compontent */
    public String getName() {
	return "Stg";
    }
    /** Get a description of the component */
    public String getDescription() {
	return "Project from antipodes to a tanget plane touching the sphere";
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
	    double fac = 2/(1+sphere[2]);
	    plane[0] = fac*sphere[0];
	    plane[1] = fac*sphere[1];
	}
    }
    
    /** The entire projection plane is valid */
    public boolean allValid() {
	return true;
    }
    
    /** Get the inverse transformation */
    public Deprojecter inverse() {
	return new Stg.StgDeproj();
    }
    
    /** Is this an inverse of some other transformation? */
    public boolean isInverse(Transformer t) {
	return t.getName().equals("StgDeproj");
    }
    public class StgDeproj extends Deprojecter {
	
	/** Get the name of the component */
	public String getName() {
	    return "StgDeproj";
	}
	
	/** Get a description of the component */
	public String getDescription() {
	    return "Transform from the stereoscopic tangent plane to the sphere";
	}
	
	/** Get the inverse transformation */
	public Projecter inverse() {
	    return Stg.this;
	}
        /** Is this an inverse of some other transformation? */
        public boolean isInverse(Transformer t) {
	    return t.getName().equals("Stg");
        }
    
        /** Deproject a point from the plane to the sphere.
         *  @param plane a double[2] vector in the tangent plane.
         *  @param sphere a preallocated double[3] vector.
         */
        public final void  transform(double[] plane, double[] sphere) {
	
	    if (!validPosition(plane)) {
	        sphere[0] = Double.NaN;
	        sphere[1] = Double.NaN;
	        sphere[2] = Double.NaN;
	    
	    } else {
		
		double x = plane[0];
		double y = plane[1];
		
		double r     = Math.sqrt(x*x + y*y);
		double theta = 2*Math.atan2(r, 2);
		double z     = Math.cos(theta);
		
	        sphere[2] = z;
		if (Math.abs(z) != 1) {
		    sphere[0] = plane[0]*(1+z)/2;
		    sphere[1] = plane[1]*(1+z)/2;
		} else {
		    sphere[0] = 0;
		    sphere[1] = 0;
		}
	    }
        }
    }
}
