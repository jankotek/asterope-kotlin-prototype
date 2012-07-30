package skyview.process.imagefinder;

import skyview.executive.Key;
import skyview.survey.Image;
import skyview.Component;
import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;

import skyview.process.ImageFinder;

import skyview.executive.Settings;

/** This class makes sure that all of the candidate
 *  images are actually present and then if any candidate
 *  was not already in the cache is redoes the
 *  image finding using the standard image finder.
 *  This accommodate cases where the proxy geometry
 *  is not a good approximation to the real geometry.
 *  It still requires that the image selection be done
 *  reasonably well.
 */
public class Prefetcher implements skyview.process.Processor {
    
    public String getName() {
	return "ImagePrefetcher";
    }
    
    public String getDescription() {
	return "Ensures image finding using downloaded images";
    }
    
    public void process(Image[] inputs, Image output, int[] source, 
				 Sampler samp, DepthSampler dsamp) {
	if (inputs == null  || source == null) {
	    return;
	}
	
	// What input images do we need?
	boolean[] need = new boolean[inputs.length];
	for (int i=0; i<source.length; i += 1) {
	    if (source[i] >= 0) {
		need[source[i]] = true;
	    }
	}
	
	// Check if any of them need to be downloaded
	// and if so do it.
	boolean redo = false;
	for (int i=0; i<need.length; i += 1) {
	    if (need[i]  && !inputs[i].valid()) {
		System.err.println("  Fetching candidate:"+i);
		inputs[i].validate();
		redo = true;
	    }
	}
	if (redo) {
	    System.err.println("  Recalculating pixel source images.");
	    ImageFinder imFin = ImageFinder.factory(Settings.get(Key.ImageFinder));
	    imFin.setStrict(Settings.has(Key.StrictGeometry));
	    int[] newMatch = imFin.findImages(inputs, output);
	    
	    System.arraycopy(newMatch, 0, source, 0, source.length);
	}
    }
	
    
    /** Update the FITS header to indicate what processing was done.
     */
    public void updateHeader(nom.tam.fits.Header header) {
	// Do nothing.  This class doesn't really do any processing.
    }
}
