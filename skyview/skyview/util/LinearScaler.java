package skyview.util;

/** Scale an object linearly.
 */
public class LinearScaler extends Scaler {
    
    
    
    /** Scale for the data. */
    private double scale;
    
    /** Minimum value for the data. */
    private double min;
    
    /** Minimum value for the scaled data. */
    private int    start;
    
    /*
     *  Scale the array linearly between 0-255.
     */
    public LinearScaler() {
    }
    
    /** Provide a scaler with a specified scaling
     *  range to a specified range of bytes.
     */
    public LinearScaler(double minVal, double maxVal,
			int minOutput, int maxOutput) {
	super(minVal, maxVal, minOutput,maxOutput);
    }
    
    
    /** Set up the scaling.
     *  @param The array to be scaled.
     */
    protected void prepareScaling(double[] c) {
	scale = (getMaxOutput()-getMinOutput())/(getMaxVal()-getMinVal());
	min   = getMinVal();
	start = getMinOutput();
    }
    
    /** Scale a single value
     *  @param val The value to be scaled.
     *  @return    The scaled value.
     */
    protected final byte scale(double val) {
	return (byte) ((val-min)*scale + start);
    }
}
		      
	    
	    
	
