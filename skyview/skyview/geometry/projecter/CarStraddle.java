package skyview.geometry.projecter;

import skyview.geometry.Projecter;
import skyview.geometry.sampler.Clip;
import static org.apache.commons.math3.util.FastMath.*;

/** Handle the Straddling of the Cartesion
 *  projection when a figure extends accross the Lon=180 line.
 */
class CarStraddle extends Straddle {
   
   Projecter proj;
   Clip myClipper;
   
   boolean doClip;
   double clipXMin, clipXMax, clipYMin, clipYMax;
   
   CarStraddle(Projecter input) {
       this.proj = input;
   }
   
   CarStraddle(Car inProj) {
       this((Projecter)inProj);
       doClip = true;
       clipXMin = -PI;
       clipXMax =  PI;
       clipYMin = -PI/2;
       clipYMax =  PI/2;
   }
   
   boolean straddle(double[][] vertices) {
   
   	boolean pos  = false;
	boolean neg  = false;
	boolean both = false;
	int n = vertices[0].length;
	boolean debug = abs(vertices[0][0]) > 179*PI/180;
	
	// First check to see if we have both positive and negative
	// x values.  If not there is no issue.
	for (int i=0; i<n; i += 1) {
	    if (vertices[0][i] > 0) {
		if (neg) {
		    both = true;
		    break;
		}
		pos = true;
	    } else if (vertices[0][i] < 0) {
		if (pos) {
		    both = true;
		    break;
		}
		neg = true;
	    }
	}
	
	if (both) {
	    double[][] tvert = new double[2][n];
	    for (int i=0; i<n; i += 1) {
		if (vertices[0][i] >= 0) {
		    tvert[0][i] = vertices[0][i];
		    tvert[1][i] = vertices[1][i];
		} else {
		    double[] shadow = proj.shadowPoint(vertices[0][i],vertices[1][i]);
		    tvert[0][i] = shadow[0];
		    tvert[1][i] = shadow[1];
		}
	    }
	    double noStraddle   = testArea(vertices);
	    double haveStraddle = testArea(tvert);
	    
	    // Retain a slight preference for not straddling.
	    return haveStraddle < 0.9*noStraddle;
	}
	return false;
    }
   
    static double testArea(double[][] inputs) {
	return Clip.convexArea(inputs[0].length, inputs[0], inputs[1]);
    }
   
    /** This addresses issues that can occur when
     *  a point is exactly on the boundary.
     *  Here the boundary is assumed symmetric in X.
     */
    void fixShadow(double x, double y, double[] shadow) {
	if ( (x > 0 && shadow[0] > 0) || (x < 0 && shadow[0] < 0)) {
	    shadow[0] = - shadow[0];
	}
    }

   
    double[][][] straddleComponents(double[][] inputs) {

   	int n = inputs[0].length;
	// We'll have two output areas.
	double[][][] areas = new double[2][2][n];
	
	for (int i=0; i<n; i += 1) {
	    double x = inputs[0][i];
	    double y = inputs[1][i];
	    double[] shad = proj.shadowPoint(x,y);
	    
	    fixShadow(x,y,shad);
	    
	    if (x < 0) {
		areas[0][0][i] = shad[0];
		areas[0][1][i] = shad[1];
		areas[1][0][i] = x;
		areas[1][1][i] = y;
	    } else {
		areas[1][0][i] = shad[0];
		areas[1][1][i] = shad[1];
		areas[0][0][i] = x;
		areas[0][1][i] = y;
	    }
	}
	
	if (doClip) {
	    if (myClipper == null) {
	        myClipper = new Clip();
	    }
	    
	    for (int i=0; i<areas.length; i += 1) {
		
		double[] xi = areas[i][0];
		double[] yi = areas[i][1];
		
		double[] xo = new double[12];
		double[] yo = new double[12];
	    
	        int np = myClipper.rectClip(n, xi, yi, xo, yo, 
			   clipXMin, clipYMin, clipXMax, clipYMax);
	        double[] xv = new double[np];
	        double[] yv = new double[np];
	        System.arraycopy(xo, 0, xv, 0, np);
	        System.arraycopy(yo, 0, yv, 0, np);
		areas[i][0] = xv;
		areas[i][1] = yv;
	    }
	}
	
	return areas;
    }
}
