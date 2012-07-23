package skyview.executive;

// External classes used in this class.

import skyview.survey.Survey;
import skyview.survey.SurveyFinder;
import skyview.survey.UserSurvey;
import skyview.survey.Image;
import skyview.survey.Subset;

import skyview.geometry.Projection;
import skyview.geometry.Projecter;
import skyview.geometry.CoordinateSystem;
import skyview.geometry.Scaler;
import skyview.geometry.WCS;
import skyview.geometry.Converter;
import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;
import skyview.geometry.Position;

import skyview.process.ImageFinder;
import skyview.process.Processor;

import skyview.ij.IJProcessor;

import skyview.executive.Settings;
import skyview.executive.SettingsUpdater;

import skyview.request.SourceCoordinates;
import skyview.util.Utilities;

import nom.tam.fits.Fits;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Header;

import nom.tam.util.ArrayFuncs;
import nom.tam.util.BufferedFile;

import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.ArrayList;

import java.lang.ref.SoftReference;

import java.io.File;

/**
 *  This class generates an image or images from user inputs.
 *  Output images are created by sampling one or more input images
 *  into a user specified grid of pixels.<br>
 *  Usage:<br>
 *  <code>java skyview.geometry.test.Imager [key=value] [key=value] ...</code><br>
 *  If the code is being executed from Jar file, then use <br>
 *  <code>java -jar skyview.jar [key=value] [key=value]...</code><br>
 *  The field keys are not case sensitive but the values may be.
 *  <p>  <b> Valid field keys include:
 * <dl>
 * <dt> Lon <dd> The longitude of the center of the output image
 *               in the coordinate system selected.  Note that the
 *               default coordinate system is J2000, so the longitude is the right ascension.
 *               The value is specified in decimal degrees.
 * <dt> Lat <dd> The latitude of the center of the output image in the coordinate system selected.
 *               J2000 is the default coordinate system, so by default Lat corresponds to declination.
 *               The value is specified in decimal degrees.
 * <dt> Position <dd> The comma separated longitude and latitude.  If Position and Lon/Lat are specified
 *               then the values specified in Lon/Lat override those in position.
 * <dt> CopyWCS <dd> An existing FITS file may be used to define the
 *               WCS to be used for the output.  If this argument
 *               is specified any other argument that specifies
 *               the output image geometry is ignored.
 * 
 * <dt> Coordinates <dd> A string describing the coordinate system to be used for the output image.
 *               It comprises an inital letter which gives the coordiante system, which prefixes
 *               a number giving the epoch of the coordiante system.
 *              <ul>
 *              <li> J: Julian (FK5) coordiantes, e.g., J2000.
 *              <li> B: Besselian coordinates, e.g., B1950.
 *              <li> E: Julian Ecliptic coordinates, e.g., E2000.
 *              <li> H: Helioecliptic coordinates (i.e., coordinates centered on the
 *                      instantaneous position of the sun), e.g., H2002.4356.
 *              <li> G: Galactic coordinates.  Characters after the first are ignored.
 *              <li> I: ICRS coordinates. Characters after the first are ignored.
 *              </ul>
 * <dt> Projection: <dd> The projection used to convert from celestial to plane coordinates.
 *              The projection string is the 3 character string used in the FITS WCS papers,
 *              with only the first letter capitalized.
 *              Supported output projections include:
 *              <ul>
 *              <li> Tan: The tangent plane on gnomonic projection.
 *              <li> Sin: The sine or orthographic projection.
 *              <li> Ait: The Hammer-Aitoff projection.
 *              <li> Car: The cartesian or plate-caree projection.
 *              <li> Zea: The Zenithal equal area projection
 *              <li> Csc: The COBE sperical cube projection.
 *              <li> Toa: The HTM TOAST projection
 *              </ul>
 * <dt> Sampler: <dd> The sampler defines how the input images are sampled to get values
 *               at the putput pixels.  Sampling algorithims include:
 *              <ul>
 *              <li> NN:  Nearest Neighbor.
 *              <li> LI:  [Bi-]Linear interpolation.
 *              <li> Lanczos[n]: A Lanczos smoothly truncated Sinc interpolator of order
 *                   n.  Lanczos defaults to order 3.
 *              <li> Spline[n]: The n'th order spline interpolation.  Defaults to cubic splines.
 *                   The order may range from 2 to 5.
 *              <li> Clip: A flux conserving exact area resampler where output pixels 
 *                   serve as clipping windows on the input image.
 *              </ul>
 * <dt> Scale: <dd> The size of the output pixels in degrees.  If the pixels are square only
 *              a single values is given. Non-square pixels can be specified
 *              as Scale=xsize,ysize.
 * <dt> Size:   <dd> The size of the entire image in degrees.
 * <dt> Pixels: <dd> The number of output pixels along an edge.
 *              If the output image is not square this can be
 *              specified as Pixels=xPixels,yPixels
 * <dt> Rotation: <dd> A rotation in the output plane to be applied to the data (in decimal degrees)
 * <dt> Survey: <dd> The survey from which the output image is to be created.  More than
 *              one survey can be specified as survey1,survey2,survey3,...
 * <dt> Ebins:  <dd> For surveys with a third dimension, the binning to be applied in this dimension.
 *              Currently this is specified using as x0,dx,n where x0 is the
 *              starting bin for the first output bin, dx is the width of the output bins,
 *              and n is the number of output bins.  These are expressed in terms of the input
 *              energy bins, such  that if the survey data has 10 bins, the first bin ranges from 0-1,
 *              the second from 1-2 and the last from 9-10.  If we wished to rebin this data into
 *              four evenly spaced bins excluding the first and last bins, then
 *              Ebins=1,2,4.  A default binning is defined for each 3-d survey included in SkyView.
 * <dt> Output: <dd> The output file name.  If data is being created from more than one survey,
 *              then the survey short name will be appended to the name. The default output filename
 *              is output.fits.  The strings "-" or "stdout" are used to specify writing to the standard output.
 * <dt> Compress: <dd> Write the output in GZIP compressed form. The value field is ignored.
 * <dt> Float:  <dd> Write output in 4 byte reals rather than 8 byte.  The value field is ignored.
 * <dt><dd> <p> The following options control where the imager task finds survey data.
 *          <p>
 * <dt> XMLRoot: <dd> The directory containing the XML survey descriptions
 * <dt> Cache:  <dd> Directory location (or locations) where cached files (survey files retrieved from
 *              remote locations) are to be found.  The first cache location is also used when a
 *              remote file is retrieved.
 * <dt> PurgeCache: <dd> Should files cached during this retrieval be deleted after processing?
 *              Only files cached during the current operation will be deleted.  If there
 *              are survey files in the cache area from previous requests that are
 *              used in the current request, these will be retained.
 * <dt> SurveyXML:<dd> Gives the name of a file containing an XML description of a survey.
 *              This allows the user to create an image from a survey they are describing.
 *              Use a "," to separate multiple survey files.
 *              <dd> The following operations allow the user to override the basic processing
 *              operations.
 * <dt><dd><p>  The following options control the classes that are used in processing the
 *              user request.  Most users can ignore these.
 *         <p>
 * <dt> SurveyFinder: 
 *             <dd> The class of the object that finds the appropriate
 *             survey object for the surveys the user has requested.
 * <dt> ImageFinder   
 *             <dd> The class of the object that finds the appropriate images 
 *             within a survey for each pixel of the output object.
 * <dt> PreProcessor: 
 *             <dd>  One or more objects that do image pre-processing.
 * <dt> Mosaicker: <dd> The object that actually generates the output image
 *             melding together the input images.
 * <dt> PostProcessor: <dd> One or more classes of object that do image post processing.
 *             One example used by default in some surveys is the
 *             skyview.geometry.Deedger.  You can force de-edging by specifying
 *             the class, or turn it off by setting PostProcessor=null.
 *          <p>
 * <dt> NoFITS: <dd> Do not create a FITS output file.  This could be used for debugging,
 *      or more likely in combination with graphics output created by the skyview.ij.IJProcessor
 *      where the user wants only a JPEG or GIF and does not need a FITS file.  See the help
 *      for the IJProcessor for details on supported formats and other options.
 * </dl>
 * 
 */
public class Imager {
    
    // Used later in parsing.
    static Pattern comma = Pattern.compile(",");
    static Pattern equal = Pattern.compile("=");
    
    /** Clone of the array giving the mapping between input images
     *  and output pixels.
     */
    private int[]   match      = null;
    
    /** The name of the last survey processed */
    private String  lastSurvey = null;
    
    /** The Hash of survey images that have been created
     *  for this imager.
     */
    private static HashMap<String, SoftReference<ImageState>> doneImages = new HashMap<String,SoftReference<ImageState>>();
    
    /** A scalar adjustment to the output value for each
     *  input image used to minimize edge effects.
     */
    private double[] edgeAdjustments;
    
    /** Width of the image */
    int nx;
    
    /** Height of the image */
    int ny;
    
    /** The number of output planes */
    int nz = 1;
    
    
    /** The beginning of the first pixel in energy space. */
    protected double bin0;
    
    /** The width of pixels in energy space */
    protected double dBin;

    /** The Scaler object for the output image. */
    protected Scaler s;
    
    /** The Projection object for the output image. */
    protected Projection p;
    
    /** The CoordinateSystem object for the output image. */
    protected CoordinateSystem c; 
    
    /** The sampler object used to create the output image. */
    protected Sampler samp;
    
    /** The energy sampler object used when creaing the output image. */
    protected DepthSampler dsamp;
    
    /** The lon/lat (or RA/dec) coordinates of the output image. */
    protected double lon = Double.NaN, lat = Double.NaN;
    
    /** The current Survey object being used. */
    protected Survey surv;
    
    /** The candidate images for use in resampling */
    protected Image[] cand;
    
    /** The current output image */
    protected Image output;
    
    /** An imager instance */
    private static Imager lastImager;
    
    /** The object to find a survey matching the requested name */
    private static SurveyFinder finder;
    
    /** Processors used*/
    private ArrayList<Processor> processes;
    
    /** The WCS used */
    private WCS wcs;
    
    
    /** Version of imager.
     *  The third number is intended to be the SVN repository
     *  number, but that's hard to get so we've given up on it.
     */
    private static String version = "2.6";
    
    /** Generate an image given the input parameters.  See class documentation
     *  for usage.
     */
    public static void main(String[] args) throws Exception {
	
	if (args.length == 0) {
	    usage();
	} else {
	    Imager  im = getImager();
	    Settings.addArgs(args);
	    im.run();
	}
	if (!Settings.has("imagej")  && !Settings.has("noexit")) {
	    System.exit(0);
	}
    }
    
    public static String getVersion() {
	return version;
    }
    
    /** Initialize the imager.
     *  Make sure that choices have been made for the basic
     *  elements needed for running the code.
     */
    public Imager() {
	if (Settings.get("surveyfinder") == null) {
	    Settings.put("surveyfinder", "XMLSurveyFinder");
	}
	lastImager = this;
    }
	
    
    /** Cleanup cache files if needed */
    private void cleanCache() {
	if (Settings.get("purgecache") != null) {
	    // Delete any cached files
	    String cacheFiles = Settings.get("_cachedfile");
	    if (cacheFiles != null) {
		String[] files = comma.split(cacheFiles);
		for (String file: files) {
		    new java.io.File(file).delete();
		}
	    }
	}
    }
    
	
    /**  Get the sampler name. */
    public String getSamplerName () {
        return samp.getName();
    }

    /** Get the Scaler used for the output image. */
    public Scaler getScaler () {
        return s;
    }

    /** Get the output data as 1-d double array */ 
    public Object getImageData () {
	return output.getDataArray();
    }

    /** Get the central longitude/RA of the output image */
    public double getLon () {
        return lon;
    }
    
    /** Get the central latitude/declination of the output image */
    public double getLat () {
        return lat;
    }
    
    /** Get the current survey being processed */
    public Survey getSurvey () {
        return surv;
    }
    
    /** Get any adjustments made to image intensities to minimize edge effects. */
    public double[] getEdgeAdjustments () {
        return edgeAdjustments;
    }
    
    /** Get the candidate images that may be used for resampling */
    public Image[] getCandidates () {
        return cand;
    }

    /** Get the energy depth of the output image */
    public int getPixelDepth () {
        return output.getDepth();
    }
    
    /** Get the width of the output image in pixels */
    public int getPixelWidth () {
        return output.getWidth();
    }
    
    /** Get the height of the output image in pixels */
    public int getPixelHeight () {
        return output.getHeight();
    }

    /** Print a message indicating how to use this class */
    private static void usage() {
	
	try {
	    java.io.BufferedReader rdr = 
	      new java.io.BufferedReader(
	       new java.io.InputStreamReader(
	        skyview.survey.Util.getResourceOrFile("onlineintro.txt")));
	    String line;
	    while ( (line=rdr.readLine()) != null) {
		System.err.println(line);
	    }
	    rdr.close();
	} catch (Exception e) {
	    System.err.println("Error reading usage file:"+e);
	}
	SurveyFinder finder;
	finder = (SurveyFinder) skyview.util.Utilities.newInstance(
			              Settings.get("surveyfinder"),
				      "skyview.survey");
	if (finder == null) {
	    System.err.println("Error creating SurveyFinder: "+Settings.get("surveyfinder"));
	    return;
	}
	
	System.err.println("\nAvailable surveys: (including all aliases)\n");
	String[] surveys = finder.getSurveys();
	java.util.Arrays.sort(surveys);
	for (int i=0; i<surveys.length; i += 1) {
	    System.err.println("  "+surveys[i]);
	}
        return;
    }
	
    /** Run an image with a given set of arguments.
     *  Retained for compatibility with V1.00.  Putting
     *  the argument handling in the main code allows
     *  more flexibility in creating Image objects.
     */
    public void run(String[] args) throws Exception {
	Settings.addArgs(args);
        run();
    }
    
    public void checkUpdateSettings()  {
	String[] updateClasses = Settings.getArray("settingsupdaters");
	for (int i=0; i<updateClasses.length; i += 1) {
	    try {
		SettingsUpdater su = (SettingsUpdater) skyview.util.Utilities.newInstance(updateClasses[i], "skyview.executive");
		su.updateSettings();
	    } catch (Exception e) {
		System.err.println("Exception during attempt to update Settings:"+e+"\nProcessing continues");
	    }
	}
    }

    public boolean init() throws Exception {
	String[] files = Settings.getArray("settings");
	for (int i=0; i<files.length; i += 1) {
	    if (files[i].equals("-")) {
		Settings.readFile(
		  new java.io.BufferedReader(
		    new java.io.InputStreamReader(System.in)
		   ));
	    } else {
		Settings.updateFromFile(files[i]);
	    }
	}
	// Are there any classes that want to take a look
	// at the Settings before we begin processing?
	checkUpdateSettings();
	
	if (Settings.get("survey") == null  && Settings.get("contour") == null) {
	    System.err.println("No survey specified");
	    return false;
	    
	} else if (!Settings.has("position") && 
		   (Settings.get("lon") == null || Settings.get("lat") == null)  &&
		   (!Settings.has("copywcs"))) {
	    System.err.println("No position specified");
	    return false;
	}
	if (finder == null) {
	    finder = (SurveyFinder) skyview.util.Utilities.newInstance(Settings.get("surveyfinder"), "skyview.survey");
	}
	
	if (!Settings.has("output")) {
	    Settings.put("output","output");
	}
	return true;
    }
    
    /** Run the command */
    public void run() throws Exception {
	if (!init()) {
	    return;
	}
	System.err.println("Imager starting v"+version+".");
	String[] survs  = Settings.getArray("survey");
	String output   = Settings.get("output");
	    
	int count = 1;
	for (String surv: survs) {
	    if (survs.length > 1) {
		updateOutput(output, count);
	    }
	    try {
		Settings.save();
		Settings.put("_surveycount", ""+count);
		
	        System.err.println("\nProcessing survey:"+surv);
		processSurvey(surv);
	    } finally {
		String scale = null;
		// If the user has specified an RGB image, then the
		// scale must be the same of all images.
		if (count == 1  && Settings.has("rgb")  && Settings.has("scale")) {
		    scale = Settings.get("scale");
		}
		Settings.restore();
		if (scale != null) {
		    Settings.put("scale", scale);
		}
	    }
	    count += 1;
	}
    }
    
    /** Make sure that when multiple files are being created we distinguish the names */
    private void updateOutput(String output, int count) {
	
	if (output.equals("-") || output.toLowerCase().equals("stdout")) {
	    return;
	}
	
	File fil = new File(output);
	// Get the end of the file name
	String path = fil.getName();
	
	if (path == null || path.length() == 0) {
	    path = "output";
	}
	
	int off = path.indexOf('.');
	
	if (off == 0) {
	    path = "" + count + path;
	    
	} else if (off > 0) {
	    path = path.substring(0,off) + "_" + count + path.substring(off);
	    
	} else {
	    path = path + "_" + count;
	}
	
	String parent = fil.getParent();
	if (parent == null) {
	    parent = "";
	}
	if (parent.length() > 0 ) {
	    path = parent + "/"+path;
	}
	Settings.put("output", path);
    }
    
    protected Survey loadSurvey(String surveyID) throws Exception {
	
	Survey surv;
	//Survey surv = finder.find(surveyID);
	if (surveyID.toLowerCase().equals("user")) {
	    surv = new UserSurvey();
	} else {
	    surv = finder.find(surveyID);
	    if (surv == null) {
	        System.err.println("  Unable to find requested survey:"+surveyID);
		return surv;
	    }
	}
	
	
	// Save the current settings before we add any survey specific ones.
	surv.updateSettings();
	return surv;
    }
    
    protected WCS loadWCS() throws Exception {
	
	WCS wcs = null;
	if (Settings.has("UseDSSWCS")) {
	    wcs.setPreferDSS(true);
	}
	
	if (Settings.has("CopyWCS")) {
	    wcs = copyWCS(Settings.get("CopyWCS"));
	    s   = wcs.getScaler();
	    int[] axes = wcs.getHeaderNaxis();
	    nx = axes[0];
	    ny = axes[1];
		
	} else {
	    if (Settings.get("Pixels") != null  && Settings.get("Pixels").length() > 0) {
	        String[] pix = comma.split(Settings.get("Pixels"));
		try {
	            nx       = Integer.parseInt(pix[0].trim());
		} catch (Exception e) {
		    Settings.put("errormsg", "Invalid pixels setting:"+Settings.get("pixels"));
		    throw new Exception("Invalid pixels setting:"+Settings.get("Pixels"));
		}
		    
		  
	
	        if (pix.length > 1) {
		    try {
	                ny      = Integer.parseInt(pix[1].trim());
		    } catch (Exception e) {
		        Settings.put("errormsg", "Invalid pixels setting:"+Settings.get("pixels"));
		        throw new Exception("Invalid pixels setting:"+Settings.get("Pixels"));
		    }
	        } else {
	            ny      = nx;
	        }
	    } else {
	        nx = 300;
	        ny = 300;
	    }
	    wcs = specifyWCS(nx, ny);
	}
	Header h = new Header();
	wcs.updateHeader(h, getScaler(), 
			 new double[]{getLon(), getLat()}, 
			 Settings.get("Projection"), 
	                 Settings.get("Coordinates")
			);
	
	// Save the key WCS parameters for possible further use.
	Settings.put("_CRPIX1", h.getDoubleValue("CRPIX1")+"");
	Settings.put("_CRPIX2", h.getDoubleValue("CRPIX2")+"");
	Settings.put("_CRVAL1", h.getDoubleValue("CRVAL1")+"");
	Settings.put("_CRVAL2", h.getDoubleValue("CRVAL2")+"");
	Settings.put("_CDELT1", h.getDoubleValue("CDELT1")+"");
	Settings.put("_CDELT2", h.getDoubleValue("CDELT2")+"");
	Settings.put("_CD1_1", h.getDoubleValue("CD1_1")+"");
	Settings.put("_CD1_2", h.getDoubleValue("CD1_2")+"");
	Settings.put("_CD2_1", h.getDoubleValue("CD2_1")+"");
	Settings.put("_CD2_2", h.getDoubleValue("CD2_2")+"");

	return wcs;
    }
    
    protected Position loadPosition() throws Exception {
	// Find the center of the image.
	double[] cpix = new double[]{nx/2.,ny/2.};
	double[] cunit = new double[3];
	wcs.inverse().transform(cpix, cunit);
			       
	cpix = skyview.geometry.Util.coord(cunit);
	cpix[0]= Math.toDegrees(cpix[0]);
	cpix[1]= Math.toDegrees(cpix[1]);
	
	if ( (cpix[0]!=cpix[0]) || (cpix[1]!=cpix[1])) {
	    // Maybe on the edge of fixed projection, or
	    // outside Sin or Tan coverage.
	    System.err.println("  Unable to locate center position in projection -- rounding error at edge?");
	    return null;
	} else {
	    return new Position(cpix[0], cpix[1], "J2000");
	}
    }
    
    protected Image loadImage() throws Exception {
	double[] data   = new double[nx*ny*this.nz];
	return new Image(data, wcs, nx, ny, nz);
    }
    
    protected Image[] loadCandidates(Position pos) throws Exception {
        // Get max scale using scale and image size
	double maxSize = Math.max(nx, ny)*wcs.getScale()*180/Math.PI;
	if (maxSize <= 0) {
	    throw new Exception("Ouput region has nil size");
	}
	Image[]  cand  = surv.getImages(pos, maxSize);
        //--- If no candidates there is no reason to continue
        if (cand.length == 0  && !Settings.has("nullimages")) {
	    String msg = "Survey: "+Settings.get("_currentSurvey")+" No candidate images were found in the region.  Position may be outside the coverage area.";
	    Settings.put("ErrorMsg", msg);
	    System.err.println("  No candidate images.  Processing of this survey is completed.");
	    // No output image.
	    output = null;
        } else {
	    System.err.println("  Number of candidate source images is "+cand.length+".");
	}
	return cand;
    }
    
    protected void loadSamplers() throws Exception {
	String sampling = Settings.get("Sampler");
	
	// Get the appropriate sampler
	samp  = Sampler.factory(sampling);
	
        // Do we need to worry about sampling in the third dimension? 
        if (Settings.get("Ebins") != null) {
	    dsamp = new DepthSampler(bin0, dBin, nz);
	}
    }
    
    protected int[] reuseMatch(String surveyID) {
	if (match != null  && !Settings.has("subset") && Settings.get("GeometryTwin") != null) {
	    // Note that we're assuming that we process twins sequentially.
	    String primary = "(^|.*,)"+lastSurvey+"($|,.*)"; 
	    
	    if (Pattern.compile(primary, Pattern.CASE_INSENSITIVE).matcher(Settings.get("GeometryTwin")).find()) {
		System.err.println("  Reusing geometry match from:"+lastSurvey);
		return match;
	    }
	}
	return null;
    }
    
    protected int[] loadMatch(String surveyID) throws Exception {
	
	// If the previous survey has the same geometry as this one,
	// we don't need to recompute the match array.
	ImageFinder imFin = ImageFinder.factory(Settings.get("imagefinder"));
	imFin.setStrict(Settings.has("StrictGeometry"));
	match = imFin.findImages(cand, output);
	lastSurvey = surveyID;
		
	if (match == null) {
            System.err.println("  No matches found for requested region");
	    if (!Settings.has("NullImages")) {
	        Settings.put("ErrorMsg", "No images in FOV");
	        output = null;
	    } else {
	        match = new int[output.getWidth()*output.getHeight()];
	        java.util.Arrays.fill(match, skyview.process.imagefinder.Border.NO_COVERAGE);
	    }
	}
	    
	return match;
    }
    
    protected void doProcess(String type) throws Exception {
	
	String[] procNames = Settings.getArray(type);
	for (int i=0; i<procNames.length; i += 1) {
	    dynoProcess(procNames[i]);
	}
    }
    
    public void dynoProcess(String name) throws Exception {
	Processor proc = (Processor) Class.forName(name).newInstance();
	proc.process(cand, output, match, samp, dsamp);
	processes.add(proc);
    }
    
    public void clearImageCache() {
	doneImages.clear();
    }
    
    protected ImageState haveImage(String surveyID,  WCS wcs) {
	ImageState oldImage = null;
	
	SoftReference<ImageState> wr = doneImages.get(surveyID.toLowerCase());
	if (wr != null) {
	    oldImage = wr.get();
	}
	if (oldImage != null && oldImage.output.getWCS().getScale() == wcs.getScale()) {
	    System.err.println("  Using cached image for "+surveyID);
	    return oldImage;
	} else {
	    return null;
	}
    }
	    
    /** Process a particular survey. */
    public void processSurvey(String surveyID) throws Exception {
        Settings.put("_currentSurvey", surveyID);
	output = loadAndProcessSurvey(surveyID);
	postprocessSurvey();
	if (match != null && output != null  && !Settings.has("nofits")) {
	   createFitsFile();
	}
    }
    
    public static  double getSum(double[] arr) {
	double sum = 0;
	for (int i=0; i<arr.length; i += 1) sum += arr[i];
	return sum;
    }
    
    public Image loadAndProcessSurvey(String surveyID) throws Exception {
	
	processes = new ArrayList<Processor>();
	surv = loadSurvey(surveyID);
	if (surv == null) {
	    return null;
	}
	wcs = loadWCS();
	if (wcs == null) {
	    // Error message should alread be printed.
	    return null;
	}
	if (haveImage(surveyID, wcs) != null) {
	    ImageState is =  haveImage(surveyID, wcs);
	    cand   = is.sources;
	    samp   = is.samp;
	    dsamp  = is.dsamp;
	    processes = is.procs;
	    return is.output;
	}
	
	Position pos = loadPosition();
	if (pos == null) {
	    return null;
	}
	parseEbins();
	Image prime  = loadImage();
	cand         = loadCandidates(pos);
	
	// Allow candidate selection and mosaicking using
	//  subset images.
	Image[] subsets  = new Image[]{prime};
	
	if (Settings.has("subset")) {
	    int tileX = 1024;
	    int tileY = 1024;
	    try {
		tileX = Integer.parseInt(Settings.get("SubsetX", "1024"));
		tileY = Integer.parseInt(Settings.get("SubsetX", "1024"));
	    } catch (Exception e){
		// Ignore
	    }
	    subsets = Subset.split(prime, tileX, tileY);
	}
	int tileCount = 0;
	for (Image img: subsets) {
	    if (subsets.length > 0) {
		tileCount += 1;
		System.err.println("  Processing subset tile:"+tileCount+" of "+subsets.length);
	    }
	    output = img;
	    if (Settings.get("Mosaicker") == null) {
		Settings.put("Mosaicker", "skyview.process.Mosaicker");
	    }
	    Processor mos = (Processor) Class.forName(Settings.get("Mosaicker")).newInstance();
	    processes.add(mos);
	
	    // Reuse the previous geometry?
	    match        = reuseMatch(surveyID);
	
	    if (match == null &&
	       (cand != null && cand.length > 0) || Settings.has("NullImages")) {
	        loadMatch(surveyID);
	    }
	
	    if (match != null) {
	        loadSamplers();
	        doProcess("Preprocessor");
		mos.process(cand, output, match, samp, dsamp);
	    }
	}
	
	// Subsequent processing should take place using the full image,
	// not the image subsets.
	 
	output = prime;
	
	if (output != null) {
	    doneImages.put(surveyID, new SoftReference(new ImageState(cand, output, match, samp, dsamp, processes)));
	}
	return output;
    }
    
    public void postprocessSurvey() throws Exception {

	// Do we have any postprocesing? (Maybe even if the image failed)
	// Use lastMatch rather than match, since match may have been consumed by the mosaicker.
	doProcess("Deedger");
	doProcess("Postprocessor");
	cleanCache();
        dsamp = null;
    }
    
    /** Parse a request for sampling in the energy dimension */
    private void parseEbins() {
	
	if (!Settings.has("Ebins")) {
	    return;
	}
	
	// Should be Ebins=bin0,dBin,nBin
	String[] tokens = comma.split(Settings.get("Ebins"));
	
	if (tokens.length != 3) {
	    throw new Error("Invalid energy bin setting:"+
		Settings.get("Ebins"));
	}
	this.bin0 = Double.parseDouble(tokens[0]);
	this.dBin = Double.parseDouble(tokens[1]);
	this.nz   = Integer.parseInt(tokens[2]);
	if (nz < 0 || bin0 < 0  || dBin < 0) {
	    throw new Error("Invalid energy bin setting:"+
		Settings.get("Ebins"));
	}
    }
    
    
    /** Create the FITS file */
    public void createFitsFile () throws Exception {
       
        Scaler scaler = getScaler();    
        Object data   = getImageData();    

        if (data == null) {
	    System.err.println("  Unexpected error: No image data found!");
            return;
        }

        if (Settings.get("float") != null) {
	    data = nom.tam.util.ArrayFuncs.convertArray(data, float.class);
        }
	
        int[] outDims;
        
        int nx = getPixelWidth();
        int ny = getPixelHeight();
        int nz = getPixelDepth();
       
        if (nz == 1) {
            outDims = new int[]{ny, nx};
        } else {
            outDims = new int[]{nz, ny, nx};
        } 

        double[] edgeAdjustments = getEdgeAdjustments();
        Image[]  cand            = getCandidates();
	
        Header h = new Header();
	h.addValue("SIMPLE", true, "Written by SkyView "+new java.util.Date());
	if (Settings.has("float")) {
	    h.addValue("BITPIX", -32, "4 byte floating point");
	} else {
	    h.addValue("BITPIX", -64, "8 byte floating point");
	}
	if (nz == 1) {
	    h.addValue("NAXIS", 2, "Two dimensional image");
	} else {
	    h.addValue("NAXIS", 3, "Three dimensional image");
	}
	h.addValue("NAXIS1", nx, "Width of image");
	h.addValue("NAXIS2", ny, "Height of image");
	if (nz != 1) {
	    h.addValue("NAXIS3", nz, "Depth of image");
	}
	
	if (Settings.has("copywcs")) {
	    wcs.copyToHeader(h);
	} else {
            wcs.updateHeader(h, scaler, 
			 new double[]{getLon(), getLat()}, 
			 Settings.get("Projection"), 
	                 Settings.get("Coordinates")
			);
	}
       
        Survey surv = getSurvey();
        surv.updateHeader(h);
	
	
	h.insertHistory("");
	h.insertHistory(" Settings used in processing:");
	h.insertHistory("");
	String[] keys = Settings.getKeys();

	java.util.Arrays.sort(keys);
	for(String key: keys) {
	    
	    if (key.charAt(0) == '_' ) {
		continue;  // Skip internal communication settings.
	    }
	    
	    String val = Settings.get(key);
	    if (val == null) {
		h.insertHistory(key + " is null");
	    } else if (val.equals("1")) {
		h.insertHistory(key);
	    } else {
		h.insertHistory(key + " = " +val);
	    }
	}
	
        // Add in the images we used.
        h.insertHistory("");
        h.insertHistory(" Map generated at: "+new java.util.Date());
        h.insertHistory("");
        h.insertHistory(" Resampler used: " + getSamplerName());
        h.insertHistory("");
	
	// Update the headers to reflect processing.
	for (Processor p: processes) {
	    p.updateHeader(h);
	}
	
        writeFits(h, data);
	if (Settings.has("samp") ) {
	    Samp.notifyFile();
	}
    }
	
    
    /**  Write the FITS file.  Handle special cases
     *   of writing to STDOUT and a compressed output.
     */
//    private void writeFits(Fits f) throws Exception {
    private void writeFits(Header h, Object data) throws Exception {
	
	java.io.OutputStream base;
       
	String suffix="";
	if (Settings.get("Compress") != null) {
	    suffix = ".gz";
	}
	
	String out = Settings.get("output");
	
	// Writing to Standard out?
	if (out.equals("-") || out.equalsIgnoreCase("stdout")) {
	    System.err.println("  Sending output to standard output stream");
	    base = System.out;
	} else {
	    String path = new File(out).getName();
	    if (path.indexOf('.') < 0) {
		out = out + ".fits";
	    }
	    out = out+suffix;
	    Settings.put("output_fits", out);
	    System.err.println("  Opening FITS file: "+out);
	    base = new java.io.FileOutputStream(out);
	}
	
	if (Settings.get("Compress") != null) {
	    base = new java.util.zip.GZIPOutputStream(base);
	}
	
	nom.tam.util.BufferedDataOutputStream bds = 
	  new nom.tam.util.BufferedDataOutputStream(base);
	
	
	// Writing out header and data separately.
	h.write(bds);
	bds.writeArray(data);
	int len = ArrayFuncs.computeSize(data);
	int need = 2880 - len%2880;
	if (need != 2880) {
	    byte[] buf = new byte[need];
	    bds.write(buf);
	}
	
	bds.close();
    }
			    
		
    private WCS specifyWCS(int nx, int ny) throws Exception {
	
	String csys     = Settings.get("Coordinates");
	String proj     = Settings.get("Projection");
	String equin    = Settings.get("Equinox");
	// The input position may be specified as 'position=ra,dec' or 
	// 'lon=ra lat=dec'. The values are in the specified coordiante 
        // system (J2000 by default).

	c = CoordinateSystem.factory(csys, equin);
	if (c != null) {
	    Settings.put("coordinates", c.getName());
	} else {
	    System.err.println("Invalid coordinates:"+csys + " "+Settings.get("equinox"));
	    Settings.put("errormsg", "Invalid coordinates or equinox:"+csys+" "+Settings.get("equinox"));
	    return null;
	}
	
	csys = c.getName();

	double[] posn = null;
	if (Settings.get("Position") != null) {
	 
            SourceCoordinates sc=new SourceCoordinates(
	      Settings.get("position"),  csys,
	      Double.parseDouble(equin),
	      Settings.get("resolver")
	    );
	
            sc.convertToCoords();
	    Position ps = sc.getPosition();
	    if (ps == null) {
		throw new Exception("Unable to recognize target/position: "+Settings.get("position"));
	    }
	    posn = ps.getCoordinates(csys);
            Settings.put("ReqXPos", ""+posn[0]);
            Settings.put("ReqYPos", ""+posn[1]);
	    
	} else if (Settings.has("Lon") && Settings.has("Lat")) {
	    SourceCoordinates sc = SourceCoordinates.factory(Settings.get("lon"),
						     Settings.get("lat"),
						     Settings.get("coordinates"));
	    if (sc == null) {
		System.err.println("Invalid coordinates:"+Settings.get("lon")+", "+Settings.get("lat")+" in "+Settings.get("coordinates"));
	        return null;
	    }
            Settings.put("ReqXPos", Settings.get("lon"));
            Settings.put("ReqYPos", Settings.get("lat"));
	    posn = sc.getPosition().getCoordinates(csys);
	    Settings.put("position", Settings.get("lon")+", "+Settings.get("lat"));
	} else {
	    System.err.println("Error: No position specified");
	    return null;
	}
	
	lon = posn[0];
	lat = posn[1];

        if (lon == Double.NaN || lat == Double.NaN) {
	    throw new Error("Invalid position/coordinates specified.");
        }

	double xscale = 1./3600;
	double yscale = 1./3600;

	// Do Size first, since that will normally be
	// a user specified item, but scale is provided
	// by the survey even if the user doesn't specify it
	// so we'd never see the user specified size.
	
	String sz = Settings.get("Size");
	if (sz != null  && sz.length() > 0 && !sz.toLowerCase().equals("default") ) {
	    String[] sizes = comma.split(sz);
	    try {
	        double xsize = Double.parseDouble(sizes[0]);
	        double ysize = xsize;
	        if (sizes.length > 1) {
		    ysize = Double.parseDouble(sizes[1]);
	        }
	        xscale = xsize/nx;
	        yscale = ysize/ny;
	    } catch (Exception e) {
		Settings.put("errormsg", "Invalid size setting:"+sz);
		throw new Exception("Invalid size setting:"+sz);
	    }
						      
	} else  if (Settings.get("Scale") != null) {
	    String[] scales = comma.split(Settings.get("Scale"));
	
	    xscale   = Double.parseDouble(scales[0]);
	
	    if (scales.length > 1) {
	        yscale      = Double.parseDouble(scales[1]);
	    } else {
	        yscale      = xscale;
	    }
	    
	} else {
		xscale = 1/3600.;
		yscale = xscale;
	}
	Settings.put("size", xscale*nx+","+yscale*ny);
	    

	double[] center = Projection.fixedPoint(proj);
	if (center != null) {
	    p = new Projection(proj);
	
	    // Does the user want a non-standard center for
	    // a fixed projection?
	    if (Settings.has("RefCoords")) {
		String[] coords = Settings.getArray("RefCoords");
		try {
		    double lon = Math.toRadians(Double.parseDouble(coords[0]));
		    double lat = Math.toRadians(Double.parseDouble(coords[1]));
		    if (lon != center[0] || lat != center[1]) {
		        p.setReference(lon,lat);
		        System.err.println("  Using non-standard image center:"+Settings.get("RefCoords"));
		    } else {
			System.err.println("  New reference center matches original");
		    }
		} catch (Exception e) {
		    System.err.println("Error resetting reference coordinates to:"+Settings.get("RefCoords")+
				       "\nProcessing continues  with defaults.");
		}
	    }
					       
	
	    // Find where the requested center is with respect to
	    // the fixed center of this projection.
	    Converter cvt  = new Converter();
	    cvt.add(p.getRotater());
	    cvt.add(p.getProjecter());
	    double[] uv     = 
	      skyview.geometry.Util.unit(Math.toRadians(lon), 
	      Math.toRadians(lat));
	    double[] coords = cvt.transform(uv);
	    s = new Scaler(0.5*nx + coords[0]/Math.toRadians(xscale), 
		           0.5*ny - coords[1]/Math.toRadians(yscale),
		           -1/Math.toRadians(xscale), 0, 
			   0, 1/Math.toRadians(yscale));
	
	} else {
	
	    p = new Projection(proj, new double[]{Math.toRadians(lon), 
  		                   Math.toRadians(lat)});
	    s = new Scaler(0.5*nx, 0.5*ny, 
		           -1/Math.toRadians(xscale), 0, 
			   0, 1/Math.toRadians(yscale));
        }	
	
	
	String  rot = Settings.get("rotation");
	
	if (rot != null  && rot.length() > 0) {
	    double angle;
	    try {
	        angle  = Math.toRadians(Double.parseDouble(rot));
	    } catch (Exception e) {
		throw new Exception("Invalid rotation setting:"+rot);
	    }
	    Scaler rScale = new Scaler(0, 0, Math.cos(angle), Math.sin(angle), 
        	                       -Math.sin(angle), Math.cos(angle));
	    s = rScale.add(s);
	}
	
	if (Settings.has("offset")) {
	    double[] deltas = new double[2];
	    try {
		String[] offsets = Settings.getArray("offset");
		if (offsets.length == 2) {
		    deltas[0] = -Double.parseDouble(offsets[0]);
		    deltas[1] = -Double.parseDouble(offsets[1]);
		} else {
		    deltas[0] = -Double.parseDouble(offsets[0]);
		    deltas[1] = deltas[0];
		}
		Scaler translate = new Scaler(deltas[0], deltas[1], 1, 0, 0, 1);
		s = s.add(translate);
		
	    } catch (Exception e) {
		System.err.println("Error parsing/applying offset:"+Settings.get("Offset"));
	    }
	}
	return new WCS(c, p, s);
    }
    
    private WCS copyWCS(String file) throws Exception {
	// This should only read the first header of the file.
	Header hdr     = new Header(new Fits(file).getStream());
	return new WCS(hdr);
    }
    
    /** Get an Imager object -- normally the last one created.
     */
    public static Imager getImager() {
	if (lastImager == null) {
	    new Imager();
	}
	return lastImager;
    }
    
    private class ImageState {
	private Image[]      sources;
	private Image        output;
	private int[]        match;
	private Sampler      samp;
	private DepthSampler dsamp;
	private ArrayList<Processor> procs;
	ImageState(Image[] sources, Image output, int[] match,
		   Sampler samp, DepthSampler dsamp, ArrayList<Processor> procs) {
	    this.sources = sources;
	    this.output  = output;
	    this.match   = match;
	    this.samp    = samp;
	    this.dsamp   = dsamp;
	    this.procs   = (ArrayList<Processor>)procs.clone();  // What processes have already been applied?
	}
    }
}
