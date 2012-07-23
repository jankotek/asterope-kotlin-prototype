package skyview.geometry;

/** The Transformer class is the superclass
 *  for a variety of classes that transform positions
 *  represented in one frame to another.  The subclasses
 *  of Transformer include:
 * <ul>
 *   <li> Projecter 3/2:       Transform celestial coordinates to a projection plane.
 *   <li> Deprojecter 2/3:     Transform coordinates in a projection plane to the celestial sphere.
 *   <li> Rotater 3/3:         Rotate coordinates in the celestial sphere
 *   <li> SphereDistorter 3/3: Non-linear transformations in the celestial sphere.
 *   <li> Distorter 2/2:       Non-linear transformations in the projection plane
 *   <li> Scaler 2/2:          Affine transformations in the projection plane
 *   <li> Converter:           Apply a series of conversions in turn.
 * The numbers after the type indicate the dimensionality of the input/output.
 */

public abstract class Transformer implements skyview.Component {
    
    
    /** Temporaries that may be used in the conversion.
     *  The use of these temporaries makes the Trasnformer classes non Thread safe.
     *  These temporaries are used rather than creating new arrays for
     *  multiple invocations.
     */
    private double[] t2 = new double[2];
    private double[] t3 = new double[3];
    
    /** Get the dimensionality of the output vectors.
     */
    protected abstract int getOutputDimension();
    
    /** Get the dimensionality of the input vectors.
     */
    protected abstract int getInputDimension();
    
    /** Convert a single point.  This method creates a new
     *  object and is not recommended when high throughput is needed.
     *  @param in  An array giving the input vector.
     *  @return An array giving the transformed vector.
     *          For projections and deprojections this will have
     *          a different dimension 
     */
    public double[] transform(double[] in) {
	int odim = getOutputDimension();
	if (odim == 0) {
	    // Identity transformation.
	    return in;
	} else {
	    double[] out = new double[odim];
	    transform(in, out);
	    return out;
	}
    }
    
    /** Get the inverse of the transformation. If the order
     *  matters, then the inverse is to be applied after the original
     *  transformation.  This is primarily an issue with Converters.
     */
    public abstract Transformer inverse() throws TransformationException;
    
    /** Convert a single point where the output vector is supplied.
     *  @param in   The input vector.
     *  @param out  The output vector, it may be the same as the input
     *              vector if the dimensionalities are the same.  All
     *              transformers are expected to work with aliased inputs and output.
     */
    public abstract void transform(double[] in, double[] out);
    
    /** Are these two transformations, inverses of each other?  This
      * method is used to optimize a series of transformations where
      * transformations.
      */
    public abstract boolean isInverse(Transformer trans);
    
    
    /** Convert an array of points where the output vectors are supplied.
     *  The vectors should have dimensionality [2][n] or [3][n].  The first
     *  dimension gives the index within the vector while the second gives
     *  which vector is being processed.  This means that the user needs
     *  to create only a few objects (3 or 4) rather than of order n objects
     *  for each array.  In practice this seems to speed up code by a factor
     *  of 4. (JDK1.5).  
     *  @param in A set of positions to be transformed.  The first dimension should
     *         be consistent with  getInputDimension, while the second is the number of
     *         points to be transferred.
     *  @param out The updated positions.  The first dimension should be consistent with
     *         getOutputDimension, while the second is the number of points to be transferred.
     *         This argument may point to the same data as the input.
     */
    public void transform(double[][] in, double[][] out) throws TransformationException {
	
	if (in == null     || out == null || 
	    in.length == 0 || out.length == 0 || 
	    in[0].length != out[0].length) {
	    throw new TransformationException("Array mismatch on vector transformation");
	}
	
	double[] xin, xout;
	
	int idim = getInputDimension();
	int odim = getOutputDimension();
	if (idim == 0 && odim == 0) {
	    // Identity transformations, e.g., converters that have no elements.
	    for (int i=0; i<in.length; i += 1) {
		System.arraycopy(in[i], 0, out[i], 0, in[i].length);
	    }
	    return;
	}
	if (idim == 2) {
	    xin = t2;
	} else {
	    xin = t3;
	}
	if (odim == 2) {
	    xout = t2;
	} else {
	    xout = t3;
	}
	
	for (int i=0; i<in[0].length; i += 1) {
	    
	    // The copying into/from the temporary array is the price we pay
	    // for defining the vectors as in[2/3][n] rather than in[n][2/3].
	    // It is possible new compilers or particular user circumstances
	    // may make this a poor choice, but currently (12/04: JDK 1.5) it saves
	    // about a factor of 4 in total program throughput.
	    
	    for (int j=0; j<idim; j += 1) {
		xin[j] = in[j][i];
	    }
	    
	    transform(xin, xout);
	    
	    for (int j=0; j<odim; j += 1) {
		out[j][i] = xout[j];
	    }
	}
    }
}
	
