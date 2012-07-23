package skyview.request;

import skyview.executive.Settings;

/** This finds the correct center and
  * scale for a user a TOAST grid output.
  */
public class ToastGridder implements skyview.executive.SettingsUpdater {
    
    /** Update the global settings for a TOAST Tile */
    public void updateSettings() {
	
        if (!Settings.has("level")  || !Settings.get("Projection","").equals("Toa")) {
	    return;
	}
	
	int level  = Integer.parseInt(Settings.get("level"));
	int tileX  = Integer.parseInt(Settings.get("tileX", "0"));
	int tileY  = Integer.parseInt(Settings.get("tileY", "0"));
	
        String format = Settings.get("format", "png");
	
	int div    = Integer.parseInt(Settings.get("Subdiv", "8"));
	
	int npix   = (int) Math.pow(2, div);
	int ntile  = (int) Math.pow(2, level);
	
	
	double scale = 180./(npix*ntile);
	
	double xoffset = npix*(tileX - 0.5*ntile + 0.5);
	double yoffset = npix*(tileY - 0.5*ntile + 0.5);
	
	// Set the center of the coordinates to 0.,90.
	Settings.put("Position", "0.,90.");
	
	if ("Clip".equals(Settings.get("Sampler"))) {
	    Settings.put("Pixels", ""+npix);
	} else {
	    // Set the number of pixels to 2^n + 1
	    Settings.put("Pixels", ""+(npix+1));
	}
	
	// Set the offset of the image appropriately
	Settings.put("Offset", xoffset+","+yoffset);
	
	// Set the scale for the data.
	Settings.put("Scale", "-"+scale+","+scale);
	
	// Set the ToastGrid parameter so that ToaProjecter
	// knows what to do.
	Settings.put("ToastGrid", level+","+tileX+","+tileY+","+div);
	
	// Set the quicklook output format.
	Settings.put("Quicklook", format);
	
	return;
    }
}
