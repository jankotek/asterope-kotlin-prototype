package skyview.geometry.spheredistorter;

import skyview.geometry.SphereDistorter;
import skyview.geometry.Transformer;


import static org.apache.commons.math3.util.FastMath.*;

/** This class implements the distortion of Besselian coordinate systems.
 */
public class Besselian extends skyview.geometry.SphereDistorter {

    private static final double D2PI = 6.2831853071795864769252867665590057683943387987502;
    private static final double pmf  = 100.0 * 60 * 60 * 360 / D2PI;
      
    /** Get the inverse distorter */
    public SphereDistorter inverse() {
	return new Besselian.BesselianInverse();
    }
    
    /** Is the the inverse of another transformation */
    public boolean isInverse(Transformer t) {
	return t instanceof BesselianInverse;
    }
    
    
    
    /** These two work arrays mean that this class is not
     *  thread safe.  If multiple threads are to be used each thread
     *  needs its own coordinate system object.
     */
    private double[] v  = new double[3];
    private double[] t1 = new double[3];
    
    public String getName() {
	return "Besselian distorter";
    }
    
    public String getDescription() {
	return "A Besselian (FK4 based) distortion.  Dynamic terms are not included.";
    }
    

    public final void transform(double[] x, double[] y) {

        double[] a = new double[] { -1.62557E-6,   -0.31919E-6, -0.13843E-6};
 
        double[][] emi = new double[][] {
          { 0.9999256795,      0.0111814828,       0.0048590039},
          {-0.0111814828,      0.9999374849,      -0.0000271771},
          {-0.0048590040,     -0.0000271557,       0.9999881946}};

        //  Convert position+velocity vector to BN system */
        for (int i=0; i<3; i += 1) {
	    t1[i] = x[0]*emi[i][0] + x[1]*emi[i][1]+x[2]*emi[i][2];
        }
    
        for (int i=0; i<3; i += 1) {
	    y[i] = t1[i];
        }
    
        double rxyz = sqrt (y[0]*y[0] + y[1]*y[1] + y[2]*y[2]);

        //  Include e-terms */
        double w = 0;
        for (int i=0; i<3; i += 1) {
	    w += a[i]*y[i];
        }
    
    
        for (int i=0; i<3; i += 1) {
            t1[i]  = (1-w)*y[i] + a[i]*rxyz;
        }
   
        //  Recompute magnitude */
        rxyz =  sqrt (t1[0]*t1[0] + t1[1]*t1[1] + t1[2]*t1[2]);

        //  Apply E-terms to both position and velocity */
        for (int i=0; i<3; i += 1) {
	    w += a[i]*y[i];
        }
        for (int i=0; i<3; i += 1) {
            y[i]  = (1-w)*y[i] + a[i]*rxyz; 
        }

        // Make sure we output a unit vector!
        rxyz = sqrt (y[0]*y[0] + y[1]*y[1] + y[2]*y[2]);
    
        for (int i=0; i<3; i += 1) {
            y[i]  /= rxyz;
        }
    }
    
    /** This inner class defines the inverse distortion
     *  to the enclosing Besselian distorter.
     */
    public class BesselianInverse extends skyview.geometry.SphereDistorter {
	
	public String getName() {
	    return "Inv. "+Besselian.this.getName();
	}
	
	public SphereDistorter inverse() {
	    return Besselian.this;
	}
	
        /** Is the the inverse of another transformation */
        public boolean isInverse(Transformer t) {
	    return t instanceof Besselian;
        }
	
	public String getDescription() {
	    return Besselian.this.getDescription()+ " (inverse)";
	}
	
        /**
         * Convert coordinates from B1950 to J2000 for epoch 1950.
         */
        public final void transform(double[] x, double[] y) {
    
            //  Canonical constants
            //  vectors a and adot, and matrix m (only half of which is needed here) */

            double[]   a  = new double[]{ -1.62557E-6,  -0.31919E-6, -0.13843E-6 };
   
            double[][] em1 = new double[][] {
              {0.9999256782, -0.0111820611, -0.0048579477},
              {0.0111820610,  0.9999374784, -0.0000271765},
              {0.0048579479, -0.0000271474,  0.9999881997}};
            double[][] em2 = new double[][] {
              {-0.000551,     -0.238565,      0.435739},
              { 0.238514,     -0.002667,     -0.008541},
              {-0.435623,      0.012254,      0.002117}};


            double w = 0;
      
            for (int i=0; i<3; i += 1) {
                //  Remove e-terms
                w  += a[i]*x[i];
            }
   
            for (int i=0; i<3; i += 1) {
                t1[i] = x[i] - a[i]-w*x[i];
            }
    
            for (int i=0; i<3; i += 1) {
         	y[i] = t1[i];
            }
    
    
            for (int i=0; i<3; i += 1) {
                t1[i] = y[0]*em1[i][0] + y[1]*em1[i][1] + y[2]*em1[i][2];
	        v[i]  = y[0]*em2[i][0] + y[1]*em2[i][1] + y[2]*em2[i][2];
            }
            for (int i=0; i<3; i += 1) {
	        y[i] = t1[i];
            }

            // -50 since this is from 1950-2000
            double tdelta = -50/pmf;

            for (int i=0; i < 3; i += 1) {
	        y[i] += tdelta*v[i];
            }
    
            // Make sure we output a unit vector.
            double rxyz = sqrt (y[0]*y[0] + y[1]*y[1] + y[2]*y[2]);
    
            for (int i=0; i<3; i += 1) {
                y[i]  /= rxyz;
            }
        }
    }
}
