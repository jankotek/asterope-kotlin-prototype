package skyview.request;

//import skyview.executive.Settings;

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

import java.util.HashMap;
import java.util.ArrayList;

import net.ivoa.util.CGI;
import skyview.executive.Settings;

/** This class writes an HTML description of a survey based upon
 *  its XML survey file.
 */
public class XMLGallery {
    
    static CGI             cgi = new CGI();
    
    public static void main(String[] args) {

        // Add any settings files now.
	for (int i=0; i<args.length; i += 1) {
	    Settings.updateFromFile(args[i]);
	}

	try {

	    printTemplate("../userimages/gallery.xml");
	} catch (Exception e) {
	    error("Exception processing request: "+e);
	}
    }
    
    public static void printTemplate(String name) throws Exception {
	System.out.println("Content-type: text/html\n");
	printGallery(name);
    }
	
    
    public static void error(String msg) {
	System.out.println("Content-type: text/html\n\n<h2>Error during processing</h2>"+msg);
    }
    
    public static void printGallery(String filename) throws Exception {
        if (Settings.get("GalleryXSLT") == null) {
	    Settings.put("GalleryXSLT", "cgifiles/imagegallerythumbs.xsl");
	}

	String xslt = Settings.get("GalleryXSLT");
	if (xslt == null) {
	    error("No description transformation file in settings");
	} 
	
	Source xmlSource = 
	   new StreamSource(skyview.survey.Util.getResourceOrFile(filename));
	Source xslSource = 
	   new StreamSource(skyview.survey.Util.getResourceOrFile(xslt));
        
	
	StringWriter sw  = new StringWriter();
	Result output    = new StreamResult(sw);
	Transformer trans= 
	   TransformerFactory.newInstance().newTransformer(xslSource);
	trans.transform(xmlSource, output);
	sw.close();
	
	String outHTML   = sw.toString();
	// Get rid of gratuitious added by transformer.
	//int off1 = outHTML.indexOf('>');
	//if (off1 > 0) {
	    //outHTML = outHTML.substring(off1+1);
	//}
	//outHTML = outHTML.replace("</HTML>", "");
	//outHTML = outHTML.replaceAll("\\<H2\\>\\s*([^>]*)\\s*\\</H2\\>", "<A NAME='$1'><H2>$1</H2></A>");
	
	System.out.print(outHTML);
    }
    
        public void startElement(String uri, String localName, String qName, Attributes attrib) {
	    
	    String lq = qName.toLowerCase();
	    
        }
    
        public void endElement(String uri, String localName, String qName) {
	    
	    String lq = qName.toLowerCase();
	    
        }

        public void characters(char[] arr, int start, int len) {
        }
    }
    
