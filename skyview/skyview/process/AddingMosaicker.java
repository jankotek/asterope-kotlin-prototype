package skyview.process;

import skyview.survey.Image;
import skyview.geometry.Sampler;
import skyview.geometry.sampler.Clip;

import skyview.geometry.DepthSampler;
import skyview.geometry.Converter;

import skyview.geometry.TransformationException;
import skyview.geometry.Transformer;
import skyview.geometry.Sampler;

import skyview.executive.Settings;

import nom.tam.fits.Header;

/** A mosaicker is an object which creates
 *  a new image from a set of input images.
 *  This mosaicker takes the input images and adds all the
 *  overlaps together to create the output.  It expects
 *  to be able to get an ExposureFinder object which for
 *  each input image returns the exposure for a given pixel.
 *  After the contributions from all of the constituent
 *  images have been found, the summed image is divided by the
 *  exposure per pixel.
 * 
 *  Substantially modified May 5, 2010 in response to issues
 *  brought up by G. Belanger.  Rather than have special code
 *  to handle the overlap of each image with the output we
 *  use the standard ImageFinder.  This works in global geometries
 *  where the prior use of clipping failed.  The ImageFinder handles
 *  the edge and radius clipping functionality that we had had to
 *  duplicate here as well.
 */
public class AddingMosaicker implements Processor {
 
    private java.util.ArrayList<String>  usedImageNames = new java.util.ArrayList<String>();
    private java.util.ArrayList<Integer> usedPixelCount = new java.util.ArrayList<Integer>();
    
    private int pixCount = 0;
    
    private int oWidth;
    private int oHeight;
    
    private int iWidth;
    private int iHeight;
    
    /** Get the name of this component */
    public String getName() {
	return "AddingMosaicker";
    }
    
    /** Get a description of this component */
    public String getDescription() {
	return "Create a new image by adding all overlapping images.";
    }
    
    /** Get the exposure given an image */
    private ExposureFinder  expFinder;
    
    public AddingMosaicker() {
	
	String finder = Settings.get("ExposureFinder");
	try {
	    if (finder != null ) {
		expFinder = (ExposureFinder) skyview.util.Utilities.newInstance(finder, "skyview.process.expfinder");
	    }
	} catch (Exception e) {
	    System.err.println("  Error instantiating exposure finder "+finder+": "+e);
	}
	expFinder = new skyview.process.expfinder.Null();
	
    }
    
    /** Populate the pixel values of the output mosaic.  Note that
     *  the output image is assumed to be created prior
     *  to the mosaic call since its WCS will have been
     *  used extensively.
     *  @param input  An array of input images.
     *  @param output The image whose data is to be filled.
     *  @param osource A dummy input array.
     *  @param samp   The sampler to be used to sample the input images.
     *  @param dsamp  The sampler (if any) in the energy dimension.
     */
    public void process(Image[] input, Image output, int[] osource, 
		        Sampler samp, DepthSampler dSampler)  {
	
	
	int width   = output.getWidth();
	oWidth      = width;
	int height  = output.getHeight();
	oHeight     = height;
	int depth   = output.getDepth();
	
	samp.setOutput(output);
	output.setAccumulate(true);
	
	if (depth == 0) {
	    depth = 1;
	}
	
	double[] exposure = new double[width*height];
	
	int imgCnt = 0;
	double[] data = output.getDataArray();
	for (int i=0; i<input.length; i += 1) {
	    if (input[i] != null) {
		
		imgCnt  += 1;
		pixCount = 0;
		
		processImage(input[i], output, exposure, samp, dSampler);
		
		if (pixCount > 0) {
		    usedImageNames.add(input[i].getName());
		    usedPixelCount.add(pixCount);
		    System.err.println("  Image "+imgCnt+" has overlap on "+pixCount+" pixels.");
		}
		
		// This allows the space to be garbage collected.
		input[i].clearData();
	    }
	}
	int offset = 0;
	
	/*
	 * If the data is not intensities, then
	 * we may want to add things together without
	 * worrying about normalizing by exposure.  E.g.,
	 * we may be adding exposures! [Thanks to
	 * G. Belanger for pointing this out.]
	 */
	if (!Settings.has("nonormalize")) {
	    // Normalize by exposure.
	    for (int i=0; i<exposure.length; i += 1) {
	        for (int k=0; k<depth; k += 1) {
		    if (exposure[i] > 0) {
		        data[offset] /= exposure[i];
		    } else {
		        data[i] = -1;
		    }
		    offset += 1;
		}
	    }
	}
    }
    
    /** Process one inpuyt image */
    private void processImage(Image input, Image output, double[] exposure, Sampler samp, DepthSampler dsamp) {
	
	// Used to use clipping to find overlap, but that
	// doesn't work with global geometries.  So we just use
	// a standard image finder and send it one image at
	// a time.
	Converter cv        = new Converter();
	Transformer clipTrans = null;
	
	try {
	    cv.add(output.getTransformer().inverse());
	    cv.add(input.getTransformer());
	    samp.setTransform(cv);
	} catch (Exception e) {
	    System.err.println("  Transformation exception for image.");
	    return;
	}
	
	ImageFinder fi = ImageFinder.factory(null);
	
	int[] overlap = fi.findImages(new Image[]{input}, output);
	
	int cnt = 0;
        if (overlap != null) {
	    for (int i=0; i<overlap.length;i += 1) {
	        if (overlap[i] >= 0) {
		    cnt += 1;
		    break;    // We don't need the total here so
		          // finding a single pixel is enough
	        }
	    }
	}
	if (cnt == 0) {
	    // There is no overlap so finish this image..
	    return;
	} else {
	    input.validate();
	    if (dsamp != null) {
		input = dsamp.sample(input);
	    }
	    
	    expFinder.setImage(input, output, samp);
	    
	    samp.setInput(input);
	    processOverlap(input, output, samp, exposure, overlap);
	}
    }
   
    /** Process the region of overlap */
    private void processOverlap(Image input, Image output,
				Sampler samp, double[] exposure, int[] overlap) {
		
	int w = output.getWidth();
	int h = output.getHeight();
	for (int j=0; j<h; j += 1) {
	    for (int i=0; i<w; i += 1) {
		int pixel = i + w*j;
	        if (overlap[pixel] >= 0) {
		    processPixel(samp, exposure, i, j);
		}
	    }
	}
    }
    
    /** Process a single pixel */
    private void processPixel(Sampler samp, double[] exposure, int i, int j) {
	int pixel        = i + j*oWidth;
	samp.sample(pixel);
	exposure[pixel] += expFinder.getExposure(pixel);
	pixCount        += 1;
    }
    
    /** Describe the mosaicking of the image . */
    public void updateHeader(Header h) {
	try {
	    h.insertHistory("");
	    h.insertHistory("Image mosaicking using skyview.geometry.AddingMosaicker");
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
	    for (int i=0; i<usedImageNames.size(); i += 1) {
	        h.insertHistory("  Used "+usedPixelCount.get(i)+" pixels from "+usedImageNames.get(i));
	    }
	    h.insertHistory("");
	} catch (nom.tam.fits.FitsException e) {
	    System.err.println("  Error updating FITS header:\n   "+e);
	    // Just continue
	}
    }
}
