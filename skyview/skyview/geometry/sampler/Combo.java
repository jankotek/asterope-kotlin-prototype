package skyview.geometry.sampler;

import skyview.survey.Image;
import skyview.geometry.Transformer;
import skyview.geometry.Sampler;
import skyview.executive.Settings;

/** This class uses two samplers.  The default sampler is
 *  used but whenever a NaN is returned, the backup sampler is tried.
 *  Normally the default sampler will a high order sampler
 *  and the backup sampler will be simpler.
 */
public class Combo extends Sampler {
    
    protected Sampler primary;
    protected Sampler backup;
    protected String  combo;
    
    public Combo() {
	combo = Settings.get("ComboSamplers");
	String[] samplers = combo.split(",");
	primary = Sampler.factory(samplers[0]);
	backup  = Sampler.factory(samplers[1]);
    }

    
    public String getName() {
	return "ComboSampler";
    }
    
    public String getDescription() {
	return "A combination sampler with a primary and backup:"+combo;
    }

    
    
    /** Use the primary unless we get a NaN */
    public void sample(int index) {
	primary.sample(index);
	if (Double.isNaN(outImage.getData(index))) {
	    backup.sample(index);
	}
    }
    
    /** Set the input image for the sampling
      */
    public void setInput(Image inImage) {
	primary.setInput(inImage);
	backup.setInput(inImage);
    }
    
    /** Set the bounds of the output image that may be asked for. */
    public void setBounds(int[] bounds) {
	primary.setBounds(bounds);
	backup.setBounds(bounds);
    }
	
        
    /** Set the output image for the sampling
      */
    public void setOutput(Image outImage) {
	this.outImage = outImage;
	primary.setOutput(outImage);
	backup.setOutput(outImage);
    }
    
    /** Set the transformation information.
     * @param transform  The transformer object.
     * @param pixels     The pixel array of the data.
     */
    public void setTransform(Transformer transform) {
	primary.setTransform(transform);
	backup.setTransform(transform);
    }
    
}
