package skyview.geometry.projecter;



/** This class implements the Zenithal Equal Area (ZEA)
 *  projection.  Note that the tangent point
 *  is assumed to be at the north pole.
 *  This class assumes preallocated arrays for
 *  maximum efficiency.
 */

import skyview.geometry.Projecter;
import skyview.geometry.Deprojecter;
import skyview.geometry.Transformer;
import static org.apache.commons.math3.util.FastMath.*;

public class Zea extends Projecter {

    /** Get a name for the component */
    public String getName() {
	return "Zea";
    }
    
    /** Get a description for the component */
    public String getDescription() {
	return "Zenithal Equal Area projecter";
    }
    
    /** Get this inverse of the transformation */
    public Deprojecter inverse() {
	return new Zea.ZeaDeproj();
    }

    /** Is this an inverse of some other transformation? */
    public boolean isInverse(Transformer t) {
	return t.getName().equals("ZeaDeproj");
    }
    
    /** Project a point from the sphere to the plane.
     *  @param sphere a double[3] unit vector
     *  @param plane  a double[2] preallocated vector.
     */
    public final void transform(double[] sphere, double[] plane) {
	
	if (Double.isNaN(sphere[2])) {
	    plane[0] = Double.NaN;
	    plane[1] = Double.NaN;
	} else {
	    double num = 2*(1-sphere[2]);
	    if (num < 0) {
		num = 0;
	    }
	    double denom = sphere[0]*sphere[0] + sphere[1]*sphere[1];
	    if (denom == 0) {
		plane[0] = 0;
		plane[1] = 0;
	    } else {
	        double ratio = sqrt(num) /
	                   sqrt(sphere[0]*sphere[0] + sphere[1]*sphere[1]);
	        plane[0] = ratio * sphere[0];
	        plane[1] = ratio * sphere[1];
	    }
	    
	}
    }
    
    public boolean validPosition(double[] plane) {
	return super.validPosition(plane) &&
	       plane[0]*plane[0] + plane[1]*plane[1] <= 4;
    }
    
    public class ZeaDeproj extends Deprojecter {
	
	/** Get the name of the component */
	public String getName() {
	    return "ZeaDeproj";
	}
	
	/** Get the description of the compontent */
	public String getDescription() {
	    return "Zenithal equal area deprojecter";
	}
	
	/** Get the inverse transformation */
	public Projecter inverse() {
	    return Zea.this;
	}
	 
        /** Is this an inverse of some other transformation? */
        public boolean isInverse(Transformer t) {
	    return t.getName().equals("Zea");
        }
	
	
        /** Deproject a point from the plane to the sphere.
         *  @param plane a double[2] vector in the tangent plane.
         *  @param spehre a preallocated double[3] vector.
         */
        public final void transform(double[] plane, double[] sphere) {
	
	
	    if (!validPosition(plane)) {
	        sphere[0] = Double.NaN;
	        sphere[1] = Double.NaN;
	        sphere[2] = Double.NaN;
	    
	    } else {
	        double r = sqrt(plane[0]*plane[0] + plane[1]*plane[1]);
	        sphere[2]  = 1 - r*r/2;
	        double ratio = (1-sphere[2]*sphere[2]);
		if (ratio > 0) {
		    ratio = sqrt(ratio)/r;
		} else {
		    ratio = 0;
		}
	        sphere[0] = ratio * plane[0];
	        sphere[1] = ratio * plane[1];
	    }
	}
    }
}
