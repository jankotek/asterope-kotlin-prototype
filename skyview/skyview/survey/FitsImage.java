package skyview.survey;

import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.FitsException;
import nom.tam.util.BufferedDataInputStream;

import skyview.executive.Key;
import skyview.geometry.TransformationException;
import skyview.survey.Image;
import skyview.survey.ImageFactory;

import skyview.executive.Settings;

/** This class defines an image gotten by reading a file */

public class FitsImage extends Image {
    
    private String fitsFile;
    private Header fitsHeader;
    
    public FitsImage(String file) throws SurveyException {
	
	Header h;
	skyview.geometry.WCS wcs;
	
	setName(file);
	data = null;
	
	this.fitsFile = file;
	
        nom.tam.util.ArrayDataInput inp = null;
	try {
	    Fits f = new Fits(file);
	    inp = f.getStream();
	
	    h = new Header(inp);
	    
	    //  Kludge to accommodate DSS2
	    if (h.getStringValue("REGION") != null) {
		setName(h.getStringValue("REGION")+":"+file);
	    }
	    
	} catch (Exception e) {
	    throw new SurveyException("Unable to read file:"+fitsFile);
	} finally {
	    if (inp != null) {
		try {
		    inp.close();
		} catch (Exception e) {
		}
	    }
	}
		   
	
        int naxis = h.getIntValue("NAXIS");
	if (naxis < 2) {
	    throw new SurveyException("Invalid FITS file: "+fitsFile+".  Dimensionality < 2");
	}
	int nx = h.getIntValue("NAXIS1");
	int ny = h.getIntValue("NAXIS2");
	int nz = 1;
	
	if (h.getIntValue("NAXIS") > 2) {
	    nz = h.getIntValue("NAXIS3");
	}
	
	if (naxis > 3) {
	    for(int i=4; i <= naxis; i += 1) {
		if (h.getIntValue("NAXIS"+i) > 1) {
		    throw new SurveyException("Invalid FITS file:"+fitsFile+".  Dimensionality > 3");
		}
	    }
	}
	
	try {
	    if (Settings.has(Key.PixelOffset)) {
		String[] crpOff= Settings.getArray(Key.PixelOffset);
		try {
		    double d1 = Double.parseDouble(crpOff[0]);
		    double d2 = d1;
		    if (crpOff.length > 0) {
			d1 = Double.parseDouble(crpOff[1]);
		    }
		    h.addValue("CRPIX1", h.getDoubleValue("CRPIX1")+d1, "");
		    h.addValue("CRPIX2", h.getDoubleValue("CRPIX2")+d2, "");
		} catch (Exception e) {
		    System.err.println("Error adding Pixel offset:"+Settings.get(Key.PixelOffset));
		    // Just go on after letting the user know.
		}
	    }
	    wcs = new skyview.geometry.WCS(h);
	} catch (TransformationException e) {
	    throw new SurveyException("Unable to create WCS for file:"+fitsFile+" ("+e+")");
	}
	
	try {
	    initialize(null, wcs, nx, ny, nz);
	} catch(TransformationException e) {
	    throw new SurveyException("Error generating tranformation for file: "+file);
	}
	fitsHeader = h;
    }
    
    
    /** Defer reading the data until it is asked for. */
    public double getData(int npix) {
	
	Fits     f = null;
	Object   o;
	BasicHDU hdu;
	
	if (data == null) {
	    
	    try {
		// We're going to read everything, so
		// don't worry if it's a file or not.
		
		try {
		    java.net.URL url = new java.net.URL(fitsFile);
		    f = new Fits(url);
		
		} catch (Exception e) {
		    // Try it as a file
		}
		    
		if (f == null) {
                    f   = new Fits(Util.getResourceOrFile(fitsFile));
		}
		    
	        hdu = f.readHDU();
	        o   = hdu.getData().getData();
		f.getStream().close();
	    } catch(Exception e) {
		throw new Error("Error reading FITS data for file: "+fitsFile+"\n\nException was:"+e);
	    }
	    
	    o = nom.tam.util.ArrayFuncs.flatten(o);
	    
	    // Data may not be double (and it may be scaled)
	    // We assume no scaling if the data is double...
	    if (! (o instanceof double[])) {
		
	        Header h = hdu.getHeader();
		double scale = h.getDoubleValue("BSCALE", 1);
		double zero  = h.getDoubleValue("BZERO", 0);
		
		// Bytes are signed integers in Java, but unsigned
		// in FITS, so if we are reading in a byte array
		// we'll need to convert the negative values.
		
		boolean bytearray = o instanceof byte[];
		
		o = nom.tam.util.ArrayFuncs.convertArray(o, double.class);
		
		data = (double[]) o;
		if (bytearray || scale != 1 || zero != 0) {
		    
		    for (int i=0; i<data.length; i += 1) {
			if (bytearray && data[i] < 0) {
			    data[i] += 256;
			}
			data[i] = scale*data[i] + zero;
		    }
		}
	    } else {
		data = (double[]) o;
	    }
	}
	
	return data[npix];
    }
    
    public Header getHeader() {
	return fitsHeader;
    }
}
