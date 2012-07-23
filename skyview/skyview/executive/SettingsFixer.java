package skyview.executive;

/** This class looks at the settings and updates as needed for
 *  use with the standard Java classes.
 */
public class SettingsFixer implements SettingsUpdater {
    
        /** Update the settings associated with this smoother */
    public void updateSettings() {
	
	// Handle the smoothing argument and translate to a class
	//  to a smoothing postprocessor.
	
	if (Settings.has("smooth")  && Settings.get("smooth").length() > 0) {
	    String[] upd = Settings.getArray("postprocessor");
	    String cname = "skyview.data.BoxSmoother";
	    boolean found = false;
	    for (int i=0; i<upd.length; i += 1) {
		if (cname.equals(upd[i])) {
		    found = true;
		    break;
		}
	    }
	    if (!found) {
	        // Put smooth before other postprocessors.
	        if (Settings.has("postprocessor")) {
		    Settings.put("postprocessor", cname+","+Settings.get("postprocessor"));
	        } else {
	            Settings.put("postprocessor", cname);
	        }
	    }
	}
	
	// Handle a catalog request by generating a preprocessor
	// the will initiate the query and a postprocessor that
	// will use the results.
	if (Settings.has("catalog")) {
	    
	    String[] pres = Settings.getArray("preprocessor");
	    String   preclass = "skyview.vo.CatalogProcessor";
	    
	    boolean found = false;
	    for (int i=0; i<pres.length; i += 1) {
		if (pres.equals(preclass)) {
		    found = true;
		}
	    }
	    if (!found) {
	        Settings.add("preprocessor",  preclass);
	    }
	    
	    String   postclass = "skyview.vo.CatalogPostProcessor";
	    
	    String[] posts = Settings.getArray("postprocessor");
	    found = false;
	    for (int i=0; i<posts.length; i += 1) {
		if (posts.equals(postclass)) {
		    found = true;
		}
	    }
	    if (!found) {
	        Settings.add("postprocessor",  postclass);
	    }
	}
      
	// If the user has requested graphic content, then they shouldn't
	// have to say they want a graphic image.
	if ( Settings.has("invert") || Settings.has("grid") ||
	     Settings.has("lut")    || Settings.has("scaling") ||
	     Settings.has("rgb")    || Settings.has("quicklook") ||
	     Settings.has("contour") ||
	     Settings.has("imagej")) {
	    Settings.add("postprocessor", "skyview.ij.IJProcessor");
	    if (!Settings.has("quicklook") && !Settings.has("imagej")) {
		Settings.add("quicklook", "jpg");
	    }
        }
	
	// Set JPEGs as the default quicklook format.
	if (Settings.has("quicklook")) {
	    String ql = Settings.get("quicklook");
	    if (ql == null || ql.length() == 0) {
		Settings.put("quicklook", "jpeg");
	    }
	}
	
	// If the user has requested the AddingMosaicker, then
	// they should use the null image finder.
	if (Settings.has("mosaicker")) {
	    String mos = Settings.get("mosaicker");
	    if (mos.indexOf ("AddingMosaicker") >= 0) {
		Settings.put("ImageFinder", "Bypass");
	    }
	}
    }
}
