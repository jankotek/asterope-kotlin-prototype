package net.ivoa.util;

import java.util.HashMap;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import static java.net.URLDecoder.decode;


/** Simple utility for parsing CGI parameters */
public class CGI {
    
    
    private HashMap<String, String[]> params = new HashMap<String, String[]>();
    public CGI() {
	parseParams();
    }
    
    public CGI(String input) {
	if (input != null && (input.indexOf('=') < 0 && input.indexOf('%') >= 0 )) {
	    // Decode the input string...
	    try {
	        input = java.net.URLDecoder.decode(input, "UTF-8");
	    } catch (Exception e) {
		System.err.println("Decoding error?? "+e);
	    }
	}
	parseString(input);
    }
    
    public  HashMap<String, String[]> getParams() {
	return params;
    }
    
    protected void parseParams() {
	
	String type = System.getenv("REQUEST_METHOD");
	if (type != null) {
	    type = type.toUpperCase();
	}
	
	if (type == null) {
	    if (System.getenv("QUERY_STRING") != null) {
		type = "GET";
	    } else {
		type = "POST";
	    }
	} else {
	    type = type.toUpperCase();
	}
	String queryString = null;
	if (type.equals("GET")) {
	    queryString = System.getenv("QUERY_STRING");
	} else {
	    String enc = System.getenv("CONTENT_TYPE");
	    if (enc != null && enc.startsWith("multipart/form")) {
		parseMultipartForm(enc);
		return;
	    } else {
	        try {
	            BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
	            queryString = bf.readLine();
	        } catch (IOException e) {
		    System.err.println("Unable to read standard input on POST");
	        }
	    }
	}
	parseString(queryString);
    }
    
    protected void parseString(String queryString) {
	if (queryString != null) {
	    String[] fields = queryString.split("\\&");
	    for (String field: fields) {
		decodeField(field);
	    }
	}
    }
    
    protected String getField(String key, String line) {
	String[] fields = line.split(";");
	for(String field: fields) {
	    field = field.trim();
	    if (field.startsWith(key+"=")) {
		String value = field.substring(key.length()+1);
		if (value.length() > 2) {
		    if (value.charAt(0) == '"'  &&
			value.charAt(value.length()-1) == '"') {
			value = value.substring(1, value.length()-1);
		    }
		}
		return value;
	    }
	}
	return null;
    }
	
    
    private interface LineReader {
	public LineReader processLine(String line);
    }
    
    private class FieldWaiter implements LineReader {
	public LineReader processLine(String line) {
	    if (line.toLowerCase().startsWith("content-disposition")) {
		fieldName = getField("name", line);
		fieldFile = getField("filename", line);
		return new BlankWaiter();
	    } else {
		return this;
	    }
	}
    }
    private class BlankWaiter implements LineReader {
	public LineReader processLine(String line) {
	    if (line.length() == 0) {
		fieldBuffer = new StringBuffer();
		return new DataReader();
	    } else {
		return this;
	    }
	}
    }
    
    private class DataReader implements LineReader {
	public LineReader processLine(String line) {
	    
	    if (line.equals(dataBoundary)) {
		addField();
		return new FieldWaiter();
		
	    } else if (line.length() == dataBoundary.length() + 2   &&
		       line.equals(dataBoundary+"--")) {
		
		// Should be all done but just in case we return a FieldWaiter
		addField();
		return new FieldWaiter();
		
	    } else {
		fieldBuffer.append(line);
		fieldBuffer.append('\n');
		return this;
	    }
	}
    }
	
    private StringBuffer fieldBuffer;
    private String dataBoundary;
	    
    private String fieldName;
    private String fieldFile;
	    
    protected void parseMultipartForm(String type) {
	
	
	LineReader lr = new FieldWaiter();
	
	dataBoundary = "--"+getField("boundary", type);
	String lastLine = null;
	
	try {  
	    BufferedReader br = new BufferedReader(
			      new InputStreamReader(System.in));
	    String line;
	
	    while ( (line=br.readLine()) != null) {
		char ch = 0;
		if (line.length() > 0) {
		    ch = line.charAt(line.length()-1);
		}
		int chr = ch;
	        
		lr = lr.processLine(line);
		lastLine = line;
	    }
	} catch (IOException e) {
	    System.err.println("Error reading standard input:"+e);
	}
	if (! (lr instanceof FieldWaiter) ) { 
	    throw new Error("Invalid state at termination of multipart form parse");
	}
    }
    
    protected void addField() {
	
	// Get rid of extra newline after normal fields.
	if (fieldFile == null && fieldBuffer.length() > 0) {
	    fieldBuffer.setLength(fieldBuffer.length()-1);
	}
	addField(fieldName, fieldBuffer.toString());
    }
    
    protected void decodeField(String field) {
	
	String[] elems = field.split("=", 2);
        try {
	    if (elems.length == 2) {
	        String key = decode(elems[0], "UTF-8");
	        String val = decode(elems[1], "UTF-8");
	        addField(key, val);
	    }
        } catch (Exception e) {	       
	    // This should be an unsupported encoding exception!
 	    throw new Error("Unexpected error:"+e);
        }
    }
    
    protected void addField(String key, String val) {
	if (params.containsKey(key)) {
	    String[] old = params.get(key);
	    String[] nw  = new String[old.length+1];
	    System.arraycopy(old, 0, nw, 0, old.length);
	    nw[old.length] = val;
	    params.put(key, nw);
	} else {
	    params.put(key, new String[]{val});
	}
    }
    
    public String value(String key) {
	if (params.containsKey(key)) {
	    return params.get(key)[0];
	} else {
	    return null;
	}
    }
    
    public String[] values(String key) {
	return params.get(key);
    }
    
    public int count(String key) {
	if (params.containsKey(key)) {
	    return params.get(key).length;
	} else {
	    return 0;
	}
    }
    
    public String[] keys() {
	return params.keySet().toArray(new String[0]);
    }

}
