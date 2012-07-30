package skyview.survey;

import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTable;
import nom.tam.fits.FitsException;
import nom.tam.util.BufferedDataInputStream;

import skyview.executive.Key;
import skyview.geometry.TransformationException;
import skyview.geometry.WCS;
import skyview.geometry.Projection;
import skyview.geometry.CoordinateSystem;
import skyview.geometry.Scaler;
import static org.apache.commons.math3.util.FastMath.*;

import skyview.geometry.projecter.Hpx;

import skyview.executive.Settings;

/** This class defines an image gotten by reading a HEALPix image
 *  where the pixels are in the nested pixel order.
 *  This assumes the FITS structures found in the WMAP data  but
 *  could be adapted to other orders as needed.
 */

public class HealPixImage extends Image {
    
    private String     fitsFile;
    private Header     fitsHeader;    
    private Hpx        hpp;
    
    private float[]  vals;
    private int      nside;
    
    public int getWidth() {
	return 4*nside;
    }
    public int getHeight() {
	return 6*nside;
    }
    
    public int getDepth() {
	return 1;
    }
    
    public HealPixImage(String file) throws SurveyException {
	
	Header h;
	skyview.geometry.WCS wcs;
	
	setName(file);
	data = null;
	int dim = 0;
	
	this.fitsFile = file;
        nom.tam.util.ArrayDataInput inp = null;
	
	try {
	    Fits  f = new Fits(file);
	    inp     = f.getStream();

            boolean isImage = Settings.has(Key.HealPixArray);
            BasicHDU hdu = null;
            if (isImage) {
                hdu = f.getHDU(0);
            } else {
	        hdu = f.getHDU(1);
            }
	    dim      = 0;
	    int  cnt = 1;
	    nside    = hdu.getHeader().getIntValue("NSIDE");
	    
	    
	    while (cnt < nside) {
		dim += 1;
		cnt *= 2;
	    }

            if (isImage) {
                vals = (float[])hdu.getKernel();
            } else {
	        BinaryTable bt  = (BinaryTable) hdu.getData();
	        vals = (float[]) bt.getColumn(0);
            }
	
	} catch (Exception e) {
	    System.err.println("Error is:"+e);
	    e.printStackTrace();
	    throw new SurveyException("Unable to read file:"+fitsFile);
	} finally {
	    if (inp != null) {
		try {
		    inp.close();
		} catch (Exception e) {
		}
	    }
	}
	
	try {
	    Projection       proj = new Projection("Hpx");
	    CoordinateSystem cs   = CoordinateSystem.Gal;
	    
	    // The 0,0 point of the oblique projection is at (+2,+2) squares.
	    // This is at the point (3 pi/4, 0) in the original projection.
	    Scaler s1 = new Scaler(-3*PI/4, 0, 1, 0, 0, 1);
	    
	    double isqrt2 = 1 / sqrt(2);
	    
	    // Now rotate by 45 degrees to get into the oblique projection.
	    s1 = s1.add(new Scaler(0., 0., isqrt2, isqrt2, -isqrt2, isqrt2));
	    
	    // Each square has a length of pi/sqrt(8), so pixels
	    // have a length of pi/(nside*sqrt(8))
	    double pixlen = PI/(nside*sqrt(8));
	    
	    // The oblique projection is 4x6 squares, so the
	    // center is 2x3 squares from the bottom left corner.
	    double crpix1 = 2*nside;
	    double crpix2 = 3*nside;
	    
	    s1 = s1.add(new Scaler(crpix1, crpix2, 1/pixlen, 0, 0, 1/pixlen));
	    
	    wcs = new WCS(cs, proj, s1);
		
	    hpp = (Hpx) proj.getProjecter();
	
	    hpp.setDimension(dim);
	    initialize(null, wcs, 4*nside, 6*nside, 1);
	    
	} catch(TransformationException e) {
	    throw new SurveyException("Error generating tranformation for file: "+file);
	}
    }
   
    
    /** Defer reading the data until it is asked for. */
    public double getData(int ipix) {
	
	// convert to healpix index.
	int npix = hpp.cvtPixel(ipix);
	double val;
	if (npix < 0 || npix >= vals.length) {
	    val = -1;
	} else {
	    val =  vals[npix];
	}
	
	return val;
    }
    
    /** Probably should happen, but just in case we
     *  support the get array function.
     */
    public double[] getDataArray() {
	
	double[] data = new double[24*nside*nside];
	
	for (int i=0; i< data.length; i += 1) {
	    data[i] = getData(i);
	}
        return data;
    }
    
    /** Support changing the data!  Probably won't use this...
     */
    public void setData(int npix, double val) {
	
	npix = hpp.cvtPixel(npix);
	if (npix != -1) {
	    vals[npix] = (float) val;
	}
    }
}
