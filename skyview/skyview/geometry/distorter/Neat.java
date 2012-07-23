package skyview.geometry.distorter;

import skyview.geometry.Transformer;
import skyview.geometry.Distorter;

/** This class implements the NEAT radial distortion.
 */
public class Neat extends Distorter {
    
    private  double x0, y0, scale;
    
    public String getName() {
	return "NeatDistorter";
    }
    
    public String  getDescription() {
	return "Invert a radial cubic distortion (find x from y where y=x+d x^3)";
    }
    
    public Distorter inverse() {
	return new NeatInv();
    }
    
    public boolean isInverse(Transformer test) {
	try {
	    return test.inverse().inverse() == this;
	} catch (Exception e) {
	    throw new Error("Unexpected exception in NeatDistorter.isInverse:"+e);
	}
    }
    
    public Neat(double scale, double x0, double y0) {
	this.x0     = x0;
	this.y0     = y0;
	this.scale  = scale;
    }
    
    public void transform(double[] in, double[] out) {
	
	double x = in[0];
	double y = in[1];
	    
	double dx = (x-x0);
	double dy = (y-y0);
	    
	double rp = Math.sqrt(dx*dx + dy*dy);
	    
	if (rp > 0) {
	    double t, delta;
	    t     = rp + scale*rp*rp*rp;
	    int loopLimit = 0;
	    do {
	        // Note that rp = (r-scale*r*r*r)
	        // We want to find r
	        delta = rp - t*(1-scale*t*t);
	    
	        t += delta/(1-3*scale*t*t); // derivative of equation
		loopLimit += 1;
		
	    } while (Math.abs(delta) > 1.e-10  && loopLimit < 10);
	    
	    dx = dx*t/rp;
	    dy = dy*t/rp;
	}
	
	out[0] = dx + x0;
	out[1] = dy + y0;
	
    }
    
    public class NeatInv extends Distorter {
	
	public String getName() {
	    return "NeatInv";
	}
	public String getDescrition() {
	    return "Perform radial distortion y = x + d x^3";
	}
	
        public boolean isInverse(Transformer test) {
	    return test == Neat.this;
	}
	
	public Distorter inverse() {
	    return Neat.this;
	}
	
	public void transform(double[] in, double[] out) {
	    
	    double x = in[0];
	    double y = in[1];
	    
	    double dx = (x-x0);
	    double dy = (y-y0);
	    
	    double r = Math.sqrt(dx*dx + dy*dy);
	    
	    if (r > 0) {
	    
	        double rp = scale * r*r*r;
	    
	        dx = dx*rp/r;
		dy = dy*rp/r;
	        out[0] = x - dx ;
	        out[1] = y - dy ;
	    } else {
		out[0] = in[0];
		out[1] = in[1];
	    }
	    
	}
    }
   
}
	
