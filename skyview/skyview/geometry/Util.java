/** This non-instantiable class defines static constants and methods
  * used in the SkyView geometry package.
  */

package skyview.geometry;

import static org.apache.commons.math3.util.FastMath.*;

public final class Util {
    
    /** This class is not instantiable. */
    private Util(){
    }
    
    /** Convert a coordinate pair to unit vectors
     *  @param ra The longitude like coordinate in radians.
     *  @param dec The latitude like coordinate in radians.
     *  @return A double[3] unit vector corresponding to the coordinates.
     */
    public static double[] unit(double ra, double dec) {
	return new double[]{cos(ra)*cos(dec), sin(ra)*cos(dec), sin(dec)};
    }
    
    /** Convert a coordinate pair to unit vectors
     *  @param coord The input coordinates
     *  @return A double[3] unit vector corresponding to the coordinates.
     */
    public static double[] unit(double[] coord) {
	return new double[]{cos(coord[0])*cos(coord[1]), 
	                    sin(coord[0])*cos(coord[1]), 
	                                  sin(coord[1])
	                   };
    }
    
    /** 
     *  Convert a coordinate pair to unit vectors.  The user supplies
     *  the array to be filled, assumed to be a vector of length 3.
     *  are created in this version.
     *  @param coord	A double[2] vector of coordinates.
     *  @param unitV    A pre-allocated double[3] unit vector.  The values
     *                  of this vector will be changed on output.
     */
    public static void  unit(double[] coord, double[] unitV) {
	unitV[0] = cos(coord[0])*cos(coord[1]);
	unitV[1] = sin(coord[0])*cos(coord[1]);
	unitV[2] =               sin(coord[1]);
    }
    
    /** 
     *  Convert a unit vector to the corresponding coordinates.
     *  @param unit	A double[3] unit vector.
     *  @return         A double[2] coordinate vector.
     */
    public static double[] coord(double[] unit) {
	
	double[] coord = new double[2];
	coord(unit, coord);
	return coord;
    }
    
    public static double[] coord(double x, double y, double z) {
	return coord(new double[]{x,y,z});
    }
    
    /** 
     * Convert a unit vector to the corresponding coordinates.
     *  @param unit	A double[3] unit vector.
     *  @param coord    A double[2] vector to hold
     *                  the output coordinates.
     */
    public static void coord( double[] unit, double coord[]) {
	
	coord[0] = atan2(unit[1], unit[0]);
	// Ensure that longitudes run from 0 to 2 PI rather than -PI to PI. 
	// 
	if (coord[0] <  0) {
	    coord[0] += 2*PI;
	}
	coord[1] = asin(unit[2]);
    }
    
    /**
     * Distance between two points on a unit sphere.
     */
    public static double sphdist(double lon1, double lat1, double lon2, double lat2) {
	double dlon = lon2-lon1;
	double dlat = lat2-lat1;
       
	double sindlat2 = sin(dlat/2);
	double sindlon2 = sin(dlon/2);
	double a = sindlat2*sindlat2 + cos(lat1)*cos(lat2)*sindlon2*sindlon2;
	if (a < 0) {
	    a = 0;
	} else if (a > 1) { 
	    a = 1;
	}
	double dist =  2*atan2(sqrt(a), sqrt(1-a));
	return dist;
    }
    
    /**
     * Distance between two points on a unit sphere with angles in degres.
     */
    public static double sphdistDeg(double lon1, double lat1, double lon2, double lat2) {
	return toDegrees(sphdist(toRadians(lon1), toRadians(lat1), 
				 toRadians(lon2), toRadians(lat2)));
    }
}
