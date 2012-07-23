package skyview.geometry.sampler;


import static org.apache.commons.math3.util.FastMath.*;

/** This class implements a linear interpolation sampling
  * scheme.
  */
public class LI extends skyview.geometry.Sampler {
    
    public String getName() {
	return "LISampler";
    }
    
    public String getDescription() {
	return "Sample using a bi-linear interpolation";
    }
    
    private double[] out =  new double[2];
 
    /** Sample at a specified pixel */
    public void sample(int pix) {
	
	
	double[] in = outImage.getCenter(pix);
	trans.transform(in, out);
	
	// The values of the pixels are assumed to be
	// at the center of the pixel.  Thus we cannot
	// interpolate past outermost half-pixel edge of the
	// map.
	double x = out[0] - 0.5;
	double y = out[1] - 0.5;
	
        if (x < 0 || x > inWidth-1 || y < 0 || y > inHeight-1) {
	    return;
	} else {
	    int ix = (int) floor(x);
	    int iy = (int) floor(y);
	    double dx = x-ix;
	    double dy = y-iy;
	
	    for (int k=0; k < inDepth; k += 1) {
	        int inOffset = k*inWidth*inHeight;
		int outOffset = k*outWidth*outHeight;
	
	        outImage.setData(pix+outOffset,  
		              (1-dx)*(1-dy)* inImage.getData(ix   +  inWidth*iy     + inOffset) + 
	                        dx  *(1-dy)* inImage.getData(ix+1 +  inWidth*iy     + inOffset) +
	                      (1-dx)*  dy  * inImage.getData(ix   +  inWidth*(iy+1) + inOffset) +
	                        dx  *  dy  * inImage.getData(ix+1 +  inWidth*(iy+1) + inOffset));
	    }
	}
    }
    
}
