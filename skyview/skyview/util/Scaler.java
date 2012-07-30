package skyview.util;

import nom.tam.util.ArrayFuncs;

/** scale an object.
 */
public abstract class Scaler {
    
    private boolean useDefault    = true;
    private double  minVal;
    private double  maxVal;
    
    private int minOutput = 0;
    private int maxOutput = 255;
    
    /** Provide default scaler.
     */
    public Scaler() {
    }
    
    /** Provide a scaler with a specified scaling
     *  range to a specified range of bytes.
     */
    public Scaler(double minVal, double maxVal,
			int minOutput, int maxOutput) {
	
	this.useDefault = false;
	this.minVal     = minVal;
	this.maxVal     = maxVal;
	this.minOutput  = minOutput;
	this.maxOutput  = maxOutput;
    }
    
    protected boolean getUseDefault() {
	return useDefault;
    }
    
    protected double getMinVal() {
	return minVal;
    }
    
    protected double getMaxVal() {
	return maxVal;
    }
    
    protected int getMinOutput() {
	return minOutput;
    }
    
    protected int getMaxOutput() {
	return maxOutput;
    }
    
    protected void setMinVal(double minVal) {
	this.minVal = minVal;
    }
    
    protected void setMaxVal(double maxVal) {
	this.maxVal = maxVal;
    }
    
    /** scale an array assumed to be a double array of arbitrary
      * dimensionality.
      */
    public Object scaleArray(Object array) {
	
	int[] dims = ArrayFuncs.getDimensions(array);
	double[] c = (double[]) ArrayFuncs.flatten(array);

        //--- flatten just returns the array if no flattening needed
        //--- clone it before scale so original values are not changed
        if (c == array) {
            c = (double[]) ((double[]) array).clone();
        }
	
	int    len  = c.length;
	byte[] out1 = new byte[len];
	
	

	if (useDefault) {
	    setMinMax(c);
	}
	
	// Handle special circumstances that will vary by the
	// particular class.
	prepareScaling(c);
	
	for (int i=0; i<len; i += 1) {
	    if (c[i] < minVal) {
		out1[i] = (byte) minOutput;
	    } else if (c[i] > maxVal) {
		out1[i] = (byte) maxOutput;
	    } else {
		out1[i] = scale(c[i]);
	    }
	}

	return ArrayFuncs.curl(out1,dims);
    }
    
    protected void setMinMax(double[] c) {
	if (c.length == 0) {
	    return;
	}
	minVal = c[0];
	maxVal = c[0];
	
	for (int i=0; i<c.length; i += 1) {
	    if (c[i] > maxVal) {
	        maxVal = c[i];
	    } else if (c[i] < minVal) {
		minVal = c[i];
	    }
	}
    }
	
    /** Get ready for scaling in this particular instance. */
    protected abstract void prepareScaling(double[] array);
    
    /** scale a single number. */
    protected abstract byte scale(double val);
}
		      
	    
	    
	
