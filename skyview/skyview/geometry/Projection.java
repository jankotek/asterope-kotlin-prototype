package skyview.geometry;

import static org.apache.commons.math3.util.FastMath.*;

/** This class implements projection algorithms to/from a projection
  * plane and the unit sphere.  Data on the unit sphere is normally
  * represented as a unit-three vector.  Data in a projection
  * plane is normally represented as a two-ple.  Note that the projection
  * is usually broken into two pieces: a transformation to a convenient
  * location on the celestial sphere (e.g., for a TAN projection,
  * the unit vectors are rotated so that the reference pixel is at the pole),
  * and a functional transformation from the sphere to the plane.
  * The project and deproject functions address
  * this later element, while the rotation needed is encoded in
  * in the eulerRotationMatrix.
  */
public class Projection {
 
    /** Unit vector for standard reference point for this
      * projection.  I.e., we need to rotate to this
      * point to use the projection algorithms.
      * For most azimuthal projections this is the North pole,
      * but it can be the coordinate origin for other projections
      * and can in principl be anything...
      */
    private double[]		refProj= new double[]{0,PI/2};
    private boolean             refSet = false;
    
    private boolean             fixedProjection = false;
     
    private Rotater		rotation;
    private Projecter		proj;
    private Distorter           dist = null;
    
    private static java.util.HashMap<String, double[]> fixedPoints =
      new java.util.HashMap<String, double[]>();
    
    static {
	fixedPoints.put("Car", new double[]{0,0});
	fixedPoints.put("Ait", new double[]{0,0});
	fixedPoints.put("Csc", new double[]{0,0});
	fixedPoints.put("Mer", new double[]{0,0});
	fixedPoints.put("Sfl", new double[]{0,0});
	fixedPoints.put("Toa", new double[]{0,90});
	fixedPoints.put("Hpx", new double[]{0,135});
	fixedPoints.put("Tea", new double[]{0,90});
    };
    
    // Set the reference point for (normally fixed) projection.
    public void setReference(double lon, double lat) {
	if (lon != refProj[0]  || lat != refProj[1]) {
	    
	    // We need to rotate between the desired reference
	    // and the standard position.
            // Rotate the standard position to the origin.
            Rotater r1 = new Rotater("ZY", -refProj[0], refProj[1], 0);
            Rotater r2 = new Rotater("ZY", -lon,        lat,        0);
	    setRotater(r1.add(r2.inverse()));
	    
	    refProj[0] = lon;
	    refProj[1] = lat;

	
	}
	refSet     = true;
    }
    
    /** This static method returns the location of the
     *  default projection center for fixed point projections.
     *  It returns a null if the projection is not
     *  normally used as a fixed point projection where
     *  the projection is expanded around some fixed point
     *  on the sphere regardless of the location of
     *  the image data.
     *  @param proj  The three letter string denoting the projection.
     *  @return The fixed point for the projection or null
     *          if not a fixed point projection.
     */
    public static double[] fixedPoint(String proj) {
	double[] ref = fixedPoints.get(proj);
	return ref;
    }
    
    /** Get the rotation that needs to be performed before the rotation. */
    public  Rotater getRotater() {
	return rotation;
    }
    
    /** Update the Rotater...*/
    public void setRotater(Rotater rot) {
	rotation = rot;
    }
    
    /** Get the projection algorithm associated with this rotation. */
    public Projecter getProjecter() {
	return proj;
    }
    
    /** Get any distortion in the plane associated with this projection. */
    public Distorter getDistorter() {
	return dist;
    }
    
    protected void setDistorter(Distorter dist) {
	this.dist = dist;
    }
    
    /* Is there a special location for the reference pixel? Other than the pole. */
    protected double[] specialReference() {
	return null;
    }
    
    /** Get the correct projection */
    public Projection(String type) throws TransformationException {
	
	this.refProj = fixedPoint(type).clone();
	if (this.refProj == null) {
	    throw new TransformationException("Invalid non-parametrized projection:"+type);
	}
	String projClass   = "skyview.geometry.projecter."+type+"Projecter";
	fixedProjection    = true;   
	
	
        this.proj =  (Projecter) skyview.util.Utilities.newInstance(type, "skyview.geometry.projecter");
	if (this.proj == null) {
	    throw new TransformationException("Error creating non-parametrized projection:"+type);
	}
	this.rotation = null;
    }
    
    /** Is this a fixed point projection? */
    public boolean isFixedProjection() {
	return fixedProjection;
    }
    
    /** Get the current reference position */
    public double[] getReferencePoint() {
	return refProj;
    }
    
    /** Create the specified projection.
     *  @param	type	   The three character string defining
     *                     the projection.
     *  @param  reference  The reference point for the projection (as a coordinate pair)
     *
     *  @throw  ProjectionException when the requested projection
     *          cannot be found or does not have an appropriate constructor.
     */
	
    public Projection (String type, double[] reference) 
      throws TransformationException {
	 
	String projClass    = "skyview.geometry.projecter."+type+"Projecter";
	
        this.proj =  (Projecter) skyview.util.Utilities.newInstance(type, "skyview.geometry.projecter");
	if (this.proj == null) {
	    throw new TransformationException("Cannot create parametrized projection:"+type+"\n");
	}
	  
	// We need to rotate the reference pixel to the pole.
//	rotation = new Rotater("ZYZ", PI+reference[0], -(PI/2 - reference[1]), PI/2);
	rotation = new Rotater("ZYZ", reference[0],  -reference[1]+PI/2, PI/2);
	if (specialReference() != null) {
	    double[] spec = specialReference();
	    rotation = rotation.add(new Rotater("ZYZ", spec[0], spec[1], spec[2]));
	}
    }
}
