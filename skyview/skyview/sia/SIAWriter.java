package skyview.sia;

import skyview.executive.Key;
import skyview.survey.Image;
import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;

import skyview.executive.Settings;

import nom.tam.fits.Header;

/** This class writes out SIA records for surveys
 *  which have coverage in the specified region.
 *  It assumes the the Checker class was used as
 *  the image finder.
 */
public class SIAWriter implements skyview.process.Processor {
    
    static int count = 0;
    
    static int getCount() {
	return count;
    }
 
    public String getName() {
	return "SIAWriter";
    }
    
    /** Get a description of this component */
    public String getDescription() {
	return "Write an SIA record or records for the given survey.";
    }
    
    /**
     */
    public void process(Image[] input, Image output, int[] osource, 
		        Sampler samp, DepthSampler dSampler)  {
	
	String survey   = Settings.get(Key._currentSurvey);
	String[] pos    = Settings.getArray(Key.pos);
	String[] pixels = Settings.getArray(Key.pixels);
	String[] size   = Settings.getArray(Key.size);
	String quicklook= SIA.getSIA().quicklook();
	String[] scale  = Settings.getArray(Key.scale);
	
	if (scale.length > 0) {
	  try {
	    
	    double xscale = Double.parseDouble(scale[0]);
	    double big   = 10000;
	    double small = 5;
	    double xsize  = Double.parseDouble(size[0]);
	    
	    if (Settings.has(Key.small)) {
		small = Double.parseDouble(Settings.get(Key.small));
		if (small < 0) {
		    small = 0;
		}
	    }
	    if (Settings.has(Key.big)) {
		big   = Double.parseDouble(Settings.get(Key.big));
	    }
	    
	    if (xsize/xscale < small) {
		return;
	    }
	    if (xsize/xscale > big) {
		return;
	    }
	
	  } catch (Exception e) {
	    // Just use defaults.
	    System.err.println("Error parsing big/small parameters");
	  }
	}
	
	String url = getURL(survey);
		
	if (Checker.getStatus()) {
	    count += 1;
	    if (SIA.getSIA().useFits()) {
	        doRecord(survey, pos, pixels, size, "image/fits", url+"&return=FITS", ""+count);
	    } 
	    if (quicklook != null) {
	        url += "&nofits=1&quicklook="+quicklook;
	        doRecord(survey, pos, pixels, size, "image/"+quicklook, url+"&return="+quicklook, ""+count);
	    }
	}
    }
    
    String getURL(String survey) {
	String url  = Settings.get(Key.SIABase);
	
	url += "position="+encode(Settings.get(Key.position));
	url += "&survey="+encode(survey);
	url += "&pixels="+encode(Settings.get(Key.pixels));
	url += "&sampler="+encode(Settings.get(Key.sampler));
	url += "&size="+encode(Settings.get(Key.size));
	url += "&projection="+encode(Settings.get(Key.projection));
	url += "&coordinates="+Settings.get(Key.coordinates);
	
	return url;
    }
				     
    
    void doRecord(String survey, String[] pos, String[] pixels, String[] size,
		  String format, String url, String log) {
	
	String[] scale = new String[]{"NaN", "NaN"};
	try {
	    scale[0] = "-"+Double.parseDouble(size[0])/Double.parseDouble(pixels[0]);
	    scale[1] = ""+Double.parseDouble(size[1])/Double.parseDouble(pixels[1]);
	} catch (Exception e) {
	}
	    
	System.out.print("<TR>");
	writeSingle(survey);
	writeSingle(pos[0]);
	writeSingle(pos[1]);
	writeSingle("2");  // Number of axes
	writeDuo(pixels);
	writeDuo(scale);
	writeSingle(format);
	writeSingle("F");
        System.out.println("<TD><![CDATA["+url+"]]></TD>");
	writeSingle(log);
	System.out.println("</TR>");
    }
    
    void writeSingle(String input) {
	System.out.print("<TD>"+input+"</TD>");
    }
    
    void writeDuo (String[] input) {
	if (input.length > 1) {
	    System.out.print("<TD>"+input[0]+" "+input[1]+"</TD>");
	} else {
	    System.out.print("<TD>"+input[0]+" "+input[0]+"</TD>");
	}
    }
    
    /** This should only be used in contexts
     *  when no FITS file is produced, so this method
     *  does nothing.
     */
    public void updateHeader(Header h) {
    }
	
    private String encode(String input) {
	try {
	    return java.net.URLEncoder.encode(input, "UTF-8");
	} catch (Exception e) {
	    return input;
	}
    }
}
    
