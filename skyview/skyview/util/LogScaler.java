package skyview.util;

/** Scale an object linearly.
 */
public class LogScaler extends Scaler {
    
    private boolean scaleNegative = false;
    private double  smallestPositive = -1;
    private double  scale;
    private double  min;
    
    /** Provide default scaler that positive values will
     *  scale the array logarithmically between 0-255.
     */
    public LogScaler() {
    }
    
    /** Provide a scaler with a specified scaling
     *  range to a specified range of bytes.
     */
    public LogScaler(double  minVal,    double maxVal,
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
	    
            //--- Find subscripts of values greater than 0
            int [] greaterThanZero = Utilities.whereInArray(c,">",0); 
       
            double delta;
	    
            if (greaterThanZero.length > 0) {

                //--- Set min value to log of first value in ">0" array
                smallestPositive = c[greaterThanZero[0]];
       
                //--- Find smallest positive value
                for (int i=0; i < greaterThanZero.length;++i) {
                    if (c[greaterThanZero[i]] < smallestPositive ) {
                        smallestPositive = c[greaterThanZero[i]];
                    }
             
                }

                //--- Get difference between actual minimum and smallest positve
                delta = smallestPositive - getMinVal();
		
	    } else {
		// If everything is negative then set shift the smallest value to .01 of the highest.
		delta = -getMinVal() + 0.01*(getMaxVal()-getMinVal());
	    }
		
            //--- increase all values by this difference  and reset the min
            //--- and max variables
            for (int i=0; i<c.length; i += 1) {
	        c[i] += delta;
            }
            setMaxVal(getMaxVal()+delta);
            setMinVal(getMinVal()+delta);
	}
	
	double smallestPositive = 1.e300;
	if (getMinVal() <= 0) {
	    for (int i=0; i<c.length; i += 1) {
		if (c[i] < smallestPositive) {
		    smallestPositive = c[i];
		}
	    }
	    
	} else {
	    smallestPositive = getMinVal();
	}
	
	this.min = smallestPositive;
	if (smallestPositive > 0 && smallestPositive < getMaxVal()) {
	    this.scale = (getMaxOutput()-getMinOutput())/(Math.log(getMaxVal() - Math.log(smallestPositive)));
	}
    }
    
    /** Scale an value.
     *  @param val The value to be scaled.
     */
    protected byte scale(double val) {
	
	if (val <= min) {
	    return (byte)getMinOutput();
	} else {
	    return (byte) ((Math.log(val)-Math.log(min))*scale + getMinOutput());
	}
    }
}
