package skyview.geometry;


/** This class defines a non-linear distortion in the image plane.
    Normally the forward distortion converts from a fiducial
    projection plane to some distorted coordinates.  The reverse
    distortion transforms from the distorted coordinates back
    to the fiducial coordinates.
  */
public abstract class SphereDistorter extends Transformer implements skyview.Component {
    
    /** A name for this object */
    public String getName() {
	return "Generic SphereDistorter";
    } 
    
    /** What does this object do? */
    public String getDescription() {
	return "Placeholder for distortions in celestial sphere";
    }
    
    public abstract SphereDistorter inverse();
    
    /** What is the output dimensionality of a Distorter? */
    protected int getOutputDimension() {
	return 3;
    }
    
    /** What is the input dimensionality of a Distorter? */
    protected int getInputDimension() {
	return 3;
    }
    
}
