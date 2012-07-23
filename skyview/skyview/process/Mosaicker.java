package skyview.process;

import skyview.survey.Image;
import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;
import skyview.geometry.Converter;
import skyview.geometry.TransformationException;

import nom.tam.fits.Header;

/** A mosaicker is an object which creates
 *  a new image from a set of input images.
 */
public class Mosaicker implements Processor {
 
    private java.util.ArrayList<String> usedImageNames = new java.util.ArrayList<String>();
    
    /** Get the name of this component */
    public String getName() {
	return "Mosaicker";
    }
    
    /** Get a description of this component */
    public String getDescription() {
	return "Create a new image by putting together resampled pixels from set of old images";
    }
    
    /** Populate the pixel values of the output mosaic.  Note that
     *  the output image is assumed to be created prior
     *  to the mosaic call since its WCS will have been
     *  used extensively.
     *  @param input  An array of input images.
     *  @param output The image whose data is to be filled.
     *  @param osource An integer array giving the source image to be used
     *                for the output pixels.  Note that depending upon
     *                the mosaicker used, source may be dimensioned as either
     *                nx*ny or (nx+1)*(ny+1).
     *  @param samp   The sampler to be used to sample the input images.
     *  @param dsamp  The sampler (if any) in the energy dimension.
     */
    public void process(Image[] input, Image output, int[] osource, 
		        Sampler samp, DepthSampler dSampler)  {
	
	int[] source = osource.clone();
	int width   = output.getWidth();
	int height  = output.getHeight();
	
	boolean newData = false;
	
	// Loop over input images.
	double[] out = new double[2];
	double[] in  = new double[2];
	  
	boolean morePixels = true;
	
	samp.setOutput(output);
	int procCount = 0;
	  
        while(morePixels) {
	    
	    morePixels = false;
	    
	    Converter cv        = null;
	    double[]  data      = null;
	    int       inWidth   = 0;
	    int       inHeight  = 0;
	    int       inDepth   = 0;
	    int       badPixel  = 0;
	    int       goodPixel = 0;
	    int       currImg   = -1;
	      
	    boolean   validImage = true;
	    
	    for (int pix=0; pix<source.length; pix += 1) {
		int img = source[pix];
		
		if (img < 0) {
		// Do nothing...    
		    
		// If this is a pixel to be processed
		// and not the current one, then we need
		// to do another pass.
		} else if (img >= 0 && currImg >= 0 && img != currImg) {
		    morePixels = true;
	        } else {
		    currImg = img;
		    // Make sure this image is fully available.
		    
		    if (inWidth == 0) {
			try {
		            input[img].validate();
		            usedImageNames.add(input[currImg].getName());
			} catch (Throwable e) {
			    validImage = false;
			    System.err.println("  Error processing candidate image #"+img+": "+e.getMessage());
			    inWidth = -1;
			}
			if (validImage) {
			    try {
		                cv = new Converter();
	                        cv.add(output.getWCS().inverse());
	                        cv.add(input[img].getWCS());
			    } catch (TransformationException e) {
			        throw new Error("Tranformation error in mosaicker:"+e);
			    }
			    procCount += 1;
			    String suffix = ordinalSuffix(procCount);
			    System.err.println("  Processing "+procCount+suffix+" candidate image #"+img);
			    inDepth  = input[img].getDepth();
			    if (inDepth > 1 && dSampler != null) {
			        input[img] = dSampler.sample(input[img]);
			    }
			    inWidth  = input[img].getWidth();
			    inHeight = input[img].getHeight();
			
			    if (input[img].isTiled()) {
			        /** If our input image is tiled we send
			         *  some information about the boundaries of the output
			         *  pixels we're interested in to the sampler.
			         */
			        samp.setBounds(getBounds(pix, source, width));
			    }
			    samp.setTransform(cv);
			    samp.setInput(input[img]);
			}
		    }
		    if (validImage) {
		        samp.sample(pix);
		    }
		    source[pix] = -4;
		}
	    }
	    if (currImg >= 0) {
		// After each image, null its data out so it can be garbage collected.
		// Otherwise we keep all of the input images in memory.
		input[currImg].clearData();
	    }
	}
    }
    
    /**
     * Get the pixel bounds for the output image pixels we
     * are extracting from the current input image.  This may be used
     * when we have a large input image and only want to have to
     * consider using a piece of it.
     * 
     * @param pix The index of the first pixel from the source we are interested in.
     * @param source The source of each output pixel.
     * @param width The width (in pixels) of the output image.
     * @return An array giving bounding pixel indices.
     */
    private int[] getBounds(int pix, int[] source, int width) {
	
	int matching = source[pix];
	
	int x0 = pix%width;
	int xe = x0;
	int y0 = pix/width;
	int ye = y0;
	
	for (int xpix = pix+1; xpix < source.length; xpix += 1) {
	    
	    if (source[xpix] == matching) {
		int x = xpix%width;
		int y = xpix/width;
		
		if (x < x0) {
		    x0 = x;
		} else if (x > xe) {
		    xe = x;
		}
		
		if (y < y0) {
		    y0 = y;
		} else if (y > ye) {
		    ye = y;
		}
	    }
	}
	return new int[]{x0+y0*width,xe+y0*width,x0+ye*width, xe+ye*width};
    }
    
    /** Describe the mosaicking of the image . */
    public void updateHeader(Header h) {
	try {
	    h.insertHistory("");
	    h.insertHistory("Image mosaicking using skyview.geometry.Mosaicker");
	    h.insertHistory("");
	    String[] names =  usedImageNames.toArray(new String[0]);
	    if (names.length == 0) {
		h.insertComment("");
		h.insertComment("************************************");
		h.insertComment("** No valid pixels for mosaicking **");
		h.insertComment("************************************");
		h.insertComment("");
		h.addValue("SV_ERROR", "No valid pixels found in mosaicker", "");
	    }
	    for (String name: names) {
	        h.insertHistory("  Used image:"+name);
	    }
	    h.insertHistory("");
	} catch (nom.tam.fits.FitsException e) {
	    System.err.println("Error updating FITS header:\n   "+e);
	    // Just continue
	}
    }
    
    /** Find appropriate ordinal suffix for
     *  a given integer.
     */
    public static String ordinalSuffix(int n) {
	if (n < 0) {
	    n = -n;
	}
	int unit = n%10;
	int tens = n%100/10;
	
	// All teens end in th (e.g., thirteenth)
	if (unit == 1 && tens != 1) {
	    return "st";
	} else if (unit == 2 && tens != 1) {
	    return "nd";
	} else if (unit == 3 && tens != 1) {
	    return "rd";
	} else {
	    return "th";
	}
    }
}
    
