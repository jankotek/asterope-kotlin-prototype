package skyview.process;

import skyview.survey.Image;
import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;
import skyview.geometry.Converter;
import skyview.geometry.TransformationException;

import nom.tam.fits.Header;

/** A mosaicker is an object which creates
 *  a new image from a set of input images.
 *  The IDMosaic does not read the images: it returns
 *  an image giving the source tile ID for each pixel.
 */
public class IDMosaic extends Mosaicker  {
 
    private String[] usedImageNames;
    
    private int[] counts;
    private int nocoverage;
    private int nonphysical;
    
    /** Get the name of this component */
    public String getName() {
	return "IDMosaic";
    }
    
    /** Get a description of this component */
    public String getDescription() {
	return "Say which tile each pixel would be sampled from.";
    }
    
    /** 
     *  Return the tiles that would be used as the mosaicked image.
     *  @param input  An array of input images.
     *  @param output The image whose data is to be filled.
     *  @param osource An integer array giving the source image to be used
     *                for the output pixels.  Note that depending upon
     *                the mosaicker used, source may be dimensioned as either
     *                nx*ny or (nx+1)*(ny+1).
     *  @param pixels The pixel locations to be used in the mosaicking.
     *  @param samp   The sampler to be used to sample the input images.
     *  @param dsamp  The sampler (if any) in the energy dimension.
     */
    public void process(Image[] input, Image output, int[] osource, 
		        Sampler samp, DepthSampler dSampler)  {
	
	int[] source = osource.clone();
	
	int width   = output.getWidth();
	int height  = output.getHeight();
	int depth   = output.getDepth();
	
	// Initialize counts array.
	counts = new int[input.length];
	usedImageNames = new String[input.length];
	
	boolean newData = false;
	
	// Loop over input images.
	double[] out = new double[2];
	double[] in  = new double[2];
	  
	boolean morePixels = true;
	
	samp.setOutput(output);
	  
        while(morePixels) {
	    
	    morePixels = false;
	    
	    double[]  data      = null;
	    int       currImg   = -4;
	      
	    for (int pix=0; pix<source.length; pix += 1) {
		int img = source[pix];
		
		if (img != -4) {
		    
		    if (currImg == -4) {
			currImg = img;
		    }
		    
		    if (img != currImg) {
		        morePixels = true;
			
	            } else {
			
			if (img >= 0) {
			    if (counts[img] == 0) {
				usedImageNames[img] = input[img].getName();
			    }
			    counts[img] += 1;
			} else if (img == -2) {
			    nocoverage += 1;
			} else if (img == -3) {
			    nonphysical += 1;
			}
		        currImg = img;
			for (int k=0; k<depth; k += 1) {
			    output.setData(pix+k*width*height, currImg);
			}
		        source[pix] = -4;
		    }
		}
	    }
	    currImg = -4;
	}
    }
        
    /** Describe the mosaicking of the image . */
    public void updateHeader(Header h) {
	
	try {
	    h.insertHistory("");
	    h.insertHistory("Image mosaicking using skyview.process.IDMosaic");
	    h.insertHistory("");
	    h.insertHistory("************************************");
	    h.insertHistory("** Images used                    **");
	    h.insertHistory("************************************");
	    h.insertHistory("");
	    
	    for (int i=0; i<counts.length; i += 1) {
		
		String res = i+" ("+counts[i]+"): ";
		if (counts[i] > 0) {
		    String name = usedImageNames[i];
		    int len = res.length();
		    // Make sure to get the end of the file name
		    // into the FITS header.
		    int used = len+8; // 8 for HISTORY keyword
		    if (used + name.length() > 80) {
			name = "..."+name.substring(used+name.length()-77);
		    }
	            h.insertHistory(res+name);
		}
		    
	    }
	    h.insertHistory("");
	    if (nocoverage > 0) {
		h.insertHistory("Uncovered pixels:"+nocoverage);
	    }
	    if (nonphysical > 0) {
		h.insertHistory("Pixels off projection:"+nonphysical);
	    }
	    h.insertHistory("");
	} catch (nom.tam.fits.FitsException e) {
	    System.err.println("Error updating FITS header:\n   "+e);
	    // Just continue
	}
    }
}
    
