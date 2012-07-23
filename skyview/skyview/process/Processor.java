package skyview.process;

/** The Processor interface is implemented by objects which
 *  wish to do processing of the input or output images.
 */

import skyview.survey.Image;
import skyview.Component;
import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;

public interface Processor extends Component {
    
    /** Perform the processing task associated with this object.
     *  
     *  @param inputs The array of input survey images.
     *  @param output The output user image.  A null output may signal
     *  that an error has taken place.  If this processor wishes
     *  to do something in response to the error, it may look at the ErrorMsg setting.
     *  @param source An array giving the source image for each output pixel.
     *  @param samp   The sampler object used to do spatial sampling of the input images.
     *  @param dsamp  The sampler in the energy dimension (often null).
     */
    public abstract void process(Image[] inputs, Image output, int[] source, 
				 Sampler samp, DepthSampler dsamp);
    
    /** Update the FITS header to indicate what processing was done.
     */
    public abstract void updateHeader(nom.tam.fits.Header header);
}
