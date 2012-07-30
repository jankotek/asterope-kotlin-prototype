package skyview.process.expfinder;

import nom.tam.fits.Header;

import skyview.executive.Key;
import skyview.survey.Image;
import skyview.survey.FitsImage;
import skyview.executive.Settings;
import skyview.geometry.Sampler;
import skyview.geometry.Transformer;
import skyview.geometry.Converter;

/** Find the exposure in the FITS file.
 */
public class ExposureFile implements skyview.process.ExposureFinder {
    
    public String expMatchStr = "^(.*)(\\.fits(.gz)?)$";
    public String expOutStr   = "$1.exp.$2";
    
    private Image       input;
    private Image       expOut;
    private FitsImage   expImage;
    private Converter   trans;
    private int         width;
    private double[]    coords;
    private Sampler     newSamp;
    
    
    public ExposureFile() {
	if (Settings.has(Key.ExposureFileMatch)) {
	    expMatchStr = Settings.get(Key.ExposureFileMatch);
	}
	if (Settings.has(Key.ExposureFileGen)) {
	    expOutStr = Settings.get(Key.ExposureFileGen);
	}
    }
    
    public void setImage(Image input, Image output, Sampler samp) {
	try {
	    String inpName = input.getName();
	    String newFile = inpName.replaceAll(expMatchStr, expOutStr);
	    expImage = new FitsImage(newFile);
	    trans    = new Converter();
	    try {
	        trans.add(output.getTransformer().inverse());
	        trans.add(expImage.getTransformer());
	    } catch (Exception e) {
		System.err.println("Unable to set exposure image transformation for "+newFile);
		throw new Error("Invalid exposure file");
	    }
	    
	    double[] expData = new double[output.getWidth()*output.getHeight()];
	    
	    expOut = new Image(expData, output.getWCS(), output.getWidth(), output.getHeight());
	    
	    newSamp  = (Sampler) samp.clone();
	    newSamp.setInput(expImage);
	    newSamp.setOutput(expOut);
	    
	    newSamp.setTransform(trans);
	    
	} catch (Exception e) {
	    System.err.println("  Error trying to get exposure from image:"+e);
	}
    }
    
    public double getExposure(int pixel) {
	newSamp.sample(pixel);
	return expOut.getData(pixel);
    }
}
