package skyview.data;

import skyview.executive.Imager;
import skyview.survey.Image;
import skyview.executive.Settings;

import java.util.HashMap;

/** Find countours of an image.  */
public class Contourer {
    
    private double  min=.25, max=1, delta=.25;
    private int     nContour=4;
    private double  data[];
    private int     nx, ny;
    private double  minGt0 = -1;
    
    private double[] copy = null;
    
    private Image  img;
    
    /** Function to transform the input to contour regions. */
    private CFunc   func = null;
    
    public void setLimits(double min, double max, int n) {
	setLimits(min, max, n, 1);
    }
    
    /** Set up the limits for the contouring.
     *  Note that if logarithmic contours are desired, then
     *  the limits should be the common logs of the limits.
     *  E.g., if you want to do 5 intervals with the first at
     *  1 and the last at 100, then enter limits of 0 and 2.
     */
    public void setLimits(double min, double max, int n, double fraction) {
	
        this.min      = min;
        this.max      = max;
	
	if (func != null) {
	    this.min = func.func(this.min);
	    this.max = func.func(this.max);
	}
	
	if (n > 1) {
	    this.delta = (this.max-this.min)/(n-1);
	} else {
	    this.delta = 1;
	}
	
	if (fraction < 1 && fraction > 0) {
	    double dfrac = (1-fraction)/2*delta;
	    this.min   += dfrac;
	    this.max   -= dfrac;
	    this.delta *= fraction;
	}
	this.nContour = n;
    }
    
    
    public void putImage(Image img) {
	data = img.getDataArray();
	nx   = img.getWidth();
	ny   = img.getHeight();
	this.img = img;
    }
	
    /** Get the data for the contourer */
    public boolean getData(String survey) {
	
	// This inherits all of the existing settings
	// which should be enough to specify the current image
	// completely (e.g., if the user did not specify
	// the scale, then the survey settings for the survey
	// on top of which we are going an overlay should
	// be specified.
	
	String scale = Settings.get("scale");
	HashMap<String,String> oldSettings = Settings.pop();
	boolean hadScale = Settings.has("scale");
	if (!hadScale) {
	    Settings.put("scale", scale);
	}
	Imager imager  = new Imager();
	
	try {
	    System.err.println("  Contour requesting image from survey "+
		survey);
	    Settings.put("preprocessor", "null");
	    img = imager.loadAndProcessSurvey(survey);
	    String[] posts   = Settings.getArray("postprocessor");
	    String[] fposts = Settings.getArray("finalpostprocessor");
           
	    
	    // When getting an image for contouring there
	    // may be postprocessors that should not be called (e.g.,
	    // smoothers or plotting utilities).
	    // Such postprocessors should be listed in the
	    // setting finalpostprocessor.
	    for (int i=0; i<posts.length; i += 1) {
		boolean match = false;
		for (int j=0; j<fposts.length; j += 1) {
		    if (posts[i].equals(fposts[j])) {
			match = true;
			break;
		    }
		}
		if (!match) {
		   // The postprocessing here may change
		   // the value of the image.  We will
		   // make a copy of the image and
		   // restore it when we are done contouring.
		   if (copy == null) {
		       copy  = img.getDataArray().clone();
		   }
			imager.dynoProcess(posts[i]);
		}
	    }
	    
	    if (img == null) {
		return false;
	    }
	    putImage(img);
	    
	} catch (Exception e) {
	    System.err.println("  Error getting contour image for "+survey);
	    e.printStackTrace();
	    System.err.println("  Continuing without contour\n");	      
	    return false;
	    
	} finally {
	    Settings.push(oldSettings);
	}
	return true;
    }
    
    public double[] getRange() {
	if (data == null) {
	    return null;
	}
	double[] range = new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};
	
	for (int i=0; i<data.length; i += 1) {
	    if (data[i] > 0 && data[i] < minGt0) {
		minGt0 = data[i];
	    }
	    if (data[i] < range[0]) {
		range[0] = data[i];
	    }
	    if (data[i] > range[1]) {
		range[1] = data[i];
	    }
	}
	return range;
    }
    
    /** Set the transformation to be done on the input
     *  image before finding contour regions.
     *  @param funcName The string "sqrt" or "log"
     *  Note that log implies common logarithms.
     */
    public void setFunction(String funcName) {
	if (funcName == null) {
	    func = null;
	    return;
	}
	funcName = funcName.toLowerCase();
	if(funcName.equals("null")) {
	    func = null;
	} else if (funcName.equals("log")) {
	    func = new CLog();
	} else if (funcName.equals("sqrt")) {
	    func = new CSqrt();
	} else {
	    func = null;  // Linear contours
	}
    }
	    
    /** Return a map of simple contours.
     *  The contour value for each pixel in the image is computed.
     *  When two adjacent pixels are in different contour regions
     *  a contour is drawn in the pixel whose value is closest to
     *  the contour value.
     *  @return  An integer array which gives the contour that
     *           should be drawn on any given pixel.
     */
    public int[] contour() {
	
	if (Settings.has("contourSmooth")) {
	    int boxWidth=3;
	    int boxHeight=3;
	    String[] cs = Settings.getArray("contourSmooth");
	    if (cs.length > 0 || cs[0].length() > 0) {
		try {
		    boxWidth  = Integer.parseInt(cs[0]);
		    boxHeight = boxWidth;
		    if (cs.length > 1) {
			boxHeight = Integer.parseInt(cs[1]);
		    }
		} catch (Exception e) {
		    System.err.println("  Error parsing contour smoothing:"+Settings.get("contourSmooth"));
		}
	    }
	    
	    double[] xdata = data.clone();
	    data = xdata;
	    BoxSmoother.smooth(data, nx, ny, 1, boxHeight, boxWidth);
	}
		
	
	int[] result = new int[nx*ny];
	
	// Look for contours between pixels along the horizontal
	// axes
	int[] counts = new int[nContour+1];
	
	for (int i=0; i<ny; i += 1) {
	    
	    double v0  = get(data[i*nx]);
	    int    iv0 = (int) v0;
	    counts[iv0] += 1;
	    
	    for (int j=1; j<nx; j += 1) {
		double v1 = get(data[j+i*nx]);
	        int   iv1 = (int) v1;
		counts[iv1] += 1;
		
		int mn = iv0;
		int mx = iv1;
		if (iv0 > iv1) {
		    mn = iv1;
		    mx = iv0;
		}
		
		if (iv0 != iv1) {
			  
		    for (int line=mn+1; line <= mx; line += 1) {
			
			if ( Math.abs(line-v0) < Math.abs(line-v1) ) {
			    result[j-1+i*nx] = line;
			} else {
			    result[j+i*nx]  = line;
			}
		    }
		}
		v0  = v1;
		iv0 = iv1;
	    }
	}
	
	// Look for contours between pixels along the vertical axes.
	for (int j=0; j<nx; j += 1) {
	    double v0  = get(data[j]);
	    int    iv0 = (int) v0;
	    for (int i=1; i<ny; i += 1) {
		double v1 = get(data[j+i*nx]);
	        int   iv1 = (int) v1;
		if (iv0 != iv1) {
		    
		    int mn = iv0;
		    int mx = iv1;
		    if (iv0 > iv1) {
		        mn = iv1;
		        mx = iv0;
		    }
		    
		    for (int line=mn+1; line<=mx; line += 1) {
			
			if ( Math.abs(line-v0) < Math.abs(line-v1) ) {
			    result[j+(i-1)*nx] = line;
			} else {
			    result[j+i*nx]  = line;
			}
		    }
		}
		v0  = v1;
		iv0 = iv1;
	    }
	}
	
	if (!Settings.has("NoContourPrint")) {
	    double basis = this.min;
	    double val0  = basis;
	    double val1  = -1;
	    if (func != null) {
		val0 = func.ifunc(basis);
	    }
	    System.err.println("  Contour histogram");
	    System.err.printf("  %8d counts below contour 1 (%.3g) %n", counts[0], val0);
	    for (int i=1; i<nContour; i += 1) {
		basis += delta;
		val1   = basis;
		if (func != null) {
		    val1 = func.ifunc(basis);
		}
		System.err.printf("  %8d counts between contours %d (%.3g) and %d (%.3g) %n", counts[i],  i, val0, i+1, val1);
		val0 = val1;
	    }
	    System.err.printf("  %8d counts above contour %d (%.3g)%n", counts[nContour], nContour, val1 );
	}
	
	// Revert image back to state before postprocessing of this image
	// in case this image is used later on as a primary image (or perhaps in
	// some other postprocessor)
	if (copy != null) {
	    img.setDataArray(copy);
	}
	
	return result;
    }
    
    /** This function returns the contour value for a given
     *  input.  The contour value is a real number 0.5<=x<=n+0.5
     *  where n is the number of contours to be drawn.  Contours
     *  are to be drawn where this function has values 1..n
     */
    private double get(double input) {
	
	if (func != null) {
	    input = func.func(input);
	}
	double val =  (input-min)/delta + 1;
	if (val < 0.5) {
	    val = 0.5;
	}
	if (val > nContour+.5) {
	    val = nContour+.5;
	}
	return val;
    }
    
    private interface CFunc {
	public abstract double func(double input);
	public abstract double ifunc(double input);
    }
    
    private class CLog implements CFunc {
	public double func(double input) {
	    if (input <= 0) {
		if (minGt0 > 0) {
		    return Math.log10(minGt0);
		} else {
		    return -10;
		}
	    } else {
		return Math.log10(input);
	    }
	}
	public double ifunc(double input) {
	    return Math.pow(10., input);
	}
    }
    
    public class CSqrt implements CFunc {
	public double func(double input) {
	    if (input < 0) {
		return 0;
	    } else {
		return Math.sqrt(input);
	    }
	}
	public double ifunc(double input) {
	    return input*input;
	}
    }
}
