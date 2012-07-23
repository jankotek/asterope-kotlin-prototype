package skyview.geometry;

import skyview.survey.Image;
import skyview.geometry.Transformer;

public abstract class Sampler implements skyview.Component, Cloneable {
    
    /** The Sampler class is the superset of classes
     *  that extract data at a given point given the data at other
     *  points.  Note that unlike the standard literature
     *  on sampling we deal with images whose boundaries are
     *  at integers, i.e., the first pixel includes the values 0 to 1.
     *  Thus when we resample it, we treat the value as having been
     *  measured at the mean value of the pixel, i.e., at 0.5.  For
     *  many samplers this means that we subtract 0.5 from the coordinates
     *  before we being sampling. This is in addition to the 0.5 offset
     *  between the pixel coordinates used here and the pixels defined in
     *  FITS files.  I.e., the center of the first pixel is 1 in FITS coordinates,
     *  0.5 in the coordinates used in most of the SkyView Java routines, and is
     *  most conveniently treated as 0 in resampling routines.
     *  <p>
     *  Many resamplers allow for optional parameters.  E.g., the Lanczos resampling
     *  can use any number of pixels.  To accommodate resamplers parametrized by
     *  a single integer value, e.g., the Lanczos and Spline resamplers, a
     *  setOrder method is provided.  For fixed resamplers, e.g., the Nearest Neighbor (NN),
     *  Linear Interpolation (LI) or Clipping (Clip) resamplers, this function has
     *  not effect.
     *  <p>
     *  All fields in this class are given protected access to allow direct
     *  use in sub-classes since this class often determines the
     *  total throughput of an operation.
     */
    
    /** The input image.  It should have a size inHeight*inWidth*inDepth */
    protected Image inImage;
    protected int inHeight, inWidth, inDepth;
    
    /** This gives the minX,maxX, minY,maxY pixel values for the current
     *  output image.  We can use this to limit the region of the input
     *  image we are interested in.
     */
    protected int[] bounds;
    
    /** The output image.  It should have a size outHeight*outWidth*outDepth */
    protected Image outImage;
    
    protected int outHeight, outWidth, outDepth;
    
    /** The transformation from the output image to the input image. */
    protected Transformer trans;
 
    /** Find the value in the input data to put in the output data.
     *  The output array defined in a previous setOutput call
     *  is updated.
     * @param index     The index into the output array.
     */
    public abstract void sample(int index);
    
    /** Allow someone to get a copy of this sampler for use on another
     *  image.
     */
    public Object clone() {
	try {
	    return super.clone();
	} catch (Exception e) {
	    return null;
	}
    }
    
    /** Set the input image for the sampling
      */
    public void setInput(Image inImage) {
	this.inImage  = inImage;
	this.inWidth  = inImage.getWidth();
	this.inHeight = inImage.getHeight();
	this.inDepth  = inImage.getDepth();
    }
    
    /** Set the bounds of the output image that may be asked for. */
    public void setBounds(int[] bounds) {
	this.bounds = bounds;
    }
	
        
    /** Set the output image for the sampling
      */
    public void setOutput(Image outImage) {
	this.outImage  = outImage;
	this.outWidth  = outImage.getWidth();
	this.outHeight = outImage.getHeight();
	this.outDepth  = outImage.getDepth();
    }
    
    
    /** Set the transformation information.
     * @param transform  The transformer object.
     * @param pixels     The pixel array of the data.
     */
    public void setTransform(Transformer transform) {
	this.trans = transform;
    }
    
    /** Factory for creating samplers */
    public static Sampler factory(String type) {
	
	String base = null;
	String numb = null;
        if (type == null || type.length() == 0 || type.toLowerCase().equals("default") ) {
            type = "NN";
        }

	int j;
	for (j=type.length()-1; j>1; j -= 1) {
	    if (type.charAt(j) < '0'  || type.charAt(j)  > '9') {
		break;
	    }
	}
	
	if (j < type.length()-1) {
	    // There's a number at the end..
	    base = type.substring(0,j+1);
	    numb = type.substring(j+1);
	} else {
	    base = type;
	}
	
	String classString = "skyview.geometry.sampler."+base+"Sampler";
	Sampler samp = (Sampler) skyview.util.Utilities.newInstance(base,"skyview.geometry.sampler");
	if (samp == null) {
	    throw new Error("Invalid sampler name:"+base);
	}
	
	if (numb != null) {
	    int param = Integer.parseInt(numb);
	    samp.setOrder(param);
	}
	return samp;
    }
    
    /** Set the order of the classifier.  By default this
     *  does nothing but it defines the order of the sampling
     *  for some samplers.
     */
    public void setOrder(int order) {
    }
}
