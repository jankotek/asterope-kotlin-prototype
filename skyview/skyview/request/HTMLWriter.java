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
import java.io.File;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class HTMLWriter implements Processor {
    
    public void process(Image[] inputs, Image output, int[] sources,
			Sampler samp, DepthSampler dpSamp) {
	if (output == null) {
	    System.out.println("<h2> Error processing survey: "+
		Settings.get("name")+"</h2><p>"+
	        "Error: "+Settings.get("ErrorMsg")+"<p><p>");
	} else {
	    updateSettings(output, samp);
	    setSettings();
	    printoutTemplate("SurveyTemplate");
	    if (Settings.has("onlinetext")) {
		String text = Settings.get("onlinetext");
		if (text.length() > 0) {
	            text = SettingsMatcher.replaceSettings(text);
		}
		System.out.print(text);
	    }
	}
    }
    
    public void updateHeader(nom.tam.fits.Header hdr) {
    }
    
    /** This method writes out the settings so that
     *  JavaScript can use them.
     */
    protected void setSettings() {
	int index = 0;
	try {
	    index = Integer.parseInt(Settings.get("_surveycount"));
        } catch (Exception e) {}
	
	System.out.println("<script language='javascript'>");
	System.out.println("x = new Object()");
	System.out.println("surveySettings["+index+"] = x");
	String[] keys = Settings.getKeys();
	java.util.Arrays.sort(keys);
	for(String key: keys) {
	    String val = Settings.get(key);
	    if (val != null) {
	        val = val.replaceAll("'", "");
		val = val.replaceAll("\n", " ");
	        System.out.println("x['"+key+"']='"+val+"'");
	    }
	}
	System.out.println("</script>");
    }
						     
	
    public String getName() {
	return "HTMLWriter";
    }
    
    public String getDescription() {
	return "Writes HTML wrappers for generated images.";
    }
    
    public void writeHeader() {
	
	System.out.println("Content-type: text/html\n");
	printoutTemplate("HeaderTemplate");
    }
    
    public void writeFooter() {
	printoutTemplate("FooterTemplate");
    }
    
    protected void printoutTemplate(String fileSetting) {
	String file = Settings.get(fileSetting);
	if (file != null) {
	    String content = slurp(file);
	    if (content != null) {
	        content = SettingsMatcher.replaceSettings(content);
	        if (content != null) {
	            System.out.print(content);
		}
	    }
	}
    }
    
    public static String slurp(String file) {
	
	try {
	    BufferedReader bf = new BufferedReader(
				  new InputStreamReader(
				    skyview.survey.Util.getResourceOrFile(file)
						       )
						   );
	    StringWriter   sw = new StringWriter();
	    String line;
	    while ( (line =bf.readLine()) != null) {
		sw.write(line);
		sw.write("\n");
	    }
	    sw.close();
	    bf.close();
	    return sw.toString();
	} catch (Exception e) {
	    return null;
	}
    }
    
    /** Update the settings before creating the appropiate HTML */
    protected void updateSettings(Image output, Sampler samp) {
	
	double[] data = output.getDataArray();
	double min= data[0];
	double max= data[0];
	for (double d: data) {
	    if (d < min) {
		min = d;
	    } else if (d > max) {
		max = d;
	    }
	}
	
	Settings.put("_ImageMin",    ""+min);
	Settings.put("_ImageMax",    ""+max);
	Settings.put("_ImageXPixel", ""+output.getWidth());
	Settings.put("_ImageYPixel", ""+output.getHeight());
	

	String[] sizes = Settings.getArray("size");
	Settings.put("_ImageXSize", sizes[0]);
	Settings.put("_ImageYSize", sizes[1]);

	if (Settings.get("position") == null) {
	    Settings.put("position",Settings.get("lon")+","+
			            Settings.get("lat"));
        } else {
           if (Settings.has("ReqXPos") && Settings.has("ReqYPos")) {

             //--- Add requested center coordiates in decimal format to 
	     //--- settings only if user did not enter decimal format.
             Pattern pattern = Pattern.compile(Settings.get("ReqXPos") +
		"\\s*,?\\s*" +Settings.get("ReqYPos"));
	     Matcher matcher = pattern.matcher(Settings.get("position")) ;
             boolean found = false;           
             while (matcher.find()) {               
                found = true;
            }
            if(!found){
              Settings.put("requested_coords", Settings.get("ReqXPos") + ", " +
                                            Settings.get("ReqYPos"));
            }

           }
        }

	if (Settings.get("scaling") == null) {
	    Settings.put("scaling", "Log");
	}
	
	String format = Settings.get("quicklook").toLowerCase();
	if (format.equals("jpeg")) {
	    format="jpg";
	}

	String imagepath =  Settings.get("output") +"." +format;
	String fitspath  =  Settings.get("output");
	String catpath  =  Settings.get("catalogFile");

        //--- Remove new lines and single quotes from meta data for 
	//--- easier javascript handling in HTML template files
        if (Settings.has("_meta_copyright")) {
           String copyright = Settings.get("_meta_copyright");
           copyright = copyright.replace("\n"," ");
           copyright = copyright.replace("\'","\"");
           Settings.put("_meta_copyright",copyright);
        }
        if (Settings.has("_meta_provenance")) {
           String provenance = Settings.get("_meta_provenance");
           provenance = provenance.replace("\n"," ");
           provenance = provenance.replace("\'","\"");
           Settings.put("_meta_provenance",provenance);
        }

        //--- Make path adjustment for web chrooted area if necessary
        if (Settings.has("webrootpath")) {
           String fullpath = fitspath;
           String webpath = Settings.get("webrootpath");
           if (fullpath.startsWith(webpath)) {
	      fitspath = 
		fullpath.substring(webpath.length(),fullpath.length());
              if (!fitspath.startsWith("/")) {fitspath = "/"+fitspath;}

	      imagepath = 
		fullpath.substring(webpath.length(),fullpath.length())+
		"."+format;
              if (!imagepath.startsWith("/")) {
		  imagepath = "/"+imagepath;
	      }
	       
              if (catpath != null) {
	         catpath = 
		   catpath.substring(webpath.length(),catpath.length());
                 if (!catpath.startsWith("/")) {
		     catpath = "/"+catpath;
		 }
              }
           }
        } 
	Settings.put("_output_ql",        imagepath);
	Settings.put("_output",           fitspath);
	Settings.put("_catalogFile",      catpath);

        //--- colortable image
        if (Settings.has("lutcbarpath") && Settings.has("lut")) {
           File fil = new File(Settings.get("lut"));
	   // Get the end of the file name
	   String path = fil.getName();
	    
	   int off = path.indexOf('.');
           if (off < 0 && path.length() >0) {
	       off=path.length();
	   }
	    
           if (off >=0) {
              path = path.substring(0,off);
              path = path.replace("-","");
              path = path.replace(" ","");
              if (Settings.has("invert")) {
                 if (Settings.get("invert").equals("on")) {
                    path=path+"_inv";
                 }
              }
	      Settings.put("_ctnumb", Settings.get("lutcbarpath")+"/" +
		   path.toLowerCase());
           }
        }

	Settings.put("_CoordinateSystem", 
		output.getWCS().getCoordinateSystem().getName());
	Settings.put("_Projection",       
		output.getWCS().getProjection().getProjecter().getName());
	if (samp != null) {
            Settings.put("_Sampler", samp.getName());
        } else {
            Settings.put("_Sampler", "null");
        }
    }
}
