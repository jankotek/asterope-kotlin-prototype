package skyview.survey;

/** Translate a simple survey name into an XML file.
 * 
 *  Surveys can come from any of three sources:
 *   <ul>
 *     <li> If the resource survey.manifest
 *      is available, each line in the manifest defines a survey XML description.
 *      As a system resource this file will be searched for in all of the
 *      places where a class might be looked for.  When SkyView is
 *      executed within a JAR file the survey manifest will be included.
 *     <li> The user can specify a base directory where
 *      all XML files are assumed to describe SkyView surveys.
 *     <li> The user can specify a survey on the command line
 *      as surveyxml=file
 *    </ul>
 *   If a survey is defined in multiple locations, the later definition
 *   (in terms of this listing) supercedes the earlier.  Thus users
 *   can override the default definitions of surveys.
 *   
 */

import skyview.executive.Key;
import skyview.survey.XMLSurvey;
import skyview.executive.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FilenameFilter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import java.util.regex.Pattern;


public class XMLSurveyFinder implements SurveyFinder {
    
    /** Default location for survey manifest */
    private static String  	defaultSurveyManifest="survey.manifest";
    
    /** Filter to get only the XML files */
    private FilenameFilter 	filter = new FilenameFilter() {
	public boolean accept(File file, String name) {
	    return name.endsWith(".xml") || name.endsWith(".XML");
	}
    };
    
    /** Hashmap binding shortname to XML file */
    private HashMap<String,String> hash = new HashMap<String,String>();

    /** Set up the SurveyFinder and populate the map */
    public XMLSurveyFinder() {
	
	getSurveysFromManifest();
	getSurveysFromRoot();
	getSurveysFromUser();
    }
    
    /** Get the surveys in the document root area */
    protected void getSurveysFromRoot() {
	
	String[] roots = Settings.getArray(Key.xmlroot);
	
        for (String root: roots) {
	    File     f      = new File(root);
	    String[] match  = f.list(filter);
	
	    for (String survey: match) {
	        process(root+"/"+survey);
	    }
	}
    }
    
    /** Get user specified surveys */
    protected void getSurveysFromUser() {
	
	String userSurveys= Settings.get(Key.surveyxml);
	if (userSurveys != null) {
	    
	    Pattern comma    = Pattern.compile(",");
	    String[] surveys = comma.split(userSurveys);
	    for (int i=0; i<surveys.length; i += 1) {
		process(surveys[i]);
	    }
	}
    }

    /** Get surveys from a user manifest.  This
     *  is how SkyView-in-a-Jar gets its surveys.
     */
    protected void getSurveysFromManifest() {
	
	try {
	    
	    String manifest = Settings.get(Key.surveymanifest);
	    if (manifest == null) {
		manifest = defaultSurveyManifest;
	    }
	
	    InputStream is = skyview.survey.Util.getResourceOrFile(manifest);
	    if (is == null) {
		return;
	    }
	    BufferedReader br = new BufferedReader(new InputStreamReader(is));
	    String survey;
	    while ( (survey = br.readLine() ) != null) {
		survey=survey.trim();
		if (survey.length() == 0 || survey.charAt(0) == '#') {
		    continue;
		}
		process(survey);
	    }
	    
	} catch (Exception e) {
	    System.err.println("Error loading surveys from manifest: "+e+"\n Processing continues");
	    // Continue with whatever surveys have already been loaded.
	}
    }
							     


    /** Do we have this survey? */
    public Survey find(String shortName) {
	String fileName =  findFile(shortName);
	if (fileName == null) {
	    return null;
	} else {
	    return new XMLSurvey(fileName);
	}
    }
    
    /** Find the survey file given the short name */
    public String findFile(String shortName) {
	return hash.get(shortName.toLowerCase());
    }
    
    /** Parse a single file */
    private void process(String name) {
	
	try {
            Reader is = XMLSurvey.getSurveyReader(name);
	
            SAXParser sp = SAXParserFactory.newInstance().newSAXParser();
	    // This should fill up the names.
	
            sp.parse(new InputSource(is), 
	  	     new XMLSurveyFinder.NameCallBack(name)
		    );
	    is.close();
	    
	} catch(ParsingTermination e){
	} catch (Exception e) {
	    e.printStackTrace(System.err);
	    throw new Error("Unexpected exception parsing file: "+name+" in SurveyFinder:"+e);
	    
	}
    }
    
    /** What surveys do we know about? */
    public String[] getSurveys() {	
	return (String[]) hash.keySet().toArray(new String[0]);
    }
    
    /** This is the class the does the parsing. */
    private class NameCallBack extends DefaultHandler {
	
	/** Buffer to accumulate text into */
	private StringBuffer buf;
	
	/** Are we in an active element? */
	private boolean active = false;
	
	/** The file */
	String fileName;	
       
	NameCallBack(String file) {
	    this.fileName = file;
	}
	Pattern pat = Pattern.compile(",");

	    
	
        public void startElement(String uri, String localName, String qName, Attributes attrib) {
	    
	    if (qName.equals("ShortName")) {
	        active = true;
		buf    = new StringBuffer();
	    }
        }
    
        public void endElement(String uri, String localName, String qName) {
	    
	    if (qName.equals("ShortName")) {
		String name = new String(buf).trim();
		String[] names = pat.split(name);
		for (String cName: names) {
		    cName = cName.trim();
		    hash.put(cName.toLowerCase(), fileName);
		}
		// Terminate the parsing... Is there a better
		// way?
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
