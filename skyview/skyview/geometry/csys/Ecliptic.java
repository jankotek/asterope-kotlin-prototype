package skyview.geometry.csys;

import skyview.geometry.CoordinateSystem;
import skyview.geometry.Rotater;

/** An ecliptic coordinate system in a Julian frame.
 */
public class Ecliptic extends CoordinateSystem 
  implements skyview.Component {
    
    private double epoch;
    private double elon;
    
    /** 
     * Get the name of this component.
     */
    public String getName() {
	return "E"+epoch;
    }
    
    /** 
     * Get a description of this component.
     */
    public String getDescription() {
	return "A coordinate system with the ecliptic as the equator at epoch of equinox"+epoch;
    }
	
	
    /** Get an Ecliptic Coordinate system at a given epoch.
     *  @epoch The epoch of the equinox of the coordinate system in calendar years.
     */
    public Ecliptic(double epoch) {
	this(epoch, 0);
    }
    
    /** Get an Ecliptic coordinate system where the 0 of longitude
     *  can be reset.
     *  @param epoch The epoch of the equinox.
     *  @param elon  The longitude in a standard coordinate system
     *               at which the prime meridian should be placed.
     */
    
    protected Ecliptic(double epoch, double elon) {
	
	this.epoch = epoch;
	this.elon  = elon;
    }
      
    public Rotater getRotater() {
        double DAS2R = 4.84813681109535993589914102e-6;

        //   Interval between basic epoch J2000.0 and current epoch (JC) */
        double t = ( epoch - 2000 ) / 100;
        //System.out.println("Ecliptic Coordinate System set, epoch=" + epoch);

 
        //   Mean obliquity 
        double eps0 = DAS2R * ( 84381.448 + ( -46.8150 + ( -0.00059 + 0.001813 * t ) * t ) * t );
 
        //   Get the matrix
	Rotater r1 = new Julian(epoch).getRotater();
	Rotater r2 = new Rotater("XZ", eps0, elon, 0.);
	if (r1 == null) {
	    return r2;
	} else {
	    return r1.add(r2);
	}
    }
}
