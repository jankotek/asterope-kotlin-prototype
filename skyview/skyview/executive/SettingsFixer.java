package skyview.executive;

/** This class looks at the settings and updates as needed for
 *  use with the standard Java classes.
 */
public class SettingsFixer implements SettingsUpdater {
    
        /** Update the settings associated with this smoother */
    public void updateSettings() {
	
	// Handle the smoothing argument and translate to a class
	//  to a smoothing postprocessor.
	
	if (Settings.has(Key.smooth)  && Settings.get(Key.smooth).length() > 0) {
	    String[] upd = Settings.getArray(Key.Postprocessor);
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
	        if (Settings.has(Key.Postprocessor)) {
		    Settings.put(Key.Postprocessor, cname+","+Settings.get(Key.Postprocessor));
	        } else {
	            Settings.put(Key.Postprocessor, cname);
	        }
	    }
	}
	
	// Handle a catalog request by generating a preprocessor
	// the will initiate the query and a postprocessor that
	// will use the results.
	if (Settings.has(Key.catalog)) {
	    
	    String[] pres = Settings.getArray(Key.Preprocessor);
	    String   preclass = "skyview.vo.CatalogProcessor";
	    
	    boolean found = false;
	    for (int i=0; i<pres.length; i += 1) {
		if (pres.equals(preclass)) {
		    found = true;
		}
	    }
	    if (!found) {
	        Settings.add(Key.Preprocessor,  preclass);
	    }
	    
	    String   postclass = "skyview.vo.CatalogPostProcessor";
	    
	    String[] posts = Settings.getArray(Key.Postprocessor);
	    found = false;
	    for (int i=0; i<posts.length; i += 1) {
		if (posts.equals(postclass)) {
		    found = true;
		}
	    }
	    if (!found) {
	        Settings.add(Key.Postprocessor,  postclass);
	    }
	}
      
	// If the user has requested graphic content, then they shouldn't
	// have to say they want a graphic image.
	if ( Settings.has(Key.invert) || Settings.has(Key.grid) ||
	     Settings.has(Key.lut)    || Settings.has(Key.scaling) ||
	     Settings.has(Key.rgb)    || Settings.has(Key.quicklook) ||
	     Settings.has(Key.contour) ||
	     Settings.has(Key.imagej)) {
	    Settings.add(Key.Postprocessor, "skyview.ij.IJProcessor");
	    if (!Settings.has(Key.quicklook) && !Settings.has(Key.imagej)) {
		Settings.add(Key.quicklook, "jpg");
	    }
        }
	
	// Set JPEGs as the default quicklook format.
	if (Settings.has(Key.quicklook)) {
	    String ql = Settings.get(Key.quicklook);
	    if (ql == null || ql.length() == 0) {
		Settings.put(Key.quicklook, "jpeg");
	    }
	}
	
	// If the user has requested the AddingMosaicker, then
	// they should use the null image finder.
	if (Settings.has(Key.Mosaicker)) {
	    String mos = Settings.get(Key.Mosaicker);
	    if (mos.indexOf ("AddingMosaicker") >= 0) {
		Settings.put(Key.ImageFinder, "Bypass");
	    }
	}
    }
}
