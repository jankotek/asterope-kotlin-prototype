package skyview.util;

import skyview.geometry.WCS;
import skyview.geometry.Util;
import skyview.geometry.CoordinateSystem;
import skyview.geometry.Converter;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;

/** This class gives the coordinate values corresponding to
 *  a given pixel location in an image.
 */
public class PixelCoords {
    
    /** Usage: <br>java skyview.util.PixelCoords file x y<br>
     *  where file is the name of the FITS file and
     *  x and y are the pixel coordinates for
     *  which the celestial coordiantes are desired.
     *  Note that the pixel coordinates for an NxM image
     *  run from 0 to N and 0 to M.  The standard definition
     *  used within FITS for coordinates (e.g., in the CRPIX
     *  values) would range from 1/2 to N+1/2  and 1/2 to M+1/2.
     * 
     */
    public static void main(String[] args) throws Exception {
	
	String file = args[0];
	String xs   = args[1];
	String ys   = args[2];
	
	double[] pixels = new double[]{Double.parseDouble(xs),
	                               Double.parseDouble(ys) };
	
	Fits f   = new Fits(file);
	Header h = f.readHDU().getHeader();
	WCS w    = new WCS(h);
	// Want this in the coordinate system of the image...
	CoordinateSystem csys = w.getCoordinateSystem();
	Converter cnv = new Converter();
	if (csys.getRotater() != null) {
	    cnv.add(csys.getRotater().inverse());
	}
	if (csys.getSphereDistorter() != null) {
	    cnv.add(csys.getSphereDistorter().inverse());
	}
	cnv.add(w);
	cnv.printElements();
//	cnv.debug(true);
	
	double[] unit   = cnv.inverse().transform(pixels);
	
	double[] coords = Util.coord(unit);
	System.out.println("Coordinates:"+Math.toDegrees(coords[0])+", "+
			                  Math.toDegrees(coords[1]));
    }
}
