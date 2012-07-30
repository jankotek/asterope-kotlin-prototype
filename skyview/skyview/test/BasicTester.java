package skyview.test;

import skyview.executive.Imager;
import skyview.executive.Key;
import skyview.executive.Settings;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static skyview.test.Util.regress;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;

/** This class does basic testing of the features of
 *  the SkyView JAR.  It primarily does end-to-end
 *  tests of SkyView requests and does a regression of the
 *  results against previous values.  The results of the
 *  previous tests are loaded using a Settings file,
 *  comparison.settings. 
 *  <p>
 *  Using the standard Ant script all testing is done in
 *  a temp subdirectory.  The comparison.settings and
 *  inputtest1.fits file are copied from the source/skyview/test
 *  directory into this directory.  The actual tests are
 *  run using JUnit.  JUnit invokes all methods with
 *  the Test annotation.  If an assertion in any method fails
 *  it continues with the next test.
 *  <p>
 *  This program does not do many unit tests of individual functions,
 *  nor does it test for bugs that may come from particular
 *  combinations of features selected by the user.  Additional
 *  tests can easily be added as appropriate however.
 *  <p>
 *  The Ant build script that compiles that Java code will run
 *  this script with 
 *  <code>ant -f build/skyview.xml test</code>.
 *  To run only some tests the user can set the environment
 *  variables MIN_TEST and MAX_TEST and only the tests
 *  that are between these values will be run.  Tests are organized
 *  in groups with each test numbered.  The following ranges
 *  define the category associated with each test.
 *  <dl>
 *     <dt>1-150    <dd> Checking individual surveys.
 *     <dt>151-199 <dd> Samplers and Mosaickers
 *     <dt>201-299 <dd> Projections
 *     <dt>301-399 <dd> Coordinate Systems
 *     <dt>401-499 <dd> Rotation and Translation
 *     <dt>501-599 <dd> Pixel scale
 *     <dt>601-699 <dd> Image sizes in pixels
 *     <dt>701-799 <dd> Settings control
 *     <dt>801-899 <dd> Different ways of specifying positions
 *     <dt>901-999 <dd> Quicklook image formats
 *     <dt>1001-1099 <dd> Gridding
 *     <dt>1101-1199 <dd> Contouring
 *     <dt>1201-1299 <dd> Smoothing
 *     <dt>1301-1399 <dd> Catalogs
 *     <dt>1401-1499 <dd> Look up tables
 *     <dt>1501-1599 <dd> RGB images
 *     <dt>1601-1601 <dd> Image intensity scaling (log, linear, sqrt)
 *     <dt>1701-1799 <dd> Plot parameters
 *   </dl>
 * Additional tests can be added to any of these, and new categories
 * of tests can be added.  Most of the tests &lt900 test the data
 * in the resulting FITS image.  Most of the higher tests test the data
 * in the quicklook image.
 * <p>
 * Changes to SkyView code will inevitably create changes in the
 * reference values even when there is no error,
 * e.g., just the text in a graphic overlay.  Tests will fail when
 * the comparison.settings file has a value for the sum of the appropriate
 * file, and the generated file does not match.  To update these,
 * simply delete the corresponding entry in comparison.settings.  If
 * a regression test is run and there is no entry in the comparison.settings
 * file, a line is written to the upd.settings file.  After the test
 * is run, this file can be concatenated to the comparison.settings
 * (in the source area) for future regression testing.
 */
public class BasicTester {
    
    private static DataOutputStream os;
    
    private static boolean first          = true;
    private static int     minTest        = 0;
    private static int     maxTest        = Integer.MAX_VALUE;
    private static int     base           = 1;
    private static boolean updateSettings = false;
    
    @Test public void testSurveys() throws Exception {
	
	base = 1;
	
        os = new DataOutputStream(new FileOutputStream("upd.settings"));
	
	if (System.getenv("MIN_TEST") != null) {
	    minTest = Integer.parseInt(System.getenv("MIN_TEST"));
	}
	if (System.getenv("MAX_TEST") != null) {
	    maxTest = Integer.parseInt(System.getenv("MAX_TEST"));
	}
	
	System.err.println("Tests from:"+minTest+" to "+maxTest);
	
	String[] surveys= new String[]{
	    "dss", "dss1r", "dss1b", "dss2r", "dss2b", "dss2ir",
	    "pspc2int", "pspc1int", "pspc2cnt", "pspc2exp",
	    "2massh", "2massj", "2massk",
	    "sdssi", "sdssu", "sdssz", "sdssg", "sdssr",
	    "iras100", "iras60", "iras25", "iras12",
	    "first", "nvss", "hriint",
	    "rass-cnt broad", "rass-cnt hard", "rass-cnt soft",
	    "halpha", "sfddust", "sfd100m",
	    "shassa_c", "shassa_cc", "shassa_h", "shassa_sm",
	    "4850mhz", "iris12", "iris25", "iris60", "iris100",
	    "wfcf1", "wfcf2", "euve83", "euve171", "euve405", "euve555",
	    "neat", "rass-int broad", "rass-int hard", "rass-int soft"
	};
	
	// Mellinger survey is done twice so that
	// we get full resolution and lower resolution images.
	String[] allSky = new String[] {
	    "1420mhz", "408mhz", "heao1a",
	    "cobeaam", "cobezsma",
	    "comptel",
	    "egretsoft", "egrethard", "egret3d",
	    "rxte3_20K_sig", "rxte3_8k_sig", "rxte8_20k_sig",
	    "nh", "0035mhz",
	    "wmapk", "wmapka", "wmapq", "wmapv", "wmapw", "wmapilc",
	    "mell-r", "mell-g", "mell-b",
            "intgal1735f","intgal1760f","intgal3580f","intgal1735e","intgal1735s"
        };
	
	String[] gc = new String[] {
	    "co", "granat_sigma_sig", "granat_sigma_flux",
	     "integralspi_gc", "batflux0", "batflux1", "batflux2",
	      "batflux3", "batflux4", "batsig0", "batsig1", "batsig2",
	      "batsig3", "batsig4", "rassbck1", "rassbck2", "rassbck3",
	      "rassbck4", "rassbck5", "rassbck6", "rassbck7",
	      "mell-r", "mell-g", "mell-b"
	};
	
	Settings.put(Key.position, "187.27791499999998,2.052388");
	
	for (int i=0; i<surveys.length; i += 1) {
	    testSurvey(surveys[i], i+base);
	}
	
	// Try some all sky surveys
	Settings.put(Key.position, "0.,0.");
	Settings.put(Key.coordinates, "galactic");
	Settings.put(Key.projection, "Car");
	Settings.put(Key.pixels, "600,300");
        Settings.put(Key.size, "360,180");
	
        base += surveys.length;
	for (int i=0; i<allSky.length; i += 1) {
	    testSurvey(allSky[i], i+base);
	}
	
        Settings.put(Key.size, "5");
	Settings.put(Key.pixels, "300");
	base += allSky.length;
	
	for (int i=0; i<gc.length; i += 1) {
	    testSurvey(gc[i], i+base);
	}
	
	Settings.put(Key.position, "0., 90.");
	Settings.put(Key.coordinates, "ICRS");
	Settings.put(Key.pixels, "500,500");
	Settings.put(Key.projection, "Tan");
	
	base += gc.length;
	
	testSurvey("wenss", base);
	base += 1;
	
	Settings.put(Key.position, "0., -90.");
	testSurvey("sumss", base);
	base += 1;
	
	Settings.put(Key.position, "10.,10.");
	// No coverage near 3c273
	Settings.put(Key.size, ".4");
	testSurvey("galexnear", base);
	base += 1;
	testSurvey("galexfar", base);
	base += 1;
	
	Settings.put(Key.size, "40");
	testSurvey("fermi1", base);
	base += 1;
	testSurvey("fermi2", base);
	base += 1;
	testSurvey("fermi3", base);
	base += 1;
	testSurvey("fermi4", base);
	base += 1;
	testSurvey("fermi5", base);
	base += 1;

        Settings.put(Key.size,"0.1");
        Settings.put(Key.position, "40.,20.");
        testSurvey("wisew1", base);
        base += 1;
        testSurvey("wisew2", base);
        base += 1;
        testSurvey("wisew3", base);
        base += 1;
        testSurvey("wisew4", base);
        base += 1;
	
	Settings.put(Key.size, "5");
	
    }

    public int jpegSum(String jpegName, int nx, int ny) throws Exception {
	
	InputStream is = new FileInputStream(jpegName);
	int[] data = new int[3*nx*ny];
	
	int sum = 0;
	data = com.sun.image.codec.jpeg.JPEGCodec.createJPEGDecoder(is)
	            .decodeAsRaster().getPixels(0,0,nx,ny,data);
	
	for (int i=0; i<data.length; i += 1) {
	    sum += data[i];
	}
	
	is.close();
	return sum;
    }
    
    Imager runImager(int index) throws Exception {
	Imager img = runImagerx(index);
	Settings.restore();
        return img;
    }
    
    Imager runImagerx(int index) throws Exception {
	
	Imager img = Imager.getImager();
	Settings.put(Key.output, "out"+index);
	
	if (first) {
	    Settings.addArgs(new String[] {"settings=comparison.settings", "noexit"});
	    img.init();
	    first = false;
	}
	
	Settings.save();
	
	if (updateSettings) {
	    img.checkUpdateSettings();
	}
	
	String surv = Settings.get(Key.survey);
	System.err.println("Processing survey:"+surv);
	img.processSurvey(surv);
	
	return img;
    }
    
    void testSurvey(String surv, int index) throws Exception {
	
	Settings.put(Key.survey, surv);

	if (minTest > 100 || index < minTest || index > maxTest) {
	    return;
	}
	
	Imager img = runImager(index);
	
	double[] data = (double[])img.getImageData();
	
	double sum = total(data);
	System.err.println("  Sum is:"+sum);
	regress(surv+"_sum_"+index, sum, os);
    }
    
    @Test public void testSamplers() throws Exception {
	
	base = 151;
	
	String[] samplers = new String[] {"NN", "LI", "Lanczos", "Lanczos3", "Lanczos4", 
	                                  "Spline","Spline3","Spline4","Spline5", "Clip"};
   
	Settings.put(Key.survey,      "user");
	Settings.put(Key.size,        "1.1");
	Settings.put(Key.pixels,      "22");
	Settings.put(Key.userfile,    "inputtest1.fits");
	Settings.put(Key.position,    "0.,0.");
	Settings.put(Key.coordinates, "J2000");
	
	doTests(samplers, Key.sampler, base);
	
	Settings.put(Key.userfile, "null");
	Settings.save();
	
      try {
	
	base += samplers.length;
	Settings.put(Key.sampler, "Clip");
	Settings.put(Key.survey, "IRIS100");
	Settings.put(Key.size,   "30");
	Settings.put(Key.pixels, "500");
	doTests(new String[]{"skyview.process.IDMosaic"}, Key.Mosaicker, base);
	base += 1;
	
	Settings.put(Key.survey, "User");
	Settings.put(Key.userfile, "inputtest1.fits,inputtest2.fits,inputtest3.fits");
	Settings.put(Key.position, "0.,0.");
	Settings.put(Key.size, "10.1");
	Settings.put(Key.pixels, "101");
	Settings.put(Key.sampler, "Clip");
	Settings.put(Key.projection, "Car");
	String[] specialFinders = new String[]{"Bypass", "Overlap"};
	Settings.put(Key.Mosaicker, "skyview.process.AddingMosaicker");
	
	doTests(specialFinders, Key.ImageFinder, base);
	base+= specialFinders.length;
	
	//Settings.put(Key.survey, "galexfar");
	//Settings.put(Key.Position, "m81");
	//Settings.put(Key.MAXRAD, "1391");
	//Settings.put(Key.ExposureFinder, "FitsKey.ord);
	//Settings.put(Key.ExposureKeyword, "EXPTIME");
	//Settings.put(Key.Size, "0.3");
	//Settings.put(Key.pixels, "600");
	//Settings.put(Key.Projection, "Tan");
	//doTests(specialFinders, "imagefinder", base);
	//base+= specialFinders.length;
      } finally {
	Settings.restore();
      }
	
    }
    
    double doTests(String[] options, Key param, int base) throws Exception {
	
	double grand = 0;
	
	for (int i=0; i<options.length; i += 1) {
	    System.err.println("Setting "+param+" to "+options[i]);
	    int index = base+i;
	    if (index < minTest || index > maxTest) {
	        continue;
	    }
	    Settings.put(param, options[i]);
	    Imager img = runImager(base+i);
	    double sum = total((double[])img.getImageData());
	    grand += sum;
	    img.clearImageCache();
	    System.err.println("  Sum is:"+sum);
	    regress(options[i]+"_sum_"+index, sum, os);
	}
	return grand;
    }
    
    private double total(double[] data) {
	double sum = 0;
	for (int j=0; j<data.length; j += 1) {
	    if (data[j] == data[j]) {
	        sum += data[j];
	    }
	}
	return sum;
    }
    
    private long fileSum(String filename) {
	long sum = 0;
	try {
	    FileInputStream bf = new FileInputStream(filename);
	    int byt;
	    while ((byt = bf.read()) >= 0) {
		sum += byt;
	    }
	    bf.close();
	} catch (Exception e) {}
	return sum;
    }
    
    private void jpegCheck(String msg, int base,int nx, int ny) throws Exception {
	String jpegFile = "out"+base+".jpg";
	int sum = jpegSum(jpegFile, nx, ny);
	regress("JPEG_"+msg+"_"+base, sum, os);
    }
    private void jpegRGBCheck(String msg, int base,int nx, int ny) throws Exception {
	String jpegFile = "out"+base+"_rgb.jpg";
	int sum = jpegSum(jpegFile, nx, ny);
	regress("JPEG_"+msg+"_"+base, sum, os);
    }
	
	
    
    @Test public void testProjections() throws Exception {
	
	String[] projections = {"Tan", "Sin", "Car", "Ait", "Csc", "Zea", "Arc", "Stg", "Sfl", "Hpx", "Tea"};
	
	Settings.put(Key.survey, "heao1a");
	Settings.put(Key.coordinates, "Galactic");
	Settings.put(Key.size, "90");
	Settings.put(Key.pixels, "300");
	
	doTests(projections, Key.projection, 201);
	
	String[] proj2 = {"Toa"};
	
	Settings.save();
      boolean updSave = updateSettings;
      try {
	
	// Want to center Toa around the pole.
	// 
	Settings.put(Key.coordinates, "J2000");
	Settings.put(Key.position,    "0.,90.");
	Settings.put(Key.survey,      "HEAO1A");
	Settings.put(Key.pixels,      "200");
	Settings.put(Key.size,        "90");
	Settings.put(Key.sampler,     "Clip");
	Settings.put(Key.ClipIntensive,"1");
	
	doTests(proj2, Key.projection, 201 + projections.length);
	
	updateSettings = true;
	// Now do a tiled TOAST projection
	Settings.put(Key.size, "null");
	Settings.put(Key.pixels, "null");
	Settings.put(Key.survey, "DSS");
	Settings.put(Key.level, "9");
	Settings.put(Key.tileX, "255");
	Settings.put(Key.tileY, "255");
	Settings.put(Key.Subdiv, "9");
	String upd = Settings.get(Key.SettingsUpdaters);
	Settings.put(Key.SettingsUpdaters, "skyview.request.ToastGridder,"+upd);
	doTests(proj2, Key.projection, 201+projections.length+proj2.length);
      } finally {
	Settings.restore();
	updateSettings = updSave;
      }
    }
    
    @Test public void testCoordinates() throws Exception {
	
	String[] coordinates = {"J2000", "B1950", "E2000", "H2000", "Galactic", "ICRS"};
	
	Settings.put(Key.survey, "heao1a");
	Settings.put(Key.projection, "Car");
	Settings.put(Key.size, "90");
	Settings.put(Key.pixels, "300");
	
	
	doTests(coordinates, Key.coordinates, 301);
    }
    
    @Test public void testRotation() throws Exception {
	
	String[] angles = {"0", "30", "60", "90", "180", "-30", "-90"};
	
	Settings.put(Key.survey, "heao1a");
	Settings.put(Key.projection, "Car");
	Settings.put(Key.size, "90");
	Settings.put(Key.coordinates, "galactic");
	Settings.put(Key.pixels, "300");
	Settings.put(Key.sampler, "NN");
	
	doTests(angles, Key.rotation, 401);
	Settings.put(Key.rotation, "null");
	
	Settings.put(Key.survey,     "dss");
	Settings.put(Key.projection, "Tan");
	Settings.put(Key.coordinates, "J2000");
	Settings.put(Key.scale,       "null");
	Settings.put(Key.size,       "null");
	Settings.put(Key.position,   "187.27791499999998,2.052388");
        
	double sum0 = doTests(new String[]{"1"},Key.min, 401+angles.length);
	
	Settings.put(Key.pixels, "150");
	double sum1 = doTests(new String[]{"-75,-75", "-75,75", "75,-75", "75,75"},Key.offset, 402+angles.length);

        System.err.println("Offset test:"+sum0+" "+sum1);
	assertEquals("Offset test:", sum0, sum1, 0);
	
	// RefCoords test
	Settings.put(Key.offset, "null");
	Settings.put(Key.pixels, "500,250");
	Settings.put(Key.size, "380,180");
	Settings.put(Key.projection, "Ait");
	Settings.put(Key.position, "0.,90.");
	Settings.put(Key.survey, "408Mhz");
	doTests(new String[]{"0.,1.", "0.,90.", "0.,-89.", "179.,0."}, Key.RefCoords, 401+angles.length+5);
	
	Settings.put(Key.position, "187.27791499999998,2.052388");
	Settings.put(Key.RefCoords, "null");
	Settings.put(Key.size, "null");
	Settings.put(Key.survey, "dss");
	
	Settings.put(Key.pixels, "300");
	Settings.put(Key.projection, "Car");
	
	
    }
    
    @Test public void testScale() throws Exception {
	
	String[] scales = {"0.25", "0.25,0.25", "0.5,0.25", "0.25,0.5", "0.1"};
	
	Settings.put(Key.survey,      "heao1a");
	Settings.put(Key.position,    "0.,0.");
	Settings.put(Key.projection,  "Car");
	Settings.put(Key.size,        "90");
	Settings.put(Key.coordinates, "Galactic");
	Settings.put(Key.pixels,      "300");
	Settings.put(Key.sampler,     "NN");
	
	doTests(scales, Key.scale, 501);
    }
    
    @Test public void testPixel() throws Exception {
	String[] pixels = {"300", "300,150", "150,300", "10,10"};
	Settings.put(Key.survey, "heao1a");
	Settings.put(Key.projection, "Car");
	Settings.put(Key.size, "90");
	Settings.put(Key.coordinates, "galactic");
	Settings.put(Key.pixels, "300");
	Settings.put(Key.sampler, "NN");
	
	doTests(pixels, Key.pixels, 601);
    }
    
    @Test public void testSettings() {
	
	base = 701;
	if (base < minTest || base> maxTest) {
	    return;
	}
	System.err.println("Testing settings");
	assertTrue("XXX not set", !Settings.has(Key.XXX));
	Settings.add(Key.XXX, "aaa");
	assertTrue("XXX should not be set", Settings.has(Key.XXX));
	assertTrue("XXX should be length 1", Settings.getArray(Key.XXX).length == 1L);
	Settings.add(Key.XXX, "bbb");
	assertTrue("XXX should be length 2", Settings.getArray(Key.XXX).length == 2L);
	System.err.println("Adds worked!");
	Settings.put(Key.XXX, "null");
	assertTrue("Cleared xxx", !Settings.has(Key.XXX));
	assertTrue("Cleared xxx2", Settings.get(Key.XXX) == null);
        org.junit.Assert.
	assertTrue("Cleared xxx3", Settings.getArray(Key.XXX).length == 0L);
	System.err.println("Delete worked");
	Settings.put(Key.XXX, "a,b,c");
	assertTrue("Reset xxx", Settings.getArray(Key.XXX).length == 3L);
	System.err.println("Settings OK");
	assertTrue("Sugg1", Settings.get(Key.XXX).equals("a,b,c"));
	Settings.suggest(Key.XXX, "d,e,f");
	assertTrue("Sugg2", Settings.get(Key.XXX).equals("a,b,c"));
	Settings.suggest(Key.yyy, "d,e,f");
	assertTrue("Sugg3", Settings.get(Key.yyy).equals("d,e,f"));
	Settings.suggest(Key.yyy, "a,b,c");
	assertTrue("Sugg4", Settings.get(Key.yyy).equals("d,e,f"));
	assertTrue("Sugg5", Settings.has(Key.yyy));
	Settings.put(Key.yyy, "null");
	assertTrue("Sugg6", !Settings.has(Key.yyy));
	Settings.suggest(Key.yyy, "abc");
	assertTrue("Sugg7", !Settings.has(Key.yyy));
	Settings.put(Key.yyy, "abc");
	assertTrue("Sugg8", Settings.has(Key.yyy));	  
		   
    }
    
    @Test public void testPosit() throws Exception {
	
	base = 801;
	Settings.put(Key.survey, "heao1a");
	Settings.put(Key.projection, "Car");
	Settings.put(Key.size, "90");
	Settings.put(Key.coordinates, "galactic");
	Settings.put(Key.pixels, "30");
	Settings.put(Key.sampler, "NN");
	Settings.put(Key.position, "180., 0.");
	
	if (minTest > 900 || maxTest < 800) {
	    return;
	}
	
	Imager img = runImager(base);
	double val1 = total((double[])img.getImageData());
	
	img.clearImageCache();
	
	Settings.put(Key.position, "null");
	Settings.put(Key.lat, "0");
	Settings.put(Key.lon, "180");
	
	base += 1;
	img = runImager(base);
	double val2 = total((double[]) img.getImageData());
	assertEquals("Use lat/lon or position", val1, val2, 0);
	img.clearImageCache();
	
	Settings.put(Key.lat, "null");
	Settings.put(Key.lon, "null");
	
	Settings.put(Key.CopyWCS, "out801.fits");
	base += 1;
	
	img = runImager(base);
	double val3 = total((double[]) img.getImageData());
	assertEquals("Using copyWCS", val1, val3, 0);
	System.err.println("LatLon and copyWCS value:"+val1);
	Settings.put(Key.CopyWCS, "null");
    }
    
    @Test public void testQLFormats() throws Exception {
	
	Settings.put(Key.survey, "heao1a");
	Settings.put(Key.projection, "Car");
	Settings.put(Key.size, "360,180");
	Settings.put(Key.coordinates, "galactic");
	Settings.put(Key.pixels, "200,100");
	Settings.put(Key.sampler, "NN");
	Settings.put(Key.position, "180., 0.");
	
	String[] formats={"", "JPEG", "JPG", "GIF", "BMP", "TIFF", "PNG"};
	
	boolean oldState = updateSettings;
	updateSettings = true;
	doTests(formats, Key.quicklook, 901);
	
	if (maxTest <900 || minTest>=1000) {
	    return;
	}
	
	assertTrue("JPEG1", new File("out901.jpg").exists());
	assertTrue("JPEG2", new File("out902.jpg").exists());
	assertTrue("JPEG3", new File("out903.jpg").exists());
	assertTrue("GIF",   new File("out904.gif").exists());
	assertTrue("BMP",   new File("out905.bmp").exists());
	assertTrue("TIFF",  new File("out906.tiff").exists());
	assertTrue("PNG",   new File("out907.png").exists());
	
	System.err.println("Quicklook formats tested");
	
	assertTrue("Have901Fits", new File("out901.fits").exists());
	base = 901 + formats.length;
	
	Settings.put(Key.quicklook, "");
	Settings.put(Key.nofits, "");
	
	if (base >= minTest && base <= maxTest) {
	    runImager(base);
	    assertTrue("Nofits test1",  (new File("out"+base+".jpg").exists()));
	    assertTrue("Nofits test2", !(new File("out"+base+".fits").exists()));
	}
	updateSettings = oldState;
    }
    
    @Test public void testGrid() throws Exception {
	
	Settings.put(Key.scale, "null");
	Settings.put(Key.survey, "heao1a");
	Settings.put(Key.projection, "Car");
	Settings.put(Key.size, "360,180");
	Settings.put(Key.coordinates, "galactic");
	Settings.put(Key.pixels, "400,200");
	Settings.put(Key.sampler, "NN");
	Settings.put(Key.position, "0., 0.");
	Settings.put(Key.nofits, "");
	
	Settings.put(Key.grid, "");
	Settings.put(Key.quicklook, "JPG");
	
        base = 1001;
	
	boolean oldupd = updateSettings;
	updateSettings = true;
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    System.err.println("Size is:"+Settings.get(Key.pixels));
	    runImager(base);
	    jpegCheck("grid", base, 400,200); 
	}
	base += 1;
	
	Settings.put(Key.GridLabels, "");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("gridlab", base, 400,200); 
	}
	base += 1;
	
	Settings.put(Key.grid, "equatorial");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("grid", base, 400,200); 
	}
	base += 1;
	
        Settings.put(Key.grid, "");
	Settings.put(Key.projection, "Tan");
	Settings.put(Key.position, "45.,90.");
	Settings.put(Key.pixels, "300");
	Settings.put(Key.size, "60");
	
	Settings.put(Key.GridLabels, "null");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("grid", base, 300,300); 
	}
	base += 1;
	
	Settings.put(Key.GridLabels, "");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("gridlab", base, 300,300); 
	}
	base += 1;
	
	Settings.put(Key.grid, "equatorial");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("gridfor", base, 300,300); 
	}
	base += 1;
	
	Settings.put(Key.grid, "null");
	Settings.put(Key.GridLabels,"null");
	updateSettings = oldupd;
    }
    
    @Test public void testContour() throws Exception {
	
	System.err.println("Test contours");
	
	Settings.put(Key.scale, "null");
	Settings.put(Key.nofits, "null");
	
	Settings.put(Key.survey,     "heao1a");
	Settings.put(Key.projection, "Car");
	Settings.put(Key.size,       "360,180");
	Settings.put(Key.coordinates, "galactic");
	Settings.put(Key.pixels,     "400,200");
	Settings.put(Key.sampler,    "NN");
	Settings.put(Key.position,   "0., 0.");
	Settings.put(Key.contour,    "heao1a");
	Settings.put(Key.quicklook,  "jpg");
	
	boolean oldState = updateSettings;
	updateSettings = true;
	
	base = 1101;
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("cont", base, 400,200); 
	}
	base += 1;
	
	Settings.put(Key.contourSmooth, "7");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("contsmooth", base, 400,200); 
	}
	base += 1;
	
	Settings.put(Key.contourSmooth, "null");
	Settings.put(Key.contour, "heao1a:linear");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("contlin", base, 400,200); 
	}
	base += 1;
	
	Settings.put(Key.contour, "heao1a:sqrt");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("contsqrt", base, 400,200); 
	}
	base += 1;
		
	Settings.put(Key.contour, "heao1a:log:6");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("contncont", base, 400,200); 
	}
	base += 1;
	
	Settings.put(Key.contour, "heao1a:log:6:1:1000");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("contspec", base, 400,200); 
	}
	base += 1;
	
	Settings.put(Key.contour, "egrethard");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("contdiff", base, 400,200); 
	}
	base += 1;
	
	Settings.put(Key.contour, "egrethard:log:5:1.e-7:0.01");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("contdiffspec", base, 400,200); 
	}
	base += 1;
	
	Settings.put(Key.contourSmooth, "5");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("contdiffsmooth", base, 400,200); 
	}
	base += 1;
	
	Settings.put(Key.noContourPrint, "");
	if (base >= minTest && base <= maxTest) {
	    System.err.println("No contour countss should be printed this time");
	    Imager.getImager().clearImageCache();
	    runImager(base);
	}
	base += 1;
	
	updateSettings = oldState;
	Settings.put(Key.contourSmooth, "null");
	Settings.put(Key.noContourPrint, "null");
	Settings.put(Key.contour, "null");
	
    }
    
    @Test public void testSmoothing() throws Exception {
	
	base = 1201;
	double[] data;
	
	boolean oldState = updateSettings;
	updateSettings = true;
	
	Settings.put(Key.survey,      "user");
	Settings.put(Key.userfile,    "inputtest1.fits");
	Settings.put(Key.CopyWCS,     "inputtest1.fits");
	
	Settings.put(Key.smooth, "5");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    data = (double[])Imager.getImager().getImageData();
	
	    System.err.println("Smoothing 5x5 box:"+data[5*11+5]);
	    assertEquals("5x5 box: in", data[5*11+5], 1./25, 0);
	    assertEquals("5x5 box: in", data[5*11+6], 1./25, 0);
   	    assertEquals("5x5 box: in", data[5*11+7], 1./25, 0);
	    assertEquals("5x5 box: in", data[6*11+5], 1./25, 0);
	    assertEquals("5x5 box: in", data[7*11+5], 1./25, 0);
	    assertEquals("5x5 box: out", data[8*11+5], 0., 0);
	    assertEquals("5x5 box: out", data[5*11+8], 0., 0);
	}
	base += 1;
	
	Settings.put(Key.smooth, "5,1");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    data = (double[])Imager.getImager().getImageData();
	
	    assertEquals("5x5 box: in", data[5*11+5], 1./5, 0);
	    assertEquals("5x5 box: in", data[5*11+6], 1./5, 0);
	    assertEquals("5x5 box: in", data[5*11+7], 1./5, 0);
	    assertEquals("5x5 box: out", data[6*11+5], 0., 0);
	    assertEquals("5x5 box: out", data[5*11+8], 0., 0);
	}
	
	base += 1;
	
	Settings.put(Key.smooth, "1,5");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    data = (double[])Imager.getImager().getImageData();
	
	    assertEquals("5x5 box: in", data[5*11+5], 1./5, 0);
	    assertEquals("5x5 box: in", data[6*11+5], 1./5, 0);
	    assertEquals("5x5 box: in", data[7*11+5], 1./5, 0);
	    assertEquals("5x5 box: out", data[5*11+6], 0., 0);
	    assertEquals("5x5 box: out", data[8*11+5], 0., 0);
	}
	
	base += 1;
	
	updateSettings = oldState;
	Settings.put(Key.userfile,    "null");
	Settings.put(Key.CopyWCS,     "null");
    }
    
    @Test public void testCatalogs() throws Exception {
	
	base = 1301;
	Settings.put(Key.survey,      "rass-cnt broad");
	Settings.put(Key.scaling,     "log");
	Settings.put(Key.size,        "15");
	Settings.put(Key.pixels,      "500");
	Settings.put(Key.catalog,     "rosmaster");
	Settings.put(Key.quicklook,   "jpg");
	Settings.put(Key.position,    "0.,0.");
	Settings.put(Key.coordinates, "Galactic");
	Settings.put(Key.projection,  "Car");
	Settings.put(Key.nofits,      "null");
	Settings.put(Key.min,         "null");
	Settings.put(Key.max,         "null");
	
	boolean oldState = updateSettings;
	updateSettings = true;
	
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("cat", base, 500,500); 
	}
	base += 1;
	
	Settings.put(Key.CatalogIDs, "");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImager(base);
	    jpegCheck("catids", base, 500,500); 
	}
	base += 1;
	
	// Clear the cache (so that output will be printed)
	skyview.vo.CatalogProcessor.clearRequests();
      
	int count1=Integer.MAX_VALUE, count2=Integer.MAX_VALUE, count3=Integer.MAX_VALUE;
	Settings.put(Key.catalogFile, "");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	
	    assertTrue("catalogcount", Settings.has(Key._totalCatalogCount));
	    assertTrue("tableexists", new File("out"+base+".tab").exists());
	
	    count1 = Integer.parseInt(Settings.get(Key._totalCatalogCount));
	    regress("catquery_"+base+"_a", count1, os);
	    Settings.restore();
	}
						
	base += 1;
	
	Settings.put(Key.CatalogRadius, "5");
	skyview.vo.CatalogProcessor.clearRequests();
      
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    
	    count2 = Integer.parseInt(Settings.get(Key._totalCatalogCount));
	    regress("catquery_"+base+"_b", count2, os);
	
	    assertTrue("radiusfilter", count2<count1);
	    Settings.restore();
	}
	
	base += 1;
	  
	Settings.put(Key.CatalogFilter, "instrument=hri");
	skyview.vo.CatalogProcessor.clearRequests();
      
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    
	    count3 = Integer.parseInt(Settings.get(Key._totalCatalogCount));
	    regress("catquery_"+base+"_c", count3, os);
	
	    assertTrue("fieldfilter", count3<count2);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.CatalogFilter, "instrument=hri,exposure>20000");
	skyview.vo.CatalogProcessor.clearRequests();
      
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    
	    count3 = Integer.parseInt(Settings.get(Key._totalCatalogCount));
	    regress("catquery_"+base+"_c", count3, os);
	
	    assertTrue("fieldfilter", count3<count2);
	    Settings.restore();
	}
	base += 1;
	// Still in Galactic coordinates -- so we use 3C273 in Gal.
	Settings.put(Key.position, "289.95087909728574,64.35997524900246");
	Settings.put(Key.survey, "dss");
	Settings.put(Key.projection, "Tan");
	Settings.put(Key.size, "0.1");
	Settings.put(Key.CatalogFilter, "null");
	Settings.put(Key.CatalogRadius, "null");
	Settings.put(Key.nofits, "null");
	Settings.put(Key.pixels, "300");
	Settings.put(Key.scale, "null");
	
	Settings.put(Key.catalog, "ned");
	skyview.vo.CatalogProcessor.clearRequests();
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    System.err.println("test:"+Settings.get(Key.scale));
	    
	    count3 = Integer.parseInt(Settings.get(Key._totalCatalogCount));
	    regress("catquery_"+base, count3, os);
	
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.catalog, "I/284");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    
	    count3 = Integer.parseInt(Settings.get(Key._totalCatalogCount));
	    regress("catquery_"+base, count3, os);
	
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.catalog, "http://heasarc.gsfc.nasa.gov/cgi-bin/vo/cone/coneGet.pl?table=rosmaster&");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    
	    count3 = Integer.parseInt(Settings.get(Key._totalCatalogCount));
	    regress("catquery_"+base, count3, os);
	
	    Settings.restore();
	}
	base += 1;
	
	// This should reuse requests
	Settings.put(Key.catalog, "ned,I/284,http://heasarc.gsfc.nasa.gov/cgi-bin/vo/cone/coneGet.pl?table=rosmaster&");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    
	    count3 = Integer.parseInt(Settings.get(Key._totalCatalogCount));
	    regress("catquery_"+base, count3, os);
	
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.catalog, "ned,I/284,http://heasarc.gsfc.nasa.gov/cgi-bin/vo/cone/coneGet.pl?table=rosmaster&");
	// Repeat after clearing requests.
	skyview.vo.CatalogProcessor.clearRequests();
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    
	    count3 = Integer.parseInt(Settings.get(Key._totalCatalogCount));
	    regress("catquery_"+base, count3, os);
	    jpegCheck("catcomp", base, 300,300); 
	    Settings.restore();
	}
	base += 1;
	
	
	Settings.put(Key.catalog, "ned");
	Settings.put(Key.CatalogFields, "");
	skyview.vo.CatalogProcessor.clearRequests();
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    Settings.restore();
	}
	base += 1;
	
	
	Settings.put(Key.catalog, "rosmaster");
	Settings.put(Key.CatalogFields, "");
	Settings.put(Key.catalogFile, "mycat.file");
	Settings.put(Key.CatalogColumns, "instrument,exposure");
	skyview.vo.CatalogProcessor.clearRequests();
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    long sum = fileSum("mycat.file");
	    regress("catquery_"+base, sum, os);
	    Settings.restore();
	}
	base += 1;
	
	
	
	Settings.put(Key.catalog,       "null");
	Settings.put(Key.CatalogFields, "null");
	Settings.put(Key.CatalogIDs,    "null");
	Settings.put(Key.CatalogRadius, "null");
	Settings.put(Key.catalogFile,   "null");
	Settings.put(Key.Preprocessor, "null");
	Settings.put(Key.Postprocessor, "null");
	
	updateSettings = oldState;
    }
    
    @Test public void testLUT() throws Exception { 
	
	Settings.put(Key.survey,      "dss");
	Settings.put(Key.quicklook,   "jpg");
	Settings.put(Key.position,    "187.27791499999998,2.052388");
	Settings.put(Key.coordinates, "J2000");
	Settings.put(Key.pixels,      "500");
	Settings.put(Key.nofits,      "");
	Settings.put(Key.Postprocessor, "null");
	
	boolean oldState = updateSettings;
	updateSettings = true;
	
        base = 1401;	
	
	
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("lutbase", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.invert, "");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("lutinv", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.invert, "null");
	Settings.put(Key.lut, "fire");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("lutij", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.invert, "");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("lutijinv", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.invert, "null");
	Settings.put(Key.lut, "null");
	Settings.put(Key.coltab, "green-pink");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("lutbatch", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.coltab, "null");
	Settings.put(Key.lut, "colortables/green-pink.bin");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("lutijinv", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.lut, "null");
	Settings.put(Key.nofits, "null");
	Settings.put(Key.quicklook, "null");
	
	updateSettings = oldState;
	
	
    }
    
    @Test public void testRGB() throws Exception {
	
	Settings.put(Key.survey,      "iras100,iras25,rass-cnt broad");
	Settings.put(Key.position,    "0.,0.");
	Settings.put(Key.coordinates, "G");
	Settings.put(Key.pixels,      "600,300");
	Settings.put(Key.size,        "40,20");
	Settings.put(Key.rgb,         "");
	
	
	base = 1501;
	
	if (base >= minTest && base <= maxTest) {
	    Settings.put(Key.output, "out"+base);
	    Imager img = new Imager();
	    new Imager().run();
	    jpegRGBCheck("rgb", base, 600, 300);
	}
	base += 1;
	
	Settings.put(Key.rgbsmooth, "1,1,5");
	if (base >= minTest && base <= maxTest) {
	    Settings.put(Key.output, "out"+base);
	    new Imager().run();
	    jpegRGBCheck("rgbsm", base, 600, 300);
	}
	base += 1;
	
	Settings.put(Key.rgbsmooth, "1,1,5");
	Settings.put(Key.grid, "");
	Settings.put(Key.GridLabels, "");
	if (base >= minTest && base <= maxTest) {
	    Settings.put(Key.output, "out"+base);
	    new Imager().run();
	    jpegRGBCheck("rgbsmgrid", base, 600, 300);
	}
	base += 1;
	
	Settings.put(Key.rgb,        "null");
	Settings.put(Key.grid,       "null");
	Settings.put(Key.GridLabels, "null");
	Settings.put(Key.rgbsmooth,  "null");
	
    }
    
    @Test public void testScaling() throws Exception {
	
	Settings.put(Key.survey,      "iras100");
	Settings.put(Key.position,    "0.,0.");
	Settings.put(Key.coordinates, "G");
	Settings.put(Key.pixels,      "500");
	Settings.put(Key.size,        "10");
	Settings.put(Key.nofits,      "null");
	
	base = 1601;
	boolean oldState = updateSettings;
	updateSettings = true;
	
	Settings.put(Key.scaling, "log");
				   
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("scalelog", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
			       
	Settings.put(Key.scaling, "linear");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("scalelin", base, 500, 500);
	    Settings.restore();
	}
	base += 1;

	Settings.put(Key.scaling, "sqrt");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("scalesqrt", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.scaling, "histeq");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("scalehisteq", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.scaling, "log");
	Settings.put(Key.min, "200");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("scalelogmin", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	
	Settings.put(Key.scaling, "log");
	Settings.put(Key.max, "5000");
	if (base >= minTest && base <= maxTest) {
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("scalelogminmax", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.scaling, "null");
	Settings.put(Key.min, "null");
	Settings.put(Key.max, "null");
	
	updateSettings = oldState;
    }
    
    
    @Test public void testPlot() throws Exception {
	
	Settings.put(Key.survey,      "iras100");
	Settings.put(Key.position,    "0.,0.");
	Settings.put(Key.coordinates, "G");
	Settings.put(Key.pixels,      "500");
	Settings.put(Key.size,        "10");
	Settings.put(Key.nofits,      "null");
	
	boolean oldSettings = updateSettings;
	updateSettings = true;
	
	base = 1701;
	
	Settings.put(Key.grid,        "");
	Settings.put(Key.GridLabels,  "");
	Settings.put(Key.catalog,     "rosmaster");
	Settings.put(Key.CatalogIDs,  "");
	
	if (base >= minTest && base <= maxTest) {
	    skyview.vo.CatalogProcessor.clearRequests();
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("plotbasic", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.plotscale, "2");
	if (base >= minTest && base <= maxTest) {
	    skyview.vo.CatalogProcessor.clearRequests();
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("plotscale", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.plotscale, "3");
	Settings.put(Key.plotfontsize, "20");
       
	if (base >= minTest && base <= maxTest) {
	    skyview.vo.CatalogProcessor.clearRequests();
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("plotscalefont", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.plotcolor, "green");
	Settings.put(Key.lut, "grays");
	if (base >= minTest && base <= maxTest) {
	    skyview.vo.CatalogProcessor.clearRequests();
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("plotcolorFAILS", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.lut, "fire");
	if (base >= minTest && base <= maxTest) {
	    skyview.vo.CatalogProcessor.clearRequests();
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("plotcolorWORKS", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.draw, "50 50,-50 -50,,50 -50,-50 50");
	if (base >= minTest && base <= maxTest) {
	    skyview.vo.CatalogProcessor.clearRequests();
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("DrawX", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.draw, "null");
	Settings.put(Key.DrawFile, "plot1.drw");
	if (base >= minTest && base <= maxTest) {
	    skyview.vo.CatalogProcessor.clearRequests();
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("DrawFile1", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.DrawFile, "plot1.drw");
	Settings.put(Key.drawAngle, "45");
	if (base >= minTest && base <= maxTest) {
	    skyview.vo.CatalogProcessor.clearRequests();
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("DrawAngle", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.drawAngle, "null");
	Settings.put(Key.DrawFile, "plot2.drw");
	if (base >= minTest && base <= maxTest) {
	    skyview.vo.CatalogProcessor.clearRequests();
	    Imager.getImager().clearImageCache();
	    runImagerx(base);
	    jpegCheck("DrawFile2", base, 500, 500);
	    Settings.restore();
	}
	base += 1;
	
	Settings.put(Key.plotcolor, "null");
	Settings.put(Key.plotscale, "null");
	Settings.put(Key.plotfontsize, "null");
	Settings.put(Key.DrawFile, "null");
	Settings.put(Key.drawAngle, "null");
	Settings.put(Key.draw, "null");
	Settings.put(Key.grid, "null");
	Settings.put(Key.GridLabels, "null");
	Settings.put(Key.catalog, "null");
	Settings.put(Key.CatalogIDs, "null");
    }
}
