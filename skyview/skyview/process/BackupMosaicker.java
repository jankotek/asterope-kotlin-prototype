package skyview.process;


import skyview.executive.Settings;

import skyview.survey.Image;
import skyview.survey.Survey;
import skyview.survey.SurveyFinder;

import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;
import skyview.geometry.Converter;
import skyview.geometry.Position;
import skyview.geometry.TransformationException;

import skyview.process.ImageFinder;

import nom.tam.fits.Header;

import java.util.ArrayList;

import static org.apache.commons.math3.util.FastMath.*;

/** This mosaicker is used to create a mosaic where
 *  if there are pixels unfilled by the primary survey
 *  one or more secondary surveys are invoked to fill them.
 */
public class BackupMosaicker extends Mosaicker {
 
    private java.util.ArrayList<String> usedImageNames = new java.util.ArrayList<String>();
    static final double BLANK = -1.e20;
    
    private Position pos;
    private double   maxSize;
    
    private ArrayList<Processor> mosaickers = new ArrayList<Processor>();
    
    
    /** Get the name of this component */
    public String getName() {
	return "BackupMosaicker";
    }
    
    /** Get a description of this component */
    public String getDescription() {
	return "Create a new image by putting together resampled pixels from multiple surveys";
    }
    
    public void process(Image[] input, Image output, int[] osource, 
		        Sampler samp, DepthSampler dSampler)  {

	int[] source = osource.clone();
	
	double[] data = output.getDataArray();
	
	for (int i=0; i<data.length; i += 1) {
	    data[i] = BLANK;  // NaNs are likely to be reproduced in sampling.
	}
	// Get a standard mosaicker and process the image.
	Processor m = new Mosaicker();
	int wid = output.getWidth();
	m.process(input, output, source, samp, dSampler);
	
	
	String[] backups = Settings.getArray("BackupSurvey");
	for (int i=0; i<backups.length; i += 1) {
	    for (int j=0; j<data.length; j += 1) {
		double datum = data[j];
		if (datum < 0 ||  Double.isNaN(datum) ) {
		    reprocess(output, source, samp, dSampler, backups[i]);
		    break;  // Break out of the inner loop and restart check.
		}
	    }
	}
    }
    
    public void reprocess(Image output, int[] source,
			  Sampler samp, DepthSampler dSampler, 
			  String survey) {
	try {
	    System.err.println("  Backup survey:"+survey);
	    // *****
	    // A quick recapitulation of what is done in Imager.
	    // Get the survey finder
	    SurveyFinder finder = (SurveyFinder) 
	      skyview.util.Utilities.newInstance(Settings.get("surveyfinder"),
						 "skyview.survey");
	
	    // Get the survey
	    Survey surv = finder.find(survey);
	    if (pos == null) {
	        // Find the center of the image.
	        int width   = output.getWidth();
	        int height  = output.getHeight();
	        double[] cpix = new double[]{width/2.,height/2.};
	        double[] cunit = new double[3];
	        output.getWCS().inverse().transform(cpix, cunit);
			       
	        cpix = skyview.geometry.Util.coord(cunit);
	        cpix[0]= toDegrees(cpix[0]);
	        cpix[1]= toDegrees(cpix[1]);
		pos = new Position(cpix[0], cpix[1], "J2000");
		
		// Find the dimension of the output image.
		maxSize = max(width, height)*output.getWCS().getScale()*180/PI;
	    }

	    // Get candidate images from the survey.
	    Image[]  cand  = surv.getImages(pos, maxSize);
	    
	    // Get an image finder
	    ImageFinder imFin = ImageFinder.factory(Settings.get("imagefinder"));
	    
	    // Find the images to be used.
	    int[] match = imFin.findImages(cand, output);
	    //*****

	    // Now we begin doing things a little differently.
	    double[] data = output.getDataArray();
	    
	    // If the pixel was already found, mark it as consumed.
	    for (int i=0; i<data.length; i += 1) {
		if (data[i] >= 0) {
		    match[i] = skyview.process.imagefinder.Border.CONSUMED;
		}
	    }
	    
	    // Get a new mosaicker.
	    Mosaicker m = new Mosaicker();
	    
	    // Process this survey.
	    m.process(cand, output, match, samp, dSampler);
	    
	    // Save the mosaicker so we can add the appropriate info
	    // to the FITS header later on.
	    mosaickers.add(m);
		    
	} catch(Exception e) {
	    System.err.println("Backup survey error:"+e);
	}
	return;
    }
    
    /** Describe the mosaicking of the image . */
    public void updateHeader(Header h) {
	try {
	    h.insertHistory("");
	    h.insertHistory("Image mosaicking using skyview.process.BackupMosaicker");
	    h.insertHistory("");
	    for (Processor p: mosaickers) {
		p.updateHeader(h);
	    }
	} catch (nom.tam.fits.FitsException e) {
	    System.err.println("Error updating FITS header:\n   "+e);
	    // Just continue
	}
    }
}
    
