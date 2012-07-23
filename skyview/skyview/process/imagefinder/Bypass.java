package skyview.process.imagefinder;

import skyview.survey.Image;
import skyview.geometry.Transformer;

/** This class is a simple image finder which returns all images
 *  and a 0 length array.
 */
public class Bypass extends skyview.process.ImageFinder {
    
    public int[] findImages(Image[] input, Image output) {
	
	if (input == null || input.length == 0) {
	    return null;
	}
	System.err.println("  ImageFinder bypassed:"+input.length+" images");
	return new int[0];
    }
}
