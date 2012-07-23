package skyview.process.expfinder;

import nom.tam.fits.Header;

import skyview.survey.Image;
import skyview.survey.FitsImage;

import skyview.executive.Settings;

import skyview.geometry.Sampler;

/** Find the exposure in the FITS file.
 */
public class FitsKeyword implements skyview.process.ExposureFinder {
    
    String  expKey = "EXPOSURE";
    double exposure = -1;
    
    public FitsKeyword() {
	if (Settings.has("ExposureKeyword")) {
	    expKey = Settings.get("ExposureKeyword");
	}
    }
    
    public void setImage(Image input, Image output, Sampler samp) {
	try {
	    exposure = ((FitsImage) input).getHeader().getDoubleValue(expKey, -1);
	} catch (Exception e) {
	    System.err.println("  Error trying to get exposure from image:"+e);
	}
    }
    
    public double getExposure(int pixel) {
	return exposure;
    }
}
