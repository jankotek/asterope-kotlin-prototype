package skyview.process;

import skyview.survey.Image;

import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;

import nom.tam.fits.Header;


/** 
 * Invoke the standard deedger.
 * Currently this is a MedianEdgeDeedger
 */

public class Deedger implements Processor {
    
    
    /** The object that will do all the work.
     *  This should be updated if a better deedger is available.
     */
    private Processor stdDeedger = new skyview.process.deedger.BoundaryMedian();
    
    public String getName() {
	return stdDeedger.getName();
    }
    
    public String getDescription() {
	return stdDeedger.getDescription();
    }
    /* Process the image.
     *  @param inputs  The array of input images.
     *  @param output  The output image
     *  @param source  The index array
     *  @param samp    The spatial sampler (not used)
     *  @param dsamp   The energy sampler (not used)
     */
    
    public void process(Image[] inputs, Image output, int[] source, 
			Sampler samp, DepthSampler dsamp) {
	
	stdDeedger.process(inputs, output, source, samp, dsamp);
    }
    
    /** Update a FITS header with the processing done here. */
    public void updateHeader(Header h) {
	stdDeedger.updateHeader(h);
    }
}
