package skyview.geometry.csys;

import skyview.geometry.CoordinateSystem;
import skyview.geometry.Rotater;

/** The class defining Julian coordinate systems.
 *  The reference coordinate system is J2000.
 *  We use the FK5 coordinate frame as the realization
 *  of the Julian coordinate system.  At the 50 milliarcsecond
 *  level this is not valid: There is an offset of J2000 and FK5.
 *  However the details of this offset are still somewhat unclear,
 *  so we use the better defined frame.
 */
public class Julian extends CoordinateSystem 
  implements skyview.Component {
    
    private double epoch;

    /**
     * Get the name of this object.
     */
    public String getName() {
	return "J"+epoch;
    }
    
    /**
     * Get a description of the object.
     */
    public String getDescription() {
	return "A Julian (FK5-based) equatorial coordinate system with epoch of the equinox of "+epoch;
    }
    
    /** Get a Julian CoordinateSystem of a given epoch.
     *  @param epoch The epoch of the equinox of the coordinates in calendar
     *  years (possibly fractional).
     */
    public Julian(double epoch) {
	this.epoch = epoch;
    }
      
    /**
     * Return the rotation associated with the coordinate system.
     */
      
    public Rotater getRotater() {
        if (epoch == 2000) {
	    return null;
	} else {
	    return precession();
	}
    }
	
    /** Get the Julian Precession Matrix for a given epoch (from J2000).
     *  Approach based  on P.Wallace's SLA library.  
     *  The equations are available
     *  in Kaplan, USNO Circular 163, 1981, page A2.  Here we assume
     *  in these equations we assume T=0 so that we are doing a
     *  transformation between J2000 and the specified ending epoch.
     *
     */
    public Rotater precession() {

        double sec2rad = 4.848136811095359935e-6;

        //  Interval over which precession required (expressed in Julian
	//  centuries. In principal it should be in units of 365.24 Julian
	//  days.
	
        double t =  (epoch-2000) / 100;
 
        //  Euler angles */
        double tas2r = t * sec2rad;
        double w     =  2306.2181;
        double zeta  = (w + (  0.30188 + 0.017998 * t ) * t ) * tas2r;
        double z     = (w + (  1.09468 + 0.018203 * t ) * t ) * tas2r;
        double theta = ( 2004.3109 + (  -0.42665  - 0.041833 * t ) * t ) * tas2r;
	
	

        return new Rotater( "ZYZ", -zeta, theta, -z);
    }
}
