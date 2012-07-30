package skyview.request;

import skyview.executive.Key;
import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;

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
		Settings.get(Key.name_)+"</h2><p>"+
	        "Error: "+Settings.get(Key.ErrorMsg)+"<p><p>");
	} else {
	    updateSettings(output, samp);
	    setSettings();
	    printoutTemplate(Key.SurveyTemplate);
	    if (Settings.has(Key.onlinetext)) {
		String text = Settings.get(Key.onlinetext);
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
	    index = Integer.parseInt(Settings.get(Key._surveyCount));
        } catch (Exception e) {}
	
	System.out.println("<script language='javascript'>");
	System.out.println("x = new Object()");
	System.out.println("surveySettings["+index+"] = x");
	Key[] keys = Settings.getKeys();
	java.util.Arrays.sort(keys);
	for(Key key: keys) {
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
	printoutTemplate(Key.HeaderTemplate);
    }
    
    public void writeFooter() {
	printoutTemplate(Key.FooterTemplate);
    }
    
    protected void printoutTemplate(Key  fileSetting) {
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
	
	Settings.put(Key._imageMin,    ""+min);
	Settings.put(Key._imageMax,    ""+max);
	Settings.put(Key._ImageXPixel, ""+output.getWidth());
	Settings.put(Key._ImageYPixel, ""+output.getHeight());
	

	String[] sizes = Settings.getArray(Key.size);
	Settings.put(Key._ImageXSize, sizes[0]);
	Settings.put(Key._ImageYSize, sizes[1]);

	if (Settings.get(Key.position) == null) {
	    Settings.put(Key.position,Settings.get(Key.lon)+","+
			            Settings.get(Key.lat));
        } else {
           if (Settings.has(Key.ReqXPos) && Settings.has(Key.ReqYPos)) {

             //--- Add requested center coordiates in decimal format to 
	     //--- settings only if user did not enter decimal format.
             Pattern pattern = Pattern.compile(Settings.get(Key.ReqXPos) +
		"\\s*,?\\s*" +Settings.get(Key.ReqYPos));
	     Matcher matcher = pattern.matcher(Settings.get(Key.position)) ;
             boolean found = false;           
             while (matcher.find()) {               
                found = true;
            }
            if(!found){
              Settings.put(Key.requested_coords, Settings.get(Key.ReqXPos) + ", " +
                                            Settings.get(Key.ReqYPos));
            }

           }
        }

	if (Settings.get(Key.scaling) == null) {
	    Settings.put(Key.scaling, "Log");
	}
	
	String format = Settings.get(Key.quicklook).toLowerCase();
	if (format.equals("jpeg")) {
	    format="jpg";
	}

	String imagepath =  Settings.get(Key.output) +"." +format;
	String fitspath  =  Settings.get(Key.output);
	String catpath  =  Settings.get(Key.catalogFile);

        //--- Remove new lines and single quotes from meta data for 
	//--- easier javascript handling in HTML template files
        if (Settings.has(Key._meta_copyright)) {
           String copyright = Settings.get(Key._meta_copyright);
           copyright = copyright.replace("\n"," ");
           copyright = copyright.replace("\'","\"");
           Settings.put(Key._meta_copyright,copyright);
        }
        if (Settings.has(Key._meta_provenance)) {
           String provenance = Settings.get(Key._meta_provenance);
           provenance = provenance.replace("\n"," ");
           provenance = provenance.replace("\'","\"");
           Settings.put(Key._meta_provenance,provenance);
        }

        //--- Make path adjustment for web chrooted area if necessary
        if (Settings.has(Key.webrootpath)) {
           String fullpath = fitspath;
           String webpath = Settings.get(Key.webrootpath);
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
	Settings.put(Key._output_ql,        imagepath);
	Settings.put(Key._output,           fitspath);
	Settings.put(Key._catalogFile,      catpath);

        //--- colortable image
        if (Settings.has(Key.lutcbarpath) && Settings.has(Key.lut)) {
           File fil = new File(Settings.get(Key.lut));
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
              if (Settings.has(Key.invert)) {
                 if (Settings.get(Key.invert).equals("on")) {
                    path=path+"_inv";
                 }
              }
	      Settings.put(Key._ctnumb, Settings.get(Key.lutcbarpath)+"/" +
		   path.toLowerCase());
           }
        }

	Settings.put(Key._CoordinateSystem,
		output.getWCS().getCoordinateSystem().getName());
	Settings.put(Key._Projection,
		output.getWCS().getProjection().getProjecter().getName());
	if (samp != null) {
            Settings.put(Key._Sampler, samp.getName());
        } else {
            Settings.put(Key._Sampler, "null");
        }
    }
}
