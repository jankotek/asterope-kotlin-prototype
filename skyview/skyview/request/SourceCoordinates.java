package skyview.request;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.StringTokenizer;
import java.util.HashMap;
import skyview.geometry.Position;

import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import skyview.executive.Settings;


/** Converts user input to coordinates and returns a position
 *  object that can be used to get a position in any coordinate system.
 *  The class attempts to handle most coordinate strings locally, but
 *  object name requests are sent to the HEASARC Object name resolver.
 *  Some more exotic coordinate strings may also be handled there.
 */
public class SourceCoordinates implements Runnable {

    /** Text the user entered */
    private String enteredText  = null;
    
    /** A position object representing this coordinate */
    private Position pos        = null;
    
    /** The data returned from the HEASARC Web service */
    private String queryReturn  = null;
    
    /** The coordinate System to be used. */
    private String coords;
    
    /** A thread in which to do the call to the HEASRAC */
    Thread thread; 
    
    /** Have we done the conversion already? */
    private boolean converted = false;
    
    private double[] values;
    
    /** Was the local processing successful? */
    boolean  processed = false;
    
    /** URL for HEASARC service */
    static final String urlBase = Settings.get("UrlCoordinates");

    /** Save requests */
    static HashMap<String, Position> savedQueries = new HashMap<String, Position>();
    
    /** Requested name resolver */
    String resolver;

    /*----------------------------------------------------------------------*/
    /** Constructor 
     *  @param s        text entered as coordinates or object name
     *  @param csn      name of coordinate system
     *  @param equinox  equinox of coordinate system
     *  @param resolver resolver to be used to resolve object name
     */
    /*----------------------------------------------------------------------*/
    public SourceCoordinates(String s, String csn, double equinox, String resolver) {
       
	if (csn == null || csn.length() == 0) {
	    csn = "J2000";
	}
	
	char initial = csn.charAt(0);
	String rest = csn.substring(1);
	
	try {
	    equinox = Double.parseDouble(rest);
	} catch (Exception e) {
	    // If it didn't work we use the old value.
	}
	
	if (! (initial == 'G' || initial == 'I') ) {
	    csn = initial+""+equinox;
	}

        this.resolver    = resolver;
	this.coords      = csn;
	this.enteredText = s;
    }
    
    public SourceCoordinates(String lon, String lat, String coords) throws IllegalArgumentException {
	lon = lon.trim();
	lat = lat.trim();
	this.coords      = coords;
	this.enteredText = lon+", "+lat;
	if (!process(new String[]{lon,lat})) {
	    throw new IllegalArgumentException("Invalid coordinates:"+enteredText);
	}
	converted = true;
    }
    
    public static SourceCoordinates factory(String s, String csn, double equinox, String resolver) {
	return new SourceCoordinates(s, csn, equinox, resolver);
    }
    
    public static SourceCoordinates factory(String lon, String lat, String coords) {
	try {
	    return new SourceCoordinates(lon, lat, coords);
	} catch (IllegalArgumentException e) {
	    return null;
	}
    }
    
    /*----------------------------------------------------------------------*/
    /** Get the position associated with these coordinates.
    /*----------------------------------------------------------------------*/
    public Position getPosition () {
	return pos;
    }

    /*----------------------------------------------------------------------*/
    /**  convert user input string to coords
     *   @return true if coordinates were successfully resolved, false if not
     */
    /*----------------------------------------------------------------------*/
    public boolean convertToCoords() {
	if (converted) {
	    return true;
	}
	converted = true;
	
	if (savedQueries.containsKey(enteredText)) {
	    pos = savedQueries.get(enteredText);
	    return true;
	}
       
        // First check to see if this might can be parsed locally.
        // If there are only the characters '0-9 ,+-,:' in the input
        // string then we assume we can parse it locally.
        if (Pattern.matches("^[\\s0-9\\.\\+\\-\\,\\:]+", enteredText)) {
 	    return parseLocal(enteredText);
	    
        } else {
       
            //--- use thread to create a time limit  FOR NOW
            thread = new Thread(this);
            thread.start();
            try {
                thread.join(35000);  // 35 second timeout.
                if (queryReturn != null) {
                    return parseCoords();
                } else  {
                    System.err.println("Error: timeout from resolver service.");
                    return false;
                }
            } catch (InterruptedException x) {
                System.err.println("Error: Accessing resolver service:\n"+x);
                return false;
            }
        }
    }
   
    /** send query to resolve user input */
    public void run() {
        queryReturn = heasarcResolve();
    }

    /*----------------------------------------------------------------------*/
    /** extract coordinates that match coordinate system of user patch 
     *  from query return
     */
    /*----------------------------------------------------------------------*/
    public boolean parseCoords() {
	
        // Get the J2000 coordinates corresponding to the users request.
	// We're really only interested in the J2000 outputs.
        StringTokenizer st = new StringTokenizer(queryReturn, "|");
        String token;
       
        String[] sexigesimals=new String[8];
        double[] decimals    =new double[8];
       
	try {
            //  Read in the data.
            for (int i=0; i<8; i += 1) {
 	        sexigesimals[i] = st.nextToken();
            }
            for (int i=0; i<8; i += 1) {
	        decimals[i] = Double.parseDouble(st.nextToken());
            }
	} catch(Exception e) { 
	    // We just return but indicate that we
	    // didn't process the request.
	    System.err.println("HEASARC name resolver unable to process input: "+enteredText);
	    return false;
	}
	try {
	    pos = new Position(decimals[0], decimals[1]);
	} catch (Exception e) {
	    return false;
	}
	
	savedQueries.put(enteredText, pos);
	return true;
    } 
      
    /*----------------------------------------------------------------------*/
    /** Set up and send query to resolve source name 
     *  @return  unparsed return from HEASARC query
     */
    /*----------------------------------------------------------------------*/
    public String heasarcResolve() {
       
	String coordSys = "J2000";
	boolean special = false;
        //--- determine correct format for coord sys part of query
        if (coords.startsWith("G")) {
            coordSys="Galactic";
        } else if (coords.startsWith("J") || coords.startsWith("B")) {
	    special = true;
            coordSys="Special Epoch";
        } else if (coords.startsWith("E")) {
	    // Note that this only really works for E2000.
	    // If the enty is an esoteric coordinate format in
	    // non 2000 ecliptic coordinates we get an erroneous position.
            coordSys="Ecliptic";
        }
        String query;
        try {
            query = urlBase + 
	             "CoordVal="   + URLEncoder.encode(enteredText,"UTF-8") +
                     "&CoordType=" + URLEncoder.encode(coordSys,"UTF-8");
	    if (special) {
	         query += "&Epoch="+ URLEncoder.encode(coords, "UTF-8");
	    }
	    //if (resolver != null && resolver.length() > 0) {
		//query += "&Resolver="+URLEncoder.encode(resolver, "UTF-8");
	    //}
	    
            if (resolver != null && resolver.length() > 0) {
               //--- Convert to convCoord format
               Pattern pattern = Pattern.compile("-");
               Matcher matcher = pattern.matcher(resolver);
               resolver = matcher.replaceAll("/");
               
               query += "&Resolver="+URLEncoder.encode(resolver, "UTF-8");
         }

        } catch (Exception e) {
            System.err.println("Error encoding query:\n " + e);
            return null;
        }
	
        String wholeString = "";
        try {
            //--- set up connection
            BufferedReader in = new BufferedReader (
				  new InputStreamReader(
				   new java.net.URL(query).openStream()
						       )
						  );
            //------------------------------------------------------------------
            //--- concatenate query results
            //------------------------------------------------------------------
            String line;
            while ((line = in.readLine()) != null) {
                wholeString=wholeString + "|" + line;
            }
            
        } catch (IOException e) {
            System.err.println("Error accessing HEASARC service:\n" + e);
        }
        return wholeString;
    }
    
    /** Test functionality. */
    public static void main(String[] args) throws Exception {
 	
        if (args.length == 0) {
	    usage();
	    
        } else {
	    String source = args[0];
	    String coords = args[1];
	    double equinox = Double.parseDouble(args[2]);
	    String resolver = args[3];
            System.err.println("resolver=" + resolver);
	    SourceCoordinates sc=new SourceCoordinates(source, coords, equinox, resolver);
	    Position pos   = sc.getPosition();
	    double[] posit = pos.getCoordinates(args[4]);
	    System.out.println("Posit:"+posit[0]+","+posit[1]);
	    
	}
    }
    
    private static void usage() {
	System.out.println("Usage: java skyview.request.SourceCoordinates text inCoord equinox resolver outCoord");
    }
    
    private boolean parseLocal(String input) {
	
	// Get rid of leading and trailing spaces.
	input = input.trim();
	
	// Connect signs to the appropriate values.
	input = skyview.survey.Util.replace(input, "\\+\\s+", "\\+", true);
	input = skyview.survey.Util.replace(input, "\\-\\s+", "\\-", true);
	
	
	// First check to see if we split on commas.
	String[] commas = Pattern.compile(",").split(input);
	if (commas.length == 2) {
	    return process(commas);
	    
	} else if (commas.length > 2) {
	    System.err.println("Error: Too many commas");
	    return false;
	    
	} else {
	    
	    // Next check to see if we split on the sign of the declination.
	    // Note that the RA may have a sign but if so it is the first
	    // character of the string.
	    String prefix = "";
	    String sign   = "-";
	    
	    if (input.charAt(0) == '+'  || input.charAt(0) == '-') {
		prefix = input.substring(0,1);
		input  = input.substring(1);
	    }
	    
	    String[] signs = Pattern.compile("(\\+|\\-)").split(input);
	    if (signs.length == 2) {
		signs[0] = prefix+signs[0];
		if (input.indexOf("+") >= 0) {
		    sign = "+";
		}
		signs[1] = sign+signs[1];
		return process(signs);
		
	    } else if (signs.length > 2) {
		System.err.println("Error in signs");
		return false;
		
		
	    } else {
		// Last chance... Let's split on spaces/colons
		// 
		input = prefix + input;
		
		String[] spaces = Pattern.compile("\\s+").split(input);
		
		if (spaces.length == 2) {
		    return process(spaces);
		} else if (spaces.length == 6) {
		    return process(new String[]{spaces[0]+" "+spaces[1]+" "+spaces[2],
			                 spaces[2]+" "+spaces[4]+" "+spaces[5]} );
		} else {
		    System.err.println("Unable to process input");
		    return false;
		}
	    }
	}
    }
    
    private boolean  process(String[] fields) {
	values = new double[2];
	for (int i=0; i<2; i += 1) {
	    if (!parseField(i, fields[i])) {
		return false;
	    }
	}
	try {
	    pos = new Position(values[0], values[1], coords);
	} catch (Exception e) {
	    return false;
	}
	return true;
    }
    
    private boolean  parseField(int index, String field) {
	
	field = field.trim();
	
        // Get rid of spaces around colons.
	field = skyview.survey.Util.replace(field, "\\s*\\:\\s*", "\\:", true);
	
	String[] comp = Pattern.compile("(\\s+|\\:)").split(field);
	
	if (comp.length > 3) {
	    return false;
	}
	
	double value  = 0;
	double sign   = 1;
	double ratio  = 1;

	if (comp[0].charAt(0) == '-') {
	    sign = -1;
	    comp[0] = comp[0].substring(1);
	} else if (comp[0].charAt(0) == '+') {
	    comp[0] = comp[0].substring(1);
	}
	
	for (int i=0; i<comp.length; i += 1) {
	    
	    if (comp[i].length() == 0) {
		return false;
	    }
	    if (i != comp.length-1 && comp[i].indexOf(".") >= 0) {
	        return false;
	    }
	    if (comp[i].indexOf("+") >= 0 || comp[i].indexOf("-") >= 0) {
		return false;
	    }
	    value += sign*Double.parseDouble(comp[i])/ratio;
	    ratio *= 60;
	}
	
	// Sexagesimal hours.
	String xcoords = coords.toUpperCase();
	if (index == 0 && comp.length > 1 && 
	    (xcoords.startsWith("J") || xcoords.startsWith("B"))) {
	    value *= 15;
	}
	values[index] = value;
	return true;
    }
}
