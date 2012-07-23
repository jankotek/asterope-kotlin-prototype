package skyview.data;

import java.text.DecimalFormat;

public class CoordinateFormatter {
    
    /** Do we want sexagesimal coordinates ? */
    private boolean isSexagesimal = true;
    
    /** Separators between the sexagesimal elements. */
    String[] sexaSep        = {" ", " "};

    /** Do we want leading/trailing 0's in the values? (This assumes
     *  sexagesimal tokens should be two digits which may not be
     *  correct for galactic coordinates.
     */
    boolean  zeroFill   = false; 

    /** Create a sexagesimal string representing the coordinate value.
     * @param value  A double precision value which is to be
     *              converted to a string representation.  The user should convert
     *              to hours prior to this call if the string is to be in hours.
     * @param  precision A integer value giving the precision to which
     *                 the value is to be shown.
     *  
     * <dl> <dt> Less than 3 <dd> Degrees (or hours), e.g. 24
     *      <dt> 3 <dd> Deg.f <it> e.g.</it>,  24.3
     *	    <dt> 4 <dd> Deg mm <it> e.g.</it>, 24 18
     *	    <dt> 5 <dd> Deg mm.f <it> e.g.</it>, 25 18.3
     *	    <dt> 6 <dd> Deg mm ss <it> e.g.</it>,  25 18 18
     *	    <dt> 7 <dd> Deg mm ss.s <it> e.g.</it>, 25 18 18.2
     *	    <dt> 8 <dd> Deg mm ss.ss <it> e.g.</it>, 25 18 18.24
     *	    <dt> Greater than 8 <dd> Deg mm ss.sss <it> e.g.</it>, 25 18 18.238
     *  </dl>
     */
    public String sexagesimal(double value, int precision) {
	
        int    deg,  min,  sec,  frac;
	double fdeg, fmin, fsec, ffrac;

	StringBuffer str = new StringBuffer();
	
	if (value < 0) {
	    value = Math.abs(value);
	    str.append("-");
	}
	
	if (precision > 13) {
	    precision = 13;
	}
	
	if (precision <= 2) {
	    
	    str.append(String.valueOf( numb((int) ( value + 0.49999999))));
	    
	} else if (precision == 3) {
	    
	    deg  = (int) value;
	    frac = (int) (10*( value-(int)value) + 0.5);
	    
	    if (frac == 10) {
	        deg += 1;
	        frac = 0;
	    }
	    
	    str.append(numb(deg)+"."+frac);
	    
	} else if (precision == 4) {
		
	    deg = (int) value;
	    min = (int) (60*(value - deg) + 0.5);
	    if (min == 60) {
	        deg += 1;
                min = 0;
            }
            str.append(numb(deg)+sexaSep[0]+numb(min));
		
	} else if (precision == 5) {
	  
	    deg = (int) value;
	    fmin = 60.*(value-deg);
		
            min  = (int) fmin;
	    frac = (int) (10.*(fmin-min) + .5);
	    
	    if (frac == 10) {
	        min += 1;
                frac = 0;
	    }
	    if (min == 60) {
	        deg += 1;
	        min = 0;
	    }
		
	    str.append(numb(deg) + sexaSep[0] + numb(min) + "." + frac);
		
	} else if (precision == 6) {
	  
	    deg  = (int) value; 
	    fmin = 60.*(value-deg); 
	    min  = (int) fmin;
	    sec  = (int) (60.*(fmin-min) + .5); 
	    if (sec == 60) { 
	        min += 1;
	        sec = 0; 
	    } 
	    if (min == 60) { 
	        deg += 1; 
	        min = 0; 
	    } 
	  
	    str.append(numb(deg) + sexaSep[0] + numb(min) + sexaSep[1] + numb(sec));
		
	} else {  
	    int i;
	    double maxval=1;
	  
	    deg   = (int) value;
	    fmin  = 60.*(value-deg);
	    min   = (int) fmin;
	    fsec  = 60.*(fmin-min);
	    sec   = (int) fsec;
	    ffrac = fsec - sec;
	  
	    for (i=6; i<precision; i += 1) {
	        ffrac  *= 10.;
	        maxval *= 10;
	    }
	    frac = (int) (ffrac + 0.5);
	  
	    if (frac == maxval) {
	        sec += 1;
	        frac = 0;
	    }
	  
	    if (sec == 60) {
	        min += 1;
	        sec = 0;
	    }
	    if (min == 60) {
	        deg += 1;
	        min = 0;
	    }
		
	    // need to format this properly
	    str.append(numb(deg)+sexaSep[0]+numb(min)+sexaSep[1]+numb(sec)+"."+frac);
        }
        return str.toString();
    }
    
    public String decimal(double value, int precision) {
	if (precision < 3) {
	    precision = 2;
	}
	
	String form = "0";
	if (zeroFill) {
	    form += "0";
	}
	form += ".";
	for (int i=2; i<precision; i += 1) {
	    if (zeroFill) {
		form += "0";
	    } else {
		form += "#";
	    }
	}
	    
	return new DecimalFormat(form).format(value);
    }
    
    /** Return the string corresponding to the input with a leading
     *  zero added if required.
     *  @param input A positive integer.
     */
    private String numb(int input) {
	if (!zeroFill  || input >= 10) {
	    return ""+input;
	} else {
	    return "0"+input;
	}
    }
    
    public void setZeroFill(boolean flag) {
	zeroFill = flag;
    }
    
    public void setSexagesimal(boolean flag) {
	isSexagesimal = flag;
    }
    
    public void setSeparators(String[] separators) {
	if (separators != null     && separators.length > 1  && 
	    separators[0] != null  && separators[1] != null) {
	    System.arraycopy(separators, 0, sexaSep, 0, 2);
	} else {
	    System.err.println("Warning: Invalid separator array ignored in CoordinateFormatter.");
	}
    }
    
    public String format(double value, int precision)  {
	if (isSexagesimal) {
	    return sexagesimal(value, precision);
	} else {
	    return decimal(value, precision);
	}
    }
    
    public static void main(String[] args) {
	
	double val  =  Double.parseDouble(args[0]);
	int    prec =  Integer.parseInt(args[1]);
	
	CoordinateFormatter c = new CoordinateFormatter();
	
	System.out.println("Without leading zeros:");
	System.out.println("   Sex:"+c.sexagesimal(val,prec));
	System.out.println("   Dec:"+c.decimal(val,prec));
	
	c.setZeroFill(true);
	
	System.out.println("With leading zeros:");
	System.out.println("   Sex:"+c.sexagesimal(val,prec));
	System.out.println("   Dec:"+c.decimal(val,prec));
	
	c.setSeparators(new String[]{":",":"});
			   
	System.out.println("With leading zeros and colon separators:");
	System.out.println("   Sex:"+c.sexagesimal(val,prec));
	System.out.println("   Dec:"+c.decimal(val,prec));
    }
	
}
