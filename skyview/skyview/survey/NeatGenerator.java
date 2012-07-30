package skyview.survey;

import skyview.executive.Key;
import skyview.executive.Settings;
import skyview.survey.Image;

import java.io.BufferedInputStream;
import java.net.URL;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

/** This class gets a set of candidates from a SIAP request */
public class NeatGenerator extends SIAPGenerator {
    
    /** Find the base URL for the SIAP service and adjust as needed */
    protected String getBaseURL()  {
	
	String urlString = Settings.get(Key.SiapURL);
	if (Settings.has(Key.NEAT_REGION)) {
	    urlString += "NEAT_REGION="+Settings.get(Key.NEAT_REGION)+"&";
	}
	return urlString;
    }
}
