package skyview.survey;

/** This class implements a cache where images
 *  may be stored after being retrieved.  This class
 *  will generate a FitsImage if the file is in the
 *  cache, or a proxy image if it is not.  The proxy
 *  will have an approximation to the WCS of the real image.
 */

import skyview.executive.Settings;

import skyview.survey.Image;
import skyview.survey.ProxyImage;

import skyview.geometry.Scaler;
import skyview.geometry.Projection;
import skyview.geometry.CoordinateSystem;
import skyview.geometry.WCS;

import java.io.File;

public class CachingImageFactory implements ImageFactory {
    
    static final public String DFT_CACHE  = "." + File.separator + "skycache" + File.separator;
    
    static private java.util.regex.Pattern comma = java.util.regex.Pattern.compile(",");
    public Image factory(String spell) {
	
	if (Settings.get("SpellSuffix") != null) {
	    spell += Settings.get("SpellSuffix");
	}
	if (Settings.get("SpellPrefix") != null) {
	    spell = Settings.get("SpellPrefix") + spell;
	}
	// The spell given to this parser is:
	//   url,file,ra,dec,proj,csys,nx,ny,dx,dy
	//    0    1   2  3    4   5    6  7  8  9  
	// 
	//   url,file,ra,dec,proj,csys,nx,ny,cd11,cd12,cd21,cd22
	//    0    1   2  3    4   5    6  7 8     9   10   11
	//
	// For projections with a fixed reference value, rather than
	// ra and dec, the numbers give the reference pixel values.

	String[] tokens = comma.split(spell);
	
	if (Settings.has("LocalURL")) {
	    String url = tokens[0];
	    String[] fields = Settings.getArray("LocalURL");
	    if (url.startsWith(fields[0])) {
		// Replace the beginning of the URL with the local file specification
		url = fields[1]+url.substring(fields[0].length());
		if (new File(url).exists()) {
		    try {
		        return new FitsImage(url);
		    } catch (SurveyException s) {
			// Just fail over to doing the remote access.
		    }
		}
	    }
	}
	    
        String   file        = tokens[1];
	
	String   cacheString  = Settings.get("cache", DFT_CACHE);
	String[] caches       = comma.split(cacheString);
	
	boolean  appendSurvey = Settings.has("SaveBySurvey");
	String   subdir       =  null;
	if (appendSurvey) {
	    String[]   dirs = Settings.getArray("shortname");
	    if (dirs.length == 0) {
	        appendSurvey = false;
	    } else {
		subdir = dirs[0];
		subdir = subdir.replaceAll("[^a-zA-Z0-9\\-\\_\\+\\.]", "_");
	    }
	}
	
	// First try n the caches without the survey
	// name appended
	for (String cache: caches) {
	    String test = cache+file;
	    if (new java.io.File(test).exists()) {
		try {
		    return new FitsImage(test);
		} catch(Exception e) {
		    System.err.println("Unexpected exception reading cached image:"+test+" :: "+e);
		    System.err.println("Trying to download the file.");
		}
	    }
	}
	
	// Now if the user has asked for the cache to
	// be split, try inside...
	if (appendSurvey) {
	    for (String cache: caches) {
	        String test = cache+subdir+File.separatorChar+file;
	        if (new java.io.File(test).exists()) {
		    try {
		        return new FitsImage(test);
		    } catch(Exception e) {
		        System.err.println("Unexpected exception reading cached image:"+test+" :: "+e);
		        System.err.println("Trying to download the file.");
		    }
	        }
	    }
	}
	
	// OK it doesn't exist now, so we'll create a proxy for it.
	Scaler s;
	int    nx = Integer.parseInt(tokens[6]);
	int    ny = Integer.parseInt(tokens[7]);

	// First create the scaler. 
	if (tokens.length == 10) {
	    // Just CDELTs specified.
	    double dx = Math.toRadians(Double.parseDouble(tokens[8]));
	    double dy = Math.toRadians(Double.parseDouble(tokens[9]));
	
	    s = new Scaler(0.5*nx, 0.5*ny, -1/dx, 0, 0, 1/dy);
	    
	} else {
	    double m00 = Math.toRadians(Double.parseDouble(tokens[8]));
	    double m01 = Math.toRadians(Double.parseDouble(tokens[9]));
	    double m10 = Math.toRadians(Double.parseDouble(tokens[10]));
	    double m11 = Math.toRadians(Double.parseDouble(tokens[11]));
	    double det = m00*m11 - m10*m01;
	    s = new Scaler(0.5*nx, 0.5*ny, m11/det, -m01/det, -m10/det, m00/det);
	}
	
	Projection p;
	double     crval1 = Math.toRadians(Double.parseDouble(tokens[2]));
	double     crval2 = Math.toRadians(Double.parseDouble(tokens[3]));
	try {
	    if (tokens[4].equalsIgnoreCase("Car") || 
		tokens[4].equalsIgnoreCase("Ait") || 
		tokens[4].equalsIgnoreCase("Csc")) {
		// Get a new scaler to shift to the offset specified in the
		// spell.  This shift occurs after everything else.
		s = s.add (new Scaler(Double.parseDouble(tokens[2]) - 0.5 - 0.5*nx, 
				  Double.parseDouble(tokens[3]) - 0.5 - 0.5*ny,
				       1,0,0,1));
		p = new Projection(tokens[4]);
		
	    } else if (tokens[4].equalsIgnoreCase("Ncp")) {
				
	        // Sin projection with projection centered at pole.
		double[] xproj = new double[] {crval1, Math.PI/2};
		if (crval2 < 0) {
		    xproj[1] = - xproj[1];
		}
		
		double poleOffset = Math.sin(xproj[1]-crval2);
		// Have we handled South pole here?
	        
	        p = new Projection("Sin", xproj);
	    
	        // NCP scales the Y-axis to accommodate the distortion of the SIN projection away
	        // from the pole.
	        Scaler ncpScale = new Scaler(0, poleOffset, 1, 0, 0, 1);
	        ncpScale = ncpScale.add(new Scaler(0., 0., 1,0,0,1/Math.sin( crval2 ) ) );
		s = ncpScale.add(s);
		
	    } else {
	        p = new Projection(tokens[4], 
				      new double[]{crval1, crval2 
				      });
	    }
	} catch (Exception e) {
	    throw new Error("Unexpected error building projection:"+e);
	}
	
	CoordinateSystem c = CoordinateSystem.factory(tokens[5]);
	try {
	    WCS    w = new WCS(c, p, s);
	    URLRetrieverFactory fac = new URLRetrieverFactory();
	    
	    if (subdir != null) {
		fac.setSubdirectory(subdir);
	    }
	    
	    ProxyImage pi =  new ProxyImage(tokens[0]+","+file, w, nx, ny, 1);
	    pi.setFactory(fac);
	    return pi;
	    
	} catch(Exception e) {
	    throw new Error("Unable to create proxy:"+e);
	}
    }
}

class URLRetrieverFactory implements ImageFactory {
    
    static private java.util.regex.Pattern comma = java.util.regex.Pattern.compile(",");
    private  String subdir = null;
    
    public Image factory(String spell) {
	
	String[] tokens = comma.split(spell);
	String[] caches = Settings.getArray("Cache");
	if (caches.length == 0) {
	    caches = new String[]{CachingImageFactory.DFT_CACHE};
	}
	
	String cache = caches[0];
	
	if (subdir != null) {
	    cache += subdir+File.separatorChar;
	}
	    
	
	String file = cache + tokens[1];
	File dir = new File(cache);
	if (!dir.exists()) {
	    try {
		dir.mkdirs();
	    } catch (Exception e) {
		System.err.println("Error creating cache:"+caches[0]);
		throw new Error("Error, "+e+", retrieving URL to file:"+tokens[0]);
	    }
	}
	try {
	    // Retrieve to temporary name and rename only after
	    // successful retrieval.
	    String tfile = file + new java.util.Date().getTime();
            System.err.println("   Retrieving remote URL: "+tokens[0]);
            skyview.survey.Util.getURL(tokens[0], tfile);
	    File f = new File(tfile);
	    f.renameTo(new File(file));
	    if (Settings.get("purgecache") != null) {
		Settings.add("_cachedfile", file);
	    }
	    return new FitsImage(file);
	} catch (Exception e) {
	    throw new Error("Error, "+e+",  retrieving URL to file:"+tokens[0]);
	}
    }
   
    void setSubdirectory(String dir) {
	this.subdir = dir;
    }
    
}
