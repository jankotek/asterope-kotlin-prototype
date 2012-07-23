package skyview.geometry;

import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static org.apache.commons.math3.util.FastMath.*;

import java.util.HashMap;
import java.util.Iterator;


import nom.tam.fits.Header;

/** A World Coordinate System defines
 *  a translation between celestial and pixel
 *  coordinates.  Note that in many cases
 *  FITS keywords describe the transformations
 *  in the other direction (from pixel to celestial)
 *  but we follow the convention that forward transformations
 *  are from celestial to pixel.
 *  Given a WCS object, wcs,  the pixel-celestial coordinates trasnformation
 *  is simply wcs.inverse();
 */

public class WCS extends Converter {
    
    /** This includes a nominal 'scale' for the WCS. While this
     *  can often be calculated from the transformation, that may sometimes
     *  be difficult.
     */
    private double wcsScale;
    
    /** The coordinate system used in the WCS. */
    private CoordinateSystem csys;
    
    /** The Projection used in the WCS. */
    private Projection        proj;
    
    /** The linear distorter used in the WCS. */
    private Distorter        distort;
    
    /** The scaler used in the WCS. */
    private Scaler           scale;
    
    /** The size of the image if a WCS is generated from a FIS header */
    int[] headerNaxis;
    
    /** Is this a standards WCS */
    private boolean stdWCS = true;
    
    private HashMap<String, Object> wcsKeys;
    
    /** Should we prefer DSS or non-DSS WCS parameters. */
    private static boolean preferDSS = false;
     
    
    /** Create a simple WCS given a scaler, CoordinateSystem and Projection.
     */
    public WCS(CoordinateSystem csys, Projection proj, Scaler scale) 
      throws TransformationException {
	 
	this.csys  = csys;
	this.proj  = proj;
	this.scale = scale;
	  
	add(csys.getSphereDistorter());
	add(csys.getRotater());
	add(proj.getRotater());
	add(proj.getProjecter());
	add(proj.getDistorter());
	add(scale);
	setWCSScale(scale);
    }
    
    /** Return a new WCS from the existing WCS where we
     *  add a scale.  This allows us to return a WCS
     *  for a subset of an image easily.
     */
    public WCS addScaler(Scaler shift) throws TransformationException {
	return new WCS(csys, proj, scale.add(shift));
    }
    
    public static void setPreferDSS(boolean flag) {
	preferDSS = flag;
    }
    
    // Accessor methods.
    
    /** Get the CoordinateSystem used in the WCS */
    public CoordinateSystem getCoordinateSystem() {
	return csys;
    }
    
    /** Get the projection used in the WCS */
    public Projection getProjection() {
	return proj;
    }
    
    /** Get the linear scaler used in the projection */
    public Scaler getScaler() {
	return scale;
    }
    
    /** Get the plane distorter used in the projection (or null) */
    public Distorter getDistorter() {
	return distort;
    }
    
    /** Set the scale of the transformation */
    private void setWCSScale(Scaler s) {
	// Use the determinant of the transformation matrix to get the scale
	double[] p   = s.getParams();
        double   det = p[2]*p[5]-p[3]*p[4];
	wcsScale     = 1/sqrt(abs(det));
    }
    
    /** Which axis is the longitude */
    private int lonAxis = -1;
    /** Which axis is the latitude */
    private int latAxis = -1;
    
    /** The FITS header */
    private Header h;
    
    /** Create the WCS using the definition given
     *  in the FITS header.
     */
    public WCS(Header h) throws TransformationException {
	
	wcsKeys = new HashMap<String, Object>();
	
	this.h = h;
	headerNaxis = new int[2];
	if (checkDSS()) {
	    headerNaxis[0] = h.getIntValue("NAXIS1");
	    headerNaxis[1] = h.getIntValue("NAXIS2");
	    if (headerNaxis[0] == 0) {
		headerNaxis[0] = h.getIntValue("XPIXELS");
		headerNaxis[1] = h.getIntValue("YPIXELS");
	    }
	    doDSSWCS();
	    stdWCS = false;
	    
	} else if (checkNeat()) {
	    headerNaxis[0] = h.getIntValue("NAXIS1");
	    headerNaxis[1] = h.getIntValue("NAXIS2");
	    doNeatWCS();
	    stdWCS = false;
	    
	    
	} else {  // More or less standard FITS WCS
	    
	    getAxes();
	    if (lonAxis == -1 || latAxis == -1) {
	        throw new TransformationException("Unable to find coordinate axes");
	    }
	    headerNaxis[0] = h.getIntValue("NAXIS"+lonAxis);
	    headerNaxis[1] = h.getIntValue("NAXIS"+latAxis);
	    extractCoordinateSystem();
	    extractProjection();
	    extractScaler();
	}
    }
    
    public boolean standardWCS() {
	return stdWCS;
    }
    
    public int[] getHeaderNaxis() {
	return headerNaxis;
    }
    
    public void setHeaderNaxis(int[] newAxes) {
	headerNaxis = new int[newAxes.length];
	System.arraycopy(newAxes, 0, headerNaxis, 0, newAxes.length);
    }    
    
    /** Let the user say which axes to use */
    public WCS(Header h, int lonAxis, int latAxis) throws TransformationException {
	this.lonAxis = lonAxis;
	this.latAxis = latAxis;
	extractCoordinateSystem();
	extractProjection();
	extractScaler();
    }
    
    /** Find which axes are being used for coordinates.
     *  Normally this will just be 1 and 2, but occasionally
     *  we may be surprised.
     */
    private void getAxes() {
	
	int naxes = h.getIntValue("NAXIS");
	
	// The first axes match are assumed to be correct.
	for (int i=1; i <= naxes; i += 1) {
	    String axis = h.getStringValue("CTYPE"+i);
	    if (axis != null && axis.length() >= 4) {
		axis = axis.substring(0,4);
	    } else {
		continue;
	    }
	    
	    if (lonAxis == -1) {
	        if (axis.equals("RA--")  || axis.equals("GLON")  || axis.equals("ELON") || axis.equals("HLON")) {
		    lonAxis = i;
		}
	    }
	    if (latAxis == -1) {
	        if (axis.equals("DEC-")  || axis.equals("GLAT")  || axis.equals("ELAT") || axis.equals("HLAT")) {
		    latAxis = i;
		}
	    }
	}
	if (lonAxis > -1) {
	    wcsKeys.put("CTYPE1", h.getStringValue("CTYPE"+lonAxis));
	}
	if (latAxis > -1) {
	    wcsKeys.put("CTYPE2", h.getStringValue("CTYPE"+latAxis));
	}
    }
    
    /** Get the scaling between the projection plane and pixel coordinates */
    private void extractScaler() throws TransformationException {
	
	// There are three ways that scaling information may be provided:
	//    CDELTn, CRPIXn, and CROTAn
	//    CDm_n, CRPIXn
	//    PCm_n, CDELTn, CRPIXn
	// We look for them in this sequence.
	//
	
	double crpix1 = h.getDoubleValue("CRPIX"+lonAxis, NaN);
	double crpix2 = h.getDoubleValue("CRPIX"+latAxis, NaN);
	wcsKeys.put("CRPIX1", crpix1);
	wcsKeys.put("CRPIX2", crpix2);
	if (isNaN(crpix1) || isNaN(crpix2)) {
	    throw new TransformationException("CRPIXn not found in header");
	}
	
	// Note that in FITS files, the center of the first pixel is
	// assumed to be at coordinates 1,1.  Thus the corner of the image
	// is at pixels coordinates 1/2, 1/2.
	crpix1 -= 0.5;
	crpix2 -= 0.5;
	
	if (extractScaler1(crpix1, crpix2)) {
	    return;
	}
	if (extractScaler2(crpix1, crpix2)) {
	    return;
	}
	    
	// No scaling information found.
	throw new TransformationException("No scaling information found in FITS header");
    }
    
    
    /** Get the scaling when CDELT is specified */
    private boolean extractScaler1(double crpix1, double crpix2) throws TransformationException {
	
	boolean matrix = false;
	
	double cdelt1 = h.getDoubleValue("CDELT"+lonAxis, NaN);
	double cdelt2 = h.getDoubleValue("CDELT"+latAxis, NaN);
	wcsKeys.put("CDELT1", cdelt1);
	wcsKeys.put("CDELT2", cdelt2);
	if (isNaN(cdelt1) || isNaN(cdelt2)) {
	    return false;
	}
	
	// We use 1 indexing to match better with the FITS files.
	double m11, m12, m21, m22;
	
	// We've got minimal information...  We might have more.
	double crota = h.getDoubleValue("CROTA"+latAxis, NaN);
	if (!isNaN(crota) && crota != 0) {
	    wcsKeys.put("CROTA2", crota);
	    crota = toRadians(crota);
	    
	    m11 =  cos(crota);
	    m12 =  sin(crota);
	    m21 = -sin(crota);
	    m22 =  cos(crota);
	    matrix = true;
	    
	} else {
	
	    m11 = h.getDoubleValue("PC"+lonAxis+"_"+lonAxis, NaN);
	    m12 = h.getDoubleValue("PC"+lonAxis+"_"+latAxis, NaN);
	    m21 = h.getDoubleValue("PC"+latAxis+"_"+lonAxis, NaN);
	    m22 = h.getDoubleValue("PC"+latAxis+"_"+latAxis, NaN);
	    matrix = ! isNaN(m11+m12+m21+m22);
	    if (matrix) {
		wcsKeys.put("PC1_1", m11);
		wcsKeys.put("PC1_2", m12);
		wcsKeys.put("PC2_1", m21);
		wcsKeys.put("PC2_2", m22);
	    }
	}
    
	
	// Note that Scaler is defined with parameters t = x0 + a00 x + a01 y; u = y0 + a10 x + a11 y
	// which is different from what we have here...  
	//    t = scalex (x-x0),  u = scaley (y-y0)
	//    t = scalex x - scalex x0; u = scaley y - scaley y0
	// or
	//    t = scalex [a11 (x-x0) + a12 (y-y0)], u = scaley [a21 (x-x0) + a22 (y-y0)] ->
	//       t = scalex a11 x - scalex a11 x0 + scalex a12 y + scalex a12 y0         ->
        //       t = - scalex (a11 x0 + a12 y0) + scalex a11 x + scalex a12 y (and similarly for u)
 
	Scaler s;
	cdelt1 = toRadians(cdelt1);
	cdelt2 = toRadians(cdelt2);
	if (!matrix) {
	    s = new Scaler(-cdelt1*crpix1,-cdelt2*crpix2,
			    cdelt1, 0, 0,  cdelt2);
	} else {
	    s = new Scaler(-cdelt1*(m11*crpix1+m12*crpix2), -cdelt2*(m21*crpix1+m22*crpix2),
				   cdelt1*m11, cdelt1*m12, cdelt2*m21,cdelt2*m22);
	}    
	
	// Note that this scaler transforms from pixel coordinates to standard projection
	// plane coordinates.  We want the inverse transformation as the scaler.
	s = s.inverse();
	
	// Are lon and lat in unusual order?
	if (lonAxis > latAxis) {
	    s.interchangeAxes();
	}
	
	this.scale = s;
	add(s);
	setWCSScale(s);
	
        return true;
    }
    
    
    /** Get the scaling when it is described as a matrix */
    private boolean extractScaler2(double crpix1, double crpix2) throws TransformationException {
	
	// Look for the CD matrix...
	// 
	double m11 = h.getDoubleValue("CD"+lonAxis+"_"+lonAxis, NaN);
	double m12 = h.getDoubleValue("CD"+lonAxis+"_"+latAxis, NaN);
	double m21 = h.getDoubleValue("CD"+latAxis+"_"+lonAxis, NaN);
	double m22 = h.getDoubleValue("CD"+latAxis+"_"+latAxis, NaN);
	      
        boolean matrix = ! isNaN(m11+m12+m21+m22);
	if (!matrix) {
	    return false;
	}
	wcsKeys.put("CD1_1", m11);
	wcsKeys.put("CD1_2", m12);
	wcsKeys.put("CD2_1", m21);
	wcsKeys.put("CD2_2", m22);
	
	m11 = toRadians(m11);
	m12 = toRadians(m12);
	m21 = toRadians(m21);
	m22 = toRadians(m22);
	
	// we have
        //   t = a11 (x-x0) + a12 (y-y0); u = a21(x-x0) + a22(y-y0)
	//       t = a11x + a12y - a11 x0 - a12 y0;
	// 
	Scaler s = new Scaler(-m11*crpix1 - m12*crpix2, -m21*crpix1 - m22*crpix2,
			      m11, m12, m21, m22);
	
	s = s.inverse();
	
	// Are longitude and latitude in unusual order?
	if (lonAxis > latAxis) {
	    s.interchangeAxes();
	}
	this.scale = s;
	add(s);
	setWCSScale(s);
        return true;
    }
    
    private void extractCoordinateSystem() throws TransformationException {
	
	// To get the coordinate system we look at the CTYPEn, EQUINOX, RADESYSm
	
	String lonType = h.getStringValue("CTYPE"+lonAxis).substring(0,4);
	String latType = h.getStringValue("CTYPE"+latAxis).substring(0,4);
	String coordSym = null;
	
	CoordinateSystem coords;
	
	if (lonType.equals("RA--") && latType.equals("DEC-") ){
	    coordSym = frame()+equinox();
	} else {
	    if (lonType.charAt(0) != latType.charAt(0)) {
	        throw new TransformationException("Inconsistent axes definitions:"+lonType+","+latType);
	    }
	
	    if (lonType.equals("GLON")) {
	        coordSym = "G";
	    } else if (lonType.equals("ELON")) {
	        coordSym = "E"+equinox();
	    } else if (lonType.equals("HLON")) {
	        coordSym = "H"+equinox();
	    }
	}
	
	coords = CoordinateSystem.factory(coordSym);
	this.csys = coords;
	
	add(coords.getSphereDistorter());
	add(coords.getRotater());
    }
    
    private double equinox() {
	
	double equin = h.getDoubleValue("EQUINOX", NaN);
	
	if (isNaN(equin)) {
	    equin = h.getDoubleValue("EPOCH", NaN);
	}
	if (isNaN(equin)) {
	    equin = 2000;
	}
	wcsKeys.put("EQUINOX", 2000);
	return equin;
    }
    
    private String frame() {
	String sys = h.getStringValue("RADESYS");
	if (sys != null) {
	    wcsKeys.put("RADESYS", sys);
	}
	if (sys == null) {
	    double equin = equinox();
	    if (equin >= 1984) {
		return "J";
	    } else {
		return "B";
	    }
	}
	if (sys.length() > 3  && sys.substring(0,3).equals("FK4")) {
	    return "B";
	} else {
	    return "J";
	}
    }
    
    private void extractProjection() throws TransformationException {
	
	Projection proj  = null;
	Scaler  ncpScale = null;
	
	String lonType = h.getStringValue("CTYPE"+lonAxis).substring(5,8);
	String latType = h.getStringValue("CTYPE"+latAxis).substring(5,8);
	if (!lonType.equals(latType)) {
	    throw new TransformationException("Inconsistent projection in FITS header: "+lonType+","+latType);
	}
	
        if (lonType.equals("AIT")) {
	    proj = new Projection("Ait");
	    
	} else if (lonType.equals("CAR")) {
	    proj = new Projection("Car");
            // Allow non-central latitudes for the Cartesian projection.
            try {
                double lon = h.getDoubleValue("CRVAL"+lonAxis);
                if (lon != 0) {
                    proj.setReference(toRadians(lon), 0);
                }
            } catch (Exception e) {
                System.err.println("Unable to read reference longitude in Cartesian projection");
            }
	    
	} else if (lonType.equals("CSC")) {
	    proj = new Projection("Csc");
	
	} else if (lonType.equals("SFL") || lonType.equals("GLS")) {
	    proj = new Projection("Sfl");
	    
	} else if (lonType.equals("TOA")) {
	    proj = new Projection("Toa");
	    
	} else {
	    
	
	    double crval1 = h.getDoubleValue("CRVAL"+lonAxis, NaN);
	    double crval2 = h.getDoubleValue("CRVAL"+latAxis, NaN);
	
	    if (isNaN(crval1+crval2)) {
	        throw new TransformationException("Unable to find reference coordinates in FITS header");
	    }
	    
	    wcsKeys.put("CRVAL1", crval1);
	    wcsKeys.put("CRVAL2", crval2);
	
	    if (lonType.equals("TAN")  || lonType.equals("SIN") || lonType.equals("ZEA") ||
		lonType.equals("ARC")  || lonType.equals("STG")
	       ) {
	    
	    
	        String type = lonType.substring(0,1)+lonType.substring(1,3).toLowerCase();
	        proj = new Projection(type, new double[]{toRadians(crval1), toRadians(crval2)});
	    
	        double lonpole = h.getDoubleValue("LONPOLE", NaN);
		if (!isNaN(lonpole)) {
		    wcsKeys.put("LONPOLE", lonpole);
		}
		//  ---- Following is probably erroneous -----
		// The WCS standard indicates that the default LONPOLE for
		// a projection is 180 when the CRVAL latitude is less than
		// the native latitude of the projection (90 degrees for the projections
		// handled here) and 0 otherwise.  This means that for a projection
		// around the pole the default lonpole is 0.  Some data (the SFD surveys)
		// seem to require that we do a rotation of 180 degrees to accommodate
		// this.  However we do not implement this unless the LONPOLE is
		// explicitly given since this seems non-intuitive to me and I suspect
		// that a user who is not careful enough to specify a LONPOLE in this
		// situation probably doesn't understand what is going on anyway.
		// ----- We now assume that our standard processing of
		// ----- zenithal projections handles lonpole of 180 and that
		// ----- this is the default for all zenithal images.
		// ----- Previously we assumed that we were using lonPole=0 at
		// ----- at the poles, but we weren't....
		// 
		
	        if (!isNaN(lonpole)) {
		    double lonDefault = 180;
		    if (lonpole != lonDefault) {
			
		       Rotater r    = proj.getRotater();
			
		       Rotater lon  = new Rotater("Z", toRadians(lonpole-lonDefault), 0, 0);
		       if (r != null) {
		           proj.setRotater(r.add(lon));
		        } else {
		            proj.setRotater(lon);
		        }
	            }
		}
	    
	    } else if (lonType.equals("NCP")) {
		
	        // Sin projection with projection centered at pole.
		double[] xproj = new double[] {toRadians(crval1), PI/2};
		if (crval2 < 0) {
		    xproj[1] = - xproj[1];
		}
		
		
		double poleOffset = sin(xproj[1]-toRadians(crval2));
		// Have we handled South pole here?
	        
	        proj = new Projection("Sin", xproj);
	    
	        // NCP scales the Y-axis to accommodate the distortion of the SIN projection away
	        // from the pole.
		ncpScale = new Scaler(0, poleOffset, 1, 0, 0, 1);
	        ncpScale = ncpScale.add(new Scaler(0., 0., 1,0,0,1/sin( toRadians(crval2) ) ) );
		
	    } else {
	        throw new TransformationException("Unsupported projection type:"+lonType);
	    }
	}
	
        this.proj = proj;
	if (ncpScale != null) {
	    this.scale = ncpScale;
	}
	add(proj.getRotater());
	add(proj.getProjecter());
	add(ncpScale);  // Ignored if null
	
	
    }

    /** Is this a DSS projection? */
    private boolean checkDSS() throws TransformationException {
	
        String origin = h.getStringValue("ORIGIN");
	if (origin == null  || h.getStringValue("CTYPE1") != null) {
	    return false;
	}
	wcsKeys.put("ORIGIN", origin);
	
	// If we have a local solution use it unless told
	// to prefer the DSS solution.
	if (h.getStringValue("CTYPE1") != null  && !preferDSS) {
	    return false;
	}
	
	if (h.getDoubleValue("XPIXELSZ", -1) == -1) {
	    return false;
	}
	
	return origin.startsWith("CASB") || origin.startsWith("STScI") ;
    }
    
    /** Handle a DSS projection */
    private void doDSSWCS() throws TransformationException {
	
        double plateRA = h.getDoubleValue("PLTRAH") + h.getDoubleValue("PLTRAM")/60 + h.getDoubleValue("PLTRAS")/3600;
	plateRA        = toRadians(15*plateRA);
	wcsKeys.put("PLTRAH", h.getDoubleValue("PLTRAH"));
	wcsKeys.put("PLTRAM", h.getDoubleValue("PLTRAM"));
	wcsKeys.put("PLTRAS", h.getDoubleValue("PLTRAS"));
	
	double plateDec = h.getDoubleValue("PLTDECD") + h.getDoubleValue("PLTDECM")/60 + h.getDoubleValue("PLTDECS")/3600;
	plateDec        = toRadians(plateDec);
	
	if (h.getStringValue("PLTDECSN").substring(0,1).equals("-")) {
	    plateDec    = -plateDec;
	}
	wcsKeys.put("PLTDECD", h.getDoubleValue("PLTDECD"));
	wcsKeys.put("PLTDECM", h.getDoubleValue("PLTDECM"));
	wcsKeys.put("PLTDECS", h.getDoubleValue("PLTDECS"));
	wcsKeys.put("PLTDECSN", h.getStringValue("PLTDECSN"));
	
	double plateScale = h.getDoubleValue("PLTSCALE");
	double xPixelSize = h.getDoubleValue("XPIXELSZ");
	double yPixelSize = h.getDoubleValue("YPIXELSZ");
	wcsKeys.put("PLTSCALE", plateScale);
	wcsKeys.put("XPIXELSZ", xPixelSize);
	wcsKeys.put("YPIXELSZ", yPixelSize);
	
	double[] xCoeff = new double[20];
	double[] yCoeff = new double[20];
	
	for (int i=1; i <= 20; i += 1) {
	    xCoeff[i-1] = h.getDoubleValue("AMDX"+i);
	    yCoeff[i-1] = h.getDoubleValue("AMDY"+i);
	    wcsKeys.put("AMDX"+i, xCoeff[i-1]);
	    wcsKeys.put("AMDY"+i, yCoeff[i-1]);
	}
	
	double[] ppo = new double[6];
	for (int i=1; i <= 6; i += 1) {
	    ppo[i-1] = h.getDoubleValue("PPO"+i);
	    wcsKeys.put("PPO"+i, ppo[i-1]);
	}
	
	double plateCenterX = ppo[2];
	double plateCenterY = ppo[5];
	
	double cdelt1 = - plateScale/1000 * xPixelSize/3600;
	double cdelt2 =   plateScale/1000 * yPixelSize/3600;
	
	wcsScale = abs(cdelt1);
	
	// This gives cdelts in degrees per pixel.  
	
	// CNPIX pixels use a have the first pixel going from 1 - 2 so they are
	// off by 0.5 from FITS (which in turn is offset by 0.5 from the internal
	// scaling, but we handle that elsewhere).
	double crpix1 =   plateCenterX/xPixelSize - h.getDoubleValue("CNPIX1", 0) - 0.5;
	double crpix2 =   plateCenterY/yPixelSize - h.getDoubleValue("CNPIX2", 0) - 0.5;
	wcsKeys.put("CNPIX1", h.getDoubleValue("CNPIX1", 0));
	wcsKeys.put("CNPIX2", h.getDoubleValue("CNPIX2", 0));
	
	
	Projection proj         = new Projection("Tan", new double[]{plateRA, plateDec});
	this.proj   = proj;
	CoordinateSystem coords = CoordinateSystem.factory("J2000");
	this.csys   = coords;
	
	cdelt1 = toRadians(cdelt1);
	cdelt2 = toRadians(cdelt2);
	
	Scaler s = new Scaler(-cdelt1*crpix1, -cdelt2*crpix2, cdelt1, 0, 0, cdelt2);
	
	// Got the transformers ready.  Add them in properly.
        add(coords.getSphereDistorter());
	add(coords.getRotater());
	add(proj.getRotater());
	add(proj.getProjecter());
	
	this.distort = new skyview.geometry.distorter.DSS(
	   plateRA, plateDec, xPixelSize, yPixelSize, plateScale, 
	   ppo, xCoeff, yCoeff);
	add(this.distort);
	
	this.scale = s.inverse();
	
	
	add(this.scale);
	    
    }
    
    /** Is this a NEAT special projection? */
    private boolean checkNeat() {
	return h.getStringValue("CTYPE1").equals("RA---XTN");
    }
    
        /** Handle the NEAT special projection */
    private void doNeatWCS() throws TransformationException {
    
        // The NEAT transformation from the standard spherical system
	// includes:
	//   Transformation to J2000 spherical coordinates (a null operation)
	//   A tangent plane projection to the standard plane
	//   A scaler transformation to corrected pixel coordinates.
	//   A distorter to distorted pixel coordinates
	//   A scaler transformation of distorted coordinates to actual pixels
    
	
	CoordinateSystem csys = CoordinateSystem.factory("J2000");
	this.csys = csys;
	
	// The RA0/DEC0 pair are the actual center of the projection.
	
	double cv1 = toRadians(h.getDoubleValue("RA0"));
	double cv2 = toRadians(h.getDoubleValue("DEC0"));
	wcsKeys.put("CRVAL1", toDegrees(cv1));
	wcsKeys.put("CRVAL2", toDegrees(cv2));
	
	Projection       proj = new Projection("Tan", new double[] {cv1, cv2});
	this.proj = proj;
	
	
	double cd1 = toRadians(h.getDoubleValue("CDELT1"));
	double cd2 = toRadians(h.getDoubleValue("CDELT2"));
	wcsKeys.put("CDELT1", toDegrees(cd1));
	wcsKeys.put("CDELT2", toDegrees(cd2));
	
	wcsScale   = abs(cd1);
	
	double cp1 = h.getDoubleValue("CRPIX1");
	double cp2 = h.getDoubleValue("CRPIX2");
	
	wcsKeys.put("CPRIX1", cp1);
	wcsKeys.put("CRPIX2", cp2);
	
	wcsKeys.put("CTYPE1", "RA---XTN");
	wcsKeys.put("CTYPE2", "DEC--XTN");
	
	Scaler s1  = new Scaler(0., 0., -1/cd1, 0, 0, -1/cd2);
	
	// Note that the the A0,A1,A2, B0,B1,B2 rotation
	// is relative to the original pixel values, so
	// we need to put this in the secondary scaler.
	// 
	double x0 = h.getDoubleValue("X0");
	double y0 = h.getDoubleValue("Y0");
	
	Distorter dis = new skyview.geometry.distorter.Neat(h.getDoubleValue("RADIAL"),
								     h.getDoubleValue("XRADIAL"),
								     h.getDoubleValue("YRADIAL"));
	
	double a0 = h.getDoubleValue("A0");
	double a1 = h.getDoubleValue("A1");
	double a2 = h.getDoubleValue("A2");
	double b0 = h.getDoubleValue("B0");
	double b1 = h.getDoubleValue("B1");
	double b2 = h.getDoubleValue("B2");
	
	// The reference pixel is to be computed in the distorted frame.
	double[] cpix = new double[]{cp1,cp2};
	double[] cout = new double[2];
	dis.transform(cpix, cout);
	Scaler s2  = new Scaler(cout[0]-a0-a1*x0-a2*y0, 
				cout[1]-b0-b2*x0-b1*y0, 
				-(1+a1), -a2, -b2, -(1+b1));
	
	wcsKeys.put("A0", a0);
	wcsKeys.put("A1", a1);
	wcsKeys.put("A2", a2);
	wcsKeys.put("B0", b0);
	wcsKeys.put("B1", b1);
	wcsKeys.put("B2", b2);
	
	this.distort = dis;
	add(csys.getSphereDistorter());
	add(csys.getRotater());
	add(proj.getRotater());
	add(proj.getProjecter());
	this.scale = s1;
	// Note that s1 is defined from the projection plane to the pixels coordinates,
	// so we don't need to invert it.
        //
        // But the second scaler, s2,  used in the NEAT correction
	// is defined in the direction from pixels to sphere, so we
	// need to take its inverse.
	this.scale = this.scale.add(s2.inverse());
	add(this.scale);
	add(dis);
    }


    /** Get the nominal scale of the WCS.
     */
    public double getScale() {
	return wcsScale;
    }
	
    
    
    /** Write FITS WCS keywords given key values.  Only relatively simple
     *  WCSs are handled here.  We assume we are dealing with axes 1 and 2.
     *  @param h  The header to be updated.
     *  @param s  A Scaler giving the transformation between standard projection
     *            coordinates and pixel/device coordinates.
     *  @param projString A three character string giving the projection used.
     *            Supported projections are: "Tan", "Sin", "Ait", "Car", "Zea".
     *  @param coordString A string giving the coordinate system used.  The first
     *            character gives the general frame.  For most frames the remainder
     *            of the string gives the equinox of the coordinate system.
     *            E.g., J2000, B1950, Galactic, "E2000", "H2020.10375".
     */
    
    public void updateHeader(Header h, Scaler s, double[] crval, String projString, String coordString) 
      throws Exception {
	
	if (proj.isFixedProjection()) {
	    h.addValue("CRVAL1", toDegrees(proj.getReferencePoint()[0]), "Fixed reference center");
	    h.addValue("CRVAL2", toDegrees(proj.getReferencePoint()[1]), "Fixed reference center");
	} else {
	    h.addValue("CRVAL1", crval[0], "Reference longitude");
	    h.addValue("CRVAL2", crval[1], "Reference latitude");
	}
	
	coordString = coordString.toUpperCase();
	String[] prefixes = new String[2];
	char c = coordString.charAt(0);
	if (c == 'J' || c == 'I') {
	    h.addValue("RADESYS", "FK5", "Coordinate system");
	    prefixes[0] = "RA--";
	    prefixes[1] = "DEC-";
	} else if (c == 'B') {
	    h.addValue("RADESYS", "FK4", "Coordinate system");
	    prefixes[0] = "RA--";
	    prefixes[1] = "DEC-";
	} else {
	    prefixes[0] = c + "LON";
	    prefixes[1] = c + "LAT";
	}
	
	if (c != 'G'  && c != 'I') {
	    try {
	        double equinox = Double.parseDouble(coordString.substring(1));
	        h.addValue("EQUINOX", equinox, "Epoch of the equinox");
	    } catch (Exception e) {
		// Couldn't parse out the equinox
	    }
	}
	if (c == 'I') {
	    h.addValue("EQUINOX", 2000, "ICRS coordinates");
	}
	
	String upProj = projString.toUpperCase();
	
	h.addValue("CTYPE1", prefixes[0]+"-"+upProj, "Coordinates -- projection");
	h.addValue("CTYPE2", prefixes[1]+"-"+upProj, "Coordinates -- projection");
	
	
	// Note that the scaler transforms from the standard projection
	// coordinates to the pixel coordinates.
        //     P = P0 + M X  where X is the standard coordinates and P is the
	// pixel coordinates.  So the reference pixels are just the constants
	// in the scaler. 
	// Remember that FITS pixels are offset by 0.5 from 0 offset pixels.
	
	h.addValue("CRPIX1", s.x0+0.5, "X reference pixel");
	h.addValue("CRPIX2", s.y0+0.5, "Y reference pixel");
	
	// Remember that the FITS values are of the form
	//    X = M(P-P0)
	// so we'll need to invert the scaler.
	// 
	// Do we need a matrix?  
	if (abs(s.a01) < 1.e-14 && abs(s.a10) < 1.e-14) {
	    // No cross terms, so we'll just use CDELTs
	    h.addValue("CDELT1", toDegrees(1/s.a00), "X scale");
	    h.addValue("CDELT2", toDegrees(1/s.a11), "Y scale");
	} else {
	    // We have cross terms.  It's simplest
	    // just to use the CD matrix and not worry about
	    // normalization.  First invert the matrix to get
	    // the transformation in the direction that FITS uses.
	    Scaler rev = s.inverse();
	    h.addValue("CD1_1", toDegrees(rev.a00), "Matrix element");
	    h.addValue("CD1_2", toDegrees(rev.a01), "Matrix element");
	    h.addValue("CD2_1", toDegrees(rev.a10), "Matrix element");
	    h.addValue("CD2_2", toDegrees(rev.a11), "Matrix element");
	}
    }
    
    public void copyToHeader(Header h) throws nom.tam.fits.HeaderCardException {
	
	String[] srt = wcsKeys.keySet().toArray(new String[0]);
	java.util.Arrays.sort(srt);
	for(String key: srt) {
	    Object o = wcsKeys.get(key);
	    if (o instanceof Integer) {
		h.addValue(key, ((Integer)o).intValue(), "Copied WCS eleemnt");
	    } else if (o instanceof Double) {
		h.addValue(key, ((Double) o).doubleValue(), "Copied WCS element");
	    } else if (o instanceof String) {
		h.addValue(key, (String) o, "Copied WCS element");
	    }
	}
    }    
}
