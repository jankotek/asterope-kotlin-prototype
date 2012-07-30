package skyview.process;

import skyview.executive.Key;
import skyview.survey.Image;
import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;
import skyview.geometry.Converter;
import skyview.geometry.TransformationException;

import skyview.executive.Settings;

import nom.tam.fits.Header;

/** Used with QueryFinder to print out whether
 *  there is any coverage in the region of the image.
 */
public class InfoPrint implements Processor {
 
    public String getName() {
	return "InfoPrint";
    }
    
    /** Get a description of this component */
    public String getDescription() {
	return "Indicate if an image has coverage in the survey.";
    }
    
    /**
     */
    public void process(Image[] input, Image output, int[] osource, 
		        Sampler samp, DepthSampler dSampler)  {
	System.out.println(Settings.get(Key._currentSurvey)+":"+ skyview.process.imagefinder.Checker.getStatus());
    }
    
    /** Describe the mosaicking of the image . */
    public void updateHeader(Header h) {
    }
}
    
