package skyview.geometry;

import static org.apache.commons.math3.util.FastMath.*;

/*
 * This class defines a rotater operater that may
 * be used to rotate vectors in 3-D space.
 */
public class Rotater extends Transformer implements skyview.Component {
    
    
    /** The matrix coefficients. */
    private double[][] matrix;
    
    /** The transpose coefficients. */
    private Rotater transRot;
    
    /** Temporary for when data is aliased.*/
    private double[] temp = new double[3];
    
    /** Create a matrix from input data. 
     *  Each row should have the same number
     *  of elements but this is not checked.
     *  The user may enter a matrix that
     *  is not a rotation matrix.
     */
    public Rotater(double[][] vectors)
      throws TransformationException {
	  
	
	if (vectors.length != 3) {
	    throw new TransformationException("Rotation not 3x3 matrix");
	}
	
	matrix = (double[][]) vectors.clone();
       	
	for (int i=0; i<matrix.length; i += 1) {
	    if (vectors[i].length != 3) {
	        throw new TransformationException("Rotation not 3x3 matrix");
	    }
	    matrix[i] = (double[]) vectors[i].clone();
	}
    }
    
    /** Get the input dimension to a Rotater */
    protected int getInputDimension() {
	return 3;
    }
    /** Get the output dimension to a Rotater */
    protected int getOutputDimension() {
	return 3;
    }
    
    /** Get the name of the component */
    public String getName() {
	return "Rotater";
    }
    
    /** Get a description of the component */
    public String getDescription() {
	return "An object that rotates 3-d vectors in space.";
    }
    
    /** Return the double coefficients for the matrix */
    public double[][] getMatrix() {
	return matrix;
    }
    
    /** Get the transpose of the Matrix.  For rotation
     *  matrices, the transpose is the inverse.  This
     *  uses a create-on-demand protocol which creates
     *  the transpose matrix on the first transpose call
     *  and simply returns the reference in later calls.
     */
    public Rotater transpose() {
	
	if (transRot == null) {
	    
	    double[][] trans = new double[3][3];
	
	    for (int i=0; i<3; i += 1) {
	        for (int j=0; j<3; j += 1) {
		    trans[i][j] = matrix[j][i];
		}
	    }
	    try {
	        transRot = new Rotater(trans);
	    } catch(Exception e) {
		System.err.println("Should be 3x3 matrix, but transpose failed?");
	    }
	}
	return transRot;
    }
    
    /** This isn't really right... We should check this is a rotation matrix better! */
    public Rotater inverse() {
	return transpose();
    }
    
    /** Add an additional rotation to the current rotation.
     *  The current rotation is applied first, and then the
     *  additional rotation.  This is equivalent to multiply
     *  the old matrix by the new matrix with new matrix on the left.
     */
    public Rotater add(Rotater r) {
	
	// If the new rotation is null, just return a copy of what we have.
	if (r == null) {
	    try {
	        return new Rotater(this.matrix);
	    } catch(Exception e) {
		// Shouldn't get here since this is just a clone...
		throw new Error("Unexpected error:"+e);
	    }
	}
	
        /** Multiply a vector by the matrix.
         *  Arguments may not be aliases for the same array for
         *  maximum efficiency.
         *  @param v The vector to be multiplied.
         *  @param r The result vector. 
         */
	double[][] a = this.matrix;
	double[][] b = r.matrix;
	
	double[][] result = new double[a.length][b[0].length];
	
	for (int i=0; i<3; i += 1) {
	    for (int j=0; j<3; j += 1) {
		for (int k=0; k < b.length; k += 1) {
		    result[i][j] += b[i][k]*a[k][j];
		}
	    }
	}
	Rotater res = null;
	try {
	    res = new Rotater(result);
	} catch (Exception e) {
	    System.err.println("Add resulted in non 3x3 matrix!");
	}
	return res;
	
    }
    
    
    /** Multiple a vector by the matrix.*/
    public void  transform(double[] in, double[] out) {
	
	// Handle aliased arguments.
	if (in == out) {
	    System.arraycopy(in,0,temp,0,in.length);
	    in = temp;
	}
	    
	
	for (int i=0; i<in.length; i += 1) {
	    out[i] = 0;
	    for (int j=0; j< matrix[0].length; j += 1) {
		out[i] += matrix[i][j]*in[j];
	    }
	}
    }
    
    /**
     *  Form a rotation from the Euler angles - three successive
     *  rotations about specified Cartesian axes
     *  <p>
     *  A rotation is positive when the reference frame rotates
     *  anticlockwise as seen looking towards the origin from the
     *  positive region of the specified axis.
     *  <p>
     *  The characters of ORDER define which axes the three successive
     *  rotations are about.  A typical value is 'ZXZ', indicating that
     *  RMAT is to become the direction cosine matrix corresponding to
     *  rotations of the reference frame through PHI radians about the
     *  old Z-axis, followed by THETA radians about the resulting X-axis,
     *  then PSI radians about the resulting Z-axis.
     *  <p>
     *  The axis names can be any of the following, in any order or
     *  combination:  X, Y, Z, uppercase or lowercase, 1, 2, 3.  Normal
     *  axis labelling/numbering conventions apply;  the xyz (=123)
     *  triad is right-handed.  Thus, the 'ZXZ' example given above
     *  could be written 'zxz' or '313' (or even 'ZxZ' or '3xZ').  ORDER
     *  is terminated by length or by the first unrecognized character.
     *  <p>
     *  Fewer than three rotations are acceptable, in which case the later
     *  angle arguments are ignored.  If all rotations are zero, the
     *  identity matrix is produced.
     *  <p>
     *  @author Adapted from P.T.Wallace's   SLA routine Deuler
     * 
     *  @param 		order   specifies about which axes the rotations occur
     *  @param  	phi     1st rotation (radians)
     *  @param          theta   2nd rotation (   "   )
     *  @param          psi     3rd rotation (   "   )
     *
     *  @return         The corresponding rotation matrix.
     */
    public Rotater(String order, double phi, double theta, double psi) {
	
      matrix = new double[3][3];
      double[][] rotn   = new double[3][3];
      char axis;



//    Initialize result matrix
      for (int j=0; j<3; j += 1) {
	  for (int i=0; i<3; i += 1) {
	      if (i != j) {
		  matrix[i][j] = 0;
	      } else {
		  matrix[i][j] = 1;
	      }
	  }
      }

//    Establish length of axis string
      int l = order.length();

//    Look at each character of axis string until finished
      for (int n=0; n < 3 && n < l; n += 1) {

//          Initialize rotation matrix for the current rotation
            for (int j=0; j<3; j += 1) {
	        for (int i=0; i<3; i += 1) {
	            if (i != j) {
		      rotn[i][j] = 0;
	            } else {
		        rotn[i][j] = 1;
	            }
		    
	        }
            }

//          Pick up the appropriate Euler angle and take sine & cosine
            double angle;
            if (n == 0) {
               angle = phi;
            } else if (n == 1) {
               angle = theta;
            } else {
               angle = psi;
	    }
            double s = sin(angle);
            double c = cos(angle);

//          Identify the axis
            axis = order.charAt(n);
            if (axis == 'x' ||
               axis == 'X'  ||
               axis == '1') {

//             Matrix for x-rotation
               rotn[1][1] =  c;
               rotn[1][2] =  s;
               rotn[2][1] = -s;
               rotn[2][2] =  c;
	    } else if (axis == 'y' ||
                       axis == 'Y' ||
                       axis == '2') {

//             Matrix for y-rotation
               rotn[0][0] =  c;
               rotn[0][2] = -s;
               rotn[2][0] =  s;
               rotn[2][2] =  c;

            } else if (axis == 'z' ||
                       axis == 'Z' ||
                       axis == '3') {

//             Matrix for z-rotation
               rotn[0][0] =  c;
               rotn[0][1] =  s;
               rotn[1][0] = -s;
               rotn[1][1] =  c;

            }

//          Apply the current rotation (matrix ROTN x matrix RESULT)
            double[][] wm = new double[3][3];
		
            for (int i=0; i<3; i += 1) {
	       for (int j=0; j<3; j += 1) {
		   
                  double w = 0e0;
                  for (int k=0; k<3; k += 1) {
                     w += rotn[i][k]*matrix[k][j];
                  }
                  wm[i][j] = w;
	       }
	    }
	    
	    for (int j=0; j<3; j += 1) {
	       for (int i=0; i<3; i += 1) {
                  matrix[i][j] = wm[i][j];
	       }
	    }
        }
    }
    
    /** Is this the inverse rotation? */
    public boolean isInverse(Transformer trans) {
	if (! (trans instanceof Rotater) ) {
	    return false;
	}
	
	Rotater rx = (Rotater) trans;
	
//	double[][] tx = ((Rotater)trans).matrix;
	Rotater sum = add((Rotater) trans);
	return sum.isUnit();
    }
    
    /** Is this the unit rotation? */
    private boolean isUnit() {
	
	double delta = abs(1-matrix[0][0]) + abs(1-matrix[1][1]) + abs(1-matrix[2][2]) +
		       abs(matrix[0][1]) + abs(matrix[0][2]) + abs(matrix[1][0]) + abs(matrix[1][2]) +
		       abs(matrix[2][0]) + abs(matrix[2][1]);
	return delta < 1.e-10;
    }
    
    /** Debug output */
    public void printOut() {
	System.err.println("\nRotation:"+this+"\n"+matrix[0][0]+" "+matrix[0][1]+" "+matrix[0][2]+"\n"+
			   "         "+matrix[1][0]+" "+matrix[1][1]+" "+matrix[1][2]+"\n"+
			   "         "+matrix[2][0]+" "+matrix[2][1]+" "+matrix[2][2]+"\n");
										
    }
					
}
