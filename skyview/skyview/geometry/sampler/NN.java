package skyview.geometry.sampler;

/** This class implements a nearest neighbor sampling */

public class NN extends skyview.geometry.Sampler {
    
    private int testCounter = 0;
    
    public String getName() {
	return "NNSampler";
    }
    
    public String getDescription() {
	return "Sample using the nearest input pixel value";
    }
    
    
    private double[] out = new double[2];
    
    private int count = -10;
    
    /** Sample at a specified pixel */
    public void sample(int pix) {
	
//	((skyview.geometry.Converter)trans).debug(true);
	double[] in = outImage.getCenter(pix);
	trans.transform(in, out);
	
//	System.out.println("NNSampler("+testCounter+") "+in[0]+","+in[1]+"   -->  "+out[0]+","+out[1]);
	testCounter += 1;
	
	// Remember that the pixel value is assumed
	// to be at the center of the pixel not the corner.
	int x = (int) out[0];
	int y = (int) out[1];
	
	if (x < 0 || x >= inWidth || y < 0 || y >= inHeight) {
//	    System.err.println("NNSampler: No data");
	    return;
	} else {
	    for (int k=0; k < inDepth; k += 1) {
		double val = inImage.getData(x+inWidth*y+k*inWidth*inHeight);
		outImage.setData(pix+k*outWidth*outHeight, val);
//	        System.out.println("NNSampler("+k+")=  "+val);
	    }
	}
    }
    
}
