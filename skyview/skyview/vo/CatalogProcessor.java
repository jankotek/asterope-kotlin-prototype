package skyview.vo;

import java.util.HashMap;
import java.util.ArrayList;

import skyview.executive.Key;
import skyview.survey.Image;
import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;

import skyview.executive.Settings;

import static org.apache.commons.math3.util.FastMath.*;

public class CatalogProcessor implements skyview.process.Processor {
    
    private static HashMap <String,ConeQuerier> requests = new HashMap<String,ConeQuerier>();
    private ArrayList<Thread> threads = new ArrayList<Thread>();
      
    private Image    outputImage;
    private String[] catalogs;
      
    private boolean  pixelsValid = false;
      
    private double   cosRad;
    private boolean  hasRad = false;
    private double[] centerUnit;
    private double[] centerCoords;
      
    private ArrayList<double[]> pixLoc;
    private ArrayList<Integer>  symbols;
    private ArrayList<String[]> extras;
    
    private static boolean  geometryMessage = false;
      
    java.io.PrintStream     ps;
    
    static CatalogProcessor lastProcessor;
    
    public CatalogProcessor() {
	lastProcessor = this;
    }
    
    public String getName() {
	return "CatalogProcessor";
    }
    
    public String getDescription() {
	return "Handle the queries of external catalogs";
    }
      
    
    /** Start up the processing.
     *  
     *  @param inputs The array of input survey images.
     *  @param output The output user image
     *  @param source An array giving the source image for each output pixel.
     *  @param samp   The sampler object used in processing.
     *  @param dsamp  The sampler in the energy dimension (often null).
     */
    public void process(Image[] inputs, Image output, int[] source, 
				 Sampler samp, DepthSampler dsamp) {
	
	if (Settings.has(Key.catalogFile)) {
	    String file  = Settings.get(Key.catalogFile);

	    if (file == null || file.length() == 0 || file.equals("1")) {
	        file = Settings.get(Key.output) + ".tab";
	    } else {
                if (Settings.has(Key._surveyCount))  {
                 //--- Add surveycount to file name for association
                 if (Settings.has(Key.dummy) && file.indexOf(".cat") >=0) {
                    file= file.replace(".cat","_" + 
			Settings.get(Key._surveyCount) + ".cat") ;
                 } else {
                     file = file + "." + Settings.get(Key._surveyCount);
                 } 
	         Settings.put(Key.CatalogFileKey,file);
               }
             }


	    try {
                if (file != null) {
		  //if (!Settings.has("_surveycount") ||
		       //Settings.get("_surveycount").equals("1") ) {

                    //--- Close previous catalog file
	           if (ps != null) {
		      ps.close();
		      ps = null;
	           }

	           ps = new java.io.PrintStream(new java.io.FileOutputStream(file));
		   geometryMessage = true;
		   System.err.println("  Generating Catalog output file "+file);
		} else {
		    ps = null;
		}
	    } catch (Exception e) {
	        System.err.println("  Unable to create table output file:"+file);
	        return;
	    }
	}
	this.outputImage = output;
	this.catalogs    = Settings.getArray(Key.catalog);
	int nx = output.getWidth();
	int ny = output.getHeight();
	
	// Get the size to use fo the catalog query.
	double imageSize = -1;
	
	// Did the user specify it?
	if (Settings.has(Key.CatalogRadius)) {
	    try {
		imageSize = Double.parseDouble(Settings.get(Key.CatalogRadius));
		cosRad    = cos(toRadians(imageSize));
		hasRad    = true;
	    } catch (Exception e) {
		System.err.println("  Invalid CatalogRadius setting:"+
		   Settings.get(Key.CatalogRadius) + " ignored");
	    }
	}
	
	
	// Otherwise use the size of the image.  We overestimage
	// a little but this should help accommodate strange geometries. 
	if (imageSize < 0) {
	    imageSize = toDegrees(output.getWCS().getScale()*(nx+ny)/2.);
	}
	
	// Get the RA and Dec (J2000) of the center of the image.
	if (centerUnit == null) {
	    centerUnit   = 
		output.getWCS().inverse().transform(new double[]{nx/2., ny/2.});
	    centerCoords = skyview.geometry.Util.coord(centerUnit);
	}
	
	// Did we already query this catalog with a radius at least as large?
	// If so we'll just use the existing results.
	if (requests.containsKey(catalogs[0])) {
	    // Only do output on first run through.
	    //if (ps != null) {
		//ps.close();
		//ps = null;
	    //}
	    double requestSize = requests.get(catalogs[0]).getSize();
	    if (requestSize >= imageSize) {
		return;
	    }
	}
	pixelsValid = false;
	
	String[]   qualArr   = Settings.getArray(Key.CatalogFilter);
	String[][] qualFlds  = new String[qualArr.length][];
	
	int      nQual       = 0;
	for (int i=0; i<qualArr.length; i += 1) {
	    String[] flds = parseQualifier(qualArr[i]);
	    if (flds != null) {
		qualFlds[nQual]  = flds;
		nQual           += 1;
	    }
	}
		
	for (int i=0; i< catalogs.length; i += 1) {
	    String cat = catalogs[i];
	    
	    ConeQuerier cq;
	    if (cat.startsWith("http:") || cat.startsWith("ftp:") ||
		cat.startsWith("file:")) {
		cq = new ConeQuerier(cat, "cat"+i, 
		   toDegrees(centerCoords[0]),
		   toDegrees(centerCoords[1]),
		   imageSize);
		
	    } else {
	        cq = ConeQuerier.factory(cat, toDegrees(centerCoords[0]), toDegrees(centerCoords[1]), imageSize);
	    }
	    
	    if (Settings.has(Key.CatalogFields)) {
	        cq.setOutput(ps);
	    }
	    if (nQual > 0) {
		for (int q=0; q<nQual; q += 1) {
		    cq.addCriterion(qualFlds[q][0], qualFlds[q][1], qualFlds[q][2]);
		}
	    }
	    requests.put(catalogs[i], cq);
	    
	    Thread thr = new Thread(cq);
	    threads.add(thr);
	    thr.start();
	}
    }
      
      
    private static String[] parseQualifier(String input) {
	if (input.indexOf("<=") > 0) {
	    return split(input, "<=");
	} else if (input.indexOf(">=") > 0) {
	    return split(input, ">=");
	} else if (input.indexOf(">") > 0) {
	    return split(input, ">");
	} else if (input.indexOf("<") > 0) {
	    return split(input, "<");
	} else if (input.indexOf("=") > 0) {
	    return split(input, "=");
	} else {
	    return null;
	}
    }
    
    private static String[] split(String input, String op) {
	int index = input.indexOf(op);
	int len   = op.length();
	if (index == 0 || (index+len == input.length()) ){
	    return null;
	}
	return new String[]{input.substring(0,index), op, input.substring(index+len) };
   }
	
	    
    
    public static CatalogProcessor getLastProcessor() {
	return lastProcessor;
    }
    
    public void waitForThreads() {
	
	for (int i=0; i<threads.size(); i += 1) {
	    
	    try {
		threads.get(i).join();
	    } catch (InterruptedException e) {
	    }
	}
	// We've waited for all the threads so we are done.
	threads.clear();
    }
    
    public ConeQuerier[] getCatalogs() {
	return requests.values().toArray(new ConeQuerier[0]);
    }
    
    /** Update the FITS header to indicate what processing was done.
     */
    public void updateHeader(nom.tam.fits.Header header) {
	try {
	    header.insertHistory("");
	    header.insertHistory("Catalogs:");
	    header.insertHistory("");
        } catch (nom.tam.fits.FitsException e) {
	    System.err.println("  Error updating FITS file in CatalogProcessor:"+e);
	}
	for(ConeQuerier cq: getCatalogs()) {
	    cq.updateHeader(header);
	}
    }
      
    /** Transform the catalog positions into pixel positions. */
    public void pixels(boolean doPrint) {
	
	if (pixelsValid  && !doPrint) {
	    return;
	}
	
	waitForThreads();
	
	pixLoc  = new ArrayList<double[]>();
	symbols = new ArrayList<Integer>();
	
	skyview.geometry.CoordinateSystem cs   = null;
	skyview.geometry.Converter        conv = null;
	// If we're printing we need to convert to the output coordinate frame to.
	if (ps != null  && doPrint) {
	    // Make sure we convert coordinates to the user requested coordinate system.
	    cs   = outputImage.getWCS().getCoordinateSystem();
	    conv = new skyview.geometry.Converter();
	    try {
	        if (cs.getSphereDistorter() != null) {
	            conv.add(cs.getSphereDistorter());
	        }
	        conv.add(cs.getRotater());
	    } catch (Exception e) {
	        ps.println("CatalogProcessor unable to transform coordinates to image system. Using J2000");
	    }
	}
		     
	
	int maxCatLen = -1;
	for (int i=0; i<catalogs.length; i += 1) {
	    if (catalogs[i].length() > maxCatLen) {
		maxCatLen = catalogs[i].length();
	    }
	}
	maxCatLen += 1;
	
	if (Settings.has(Key._surveyCount)) {
	    String[] surveys = Settings.getArray(Key.survey);
	    if (geometryMessage) {
	        System.err.println("   Catalog output file uses image geometry for survey="+surveys[0]+".");
	        geometryMessage = false;
	    }
	}
	
	double [] px     = new double[2];
	double [] unit   = new double[3]; 
	double [] newCoo = new double[2];

	int nx = outputImage.getWidth();
	int ny = outputImage.getHeight();

	if (ps != null  && doPrint) {
	    ps.printf(" %3s | %-"+maxCatLen+"s| %-20s| %9s| %9s|",
		      "N ", "Cat", "ID/Name", "RA/lon ", "Dec/lat ");
	    String[] xtras = Settings.getArray(Key.CatalogColumns);
	    if (xtras != null) {
		for (int i=0; i<xtras.length; i += 1) {
		     ps.printf("%19s|", xtras[i]);
		}
	    }
	    ps.printf("  %6s| %6s\n", "X ", "Y ");
	}
	int count = 0;
	for (int i=0; i<catalogs.length; i += 1) {
	    
	    
	    ConeQuerier cq = requests.get(catalogs[i]);
	    double[][] pos = cq.getPositions();
	    String[]   ids = cq.getIDs();
	    ArrayList<String[]> extra = cq.getExtras();
	    
	    int  catCount = 0;
	   
	    for (int p=0; p<pos.length; p += 1) {
		px[0] = toRadians(pos[p][0]);
		px[1] = toRadians(pos[p][1]);
		unit  = skyview.geometry.Util.unit(px);
		
	        outputImage.getWCS().transform(unit, px);
		if (px[0] >= 0 && px[1] >= 0 && px[0] <= nx  && px[1] <= ny) {
		    
		    // If the user specified a maximum radius, then we need to check
		    // that as well.  A cone search is allowed to return
		    // objects outside the requested area.
		    if (hasRad) {
			if (unit[0]*centerUnit[0] + unit[1]*centerUnit[1] + 
			    unit[2]*centerUnit[2] < cosRad) {
			    // Skip this row
			    continue;
		        }
		    }
		    
		    count    += 1;
		    catCount += 1;
		    
		    if (ps != null && doPrint) {
		        // Convert to the user coordinate system
		        conv.transform(unit, unit);
		        newCoo = skyview.geometry.Util.coord(unit);
		    
		        newCoo[0] = toDegrees(newCoo[0]);
		        newCoo[1] = toDegrees(newCoo[1]);
		    
			
		        String[] xx = null;
		        if (extra != null) {
		            xx = extra.get(p);
		        }
			
		        ps.printf(" %3d | %-"+maxCatLen+
			   "s| %-20s| %9.4f| %9.4f|",
			   count, catalogs[i], ids[p], newCoo[0], newCoo[1]);
			if (xx != null) {
			    for (int ii=0; ii<xx.length; ii += 1) {
				ps.printf("%19s|",xx[ii]);
			    }
			}
			ps.printf("  %6.1f| %6.1f\n",
			   px[0], px[1]);
		    }
		    pixLoc.add(px.clone());
		    symbols.add(i);
		}
	    }
	    // Note how many entries we used for this catalog.
	    cq.setEntriesUsed(catCount);
	}
	pixelsValid = true;
	Settings.put(Key._totalCatalogCount, ""+count);
    }
      
    public void postProcess(Image[] inputs, Image output, int[] source, 
				 Sampler samp, DepthSampler dsamp) {
	//if (  Settings.has("CatalogFile") 
	    //(!Settings.has("_surveycount") || Settings.get("_surveycount").equals("1"))
	   //) {
	if (  Settings.has(Key.catalogFile) ) {
	    pixels(true);
	}
    }
      
    public double[][] getPixels() {
	pixels(false);
	return pixLoc.toArray(new double[0][]);
    }
      
    public int[] getSymbols() {
	pixels(false);
	int[] arr = new int[symbols.size()];
	for (int i=0; i <symbols.size(); i += 1) {
	    arr[i] = symbols.get(i);
	}
	return arr;
    }
    
    /** Empty the request hash */
    public static void clearRequests() {
	requests.clear();
    }

}
