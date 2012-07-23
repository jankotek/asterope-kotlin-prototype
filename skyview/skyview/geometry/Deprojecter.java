package skyview.geometry;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/** This class deprojects a point from a projection plane
 *  onto the celestial sphere.
 */
public abstract class Deprojecter extends Transformer<Vector2D, Vector3D> {
    
    
    /** What is the output dimensionality of a deprojecter? */
    protected int getOutputDimension() {
	return 3;
    }
    
    /** What is the input dimensionality of a deprojecter? */
    protected int getInputDimension() {
	return 2;
    }
}
