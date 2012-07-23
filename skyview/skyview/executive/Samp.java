package skyview.executive;

//import org.astrogrid.samp.*;
//import org.astrogrid.samp.client.*;
//import org.astrogrid.samp.xmlrpc.*;
//import org.astrogrid.samp.xmlrpc.internal.*;

import java.util.HashMap;
import java.io.File;

/** This class implements the SAMP protocol for SkyView.
 *  SAMP is designed to promote interprocess communication between
 *  desktop applications.
 *  When the SAMP setting is specified SkyView will connect to the
 *  SAMP hub and send a image.load.fits message to all registered
 *  applications for the images that have been generated.
 */
 public class Samp {

    // All this nasty static stuff should be changed, but
    // probably doesn't matter too much for most users.
//    private static HubConnection hc;
    public static void notifyFile() {

//	try {
//
//	    HashMap  hm  = new HashMap();
//	    String uri = new File(Settings.get("output_fits")).toURI().toString();
//	    hm.put("url", uri);
//	    hm.put("name", "SkV:"+Settings.get("_currentSurvey"));
//	    hm.put("image-id", "SkV:"+Settings.get("_currentSurvey"));
//
//	    Message  msg = new Message("image.load.fits", hm);
//
//	    // Only connect if we don't already have a connection
//	    if (hc == null) {
//	        StandardClientProfile scp = StandardClientProfile.getInstance();
//	        if (scp != null) {
//	            hc = scp.register();
//		    if (hc != null) {
//			HashMap meta = new HashMap();
//			meta.put("samp.name", "SkyView "+Imager.getVersion());
//			meta.put("samp.description.text", "SkyView Image Generation Service");
//			meta.put("samp.documentation.url", Settings.get("URLLocalHelp"));
//			hc.declareMetadata(meta);
//	            } else {
//		        System.err.println("  Unable to get HubConnection.  No hub?");
//	            }
//	        } else {
//	            System.err.println("  Unable to get SAMP standard profile");
//	        }
//            }
//
//	    if (hc != null) {
//	        hc.notifyAll(msg);
//	    }
//	} catch (Exception e) {
//	    System.err.println("  Unable to send SAMP notification. Error: "+e);
//	}
    }
}
