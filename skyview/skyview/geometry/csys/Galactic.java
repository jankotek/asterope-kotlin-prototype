package skyview.geometry.csys;

import skyview.geometry.CoordinateSystem;
import skyview.geometry.Rotater;

/** The class defining Galactic coordinate system.
 */
public class Galactic extends CoordinateSystem 
  implements skyview.Component {
    
    /**
     * Get the name of this object.
     */
    public String getName() {
	return "Galactic";
    }
    
    /**
     * Get a description of the object.
     */
    public String getDescription() {
	return "Coordinate system based upon the orientation and center of the Galaxy";
    }
    
    /**
     * Return the rotation associated with the coordinate system.
     */
      
    public Rotater getRotater() {
	double[] poles = new double[] {122.931918, 27.128251, 192.859481};
	return new Rotater("ZYZ", Math.toRadians(poles[2]),
			          Math.toRadians(90-poles[1]),
			          Math.toRadians(180-poles[0]));
    }
}
