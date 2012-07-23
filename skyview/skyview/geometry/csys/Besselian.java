package skyview.geometry.csys;

import skyview.geometry.CoordinateSystem;
import skyview.geometry.Rotater;
import skyview.geometry.SphereDistorter;

import static java.lang.Math.*;


/** This class implements Besselian coordinate systems.
 *  These systems are not simple rotations from the reference
 *  coordinate frame.  These coordinate systems are implemented
 *  such that the rotation matrix is appropriate for J2000 coordinates
 *  but the rectify and derectify function perform transformation
 *  from/to Besselian coordinates.  The transformations do
 *  not use any proper motion or distance information supplied
 *  by the user.  The methods in this class are based on P. Wallaces
 *  SLA library substantially modified for use within Java and SkyView.
 */
public class Besselian extends CoordinateSystem
  implements skyview.Component {

    private static final double D2PI = 6.2831853071795864769252867665590057683943387987502;
    private static final double pmf = 100.0 * 60 * 60 * 360 / D2PI;
      
    private double epoch;
    
    /** These two work arrays mean that this class is not
     *  thread safe.  If multiple threads are to be used each thread
     *  needs its own coordinate system object.
     */
    /** Get a CoordinateSystem of a given epoch.
     *  @param epoch The epoch as a calendar year (possibly fractional).
     */    
    public Besselian(double epoch) {
	this.epoch     = epoch;
    }
    
    /** This coordinate system is not just a rotation away from the reference frame.*/
    public boolean isRotation() {
	return false;
    }
    
    public String getName() {
	return "B"+epoch;
    }
    
    public String getDescription() {
	return "A Beseelian (FK4 based) equatorial coordinate system.  Dynamic terms are not included.";
    }
    
    
    public Rotater getRotater() {
	return precession(epoch);
    }
    
    public SphereDistorter getSphereDistorter() {
	return new skyview.geometry.spheredistorter.Besselian();
    }

    /**
     * Calculate the Besselian Precession between 1950 and the given epoch.
     */

    private Rotater precession(double epoch) {

        double DAS2R = 4.8481368110953599358991410235794797595635330237270e-6;
   
        //  Interval between basic epoch B1850.0 and beginning epoch in TC */
        double bigt  = ( 1950 - 1850 ) / 100.0;
   
        // Interval over which precession required, in tropical centuries */
        double t = (epoch - 1950 ) / 100.0;
 
        //  Euler angles */
        double tas2r = t * DAS2R;
        double w     = 2303.5548 + ( 1.39720 + 0.000059 * bigt ) * bigt;
        double zeta  = (w + ( 0.30242 - 0.000269 * bigt + 0.017996 * t ) * t ) * tas2r;
        double z     = (w + ( 1.09478 + 0.000387 * bigt + 0.018324 * t ) * t ) * tas2r;
        double theta = ( 2005.1125 + ( - 0.85294 - 0.000365* bigt ) * bigt +       
                       ( - 0.42647 - 0.000365 * bigt - 0.041802 * t ) * t ) * tas2r;
 
        return  new Rotater( "ZYZ", -zeta, theta, -z);
    }
}
