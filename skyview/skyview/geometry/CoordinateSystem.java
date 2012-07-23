package skyview.geometry;

import skyview.geometry.csys.*;

/** 
 * The class defines coordinate systems in terms of the
 * operations needed to transform the standard coordinate
 * system (currently J2000) to the coordinate system
 * associated with the object.  Most coordinate systems will
 * be defined as simple rotations, but some coordinate systems
 * may involve more complext transformations.
 *  A factory method is available to generate Coordinate systems from
 *  a given string.   Typically a string is comprised
 *  of an initial and an epoch (e.g., "B1950", "J1975.5").  Any string
 *  beginning with "G" is assumed to be Galactic coordinates.
 */
public abstract class CoordinateSystem implements skyview.Component {
    
    /** Get the rotation associated with the projection.
     */
    public abstract Rotater getRotater();
    
    /** Get the distortion associated with the projection.
     *  By default there is no distortion, but subclasses,
     *  notably BesselianProjection, can override this.
     */
    public SphereDistorter getSphereDistorter() {
	return null;
    }
    
    /** Standard J2000 coordinates -- the reference frame */
    public static final CoordinateSystem J2000 = new Julian(2000);
    
    /** Standard B1950 coordinates */
    public static final CoordinateSystem B1950 = new Besselian(1950);
    
    /** Standard Galactic coordinates */
    public static final CoordinateSystem Gal = new Galactic();
    
    /** Standard ICRS coordinates */
    public static final CoordinateSystem ICRS = new ICRS();
    
    /** Get a coordinate system by name.
     *  @param name A designation of the desired coordinate
     *              system.  Normally the name is an initial
     *              designating the general frame and orientation of
     *              the coordinate system followed by an epoch of equinox,
     *              e.g., J2000, B1950 E2000.45.
     *              The initial letters are:
     *  <dl><dt>J   <dd> Julian Equatorial Coordinates.
     *      <dt>B   <dd> Besselian Equatorial Coordinates.
     *      <dt>E   <dd> Julian Ecliptic Coordinates
     *      <dt>H   <dd> Helioecliptic coordinates.
     *      <dt>G   <dd> Galactic coordinates.  Only the first letter is parsed.
     *  </dl>
     *  The name is not case-sensitive.
     */
    public static CoordinateSystem factory(String name) {
	return factory(name, null);
    }
    
    public static CoordinateSystem factory(String name, String equinox) {
	
	name = name.toUpperCase();
	if (name.equals("ICRS")) {
	    return ICRS;
	}
	
	char c = name.charAt(0);
	
	if (c == 'G') {
	    return Gal;
	}
	
	String sepoch = name.substring(1);
	double epoch = -1;
	try {
	    epoch = Double.parseDouble(name.substring(1));
	} catch (Exception e) {
	    try {
	        if (equinox != null) {
	            epoch = Double.parseDouble(equinox);
		}
	    } catch (Exception f) {}
	}
	
	switch (c) { 
	    
          case 'J':
	    if  (epoch < 0) {
		epoch = 2000;
	    }
	    return new Julian(epoch);
          case 'B':
	    if (epoch < 0) {
		epoch = 1950;
	    }
	    return new Besselian(epoch);
          case 'E':
	    return new Ecliptic(epoch);
          case 'H':
	    return new Helioecliptic(epoch);
          default:
	    return null;
	}
    }
}
