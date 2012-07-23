package skyview.util;


import static org.apache.commons.math3.util.FastMath.*;

/** Scale an object linearly.
 */
public class SqrtScaler extends Scaler {
    
    private boolean scaleNegative = false;
    private double  smallestPositive = -1;
    private double  scale;
    private double  min;
    
    /** Provide default scaler that positive values will
     *  scale the array  as the square root between 0-255.
     */
    public SqrtScaler() {
    }
    
    /** Provide a scaler with a specified scaling
     *  range to a specified range of bytes.
     */
    public SqrtScaler(double  minVal,    double maxVal,
		     int     minOutput, int    maxOutput,
		     boolean scaleNegative) {
	
	super(minVal, maxVal, minOutput, maxOutput);
	this.scaleNegative = scaleNegative;
    }
    
    
    /** Prepare to scale the data
     *  @param c	Array to be scaled.
     */
    protected void prepareScaling(double[] c) {
	
	if (scaleNegative && getMinVal() < 0) {
	    double delta = getMinVal();
	    for (int i=0; i < c.length; i += 1) {
		c[i] += delta;
	    }
	    setMinVal(getMinVal()+delta);
	    setMaxVal(getMaxVal()+delta);
	}
	
	double smallestPositive = 1.e300;
	if (getMinVal() >= 0) {
	    smallestPositive = getMinVal();
	} else if (getMaxVal() >= 0) {
	    for (int i=0; i<c.length; i += 1) {
		if (c[i] >= 0 && c[i] < smallestPositive) {
		    smallestPositive = c[i];
		}
	    }
	}
	min = smallestPositive;
	if (min < getMaxVal()) {
	    scale = (getMaxOutput()-getMinOutput())/(sqrt(getMaxVal() - sqrt(smallestPositive)));
	}
    }
	
    
    /** Scale an value.
     *  @param val The value to be scaled.
     */
    protected byte scale(double val) {
	if (val <= min) {
	    return (byte) getMinOutput();
	} else {
	    return (byte) ((sqrt(val)-sqrt(min))*scale + getMinOutput());
	}
    }
}
