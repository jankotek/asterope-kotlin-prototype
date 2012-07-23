package skyview.geometry.projecter;

/** This class implements the COBE CSC projection.
 *  Note that the deprojection is not an exact
 *  inverse of the projection (typical errors 4")
 *  This uses the standard sideways T arrangement
 *  for the faces.
 *  See Calabretta and Greisen (1999) for details.
 *  However, note this routine does uses the
 *  standard orientation of coordinates -- not
 *  typical astronomical orientation so the
 *  T points to the right, not to the left.
 *  The negative value of the CDELT in most
 *  astronomical images reverses the orientation.
 *  <p>
 *  This projection assumes that we are using
 *  pseudo-radians in the projection plane (as
 *  do all SkyView projection routines).  Thus the
 *  T has a height of 3PI/2 and a width of 2PI
 *  rather than the 270deg and 360 degrees
 *  in the units used in Calabretta and Greisen.
 *  <p>
 *  If other cubic projections were to be included,
 *  this projection should probably extend a CubeProjection
 *  class which would include the routines and information
 *  for handling the division of the sky into faces.
 */

import skyview.geometry.Projecter;
import skyview.geometry.Deprojecter;
import skyview.geometry.Transformer;

public final class Csc extends skyview.geometry.Projecter {
    

    /** Association of positive maximum unit vector index with face. */
    private static final int[] posIndex = new int[] {1,2,0};
    /** Association of negative maximum unit vector index with face. */
    private static final int[] negIndex = new int[] {3,4,5};
    
    /** Which index of sphere should be epsilon? */
    private static final int[] epsIndex = new int[] {1,1,0,1,0,1};
    /** Which index of sphere should be zeta? */
    private static final int[] zetIndex = new int[] {0,2,2,2,2,0};
    /** Which index of sphere should be eta? */
    private static final int[] etaIndex = new int[] {2,0,1,0,1,2};
    /** Do we flip epsilon axis? */
    private static final int[] epsSign  = new int[] { 1, 1,-1,-1, 1, 1};
    /** Do we flip zeta axis? */
    private static final int[] zetSign  = new int[] {-1, 1, 1, 1, 1, 1};
    /** Do we flip eta axis? */
    private static final int[] etaSign  = new int[] { 1, 1, 1,-1,-1,-1};
    
    
    /** Location of the center of each face relative to the 0,0 point 
     *  in face 1. Each face is assumed to be pi/2 radians square. 
     *  The faces are assumed to be in the sidways T.
     *  <pre>
     *      0
     *      1234
     *      5
     *  </pre>
     *  The orientation of the T is reversed for most astronomical
     *  images since CRPIX1 is negative.
     */
    private static double[][] faceCenter = new double[][]{
	  {0, Math.PI/2}, {0,0},            {Math.PI/2,0},
	  {Math.PI, 0},   {3*Math.PI/2, 0}, {0, -Math.PI/2}
    };

    // Projection constants.
    private static final double r_0    =  0.577350269;
    private static final double gam_s  =  1.37484847732;
    private static final double em     =  0.004869491981;
    private static final double gam    = -0.13161671474;
    private static final double ome    = -0.159596235474;
    private static final double d_0    =  0.0759196200467;
    private static final double d_1    = -0.0217762490699;
    private static final double c_00   =  0.141189631152;
    private static final double c_10   =  0.0809701286525;
    private static final double c_01   = -0.281528535557;
    private static final double c_20   = -0.178251207466;
    private static final double c_11   =  0.15384112876;
    private static final double c_02   =  0.106959469314;
    
    /** Transformation matrix used in deprojection.
     */
    private static final double[][] p = new double[][] { 
      { 
       -0.27292696, -0.02819452,  0.27058160, -0.60441560,  0.93412077, -0.63915306,  0.14381585
      },{
       -0.07629969, -0.01471565, -0.56800938,  1.50880086, -1.41601920,  0.52032238
      },{
       -0.22797056,  0.48051509,  0.30803317, -0.93678576,  0.33887446
      },{
        0.54852384, -1.74114454,  0.98938102,  0.08693841
      },{
       -0.62930065,  1.71547508, -0.83180469
      },{
        0.25795794, -0.53022337
      },{
        0.02584375,
      }
    };
    
    /** Get the name of the component */
    public String getName() {
	return "Csc";
    }
    
    /** Get a descrption of the component */
    public String getDescription() {
	return "Project from a sphere to the surface of a cube using the COBE projection.";
    }
    
    /** Get the inverse transformation */
    public Deprojecter inverse() {
	return new Csc.CscDeproj();
    }
    
    /** Is this an inverse of some other transformation? */
    public boolean isInverse(Transformer t) {
	return t.getName().equals("CscDeproj");
    }
    
    /** This map can repeat in X. */
    public double getXTiling() {
	return 2*Math.PI;
    }
    
    /** Project a point from the sphere to the plane.
     *  @param sphere a double[3] unit vector
     *  @param plane  a double[2] preallocated vector.
     */
    public final void transform(double[] sphere, double[] plane) {
	
	if (Double.isNaN(sphere[2]) ) {
	    
	    plane[0] = Double.NaN;
	    plane[1] = Double.NaN;
	    
	} else {
	    // Get the face by looking for the largest direction cosine.
	    int    face = -1;
	    double max  = 0;
	    for (int i=0; i<3; i += 1) {
		if (Math.abs(sphere[i]) > max) {
		    max = Math.abs(sphere[i]);
		    face = i;
		}
	    }
	    if (sphere[face] < 0) {
		face = negIndex[face];
	    } else {
		face = posIndex[face];
	    }
	    
	    // Get the coordinates within the face.
	    double eps = sphere[epsIndex[face]]*epsSign[face];
	    double zet = sphere[zetIndex[face]]*zetSign[face];
	    double eta = sphere[etaIndex[face]]*etaSign[face];
	    
	    
	    double alpha = eps/eta;
	    double beta  = zet/eta;
	    
	    plane[0] = f(alpha,beta) + faceCenter[face][0];
	    plane[1] = f(beta,alpha) + faceCenter[face][1];
			               
	}
    }

    /** Projection function.
     */
    private static double f(double alpha, double beta) {
       
        double alpha2 = alpha*alpha;
        double alpha4 = alpha2*alpha2;
        double beta2  = beta*beta;
        double beta4  = beta2*beta2;

        double res= alpha * gam_s + 
	        alpha2*alpha*(1-gam_s) +
	        alpha*beta2*(1-alpha2) * (gam+(em-gam)*alpha2 +
		(1-beta2)*(c_00+c_10*alpha2+c_01*beta2+ c_20*alpha4+c_11*alpha2*beta2+c_02*beta4)) +
	        alpha*alpha2*(1-alpha2) * (ome-(1-alpha2)*(d_0+d_1*alpha2));
	return Math.PI/4 * res;
    }
    
    public boolean validPosition(double[] plane) {
	double x = plane[0];
	double y = plane[1];
	double p4  = Math.PI/4;
        return super.validPosition(plane) && (
	      (x >=  p4  &&  x <= 7*p4 && y >= -p4   && y <= p4) ||
	      (x >= -p4  &&  x <= p4   && y >= -3*p4 && y <= 3*p4) );
    }
    
    public class CscDeproj extends skyview.geometry.Deprojecter {
	
	/** Get the name of the component */
	public String getName() {
	    return "CscDeproj";
	}
	
	/** Get a description of the component */
	public String getDescription() {
	    return "Transform from the surface of the plane to the celestial sphere using the COBE algorithm";
	}
	
	
	public Projecter inverse() {
	    return Csc.this;
	}
	
        /** Is this an inverse of some other transformation? */
        public boolean isInverse(Transformer t) {
	    return t.getName().equals("Csc");
        }
	
        /** Deproject a point from the plane to the sphere.
         *  @param plane a double[2] vector in the tangent plane.
         *  @param spehre a preallocated double[3] vector.
         */
        public final void transform(double[] plane, double[] sphere) {
	
	    if (Double.isNaN(plane[0])) {
	        sphere[0] = Double.NaN;
        	sphere[1] = Double.NaN;
	        sphere[2] = Double.NaN;
	    
	    } else {
	    
	        double x = plane[0];
	        double y = plane[1];
	    
	        // What face are we on?
	        int face = findFace(x,y);
	    
	        if (face < 0) {
	            sphere[0] = Double.NaN;
	            sphere[1] = Double.NaN;
	            sphere[2] = Double.NaN;
		    return;
	        }
	    
	        // Get the distance from the center of the face.
	        // Normalize within the face to -1 to 1.
	        x = (x - faceCenter[face][0])/(Math.PI/4);
	        y = (y - faceCenter[face][1])/(Math.PI/4);
	    
	        double alpha = g(x,y);
	        double beta  = g(y,x);
	    
	        // Alpha and beta are tangent coordinates for one
	        // of the tangent planes.  So we fill the unit
	        // vector with these two values and a 1 for
	        // the eta axis -- i.e., the plane is tangent to the
	        // unit sphere.
	        sphere[epsIndex[face]] = alpha*epsSign[face];
	        sphere[zetIndex[face]] = beta*zetSign[face];
	        sphere[etaIndex[face]] = 1*etaSign[face];
	    
	        // Now normalize back to a unit vector.
	        double norm = 1/Math.sqrt(alpha*alpha + beta*beta + 1);
	        for (int i=0; i<3; i += 1) {
	            sphere[i] *= norm;
	        }
	    }
        }
    

        /** Deprojection equation. */
        private final double g(double x, double y) {
	
	    double sum = 0;
	    double yp = 1;
	    for (int j=0; j<7; j += 1) {
	        double xp = 1;
	        for (int i=0; i< 7-j; i += 1) {
		    sum += p[i][j]*xp*yp;
		    xp *= x*x;
	        }
	        yp *= y*y;
	    }
	    sum = x + x*(1-x*x)*sum;
    	    return sum;
        }
     
	
    }
    
    /** Find the face corresponding to x,y.  Returns -1 if not
     *  in standard T area.
     *  Note that we assume that the a sideways T with
     *  the vertical on the left, since CRPIX1 is normally negative.
     */
    static private int findFace(double x, double y) {
	
	double p4 = Math.PI/4;
	
	if (7*p4 >= x && x > -p4) {
	    if (Math.abs(y) <= p4) {
		if (x > 5*p4) {
		    return 4;
		} else if (x > 3*p4) {
		    return 3;
		} else if (x > p4) {
		    return 2;
		} else {
		    return 1;
		}
	    }
	}
	
	if (Math.abs(x) <= p4) {
	    if (y <= 3*p4 && y >= p4) {
		return 0;
	    } else if (y <= -p4  && y >= -3*p4) { 
		return 5;
	    }
	}
	return -1;
    }

}
