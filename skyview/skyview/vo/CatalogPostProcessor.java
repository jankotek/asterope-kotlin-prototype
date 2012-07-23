package skyview.vo;
import skyview.survey.Image;
import skyview.Component;
import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;

/** The CatalogPostProcessor is called to finish processing
 *  for catalog requests.  It simply finds the last CatalogProcessor
 *  and invokes appropriate methods there.
 */
public class CatalogPostProcessor implements skyview.process.Processor {
    
    public String getName() {
	return "CatalogPostProcessor";
    }
    
    public String getDescription() {
	return "Post-mosaicking handling of catalog requests.";
    }
    
    /** Perform the processing task associated with this object.
     *  
     *  @param inputs The array of input survey images.
     *  @param output The output user image
     *  @param source An array giving the source image for each output pixel.
     *  @param samp   The sampler object used in processing.
     *  @param dsamp  The sampler in the energy dimension (often null).
     */
    public void process(Image[] inputs, Image output, int[] source, 
				 Sampler samp, DepthSampler dsamp) {
	
	// If there is no output just return.
	if  (output == null) {
	    return;
	}
	
	if (CatalogProcessor.getLastProcessor() != null) {
	    CatalogProcessor.getLastProcessor().postProcess(inputs, output, source, samp, dsamp);
	}
    }
    
    /** Update the FITS header to indicate what processing was done.
     */
    public void updateHeader(nom.tam.fits.Header header) {
	// This is handled by the CatalogProcessor directly
    }
}
