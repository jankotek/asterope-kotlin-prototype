
package skyview.geometry;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

import static org.apache.commons.math3.util.FastMath.*;

/** This class does 2-D scalings, rotations and linear transformations.
 */
public class Scaler extends Transformer<Vector2D, Vector2D> implements skyview.Component {
    
    /** Parameters of the transformation */
    double x0  = 0;
    double y0  = 0;
    double a00 = 1;
    double a01 = 0;
    double a10 = 0;
    double a11 = 1;
    
    /** Create a scaler where y0 = x0 + a00*x0 + a01*x1, y1 = x0+a10*x0+a11*x1.
     * @param x0  The X offset
     * @param y0  The Y offset
     * @param a00 Coefficient of the transformation matrix.
     * @param a01 Coefficient of the transformation matrix.
     * @param a10 Coefficient of the transformation matrix.
     * @param a11 Coefficient of the transformation matrix.
     */
    public Scaler(double x0, double y0, double a00, double a01, double a10, double a11) {
	
	this.x0  = x0;
	this.y0  = y0;
	this.a00 = a00;
	this.a01 = a01;
	this.a10 = a10;
	this.a11 = a11;
    }
    
    public double[] getParams()  {
	return new double[]{x0, y0, a00, a01, a10, a11
	};
    }
    
    /** What is the dimensionality of the output of a Scaler */
    protected int getOutputDimension() {
	return 2;
    }
    /** What is the dimensionality of the input to a Scaler */
    protected int getInputDimension() {
	return 2;
    }
    
    /** What is a name for this component? */
    public String getName() {
	return "Scaler";
    }
    
    /** What is a descrition for this component? */
    public String getDescription() {
	return "General Affine Transformation for 2-d points";
    }
    
    /** Scale a single point where the user supplies the output.
     * @param x The input point (should be double[2])
     * @param y The output point (should be double[2])
     */
    public void transform (double[] x, double[] y) {
	
	// Use temporary to allow x and y to be the same.
	double t;
	t    = x0 + a00*x[0] + a01*x[1];
	y[1] = y0 + a10*x[0] + a11*x[1];
	y[0] = t;
    }
    
    /** 
     * Return the inverse transformation.
     * @return A transformation object that scales in the opposite direction.
     * @throws TransformationException if the forward transformation matrix is singular.
     */
    
    public Scaler inverse() throws TransformationException {
	
	// f(X) = X0 + M X
	// g(U) = U0 + N U
	// we want g(f(x)) = x
	// Let N = Minverse = Mi
	// X = U0 + Mi(X0 + M X) = U0 + Mi X0 + Mi M X = UX + Mi X0 + X
	// So
	// U0 = - Mi X0
	// 
	double sum = abs(a00) + abs(a01) + abs(a10) + abs(a11);
	if (sum == 0) {
	    throw new TransformationException("Zero matrix in Scaler");
	}
	
	double det = a00*a11 - a01*a10;
	  
	if (det == 0) {
	    throw new TransformationException("Non-invertible transformation in Scaler");
	}
	       
	if (abs(det)/abs(sum) < 1.e-10) {
	    System.err.println("Scaler transformation is likely not invertible");
	}
	
	return new Scaler(-x0*a11/det + y0*a01/det, 
			   x0*a10/det - y0*a00/det, 
			   a11/det, -a01/det, -a10/det, a00/det);
    }
    
    /** 
     * Add a second affine transformation to this one and return the composite
     * transformation.
     * @param trans	A second transformation which is applied after the transformation
     * described in 'this'.
     * @return The combined transformation.
     */
    public Scaler add(Scaler trans) {
	
	// If the new scaler is null just return a copy of what we've got.
	if (trans == null) {
	    return new Scaler(x0, y0, a00, a01, a10, a11);
	}
	
	Scaler ret =  new Scaler(trans.x0 + trans.a00*x0 + trans.a01*y0,
			  trans.y0 + trans.a10*x0 + trans.a11*y0,
			  trans.a00*this.a00 + trans.a01*this.a10,
			  trans.a00*this.a01 + trans.a01*this.a11,
			  trans.a10*this.a00 + trans.a11*this.a10,
			  trans.a10*this.a01 + trans.a11*this.a11);
	return ret;
    }
    
    /** Is this an inverse of the current scaler? */
    public boolean isInverse(Transformer trans) {
	if (! (trans instanceof Scaler) ) {
	    return false;
	}
	Scaler sum = add((Scaler)trans);
	return sum.isUnit();
    }
    
    /** What is the scale of this transformation? This is defined as
     *  the ratio of the lengths between a unit transformation on input.
     *  and the output.
     */
    public double scale() {
	return sqrt((a00 + a01)*(a00+a01)+(a10+a11)*(a10+a11))/sqrt(2);
    }
    
    /** Interchange the X and Y axes */
    public void interchangeAxes() {
	double temp;
	
	temp = x0;
	x0   = y0;
	y0   = temp;
	
	temp = a00;
	a00  = a10;
	a10  = temp;
	
	temp = a01;
	a01  = a11;
	a11  = temp;
    }
    
    /** Show the scaler. */
    public void dump(java.io.PrintStream out) {
	out.printf("Scaler:%s\n Offset: %12.6f,%12.6f\n Matrix: %12.6f,%12.6f\n         %12.6f,%12.6f\n\n",
		   this,x0,y0,a00,a01,a10,a11);
    }
    
    
    /** Is this a unit scaler? */
    private boolean isUnit() {
	return (abs(x0) + abs(y0) + abs(a01) + abs(a10) + abs(1-a00)+abs(1-a11)) < 1.e-10;
    }
	
}
