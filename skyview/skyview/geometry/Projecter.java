package skyview.geometry;

/** This class projects a point from the celestial sphere
 *  to a projection plane.
 */
public abstract class Projecter extends Transformer {
    
    /** Get the inverse */
    public abstract Deprojecter inverse();
    
    /** What is the output dimensionality of a projecter? */
    protected int getOutputDimension() {
	return 2;
    }
    
    /** What is the input dimensionality of a projecter? */
    protected int getInputDimension() {
	return 3;
    }
    
    /** Some projections can tile the projection plane with repeated
     *  copies.  This method gives the vectors along which the tiles repeat.
     *  Note that for azimuthal like (e.g., Mercator) projections, the tiling may be possible in
     *  the longitudinal direction, but not in the latitudinal direction.
     *  Some projections (e.g., CAR) can tile in both directions (but note that the periodicity
     *  for tiling is 360 degrees in both directions in the CAR projection since the latitudes
     *  will run ... -90 -80 ...0 ... 80 90 80 ... 0 ... -80 -90 -80 ....
     *  It's also conceivable that the projection tiling is along but we do not accomodate
     *  this.
     *  @return The period in X in radians.  A value of 0 means that there is not periodicity.
     * 
     */
    public double getXTiling() {
	return 0;
    }
    
    /** The tiling period in Y 
     *  @return The tiling period in radians.  A value of 0 means that
     *  there is no period.
     */
    public double getYTiling() {
	return 0;
    }
    
    /** Is this a valid position in the projection plane for this image. This
     *  default is appropriate for all projections where the projection plane is infinite.
     */
    public boolean validPosition(double[] pos) {
	return pos != null && !Double.isNaN(pos[0]);
    }
    
    /** Are all points in the projection plane valid?
     */
    public boolean allValid() {
	return false;
    }
    
    /** Is it possible for a pixel to straddle the valid region.
     */
    public boolean straddleable() {
	return false;
    }
    
    /** Does this pixel go wrap around the standard region of the image?
     *  E.g., does it straddle the 180 deg in a Car or Ait projection?
     */
    public boolean straddle(double[][] pnts) {
	return false;
    }
    
    /** If this is a straddling pixel, then return the straddle components */
    public double[][][] straddleComponents(double[][] pnts) {
	return new double[][][]{pnts};
    }
    
    /** Return a shadowpoint for the input location.
     *  Shadowpoints are not defined for all projections.
     */
    public double[] shadowPoint(double x, double y) {
	throw new UnsupportedOperationException("No shadow points in requested projection");
    }

    /** Return the Tissot ellipse parameters for the projection.
     *  This gives the axes and position angle of an ellipse that reflects
     *  the distotortion at the given point.
     *  If there is no distortion or rotation, then the values
     *   (1,1,0)
     *  are returned.  If the major axis is aligned with latitude, then
     *  the angle (the third parameter) will be 0, or pi/2 if
     *  greater aligned with longitude.  If the projection is equal-area
     *  then the first two parameters are multiplicative inverses.
     *
     *  This implementation is a place holder which should be overriden
     *  by specific projections.  It returns a set of NaNs to indicate
     *  that the calculation has not been done.
     *
     *  @param x The X-value at which the Tissot indicatrix is to be calculated.
     *  @param y The Y-value at which the Tissot indicatrix is to be calculated.
     *  @return A three element double vector [Semimajor, semiminor, position angle]
     *  with the axes size and positional angle of the ellipse.
     *
     */

    public double[] tissot(double x, double y) {
        return new double[] {Double.NaN, Double.NaN, Double.NaN};
    }

}
