package skyview.geometry.projecter;

import static java.lang.Math.sqrt;
import static java.lang.Math.abs;

import skyview.geometry.Projecter;
import skyview.geometry.Deprojecter;
import skyview.geometry.Transformer;

/** This class implements the Mercator projection
 */

public class Mer extends Projecter {
    
    /** The mercator projection has the same straddling properties as
     *  the Car projection.
     */
    private Straddle myStraddler = new MerStraddle(this);
    
    /** The name of the Component */
    public String getName() {
	return "Mer";
    }
    
    /** A description of the component */
    public String getDescription() {
	return "Project to an Mercator projection";
    }
    
    /** Get the associated deprojecter */
    public Deprojecter inverse() {
	return new Mer.MerDeproj();
    }
    
    /** Is this the inverse of another transformation? */
    public boolean isInverse(Transformer trans) {
	return trans.getName().equals("MerDeproj");
    }
    
    public final void transform(double[] sphere, double[] plane) {
	double lon;
	if (sphere[0] == 0) {
	    lon = 0;
	} else {
	    lon = Math.atan2(sphere[1], sphere[0]);
	}
	double lat = Math.asin(sphere[2]);
	
	double x = Math.log(Math.tan(Math.PI/4 + lat));
	plane[0] = lon;
	plane[1] = x;
    }
    
    public boolean validPosition(double[] plane) {
        return true;
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
	if (x <= 0) {
	    xx[0] = x+2*Math.PI;
	} else {
	    xx[0] = x-2*Math.PI;
	}
	return xx;
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
  
    
    public class MerDeproj extends Deprojecter {
	
	/** Name of component */
	public String getName() {
	    return "MerDeproj";
	}
	
	/** Description of component */
	public String getDescription() {
	    return "Deproject from a Mercator projection back to the sphere.";
	}
	
	/** Get the inverse transformation */
	public Projecter inverse() {
	    return Mer.this;
	}
    
        /** Is this the inverse of another transformation? */
        public boolean isInverse(Transformer trans) {
	    return trans.getName().equals("Mer");
        }
	
        /** Deproject a point from the plane to the sphere.
         *  @param plane  The input position in the projection  plane.
         *  @param sphere A preallocated 3-vector to hold the unit vector result.
         */
        public final void transform(double[] plane, double[] sphere) {
	    double lon = plane[0];
	    double lat = 2*Math.atan(Math.exp(plane[1])) - Math.PI/2;
	    sphere[0] = Math.cos(lon)*Math.cos(lat);
	    sphere[1] = Math.sin(lon)*Math.cos(lat);
	    sphere[2] = Math.sin(lat);
	}
    }
}
