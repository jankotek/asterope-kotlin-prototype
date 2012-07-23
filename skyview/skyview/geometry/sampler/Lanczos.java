
package skyview.geometry.sampler;

/** This class implements a nearest neighbor sampling
  * scheme.
  */
public class Lanczos extends skyview.geometry.Sampler {
    
    
    public String getName() {
	return "Lanczos"+nLobe+" Sampler";
    }
    
    public String getDescription() {
	return "Sample using smoothly truncated sinc kernel";
    }
    
    /** The number of lobes in the window */
    private int nLobe;
    private double[] out = new double[2];
    
    private double coef, coef2;
    
    /** Weights used internally */
    private double[] xw;
    private double[] yw;
    
    /** Create a Lanczos sample of specified width sampler 
     *  The data will be set later.
     *  
     */
    public void setOrder(int n) {
	init(n);
    }
    
    /** Create a three lobe lanczos sampler as a default width.
     */
    public Lanczos() {
	init(3);
    }
    
    
    
    private void init (int n) {
	this.nLobe = n;
	this.coef  = Math.PI/n;
	this.coef2 = coef*Math.PI;
	xw = new double[2*n];
	yw = new double[2*n];
    }
    
    /** Sample a single pixel
      * @param coords  The x,y coordinates of the center of the pixel.
      * @return        The sample value.
      */
    public void sample(int pix) {
	
	double output = 0;
        double[] in = outImage.getCenter(pix);
	trans.transform(in, out);
	
	double x = out[0]-0.5;
	double y = out[1]-0.5;

        int ix = (int) Math.floor(x);
	int iy = (int) Math.floor(y);
	
	double dx = ix - x - (nLobe-1);
	double dy = iy - y - (nLobe-1);
	
	
	
	if (ix <nLobe-1 || y < nLobe-1 || ix >= inWidth-nLobe || iy >= inHeight-nLobe) {
	    return;
	    
	} else {
	    for (int xc=0; xc < 2*nLobe; xc += 1) {
		if (Math.abs(dx) < 1.e-10) {
		    xw[xc] = 1;
		} else {
		    xw[xc] = Math.sin(coef*dx)*Math.sin(Math.PI*dx)/(coef2*dx*dx);
		}
		dx += 1;
	    }
	    
	    for (int yc=0; yc < 2*nLobe; yc += 1) {
		if (Math.abs(dy) < 1.e-10) {
		    yw[yc] = 1;
		} else {
		    yw[yc] = Math.sin(coef*dy)*Math.sin(Math.PI*dy)/(coef2*dy*dy);
		}
		dy += 1;
	    }
	    int p=0,pstart=0;
	    
	    for (int k=0; k<inDepth; k += 1) {
		
		
		p  = (iy-(nLobe-1))*inWidth + ix-(nLobe-1) + k*inWidth*inHeight;
		
		for (int yc=0; yc<2*nLobe; yc += 1) {
		   
		    for (int xc=0; xc<2*nLobe; xc += 1) {
			
			output += inImage.getData(p)*xw[xc]*yw[yc];
			p += 1;
		    }
		    p += inWidth - 2*nLobe;
		}
	    }
	}
	outImage.setData(pix, output);
    }
}

