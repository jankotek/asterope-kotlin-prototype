package skyview.vo;

import skyview.executive.Settings;

import java.io.BufferedInputStream;
import java.net.URL;


import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import java.util.ArrayList;

/** This class queries a cone search service and returns the positions and IDs. */
public class ConeQuerier implements Runnable {
    
    private int nCol   =  0;
    
    private int raCol   = -1;
    private int decCol  = -1;
    private int idCol   = -1;
    private int currCol = -1;
    
    private int[] addCols = new int[0];
    private String[] addColNames;
    
    private static final int MAX_COL=1024;
    
    private double size;
    private String tableID;
    
    private int entriesUsed = 0;
    
    private String finishMessage="Finished";
    
    private String urlString;
    
    private boolean[] useFlags;
    
    
    private ArrayList<double[]> positions = new ArrayList<double []>();
    private ArrayList<String>   ids       = new ArrayList<String>();
    private ArrayList<String[]> extraCols;
    
    private ArrayList<ConeQualifier> qualifiers = new ArrayList<ConeQualifier>();
    
    private boolean criteria = false;
    private boolean criteriaMet;
    
    private java.io.PrintStream output = null;
    
    /** Build a cone querier using the base URL and cone search
     *  parameters.
     */
    public ConeQuerier (String baseURL, String id, double ra, double dec, double size)  {
	this(checkURLEnd(baseURL) + "RA="+ra+"&DEC="+dec+"&SR="+size, id);
	this.size = size;
	this.tableID   = id;
    }
    
    /** Build a cone querier using the full URL. */
    public ConeQuerier (String URL, String id) {
	urlString    = URL;
	this.tableID = id;
	this.size    = -1;
	if (Settings.has("CatalogColumns")) {
	    addColNames = Settings.getArray("CatalogColumns");
	    addCols     = new int[addColNames.length];
	    java.util.Arrays.fill(addCols,-1);
	    extraCols   = new ArrayList<String[]>();
	}
    }
    
    /** Make sure that URLs can be appended to */
    public static String checkURLEnd(String url) {
	char last        = url.charAt(url.length()-1);
	if (last == '&'  || last == '?') {
	    return url;
	}
	if (url.indexOf("?") > 0 ) {
	    return url + "&";
	} else {
	    return url + "?";
	}
    }
    
    public void setEntriesUsed(int n) {
	entriesUsed = n;
    }
    
    public void setOutput(java.io.PrintStream str) {
	output = str;
    }
	    
    
    public void addCriterion(String name, String op, String value) {
	if (!criteria) {
	    criteria = true;
	    useFlags   = new boolean[MAX_COL];
	    java.util.Arrays.fill(useFlags, false);
	}
	qualifiers.add(new ConeQualifier(name, op, value));
    }
		       
    
    /** Get the size of the request */
    public double getSize() {
	return size;
    }
    
    /** Run the query */
    public void run() {
	try {
            java.io.BufferedInputStream   bi = new java.io.BufferedInputStream(
					          new URL(urlString).openStream());
	    java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream(32768);
	
	    byte[] buf = new byte[32768];
	    int len;
	    while ( (len=bi.read(buf)) > 0) {
	        bo.write(buf, 0, len);
	    }
	    bi.close();
	    bo.close();
	
            String response = bo.toString();
	    response = response.replaceAll("<!DOCTYPE.*", "");
	    java.io.ByteArrayInputStream byi = new java.io.ByteArrayInputStream(response.getBytes());
	    
	    try {
                SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
	        // This should fill images with the strings for any images we want.
                sp.parse(byi, new ConeQuerier.ConeQuerierCallBack());
            } catch (Error e) {
		if (!e.getMessage().equals(finishMessage)) {
		    throw e;
		}
	    } catch (Exception e) {
	        throw new Error("Error parsing ConeQuery:"+e);
	    }
	} catch (Exception e) {
	    throw new Error("Unable to do IO in ConeQuery processing:"+e);
	}
    }
    
    /** Get the array of position
     *  @return a double[n][2] array.
     */
    public double[][] getPositions() {
	return positions.toArray(new double[0][]);
    }
    
    /** Get the specified IDs */
    public String[] getIDs() {
	return ids.toArray(new String[0]);
    }
    
    /** Get the number of rows returned */
    public int getCount() {
	return positions.size();
    }
    
    /** Get the URL used. */
    public String getURL() {
	return urlString;
    }
    
    /** Get the list of extra columns */
    public ArrayList<String[]> getExtras() {
	return extraCols;
    }
    
    /** Get the names of extra columns */
    public String[] getExtraNames() {
	return addColNames;
    }

    /** This class is used to parse the XML document returned by
     *  the cone search service.
     */
    private class ConeQuerierCallBack extends DefaultHandler {	
	
	/** Buffer to accumulate text into */
	private StringBuffer buf;
	
	/** Position on the current row */
	private double ra  = Double.NaN;
	private double dec = Double.NaN;
	
	/** Extra columns the user wants to see. */
	String[] xcols     = null;
	
	/** Id of the current row */
	private String id = null;
	
	/** Are we in an active element? */
	private boolean active = false;
	
	/** Process the beginning of each element.
	 *  We are only interested in FIELDs to find the appropriate
	 *  columns, TRs to set the column count to 0 and TDs to indicate
	 *  that we are processing a column.
	 */
        public void startElement(String uri, String localName, String qName, Attributes attrib) {
	    
	    if (qName.equals("FIELD")) {
		String ucd = attrib.getValue("ucd");
		if (ucd != null) {
		    ucd = ucd.toLowerCase();
		    if (ucd.equals("pos_eq_ra_main") || ucd.equals("pos.eq.ra;meta.main") ) {
		        raCol  = nCol;
		    } else if (ucd.equals("pos_eq_dec_main")  || ucd.equals("pos.eq.dec;meta.main")) {
		        decCol = nCol;
		    } else if (ucd.equals("id_main")  || ucd.equals("meta.id;meta.main")) {
		        idCol  = nCol;
		    }
		}
		String name = attrib.getValue("name");
		
		if (output != null) {
		    if (name != null) {
			output.print("Table: "+tableID+" Field:"+name+"\n      ");
			if (attrib.getValue("datatype") != null) {
			    output.print("type:"+attrib.getValue("datatype"));
			}
			if (attrib.getValue("arraysize") != null) {
			    output.print("["+attrib.getValue("arraysize")+"]");
			}
			if (attrib.getValue("ucd") != null) {
			    output.print(" UCD:"+attrib.getValue("ucd"));
			}
			if (attrib.getValue("unit") != null) {
			    output.print(" units:"+attrib.getValue("unit"));
			}
			output.println("");
		    }
		}
		
		if ((criteria || addCols.length > 0) && name != null) {
		    
		    for(int i=0; i<qualifiers.size(); i += 1) {
			if (qualifiers.get(i).getName().equals(name)) {
			    qualifiers.get(i).setColumnType(nCol, attrib.getValue("datatype"));
			    if (nCol >= MAX_COL) {
				System.err.println("Number of columns exceeds maximum for qualified query ("+MAX_COL+") for table "+tableID);
				throw new Error(finishMessage);
			    }
			    useFlags[nCol] = true;
			}
		    }
		    for(int i=0; i<addCols.length; i += 1) {
			if (addColNames[i].equals(name)) {
			    addCols[i] = nCol;
			}
		    }
		}
		
		// At the HEASARC we use the key columns as the ID column, but
		// we often have a 'name' column.  We'll defer to that if we find it.
		if (name != null && name.equals("name") && idCol == 0) {
		    idCol = nCol;
		}
		nCol += 1;
		
	    } else if (qName.equals("TR")) {
	        currCol     = 0;
		criteriaMet = true;
		if (addCols.length > 0) {
		    xcols = new String[addCols.length];
		}
	    } else if (qName.equals("TD")) {
		if ( currCol == raCol || currCol == decCol || currCol == idCol || 
		    (criteria && useFlags[currCol])) {
		    active = true;
		    buf    = new StringBuffer();
		} else {
		    for (int i=0; i<addCols.length; i += 1) {
			if (currCol == addCols[i]) {
			    active = true;
			    buf    = new StringBuffer();
			}
		    }
		}
		
	    } else if (criteria && qName.equals("TABLEDATA")) {
		// We can check to see if all of the fields that we are querying on have data.
		for (int i=0; i<qualifiers.size(); i += 1) {
		    if (qualifiers.get(i).getColumn() < 0) {
			System.err.println("Warning: Table "+tableID+" does not have qualified column '"+qualifiers.get(i).getName()+"'\nTable not queried.");
			if (output != null) {
			    output.println("Warning: Table "+tableID+" does not have qualified column '"+qualifiers.get(i).getName()+"'\nTable not queried.");
			}
			throw new Error(finishMessage);
		    }
		}
	    }
        }
    
	/** Process the end of each element.
	 *  We are interested in them when we are active (inside a TD) since
	 *  we want to handle the result, after a TD (to increment the column
	 *  count), after a TR (to process the results from the current line),
	 *  and RESOURCE (we just process the first resource returned).
	 */
        public void endElement(String uri, String localName, String qName) {
	    
	    // One of the fields we are looking for...
	    if (active) {
	        active = false;
		String s = new String(buf).trim();
		
		if (currCol == raCol) {
		    try {
			ra = Double.parseDouble(s);
		    } catch (Exception e) {
			ra = Double.NaN;
		    }
		} else if (currCol == decCol) {
		    try {
			dec = Double.parseDouble(s);
		    } catch (Exception e) {
			dec = Double.NaN;
		    }
		} else if (currCol == idCol) {
		    id = s;
		} else {
		    for (int i=0; i<addCols.length; i += 1) {
			if (currCol == addCols[i]) {
			    xcols[i] = s;
			    break;
			}
		    }
		}
		
		// See if it meets requested criteria
		if (criteria && useFlags[currCol]) {
		    for (int i=0; i<qualifiers.size() && criteriaMet; i += 1) {
			if (qualifiers.get(i).getColumn() == currCol) {
		             criteriaMet = qualifiers.get(i).check(s);
			}
		    }
		}		    
	    }
	    
	    if (qName.equals("TD")) {
	        currCol += 1;
		
	    } else if (qName.equals("TR")) {
		// Process the row if we have a sensible RA and Dec.
		// It's OK for the ID to be null (but I'm not sure
		// that's possible.
		if ( criteriaMet && !Double.isNaN(ra) && !Double.isNaN(dec) ) {
		    positions.add( new double[]{ra,dec} );
		    ids.add(id);
		    if (addCols.length > 0) {
			extraCols.add(xcols);
		    }
		}
		
	    } else if (qName.equals("RESOURCE")) {
		// Ugly but apparently have to throw an exception to terminate parsing!
		// Some `cone' services actually have a bunch of resources.
		// We don't really understand how to handle that, so we'll just
		// ignore everything after the first.
		throw new Error(finishMessage);
	    }
	}
        public void characters(char[] arr, int start, int len) {
	    if (active) {
	        buf.append(arr, start, len);
	    }
        }
	
    }
    

    /** Create a ConeQuerier Object.
     * @param id <dl>
     *            <dt> NED<dd> query the NED cone search services 
     *            <dt> SIMBAD<dd> query SIMBAD 
     *            <dt> contains '/' <dd> assume this is a Vizier table. 
     *           <dt> otherwise <dd> assume this is a HEASARC table. 
     */
    public static ConeQuerier factory(String id, double ra, double dec, double size) {
	
	String xid = id.toLowerCase();
	String baseURL;

        if (Settings.has("Url."+xid)) {
            baseURL = Settings.get("Url."+xid);
	} else if (xid.indexOf("/") > 0) {
	    baseURL = Settings.get("Url.VizierBase")+id+"&";
	} else {
	    baseURL = Settings.get("Url.HeasarcBase")+xid+"&";
	}
	return new ConeQuerier(baseURL, id, ra, dec, size);
    }
    
    public static void main(String[] args) throws Exception {
	
	String id = args[0];
	double ra = Double.parseDouble(args[1]);
	double dec = Double.parseDouble(args[2]);
	double size = Double.parseDouble(args[3]);
	
	ConeQuerier cq = ConeQuerier.factory(id, ra, dec, size);
	cq.setOutput(System.out);
	
	for (int i=4; i<args.length; i += 1) {
	    String[] fields = args[i].split(",");
	    System.out.println("Fields are:"+fields.length);
	    
	    cq.addCriterion(fields[0], fields[1], fields[2]);
	}
	
	cq.run();
	
	double[][] ps  = cq.getPositions();
	String[]   ids = cq.getIDs();
	
	for (int i=0; i<ps.length; i += 1) {
	    System.out.println(ids[i]+":"+ps[i][0]+","+ps[i][1]);
	}
    }
    
    public void updateHeader(nom.tam.fits.Header hdr) {
	try {
	    hdr.insertHistory("Catalog: "+tableID+"  number of entries found:"+entriesUsed);
	} catch (nom.tam.fits.FitsException e) {
	    System.err.println("Error updating FITS header in CatalogQuerier:"+e);
	}
    }
}
