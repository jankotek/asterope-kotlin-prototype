package skyview.vo;

class ConeQualifier {
    
    /** Column name */
    private String   name;
    
    /** Criterion value as string */
    private String   sValue;
    
    /** Comparison operator */
    private String   op;
    
    /** Criterion value as number */
    private double   nValue;
    
    /** Column field is found at */
    private int      column = -1;
    
    /** Is this a numeric column? */
    private boolean  numeric = false;
    
    /** Is the a regular expression comparison? */
    private boolean  pattern = false;
    
    /** For non RE comparisons, what do we return on &lt; = &gt; respectively */
    private boolean[] codes = new boolean[3];
    
    /** Did the user specify a not? */
    private boolean isNot = false;
    
    java.util.regex.Pattern  pat;
    
    
    
    ConeQualifier(String name, String op, String value) {
	
	this.name   = name;
	this.op     = op;
	this.sValue = value;
	
	// We allow things like =!< but expect  !> or !>=
	codes[0] = op.indexOf("<") >= 0; 
	codes[1] = op.indexOf("=") >= 0; 
	codes[2] = op.indexOf(">") >= 0; 
	
	isNot = op.indexOf("!") >= 0;
    }
    
    void setColumnType(int col, String type) {
	
	this.column = col;
	
	if (type.equals("char")  || type.equals("unicodeChar")) {
	    numeric = false;
	    // We replace the value * with the value .* which we
	    // can use in a regular expression match/
	    if ((op.equals("=") || op.equals("!=")) && sValue.indexOf("*") >= 0) {
		pattern = true;
		sValue = sValue.replace("*", ".*");
		pat    = java.util.regex.Pattern.compile(sValue, java.util.regex.Pattern.CASE_INSENSITIVE);
	    }
		
	} else {
	    numeric = true;
	    nValue  = Double.parseDouble(sValue);
	}
    }
    
    String getName() {
	return name;
    }
    
    
    int getColumn() {
	return column;
    }
    
    boolean check(String test) {
	
	int code = 0;
	if (!numeric) {
	    if (pattern) {
	        return pat.matcher(test).matches() != isNot;
	    }
	    code = test.compareToIgnoreCase(sValue);
            if (code < 0) {
		code = -1;
	    } else if (code > 0) {
		code = 1;
	    }
	} else {
	    try {
		double val = Double.parseDouble(test);
		if (val < nValue) {
		    code = -1;
		} else if (val > nValue) {
		    code = 1;
		}
	    } catch (Exception e) {
		return false;
	    }
	}
	return codes[code+1] != isNot;
    }
}
