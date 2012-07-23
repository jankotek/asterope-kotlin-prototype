package skyview.geometry;


import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/** This class defines a non-linear distortion in the image plane.
    Normally the forward distortion converts from a fiducial
    projection plane to some distorted coordinates.  The reverse
    distortion transforms from the distorted coordinates back
    to the fiducial coordinates.
  */
public abstract class Distorter extends Transformer<Vector2D, Vector2D> implements skyview.Component {
    
    /** A name for this object */
    public String getName() {
	return "Generic Distorter";
    } 
    
    /** What does this object do? */
    public String getDescription() {
	return "Placeholder for distortions in projection plane";
    }
    
    public abstract Distorter inverse();
    
    /** What is the output dimensionality of a Distorter? */
    protected int getOutputDimension() {
	return 2;
    }
    
    /** What is the input dimensionality of a Distorter? */
    protected int getInputDimension() {
	return 2;
    }
    
}
