package skyview.util;

/** Scale an object linearly.
 */
public class HistScaler extends Scaler {
    
    private int scale;
    private double[] array;    
    private int start;
    
    /** Provide default scaler that positive values will
     *  scale the array logarithmically between 0-255.
     */
    public HistScaler() {
    }
    
    /** Provide a scaler with a specified scaling
     *  range to a specified range of bytes.
     */
    public HistScaler(double minVal, double maxVal,
			int minOutput, int maxOutput) {
	
	super(minVal,maxVal,minOutput,maxOutput);
    }
    
    
    protected void setMinMax(double[] old) {
	array = new double[old.length];
	java.util.Arrays.sort(array);
    }
    
    protected void prepareScaling(double[] c) {
	scale = getMaxOutput() - getMinOutput();
	start = getMinOutput();
    }
    
    protected byte scale(double val) {
	int index = java.util.Arrays.binarySearch(array, val);
	return (byte) (index*scale/array.length + start);
    }
}
