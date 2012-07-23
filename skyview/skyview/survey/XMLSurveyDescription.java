package skyview.survey;

import skyview.executive.Settings;

import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.Reader;

import java.util.HashMap;
import java.util.ArrayList;

import net.ivoa.util.CGI;

/** This class writes an HTML description of a survey based upon
 *  its XML survey file.
 */
public class XMLSurveyDescription {
    
    static XMLSurveyFinder sf = new XMLSurveyFinder();
    static CGI             cgi = new CGI();
    static String          currentRegime;
    
    public static void main(String[] args) {
	try {
	    // Add any settings files now.
	    for (int i=0; i<args.length; i += 1) {
	        Settings.updateFromFile(args[i]);
	    }
	    String survey = cgi.value("survey");
	    if (survey == null) {
	        allSurveys();
	    } else {
	        String name   = sf.findFile(survey);
	        if (name == null) {
		    error("No survey description availble for specified survey");
	        } else {
		    printSurvey(name);
	        }
	    }
	} catch (Exception e) {
	    error("Exception processing request: "+e);
	}
    }
    
    public static void printSurvey(String name) throws Exception {
	System.out.println("Content-type: text/html\n");
	System.out.println("<html><head><title>Survey Description:"+name+"</title></head><body>");
	printDescription(name);
	System.out.println("</body></html>");
    }
	
    
    public static void error(String msg) {
	System.out.println("Content-type: text/html\n\n<h2>Error during processing</h2>"+msg);
    }
    
    public static void printDescription(String filename) throws Exception {
	String xslt = Settings.get("DescriptionXSLT");
	if (xslt == null) {
	    error("No description transformation file in settings");
	}
	
	Source xmlSource = new StreamSource(XMLSurvey.getSurveyReader(filename));
	Source xslSource = new StreamSource(Util.getResourceOrFile(xslt));
	
	StringWriter sw  = new StringWriter();
	Result output    = new StreamResult(sw);
	Transformer trans= TransformerFactory.newInstance().newTransformer(xslSource);
	trans.transform(xmlSource, output);
	sw.close();
	
	String outHTML   = sw.toString();
	// Get rid of gratuitious added by transformer.
	int off1 = outHTML.indexOf('>');
	if (off1 > 0) {
	    outHTML = outHTML.substring(off1+1);
	}
	outHTML = outHTML.replace("</HTML>", "");
	outHTML = outHTML.replaceAll("\\<H2\\>\\s*([^>]*)\\s*\\</H2\\>", "<A NAME='$1'><H2>$1</H2></A>");
	
	System.out.print(outHTML);
    }
    
    public static void allSurveys() throws Exception {
	
	String[] names = sf.getSurveys();
	HashMap<String, String> fileHash = new HashMap<String,String>();
	
	for (String name: names) {
	    String file = sf.findFile(name);
	    fileHash.put(file, "");
	}
	String[] files = (String[])fileHash.keySet().toArray(new String[0]);
	
	// Now find the regime for each survey file.
	for (String file: files) {
	    fileHash.put(file, getRegime(file));
	}
	// Print out any header file we have.
	boolean wroteHeader = false;
	if (Settings.has("SurveysHeader")) {
	    String header = skyview.request.HTMLWriter.slurp(Settings.get("SurveysHeader"));
	    if (header.length() > 10) { 
		System.out.print(header);
		wroteHeader = true;
	    }
	}
	
	// Now loop over regimes of interest.
	String[] regimes = {"Radio", "Infrared", "Optical", "Ultraviolet", "X-ray", "Gamma ray"};
	if (!wroteHeader) {
	    printAllHeader();
	}
	    
	for (String regime: regimes) {
	    // This elimates the surveys as we process them.
	    fileHash = processRegime(regime, fileHash);
	}
	processRemaining(fileHash);
	if (Settings.has("SurveysTrailer")) {
	    String trailer = skyview.request.HTMLWriter.slurp(Settings.get("SurveysTrailer"));
	    System.out.print(trailer);
	} else {
	    printAllTrailer();
	}
    }
    
    public static String getRegime(String file) {
        /** The class the is called to add survey specific settings.
         */
	RegimeFinder handler = new RegimeFinder();
        try {
	    
            SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
	    Reader is    = XMLSurvey.getSurveyReader(file);
            sp.parse(new InputSource(is), handler);
	    is.close();
	    
	} catch (ParsingTermination p) {
	    return handler.getRegime();
	    
	} catch (Exception e) {
	    return null;
	}
	// Should never get here...
	return null;
    }
    
    static void printAllHeader() {
	System.out.println("Content-type: text/html\n\n<HTML><HEAD><Title>SkyView Surveys</Title></HEAD><BODY>");
    }
    
    static void printAllTrailer() {
	System.out.println("</BODY></HTML>");
    }
    
    static HashMap<String,String> processRegime(String regime, HashMap<String, String> surveys) throws Exception {
	
	System.out.println("<h1> " + regime+" surveys </h1>");
	HashMap<String,String> remaining = new HashMap<String, String>();
	regime = regime.toLowerCase();
	ArrayList<String> names = new ArrayList<String>();
	for (String s: surveys.keySet()) {
	    if (surveys.get(s).toLowerCase().equals(regime)) {
		names.add(s);
	    } else {
		remaining.put(s, surveys.get(s));
	    }
	}
	java.util.Collections.sort(names);
	for (String s: names) {
		printDescription(s);
	}
	return remaining;
    }
    
    static void processRemaining(HashMap<String, String> surveys) throws Exception {
	if (surveys.size() < 1) {
	    return;
	}
	System.out.println("<p>-------------- Other surveys --------------<p>");
	for (String s: surveys.keySet()) {
	    printDescription(s);
	}
    }
    
    private static class RegimeFinder extends DefaultHandler {
	
	/** Buffer to accumulate text into */
	private StringBuffer buf;
	
	/** Are we in an active element? */
	private boolean active = false;
	
	/** Are we in the Regim setting? */
	private boolean inRegime = true;
	    
	private static String regime;
	    
	public String getRegime() {
	    return regime;
	}	
	
        public void startElement(String uri, String localName, String qName, Attributes attrib) {
	    
	    String lq = qName.toLowerCase();
	    
	    if (inRegime) {
	        active = true;
		buf    = new StringBuffer();
	    }
	    if (lq.equals("regime") ) {
		inRegime = true;
	    }
        }
    
        public void endElement(String uri, String localName, String qName) {
	    
	    String lq = qName.toLowerCase();
	    
	    if (lq.equals("regime")) {
		regime = new String(buf).trim();
		throw new ParsingTermination();
	    }
        }

        public void characters(char[] arr, int start, int len) {
	    if (active) {
	        buf.append(arr, start, len);
	    }
        }
    }
    
}
