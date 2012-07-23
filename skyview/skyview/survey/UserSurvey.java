package skyview.survey;

/** This class allows the user to define a set of
 *  files in the settings and use them as their survey files without
 *  having to build up any XML for them.
 */

import nom.tam.fits.Header;
import skyview.geometry.Position;
import skyview.executive.Settings;


public class UserSurvey implements Survey {
    
    private String[] files;
    public UserSurvey() {
	files = Settings.getArray("userfile");
    }
    
    public String getName() {
	return "UserSurvey";
    }
    
    public String getDescription() {
	return "A survey comprised of a set of user specified files.";
    }
	
       
    
    /** This is a NOP for the User survey */
    public void updateSettings() {
    }
    
    public void updateHeader(Header fitsHeader) {
	
	try {
	    fitsHeader.insertHistory("");
	    fitsHeader.insertHistory("User defined survey comprised of the files:");
            for (int i=0; i<files.length; i += 1) {
	        fitsHeader.insertHistory("   "+files[i]);
	    }
	
	    fitsHeader.insertHistory("");
	} catch (Exception e) {
	    System.err.println("Error updating header in UserSurvey:"+e);
	    // Just continue processing.
	}
    }
    
    public Image[] getImages(Position pos, double size) {
	
	if (files == null) {
	    throw new Error("Attempt to invoke User survey without specifying constituent files");
	}
	
	Image[] userImages = new FitsImage[files.length];
	for (int i=0; i<files.length; i += 1) {
	    try {
	        userImages[i] = new FitsImage(files[i]);
	    } catch (Exception e) {
		System.err.println("Unable to read user image:"+files[i]+"\n"+e);
		e.printStackTrace();
		throw new Error("Unreadable user image");
	    }
	}
	return userImages;
    }
}
    
    
