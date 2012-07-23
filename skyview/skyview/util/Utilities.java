package skyview.util;

import skyview.util.SmartIntArray;
import java.text.DecimalFormat;

/** utlity functions to use with SkyView */
public class Utilities {

    static final double pi = Math.PI;

    /** Calculates angular distance between two points on a sphere.
      * All angles are measured in degrees.  
      * @param cx1  Longitude like coordinate of first point
      * @param cy1  Latitude like coordinate of first point
      * @param cx2  Longitude like coordinate of second point
      * @param cy1  Latitude like coordinate of second point
      * @return distance between the two points in degrees
      */
    public static double angularDistance(double cx1, double cy1, 
					 double cx2, double cy2) {
      double xx1=Math.toRadians(cx1);
      double yy1=Math.toRadians(cy1);
      double xx2=Math.toRadians(cx2);
      double yy2=Math.toRadians(cy2);

      double distRad=Math.acos(Math.sin(yy1) * Math.sin(yy2) +
	            Math.cos(yy1) * Math.cos(yy2) *
                    Math.cos(xx2 - xx1));
      return Math.toDegrees(distRad); 
    }

    /** The the extremum values in an array
      * @param d An array of doubles
      * @return  An array with two elements giving the min and max of the array.
      *          Returns null if the array is not defined or has zero length.
      */
    public static double[] minMaxInArray(double[] d ) {
	
 	double currentMax; 
 	double currentMin; 
	
	if (d == null || d.length == 0) {
	    return null;
	}
	
        currentMax = d[0];
        currentMin = d[0];
	
	for (int i=1; i < d.length; ++i) {
           if (d[i] > currentMax) {
	      currentMax = d[i];
	       
	   // Can't change the min and max simultaneously
           } else if (d[i] < currentMin) {
	      currentMin = d[i];
	   }
        }
	
        return new double[]{currentMin, currentMax};
    }
    
    /** Find the maximum value in an integer array
      * 
      * @param d Array to be checked
      * 
      * @return  The maximum or the smallest integer value if
      *          d is null or of zero length.
      */
    public static int  maxInArray(int[] d ) {
	
 	int currentMax; 
	if (d == null || d.length == 0) {
	    return 2<<31;
	}
	
        currentMax = d[0];
	for (int i=1; i < d.length; ++i) {
           if (d[i] > currentMax) {
	      currentMax = d[i];
           }
        }
        return currentMax;
    }
    
    /** Find the  maximum value of a double array.
      * @param d  The array to be checked
      * @return   The maximum value or NaN if the array
      *           is undefined or of zero length
      */
    public static double  maxInArray(double[] d ) {
	
 	double currentMax; 
	
	if (d == null || d.length == 0) {
	    return Double.NaN;
	}
	
        currentMax = d[0];
	for (int i=1; i < d.length; ++i) {
           if (d[i] > currentMax) {
	      currentMax = d[i];
           }
        }
        return currentMax;
    }
    
    /** Find the minimum value of an array.
      * @param d 	The array to be checked.
      * @return 	The minimum value or NaN if the array
      *                 is null or of zero length.
      */
    public static double  minInArray(double [] d ) {
 	double currentMin; 
	if (d == null || d.length == 0) {
	    return Double.NaN;
	}
        currentMin = d[0];
	for (int i=1; i < d.length; ++i) {
           if (d[i] < currentMin) {
	      currentMin = d[i];
           }
        }
        return currentMin;
    }
    

    /** Ensure that a value is in the region -PI to PI.  This
     *  is appropriate for longitude like coordinates.
     *  @param c	The input value.
     *  @return         The value adjusted to the proper range.
     */
    public static double  adjustCoordX(double  c ) {
	
        if (c > Math.PI) {
	    c -= 2*Math.PI; 
	} else if (c < -Math.PI) { 
	    c += 2*Math.PI; 
	}
        return c;
    }

    /** Ensure that a value is in the approrpiate range from
     *  -PI/2 to PI/2.  This is appropriate for latitude like
     *  coordinates.  Note that the behavior is appropriate
     *  for going 'over' the pole.  However, this should really
     *  be called in conjunction with the longitude like coordinate.
     *  E.g., (using degrees for clarity) we'd want:
     *     <pre>(0, -91) -> (180, -89)</pre>
     *  but this only addresses the latitudes.
     * 
     *  @param c	The input value.
     *  @return         The value adjusted to the proper range.
     */
    public static double  adjustCoordY(double  c ) {

        if (c < -Math.PI/2) { 
	    c = -Math.PI - c; 
	} else if (c > Math.PI/2) { 
	    c = Math.PI - c; 
	}
        return c;
    }
    
    /** Find the indices of a double array that are NaNs.
      * @param d  		The array
      * @param searchValue 	Value searched for.
      * @return  		Array of indices where value is found
      */
    public static int [] whereNaNInArray(double [] d ) {
	
        SmartIntArray hold = new SmartIntArray(d.length);
        int count = 0;
	
        for (int ii=0;ii<d.length; ii += 1) {
             if (Double.isNaN(d[ii])) {
                hold.add(ii);
                count += 1;
             }
        } 

        if (count > 0 ) {
            return (hold.toArray());
        } else {
            return new int[0];
        }
    }
    
    
    /** Find the indices of a double array that are equal a given value.
      * @param d  		The array
      * @param searchValue 	Value searched for.
      * @return  		Array of indices where value is found
      */
    
    public static int [] whereInArray(double [] d, double searchValue ) {
	
        SmartIntArray hold = new SmartIntArray(d.length);
        int count = 0;
        for (int ii=0; ii<d.length; ii += 1) {
            if (d[ii] == searchValue) {
                hold.add(ii);
                count += 1;
            }
        } 
	
        if (count > 0 ) {
            return (hold.toArray());
        } else {
	    return new int[0];
        }
    }
    
    
    /** Find the indices of a double array that satisfy the
      * given criterion.
      * @param d  		The array
      * @param operation	Looking for equality, <, or >.
      * @param searchValue 	Value searched for.
      * @return  		Array of indices where value is found
      */
    public static int [] whereInArray(double [] d, String operation, double searchValue ) {
	
        SmartIntArray hold = new SmartIntArray(d.length);
        int count = 0;
        if (operation.equals("==")) {
	    return whereInArray(d, searchValue);
        }
	
        if (operation.equals("<")) {
            for (int ii=0; ii<d.length; ii += 1) {
                if (d[ii] < searchValue) {
                   hold.add(ii);
                   count += 1;
                }
            }
	}  else if (operation.equals(">")) {
            for (int ii=0; ii<d.length; ii += 1) {
                if (d[ii] > searchValue) {
                    hold.add(ii);
                    ++count;
                }
            }
        } else if (operation.equals("!=")) {
            for (int ii=0; ii<d.length; ii+= 1) {
                if (d[ii] != searchValue) {
                   hold.add(ii);
                   count += 1;
                }
	    }
        } else if (operation.equals(">=")) {
            for (int ii=0; ii<d.length; ii += 1) {
                if (d[ii] >= searchValue) {
                   hold.add(ii);
                   count += 1;
                }
            }
        } else if (operation.equals("<=")) {
            for (int ii=0; ii<d.length;  ii += 1) {
                if (d[ii] <= searchValue) {
                     hold.add(ii);
                     count += 1;
                }
            }
        }
	
        if (count > 0 ) {
            return (hold.toArray());
        } else {
	    return new int[0];
	}
    }


    /** Handle exponential notation.
     *  @param str	Input string
     *  @return         The string converted to a double precision value.
     */
    public static double convertNotation(String str ) {
	
       int e_loc = str.indexOf('e');
	
       double val= 
	   Double.valueOf(str.substring(0,e_loc-1).trim()).doubleValue() *
	   Math.pow(10,Double.valueOf(str.substring(e_loc+1,str.length()).
	   trim()).doubleValue());
       return val;
    }


    /** Format a sexagesimal coordinate string.
      * @param value     A double precision value which is to be
      *                  converted to a string representation.  
      *                  The user should convert
      *                  to hours prior to this call if needed.
      * @param precision A integer value giving the precision to which
      *                    the value is to be shown.
      *  
      * <dl> 
      *   <dt> &le= 0 <dd> Degrees (or hours), e.g. 24
      *   <dt> 1 <dd> Deg.f <it> e.g.</it>,  24.3
      *   <dt> 2 <dd> Deg mm <it> e.g.</it>, 24 18
      *	  <dt> 3 <dd> Deg mm.f <it> e.g.</it>, 25 18.3
      *	  <dt> 4 <dd> Deg mm ss <it> e.g.</it>,  25 18 18
      *	  <dt> 5 <dd> Deg mm ss.s <it> e.g.</it>, 25 18 18.2
      *	  <dt> 6 <dd> Deg mm ss.ss <it> e.g.</it>, 25 18 18.24
      *	  <dt> &gt;6 <dd> Deg mm ss.sss <it> e.g.</it>, 25 18 18.238
      * </dl>
      */
    public static String sexagesimal(double value, int precision) {
    
    
        int    deg, min, sec, frac;
	double fdeg, fmin, fsec, ffrac;
	long   nelem;
	
	double[] offset = {
	   0.5, 0.05, 0.5/60, 0.05/60, 0.5/3600
	};
	
	double delta;
	
	if (precision > 9) {
	    precision = 9;
	}
	
	if (precision < 0) {
	    delta = offset[0];
	    
	} else if (precision >= offset.length) {
	    delta = offset[offset.length-1] / 
	             Math.pow(10,precision-offset.length+1);
	} else {
	    delta = offset[precision];
	}

	StringBuffer str = new StringBuffer();
	
	
	if (value < 0) {
	    value = Math.abs(value);
	    str.append("-");
	}
	
	
	DecimalFormat f = new DecimalFormat("#00");
	
	if (precision <= 0) {
	    str.append(f.format(new Double(value)));
	  
	} else if (precision == 1) {
	    f = new DecimalFormat("#00.0");
	    str.append(f.format(new Double(value)));
	    
	} else {
	    
	    value += delta;
	    if (precision == 2) {
	       nelem = (long)(60*value);
	    
	       deg = (int) (nelem/60);
	       min = (int) (nelem%60);
	       str.append(f.format(new Integer(deg))+" "+
			  f.format(new Integer(min)));
	    
	    } else if (precision == 3) {
	
	        nelem = (long) (600*value);
	  
	        deg  = (int) (nelem/600);
	        min  = (int) (nelem%600)/10;
	        frac = (int) (nelem%10);
	        str.append(f.format(new Integer(deg)) + " " + 
			   f.format(new Integer(min)) + "." + frac);
	    
	    } else if (precision == 4) {
	
	        nelem = (long) (3600*value);
	  
	        deg = (int) (nelem/3600);
	        min = (int) (nelem%3600)/60;
	        sec = (int) (nelem%60);
	        str.append(f.format(new Integer(deg)) + " " + 
			   f.format(new Integer(min)) + " " + 
			   f.format(new Integer(sec)));
	    
	    } else {
	
	        double mult = Math.pow(10, precision-4);
		
	        long fracMod = (long) (mult);
		
	        nelem = (long)(3600*fracMod*value);
	    
	        deg = (int)(nelem / (3600*fracMod) );
	        min = (int)(nelem % (3600*fracMod) / (60*fracMod) );
		sec = (int)(nelem % (60*fracMod) / fracMod);
			    
	        str.append(f.format(new Integer(deg)) + " " + 
			   f.format(new Integer(min)) + " " + 
			   f.format(new Integer(sec)) + ".");
	    
		frac = (int) (nelem%fracMod);
	        int divisor = 1;
	        for (int i=5; i<precision; i += 1) {
		    divisor *= 10;
		}
	        while (divisor > 0) {
		    str.append((char)('0'+(frac/divisor)%10));
		    divisor /= 10;
		}
	    }
	}

	    
        return str.toString();
    }
    
    
    /** Average a 2-D array over it's first dimension.  The
     *  array is assumed to be square.  In practice the first
     *  dimension will be an energy dimension, while the
     *  spatial dimensions will both be incorporated in the
     *  second index.
     * 
     *  @param data  The array to be averaged.
     *  @return      The averaged array.
     */
    public static double [] average3dData(double [][] data) {
	
	
	int bands = data.length;
	int size  = data[0].length;
        double[] baseArray = new double[size];
	System.arraycopy(data[0], 0, baseArray, 0, data[0].length);
	
	if (bands == 1) {
	    return baseArray;
	}
	

        int bandsToAverage=data.length;
        for (int i=1; i <bands; i += 1) {
	    for (int j=0; j<size; j += 1) {
		baseArray[j] += data[i][j];
	    }
	}
	
	for (int j=0; j<size; j += 1) {
            baseArray[j] *= 1./bands;
        }
        return baseArray;
    }
    
    
    /** Create an object of a given class.
     *  The input can be a class in a specified package,
     *  or a full specified class.
     *  We first try to instantiate the object within
     *  the specified package, then try it as a fully
     *  qualified class.
     */
    public static Object newInstance(String cls, String pkg) {
	
	Object o  = null;
	if (pkg != null) {
	    try {
		String fullName = pkg+"."+cls;
	        o = Class.forName(pkg+"."+cls).newInstance();
	    } catch (Exception e) {
		// OK...  We'll try it without the package prefix
	    }
	}
	if (o == null) {
	    try {
		o = Class.forName(cls).newInstance();
	    } catch (Exception e) {
		System.err.println("  Unable to instantiate dynamic class "+cls+" in package "+pkg);
	    }
	}
	return o;
    }
	    
}
