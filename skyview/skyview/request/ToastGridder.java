package skyview.request;

import skyview.executive.Key;
import skyview.executive.Settings;
import static org.apache.commons.math3.util.FastMath.*;

/** This finds the correct center and
  * scale for a user a TOAST grid output.
  */
public class ToastGridder implements skyview.executive.SettingsUpdater {
    
    /** Update the global settings for a TOAST Tile */
    public void updateSettings() {
	
        if (!Settings.has(Key.level)  || !Settings.get(Key.projection,"").equals("Toa")) {
	    return;
	}
	
	int level  = Integer.parseInt(Settings.get(Key.level));
	int tileX  = Integer.parseInt(Settings.get(Key.tileX, "0"));
	int tileY  = Integer.parseInt(Settings.get(Key.tileY, "0"));
	
        String format = Settings.get(Key.format, "png");
	
	int div    = Integer.parseInt(Settings.get(Key.Subdiv, "8"));
	
	int npix   = (int) pow(2, div);
	int ntile  = (int) pow(2, level);
	
	
	double scale = 180./(npix*ntile);
	
	double xoffset = npix*(tileX - 0.5*ntile + 0.5);
	double yoffset = npix*(tileY - 0.5*ntile + 0.5);
	
	// Set the center of the coordinates to 0.,90.
	Settings.put(Key.position, "0.,90.");
	
	if ("Clip".equals(Settings.get(Key.sampler))) {
	    Settings.put(Key.pixels, ""+npix);
	} else {
	    // Set the number of pixels to 2^n + 1
	    Settings.put(Key.pixels, ""+(npix+1));
	}
	
	// Set the offset of the image appropriately
	Settings.put(Key.offset, xoffset+","+yoffset);
	
	// Set the scale for the data.
	Settings.put(Key.scale, "-"+scale+","+scale);
	
	// Set the ToastGrid parameter so that ToaProjecter
	// knows what to do.
	Settings.put(Key.ToastGrid, level+","+tileX+","+tileY+","+div);
	
	// Set the quicklook output format.
	Settings.put(Key.quicklook, format);
	
	return;
    }
}
