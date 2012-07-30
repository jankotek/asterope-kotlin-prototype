package skyview.process.expfinder;

import nom.tam.fits.Header;

import skyview.executive.Key;
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
	if (Settings.has(Key.ExposureKeyword)) {
	    expKey = Settings.get(Key.ExposureKeyword);
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
