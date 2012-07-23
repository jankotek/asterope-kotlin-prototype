package skyview.request;

import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;
import skyview.geometry.Scaler;

import skyview.process.Processor;
import skyview.survey.Image;
import skyview.executive.Settings;

import java.io.StringWriter;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class RGBWriter extends HTMLWriter {
    
    private static java.util.HashMap<String,String> saved = new java.util.HashMap<String,String>();
    
    public RGBWriter() {
	System.err.println("Creating RGBWriter");
    }
    public void process(Image[] inputs, Image output, int[] sources,
			Sampler samp, DepthSampler dpSamp) {
	System.err.println("Calling process:"+Settings.get("output"));
	
	if (output == null) {
	    System.out.println("<h2> Error processing survey:"+
		Settings.get("name")+"</h2><p>"+
		"Error: "+Settings.get("ErrorMsg")+"<p><p>");
	} else {
	    updateSettings(output, samp);
	    setSettings();
	    int index = 0;
	    String cnt = Settings.get("_surveyCount");
	    
	    if (cnt != null) {
		index = Integer.parseInt(cnt)-1;
	    }
	    
	    saved.put("_name"+index,     Settings.get("name"));
	    saved.put("_survey"+index,   Settings.getArray("survey")[index]);
	    saved.put("_imageMin"+index, Settings.get("_imageMin"));
	    saved.put("_imageMax"+index, Settings.get("_imageMax"));
	    saved.put("_output"+index,   Settings.get("output"));
	    
	    if (index == Settings.getArray("survey").length-1) {
		
		String out = Settings.get("_output");
	        out = out.substring(0,out.length()-1)+"rgb.jpg";
		Settings.put("_output_rgb", out);
		
		for (String key: saved.keySet()) {
		    System.err.println("Restoring: "+key);
		    Settings.put(key, saved.get(key));
		}
		
	        printoutTemplate("RGBTemplate");
	    }
	}
    }
    
    public String getName() {
	return "RGBWriter";
    }
    
    public String getDescription() {
	return "Writes HTML wrappers for generated RGB images.";
    }
    
    /** Update the settings before creating the appropiate HTML */
    protected void updateSettings(Image output, Sampler samp) {
	super.updateSettings(output,samp);
    }
}
