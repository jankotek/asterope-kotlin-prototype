package skyview.util;

import skyview.geometry.WCS;
import skyview.geometry.Util;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;


/** This class gives the coordinate values corresponding to
 *  a given pixel location in an image.
 */
public class CoordsPixel {
    
    /** Usage: <br>java skyview.util.CoordsPixel file lon lat<br>
     *  where file is the name of the FITS file and
     *  lon and lat are the celestial coordinates (in the coordinate
     *  system of the image for
     *  which the pixel coordiantes are desired.
     *  Note that the pixel coordinates for an NxM image
     *  run from 0 to N and 0 to M.  The standard definition
     *  used within FITS for coordinates (e.g., in the CRPIX
     *  values) would range from 1/2 to N+1/2  and 1/2 to M+1/2.
     * 
     */
    public static void main(String[] args) throws Exception {
	
	String file = args[0];
	double lon  = Double.parseDouble(args[1]);
	double lat  = Double.parseDouble(args[2]);
	
	double[] pixels = new double[2];
	double[] unit   = Util.unit(Math.toRadians(lon),Math.toRadians(lat));
	
	Fits f   = new Fits(file);
	Header h = f.readHDU().getHeader();
	WCS w    = new WCS(h);
//	w.debug(true);
	
	double[] pixel  = w.transform(unit);
	
	System.out.println("Pixels: "+pixel[0]+", "+pixel[1]);
    }
}
