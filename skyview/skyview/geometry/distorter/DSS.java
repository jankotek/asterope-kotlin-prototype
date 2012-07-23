package skyview.geometry.distorter;

import static java.lang.Math.*;
import static java.lang.Double.NaN;
import skyview.geometry.Distorter;
import skyview.geometry.Transformer;
import skyview.geometry.TransformationException;


/** The distortion object for a given DSS structure. This class is
 *  not thread safe.
 */


public class DSS extends Distorter
  implements skyview.Component 
{
    
    private double CONS2R    = toDegrees(1)*3600;
    private double COND2R    = 1.745329252e-2;
    private double TWOPI     = 2*Math.PI;
    private double TOLERANCE = 0.0000005;
    

    /* Position coefficients */
    private double xmm;
    private double ymm;
    
    private double xy;
    private double x2;
    private double y2;
    private double x2y;
    private double y2x;
    
    private double x2y2;
    private double x4y4;
    private double x3;
    private double y3;
    private double x4;
    private double y4;
    
    /* Plate parameters */
    private double  plate_ra;	        /* Right ascension of plate center */
    private double  plate_dec;	        /* Declination of plate center */
    private double  plate_scale;	/* Plate scale in arcsec/mm */
    private double  x_pixel_size;	/* X pixel_size */
    private double  y_pixel_size;	/* Y pixel_size */
    private double  ppo_coeff[];	/* pixel to plate coefficients for DSS */
    private double  x_coeff[];	        /* X coefficients for plate model */
    private double  y_coeff[];	        /* Y coefficients for plate model */
    
    public DSS(
	     double   plate_ra,
	     double   plate_dec,
             double   x_pixel_size,
	     double   y_pixel_size,
	     double   plate_scale,
	     double[] ppo_coeff,
	     double[] x_coeff,
	     double[] y_coeff
			 ) 
    {
        this.plate_ra     = plate_ra;
        this.plate_dec    = plate_dec;
        this.x_pixel_size = x_pixel_size;
        this.y_pixel_size = y_pixel_size;
	this.plate_scale  = plate_scale;
        this.ppo_coeff    = ppo_coeff;
        this.x_coeff      = x_coeff;
        this.y_coeff      = y_coeff;
    }
    
	
    private void setPosition(double xmm, double ymm) {
    	this.xmm     = xmm;
	this.ymm     = ymm;
    
        this.xy      = xmm * ymm;
        this.x2      = xmm * xmm;
        this.y2      = ymm * ymm;
        this.x2y     = x2 * ymm;
        this.y2x     = y2 * xmm;
        this.x2y2    = x2 + y2;
        this.x4y4    = x2y2 * x2y2;
        this.x3      = x2 * xmm;
        this.y3      = y2 * ymm;
        this.x4      = x2 * x2;
        this.y4      = y2 * y2;
    }

    
    public void transform(double[] x, double[] y) {
		  
        int    max_iterations = 50;
	
	// Convert to seconds.
	double xi  = x[0]*CONS2R;
	double eta = x[1]*CONS2R;
	
	/* Convert to millimeters. */
        double xmm = xi  / this.plate_scale;
        double ymm = eta / this.plate_scale;
	

	int i;
        /* Iterate by Newton's method */
	
	double ft = 0;
	double fx = 0;
	double fy = 0;
	double gt = 0;
	double gx = 0;
	double gy = 0;
	
        for (i=0; i < max_iterations; i++) {
	    
	    setPosition(xmm,ymm);
	    
	    ft = f();
            gt = g();
	    
	    
	    fx = dfdx();
	    fy = dfdy();
        
            gx = dgdx();
            gy = dgdy();
        
            double df = ft - xi;
            double dg = gt - eta;
	    
	    
            double dx = ((-df * gy) + (dg * fy)) / ((fx * gy) - (fy * gx));
            double dy = ((-dg * fx) + (df * gx)) / ((fx * gy) - (fy * gx));
	    
            xmm = xmm + dx;
            ymm = ymm + dy;
	    
            if ((abs(dx) < TOLERANCE) && (abs(dy) < TOLERANCE)) {
        	   break;
	    }
        }
	
	if (i > max_iterations) {
	    y[0] = NaN;
	    y[1] = NaN;
	} else {
	    // Convert to radians and return.
	    y[0] = xmm*this.plate_scale/CONS2R;
	    y[1] = ymm*this.plate_scale/CONS2R;
	}
    }
    
    /** Get the name of this component */
    public String getName() {
	return "DSS Distorter";
    }
    
    public String getDescription() {
	return "Transform from a fiducial projection plane to the DSS distorted projection plane.";
    }
    
    protected boolean preserves() {
	return false;
    }
    

    /** Give the corrected X coordinate for the current actual position */
    private double f() {
        return  x_coeff[0]*xmm      + x_coeff[1]*ymm +
                x_coeff[2]          + x_coeff[3]*x2 +
                x_coeff[4]*xy       + x_coeff[5]*y2 +
                x_coeff[6]*x2y2     + x_coeff[7]*x3 +
                x_coeff[8]*x2y      + x_coeff[9]*y2x +
                x_coeff[10]*y3      + x_coeff[11]*xmm*x2y2 +
                x_coeff[12]*xmm*x4y4;
    }

    /** Derivative of corrected X coordinate with respect to actual X coordinate */
    private double dfdx() {    
           /*  Derivative of X model wrt x */
        return   x_coeff[0]           + x_coeff[3]*2.0*xmm +
                 x_coeff[4]*ymm       + x_coeff[6]*2.0*xmm +
                 x_coeff[7]*3.0*x2    + x_coeff[8]*2.0*xy +
                 x_coeff[9]*y2        + x_coeff[11]*(3.0*x2+y2) +
                 x_coeff[12]*(5.0*x4 +6.0*x2*y2+y4);
    }

    /** Derivative of corrected X coordinate with respect to actual Y coordinate */
    private double dfdy() {
        
            /* Derivative of X model wrt y */
        return   x_coeff[1]           + x_coeff[4]*xmm +
                 x_coeff[5]*2.0*ymm   + x_coeff[6]*2.0*ymm +
                 x_coeff[8]*x2        + x_coeff[9]*2.0*xy +
                 x_coeff[10]*3.0*y2   + x_coeff[11]*2.0*xy +
                 x_coeff[12]*4.0*xy*x2y2;
    }
        
    /** Give the corrected Y coordinate for the currenat actual position */
    private double g() {
         return y_coeff[0]*ymm       + y_coeff[1]*xmm +
                y_coeff[2]            + y_coeff[3]*y2 +
                y_coeff[4]*xy         + y_coeff[5]*x2 +
                y_coeff[6]*x2y2       + y_coeff[7]*y3 +
                y_coeff[8]*y2x        + y_coeff[9]*x2y +
                y_coeff[10]*x3        + y_coeff[11]*ymm*x2y2 +
                y_coeff[12]*ymm*x4y4;
    }

    /** Derivative of corrected Y coordinate with respect to actual X coordinate */
    private double dgdx() {
        return   y_coeff[1]           + y_coeff[4]*ymm +
                 y_coeff[5]*2.0*xmm   + y_coeff[6]*2.0*xmm +
                 y_coeff[8]*y2       + y_coeff[9]*2.0*xy +
                 y_coeff[10]*3.0*x2  + y_coeff[11]*2.0*xy +
                 y_coeff[12]*4.0*xy*x2y2;
    }

    /** Derivative of corrected Y coordinate with respect to actual Y coordinate */
    private double dgdy() {
        return   y_coeff[0]            + y_coeff[3]*2.0*ymm +
                 y_coeff[4]*xmm        + y_coeff[6]*2.0*ymm +
                 y_coeff[7]*3.0*y2     + y_coeff[8]*2.0*xy +
                 y_coeff[9]*x2         + y_coeff[11]*(x2+3.0*y2) +
                 y_coeff[12]*(5.0*y4 + 6.0*x2*y2 + x4);
    }
    
    /** The inverse Distorter (i.e., the undistorter) uses much of the same
     *  machinery, so we generate it as a inner class of the distorter.
     */
    public Distorter inverse() {
	return new DSS.DSSInv();
    }
    
    /** Is this the inverse of another distorter? */
    public boolean isInverse(Transformer t) {
	
	try {
	    return t.inverse() == this;
	} catch(TransformationException e) {
	    return false;
	}
	
    }

    /** This inner class is the inverse of the DSS Distorter and corrects
     *  the distortion generated there.  For the DSS projection, this
     *  direction is described analytically using a polynomial expansion,
     *  while the 'forward' distortion must be done by inverting the polynomial
     *  using Newton's method.
     */
    public class DSSInv extends Distorter {
	
	/** Get the forward distorter back */
	public Distorter invert() {
	    return DSS.this;
	}
	
	/** Get the name of this component */
	public String getName() {
	    return "DSSInv";
	}
	
	/** Get a description of this component */
	public String getDescription() {
	    return "Transform from DSS distorted coordinates to the ficucial projection plane";
	}
	
	/** Get the inverse Distorter */
	public Distorter inverse() {
	    return DSS.this;
	}
     
        /** Is this the inverse of another distorter? */
        public boolean isInverse(Transformer t) {
	    return t == DSS.this;
        }
	
	/** Transform a point */
        public void transform(double[] x, double[] y) {

            // Need to convert from radians to mm
	    // 
	    // xmicron      = xradian * ["/radian] / ["/mm]         
	    // 
            double	xmm = x[0] * CONS2R / plate_scale;	
            double	ymm = x[1] * CONS2R / plate_scale;

            setPosition(xmm,ymm);
    
            //  Compute corrected coordinates XI,ETA in "

            double xi  = f();
            double eta = g();
	  
	  
            /* Convert from " to radians */
            y[0] = xi  / CONS2R;
            y[1] = eta / CONS2R;
	}	
    }
}

    
