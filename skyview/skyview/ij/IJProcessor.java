package skyview.ij;


import skyview.executive.Key;
import skyview.survey.Image;
import skyview.executive.Settings;

import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;

import skyview.vo.CatalogProcessor;
import skyview.data.Gridder;
import skyview.data.Contourer;
import skyview.data.BoxSmoother;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.IndexColorModel;

import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import static org.apache.commons.math3.util.FastMath.*;

/** This class uses ImageJ to create non-FITS image products.
 *  The following keyword settings are supported:
 * 
 *  <dl>
 *   <dt> Quicklook
 *     <dd> Specifies the format to be used for a quicklook image.
 *     Supported formats are: JPEG, GIF, TIFF, BMP and PNG.
 *   <dt> Scaling
 *      <dd> Specifies how the brigtness of the image is to be scaled.
 *      Supported values include:
 *      <dl><dt> Log <dd> Logarithmic scaling.
 *          <dt> Sqrt <dd> Scaling as the square root of the pixel value.
 *          <dt> Linear <dd> Linear scaling.
 *          <dt> HistEq <dd> Histogram equalization scaling.
 *          <dt> LogLog </dd> Double log scaling.
 *      </dl>
 *   <dt> Inverse <dd> Invert the color table.
 *   <dt> Lut <dd> Load a look-up table.
 *  </dl>
 *  If any of these keywords are found, the updateSettings will
 *  ensure that there the IJProcessor is included as a postprocessor,
 *  so the user need not explicitly specify this.
 */
public class IJProcessor implements skyview.process.Processor {
    
    private static BufferedImage[] rgb = new BufferedImage[3];

    private static ArrayList<BufferedImage> savedImages;
    
    private static String[] stdLUTs= {"fire", "grays", "ice", "spectrum", "3-3-2 rgb",
	                   "red", "green", "cyan", "magenta", "yellow", "red/green"};
	
    private boolean headless = false;
    
    private double gridScale = Double.NaN;
    private Gridder grid;
    
    
    private ImageProcessor ip;
    
    private int nx;
    private int ny;
    
    private BufferedImage bi;
    
    static  IndexColorModel icm;
    private Font userFont;
    private boolean setUserFont = false;
    
    private HashSet<Color> colorHash;
    
    static {
	byte[] r = new byte[256];
	for (int i=0; i<r.length; i += 1) {
	    r[i] = (byte)i;
	}
	icm = new IndexColorModel(8, 256, r, r, r); 
    }
    
    
    private skyview.survey.Image output;
    
    public String getName() {
	return "IJProcess";
    }
    public String getDescription() {
	return "Do image processing in ImageJ";
    }
    
    private void processMin(String min) {
	if (min != null) {
	    try {
	        double fmin = Double.parseDouble(min);
	        ip.min(fmin);
	    } catch (Exception e) {
	        System.err.println("  Error parsing min value: "+min);
	    }
	}
    }
    
    private void processMax(String max) {
	if (max != null) {
	    try {
	        double fmax = Double.parseDouble(max);
	        ip.max(fmax);
	    } catch (Exception e) {
	        System.err.println("  Error parsing max value: "+max);
	    }
	}
    }
    
    private void processScale(String scale) {
	
	if (scale == null) {
	    scale = "log";
	}
	scale = scale.toLowerCase();
	if ( (scale.equals("log") || scale.equals("sqrt")) &&
	     !Settings.has(Key.min)) {
	    double zmin = 1.e20;
	    double zmax = -1.e20;
	    double[] data = output.getDataArray();
	    for (int i=0; i<data.length; i += 1) {
		if (data[i] > 0 && data[i] < zmin) {
		    zmin = data[i];
		}
		if (data[i] > zmax) {
		    zmax = data[i];
		}
	    }
	    if (zmax/zmin > 100000) {
		zmin = zmax/100000;
	    }
	    ip.min(zmin);
	}
	
	if (scale.equals("log")) {
	    ip.log();
	    
	} else if (scale.equals("loglog")) {
	    
	    double[] data = output.getDataArray();
	    double min = Double.NaN;
	    for (int i=0; i<data.length; i += 1) {
	        if (min != min || (min == min && data[i] > 0 && min>data[i])) {
		    min = data[i];
		}
	    }
	    if (min == min) {
		ip.multiply(1.01/min);
		ip.log();  // Smallest >zero should scale to 1.01
		ip.log();
	    }
	} else if (scale.equals("sqrt")) {
	    ip.sqrt();
	}
	if (scale.equals("histeq")) {
        ip = new ByteProcessor(ip.createImage());
	    new ContrastEnhancer().equalize(ip);
	} else {
	    if (Settings.has(Key.min) && Settings.has(Key.max)) {
		standardScale(scale);
	    }
	}
    }
    
    private String getIndexedString(Key field) {
	if (Settings.get(Key._surveyCount) == null ) {
	    return Settings.get(field);
	} else {
	    int index = Integer.parseInt(Settings.get(Key._surveyCount));
	    String[] flds = Settings.getArray(field);
	    if (flds == null || flds.length == 0) {
		return null;
	    }
	    return flds[(index-1)%flds.length];
	}
    }
	    
    
    /** User has specified both min and max, so use that
     *  to define the translation to intensity rather than
     *  the actual pixel values.
     */
    private void standardScale(String scale) {
	float mn = Float.parseFloat(getIndexedString(Key.min));
	float mx = Float.parseFloat(getIndexedString(Key.max));
	if (mx <= mn) {
	    System.err.println("Scaling has Max < Min");
	}
	if (scale.equals("sqrt")) {
	    mn = (float) sqrt(mn);
	    mx = (float) sqrt(mx);
	} else if (scale.equals("log")) {
	    mn = (float) log(mn);
	    mx = (float)  log(mx);
	}
	float delta   = (mx-mn)/256;
	float[] data = (float[]) ip.getPixels();
	for (int i=0; i<data.length; i += 1) {
	    data[i] = (data[i]-mn)/delta;
	}
	ip.setPixels(data);
	ip =  new ByteProcessor(ip.createImage()); // ip.convertToByte(false);
    }
    
    ImageProcessor getImageProcessor() {
	return ip;
    }
    
    private void processCatalog(String catalog) {
	if (catalog == null) {
	    return;
	}
	boolean labels       = Settings.has(Key.CatalogIDs);
	CatalogProcessor cp  = CatalogProcessor.getLastProcessor();
	double[][] pixels    = cp.getPixels();
	
	
	int[] symbols = cp.getSymbols();
		    
	for (int i=0; i<pixels.length; i += 1) {
	    drawSymbol(pixels[i][0], ny-pixels[i][1], symbols[i]);
	}
	if (labels) {
	    for (int i=0; i<pixels.length; i += 1) {
		ip.addPlotString(""+(i+1), (pixels[i][0]-3 + 0.5), (ny-pixels[i][1] - 3), 0);
	    }
	}
    }
    
    /** Handle image contours.
     */
    private void processContour(String contourStr) {
	
	if (contourStr == null) {
	    return;
	}
	String[] contours = Settings.getArray(Key.contour);
	for (int i=0; i<contours.length; i += 1) {
	    if (contours[i] == null || contours[i].length() == 0) {
		continue;
	    }
	    String[] flds = contours[i].split(":");
	    String survey = flds[0];
	    Contourer cntr = new Contourer();
	    
	    int nContour        = 4;
	    String contourScale = "Log";
	    double[] range = null;
	    // Could we get data?
	    if (survey.toLowerCase().equals(Settings.get(Key._currentSurvey).toLowerCase())) {
		cntr.putImage(output);
	    } else if (!cntr.getData(survey)) {
		continue;
	    }
	    if (flds.length > 1) {
		contourScale = flds[1];
	    }
	    cntr.setFunction(contourScale);
	    if (flds.length > 2) {
		try {
		    nContour = Integer.parseInt(flds[2]);
		} catch (Exception e) {
		    System.err.println("  Error parsing nContour:"+flds[2]+" using default");
		}
	    }
	    if (flds.length > 4) {
		try {
		    double mn = Double.parseDouble(flds[3]);
		    double mx = Double.parseDouble(flds[4]);
		    range = new double[]{mn,mx};
		} catch (Exception e) {
		    System.err.println("  Error parsing range values:"+flds[3]+" - "+flds[4]);
		}
	    }
	    double delta = 1;
	    if (range == null) {
		delta = 0.8;
		range = cntr.getRange();
	    }
	    cntr.setLimits(range[0], range[1], nContour, delta);
	    
	    
	    int[] overlay = cntr.contour();
	    for (int j=0; j<overlay.length; j += 1) {
		if (overlay[j] != 0) {
		    int x = j%nx;
		    int y = j/nx;
		    ip.drawPixel(x, ny-y);
		}
	    }
	}
    }
    
    private void processGrid(String gridStr) {
	
	
	if (gridStr != null) {
	    
	    try {
	        if (output.getWCS().getScale() != gridScale) {
		    if (gridStr.equals("1")) {
			gridStr = null;
		    }
		    grid = new Gridder(output, gridStr);
		    grid.grid();
		    gridScale = output.getWCS().getScale();
	        }
	    
	        double[][][] lines = grid.getLines();
	        for (int i=0; i<lines.length; i += 1) {
		    double[][] line = lines[i];
		    for (int j=1; j<line.length; j += 1) {
		        double[] p0 = line[j-1];
		        double[] p1 = line[j];
		        drawLine(p0[0], ny-p0[1], p1[0], ny-p1[1]);
		    }
	        }
		
	        if (Settings.has(Key.GridLabels)) {
	            String[] labels    = grid.getLabels();
		
		    for (int i=0; i<labels.length; i += 1) {
		        if (labels[i] == null ) {
			    continue;  // We're told not to label this one!
		        }
		        double[][] line = lines[i];
		        if (line.length < 4) {
			    continue;
		        }
		        double len = 0;
		        for (int j=1; j<line.length; j += 1) {
			    len += abs(line[j][0] - line[j - 1][0]) +
			           abs(line[j][1] - line[j - 1][1]);
		        }
		        if (len < 30) {
			    continue;
		        }
		        int p = (int)(0.3*lines[i].length);
		        double angle = atan2(-line[p][1] + line[p - 1][1], line[p][0] - line[p - 1][0]);
			// Make sure the letters aren't upside down.
			if (angle > PI/2) {
			    angle -= PI;
			} else if (angle < -PI/2) {
			    angle += PI;
			}
		        plotString(labels[i], line[p][0], line[p][1], angle);
		    }
	        }
		
	    } catch (Exception e) {
	        System.err.println("  Error gridding image:"+e);
		e.printStackTrace();
	    }
	}
    }
    
    void plotString(String label, double x, double y, double angle) {
	double sa = sin(angle);
	double ca = cos(angle);
        // Use local version to accommodate angle...
        //TODO add string
//	ip.addPlotString(label, x+3*sa+0.5, ny-(y+ca+0.5), angle);
    }
		    

    private boolean processRGB(String rgbStr, String outStem, int index) {
	if (rgbStr != null) {
	    
	    if (Settings.has(Key.rgboffset)) {
		try {
		    String[] offset = Settings.getArray(Key.rgboffset);
		    if (offset.length > index) {
			float off = Float.parseFloat(offset[index]);
		        float[] pix = (float[])ip.getPixels();
			for (int i=0; i<pix.length; i += 1) {
			    pix[i] += off;
			}
			ip.setPixels(pix);
		    }
		} catch (Exception e) {
		    System.err.println("  Unable to parse rgboffset:"+Settings.get(Key.rgboffset));
		}
	    }
	    
	    if (Settings.has(Key.rgbscale)) {
		try {
		    String[] scales = Settings.getArray(Key.rgbscale);
		    if (scales.length > index) {
			float scale = Float.parseFloat(scales[index]);
			float[] pix = (float[])ip.getPixels();
			for (int i=0; i<pix.length; i += 1) {
			    pix[i] *= scale;
			}
			ip.setPixels(pix);
		    }
		} catch (Exception e) {
		    System.err.println("  Unable to parse rgbscale:"+Settings.get(Key.rgbscale));
		}
	    }
	    
	    if (index < 3) {
		
	        String[] surveys = Settings.getArray(Key.survey);
	        rgb[index]      = toBufferedImage(ip.createImage());
		
	        if (index == 2 || index == surveys.length-1) {
              //merge images into RGB using Java Image API
                BufferedImage img = new BufferedImage(ip.getWidth(),ip.getHeight(), BufferedImage.TYPE_INT_RGB);

                for(int x= 0;x<img.getWidth();x++){
                    for(int y=0;y<img.getHeight();y++){
                        //TODO terribly inefficient, optimize !
                        int r = new Color(rgb[0].getRGB(x,y)).getRed();
                        int g = new Color(rgb[1].getRGB(x, y)).getGreen();
                        int b = new Color(rgb[2].getRGB(x, y)).getBlue();
                        Color c = new Color(r,g,b);
                        img.setRGB(x,y,c.getRGB());
                    }
                }


		    
//		    RGBStackMerge rsm = new RGBStackMerge();
//		    ImageStack is   = rsm.mergeStacks(output.getWidth(), output.getHeight(), 1,
//						    rgb[0], rgb[1], rgb[2], true);
//		    ImagePlus imp   = new ImagePlus("rgb", is);
		    if (Settings.has(Key.quicklook) ) {
		        String filename = outStem.substring(0,outStem.length()-1)+"rgb.jpg";
                try {
                    ImageIO.write(img, "jpg",new File(filename));
                } catch (IOException e) {
                    throw new IOError(e);
                }
                //   new FileSaver(imp).saveAsJpeg(filename);
		        System.err.println("  Writing 3-color image: " + filename);
		    }
	        }
	    }
	    return true;
	}
	return false;
    }
    
    void setColor(String colorString) {
// TODO plot strings
//	ip.plotStrings();
	Color col = getColor(colorString);
	if (col != null) {
	    if (colorHash == null) {
		colorHash = new HashSet<Color>();
	    }
	    if (!colorHash.contains(col)) {
	        ip.addColor(col, 255-colorHash.size());
		colorHash.add(col);
	    }
	    ip.setColor(col.getRGB());
	} else {
	    System.err.println("  Unknown plot color specified:"+Settings.get(Key.plotcolor));
	}
    }
    
    public void process(Image[] inputs, Image output, int[] source, 
				 Sampler samp, DepthSampler dsamp) {
    
	// If there is no output just return.  Also do no image processing
	// if we are just getting the image for contour.
	// 
	
	if (output == null) {
	    return;
	}
	;
	// Get what we need from the output image.
        double img[] = output.getDataArray();
       
	nx           = output.getWidth();
	ny           = output.getHeight();
	int nz       = output.getDepth();
	if (nz > 1) {
	    int len = nx*ny;
	    double[] ximg = new double[nx*ny];
	    for (int p=0; p<ximg.length; p += 1) {
		for (int z=0; z<nz; z += 1) {
		    ximg[p] += img[len*z + p];
		}
	    }
	    img = ximg;
	}
	
	this.output  = output;
	
	setUserFont = false;
	if (Settings.has(Key.plotfontsize)) {
	    try {
		if (userFont == null) {
		    userFont = new Font("SansSerif", Font.PLAIN, Integer.parseInt(Settings.get(Key.plotfontsize)));
	            setUserFont = true;
		}
	    } catch (Exception e) {
		System.err.println("  Warning: Unable to set font to size "+Settings.get(Key.plotfontsize));
	    }
	}
	
	String out = Settings.get(Key.output);
	
	String indexStr = Settings.get(Key._surveyCount);
	// Note that the survey index is 1 based (so that we start with file1 rather than file0)
	int index = 0;
	if (indexStr == null) {
	    index = 0;
	} else {
	    index = Integer.parseInt(indexStr) - 1;
	}

	if (Settings.has(Key.rgbsmooth)) {
	    String[] smoothings = Settings.getArray(Key.rgbsmooth);
	    if (smoothings.length >= index) {
		img = imgSmooth(img, smoothings[index]);
	    }
	}
		
        ip = new FloatProcessor(output.getWidth(), output.getHeight(), img);

	// Astronomers have Y start at the bottom, but ImageJ uses the typical image
	// convention and starts from the top.
	
	ip.flipVertical();


	// First process things that actually change the pixel values.
	// Here we need to treat the data as real.
	processMin(getIndexedString(Key.min));
	processMax(getIndexedString(Key.max));
	
	// Allow different scalings for different surveys
	if (Settings.has(Key.scaling)) {
	    
	    int scount = 1;
	    try {
		scount = Integer.parseInt(Settings.get(Key._surveyCount));
	    } catch (Exception e) {}
	    
	    String[] sarr = Settings.getArray(Key.scaling);
	    if (sarr.length > 0) {
	        processScale(sarr[(scount-1) % sarr.length]);
	    } else {
		processScale(null);
	    }
	} else {
	    processScale(null);
	}
	    

    ip = new ByteProcessor(ip.createImage());
	ip.setValue(255);

	// Now we're done with the pixels -- we play
	// with the color tables.
	processLUT(Settings.get(Key.lut));
	
        // This one's easy enought to do here!
	if (Settings.has(Key.invert)) {
	    ip.invertLut();
	}
	
	
	if (Settings.has(Key.plotcolor)) {
	    setColor(Settings.get(Key.plotcolor));
	}
	
	// Set the font if the user specified it.
	if (setUserFont) {
//    TODO set font
//	    ip.setFont(userFont);
	}
	processContour(Settings.get(Key.contour));
	processCatalog(Settings.get(Key.catalog));
	processGrid(Settings.get(Key.grid));
	
	
	// Draw lines if needed.
	if (Settings.has(Key.Annotations) || Settings.has(Key.draw)  || Settings.has(Key.DrawFile)) {
	    Drawer ld = new Drawer(this, nx, ny, output.getWCS());
	    
	    // Annotations are system specified and typically associated
	    // with surveys.
	    if (Settings.has(Key.Annotations)) {
	        for (String file: Settings.getArray(Key.Annotations)) {
		    ld.reset();
		    ld.drawFile(file);
	        }	    
	    }
	    
	    // Draw files are used specified.  User commands are drawn
	    // before the Draw file (if any).  This allows the
	    // user to do things like rotate the image.
	    if (Settings.has(Key.DrawFile)) {
	        for (String file: Settings.getArray(Key.DrawFile)) {
		    ld.reset();
		    ld.drawCommands();
		    ld.drawFile(file);
	        }	    
	    } else {
		ld.reset();
		ld.drawCommands();
	    }
	}
	
	// This will be a NOP if there were no strings plotted.
    //TODO plot strings
	//ip.plotStrings();
	
	// This should be done after we add anything to the image
	// It writes the output so we're done if we do this processing.
	if (processRGB(Settings.get(Key.rgb), out, index)) {
	    return;
	}
	
	// Time to write out the file.
	if (Settings.has(Key.quicklook)) {
	    writeFile(out, index);
	}
	
	// Display the image.
	if (Settings.has(Key.imagej)) {
        throw new UnsupportedOperationException("imagej not supported");
//	    String sname = Settings.getArray("survey")[index];
//	    imp = new ImagePlus(sname, ip);
//	    showImp(imp);
	}
    }
    
    private Color getColor(String color) {
	try {
	    java.lang.reflect.Field fld  = java.awt.Color.class.getField(color.toUpperCase());
	    if (fld != null) {
		return  (java.awt.Color) fld.get(null);
	    } else {
		return null;
	    }
	} catch (Exception e) {
	    return null;
	}
    }

     
    
    private void writeFile(String outStem, int index) {
	
	String    sname = Settings.getArray(Key.survey)[index];

    BufferedImage imp1 = toBufferedImage(ip.createImage());

	
	String format = Settings.get(Key.quicklook);
	if (format == null  || format.length() == 0) {
	    // If the user specifies RGB but not a quicklook format
	    // the they get only the RGB image.
	    if (Settings.has(Key.rgb)) {
		return;
	    }
	    format = "jpeg";
	}
	format = format.toLowerCase();

    try{
	if (format.equals("object")) {
	    if (savedImages == null) {
		savedImages = new ArrayList<BufferedImage>();
	    }
	    savedImages.add(imp1);
	

	} else if (format.equals("jpeg") || format.equals("jpg")) {
	    String file = outStem;
	    if (!file.equals("-")) {
		file += ".jpg";
	    }
        ImageIO.write(imp1, "jpg",new File(file));
	    System.err.println("  Creating quicklook image: "+file);
	    

	} else if (format.equals("png")) {
            ImageIO.write(imp1, "png",new File(outStem+".png"));
	        System.err.println("  Creating quicklook image: "+outStem+".png");

	} else {
	    System.err.println("  Error: Unrecognized quicklook format: " + format);
	}
    }catch(IOException e){
        throw new IOError(e);
    }
    }

    private void displayeImage(){
        BufferedImage imp1 = toBufferedImage(ip.createImage());
        JFrame frame = new JFrame();
        frame.getContentPane().setLayout(new FlowLayout());
        frame.getContentPane().add(new JLabel(new ImageIcon(imp1)));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setPreferredSize(new Dimension(imp1.getWidth(),imp1.getHeight()));
        frame.pack();
        frame.setVisible(true);
    }

    public void updateHeader(nom.tam.fits.Header header) {
	// Doesn't know anything about the FITS header,
	// so just skip it.
    }
    
    private void processLUT(String origLUT) {
	if (origLUT == null) {
	    return;
	}
	
	String lut = origLUT.toLowerCase();
	for (int i=0; i<stdLUTs.length; i += 1) {
	    if (stdLUTs[i].equals(lut)) {
		if (lut.equals("3-3-2 rgb")) {
		    lut = "3-3-2 RGB";
		}
		new LutLoader().run(lut, ip);
	        return;
	    }
	}
	
	byte[] red   = new byte[256];
	byte[] green = new byte[256];
	byte[] blue  = new byte[256];
	try {
	    java.io.InputStream is =  skyview.survey.Util.getResourceOrFile(lut);
	    
	    // If user is opening an LUT file, then on many operating
	    // systems we need to use the correct case.
	    if (is == null && !lut.equals(origLUT)) {
		is = skyview.survey.Util.getResourceOrFile(origLUT);
	    }
	    if (is == null) {
		System.err.println("  Error: Unable to find LUT "+origLUT);
		return;
	    }
	    
	    java.io.DataInputStream dis = new java.io.DataInputStream(is);
	    dis.readFully(red);
	    dis.readFully(green);
	    dis.readFully(blue);
	    dis.close();
	    dis = null;
	    
	} catch (Exception e) {
	    System.err.println("  Error trying to open/read LUT: "+origLUT+". "+e);
	    return;
	}
	
	IndexColorModel   cm = new IndexColorModel(8, 256, red, green, blue);
        ip.setColorModel(cm);
    }
    
    void drawSymbol(double x, double y, int symbol) {
	
	double scale = 1;
	if (Settings.has(Key.plotscale)) {
	    try {
		scale = Double.parseDouble(Settings.get(Key.plotscale));
	    } catch (Exception e) {
		System.err.println("  Warning: Unable to change plot scale to "+Settings.get(Key.plotscale));
	    }
	}
	
	symbol = symbol % 5;
	if (symbol == 0) {
	    drawLine( x-2*scale, y, x+2*scale, y);
	    drawLine( x, y-2*scale, x, y+2*scale);
	    
	} else if (symbol == 1) {
	    drawLine( x-2*scale, y-2*scale, x+2*scale, y+2*scale);
	    drawLine( x+2*scale, y-2*scale, x-2*scale, y+2*scale);
	    
	} else if (symbol == 2) {
	    drawLine( x-scale,y+scale, x+scale,y+scale);
	    drawLine( x+scale,y+scale, x,  y-scale);
	    drawLine( x,  y-scale, x-scale,y+scale);
	    
	} else if (symbol == 3) {
	    drawLine( x-scale,y-scale, x+scale,y-scale);
	    drawLine( x+scale,y-scale, x+scale,y+scale);
	    drawLine( x+scale,y+scale, x-scale,y+scale);
	    drawLine( x-scale,y+scale, x-scale,y-scale);
	} else if (symbol == 4) {
	    drawLine( x+scale,y,   x,  y+scale);
	    drawLine( x  ,y+scale, x-scale,y);
	    drawLine( x-scale,y,   x,  y-scale);
	    drawLine( x,  y-scale, x+scale,y);
	}
    }
    
    void drawLine(double x0, double y0, double x1, double y1) {
	int ix0 = (int)(x0+.5);
	int ix1 = (int)(x1+.5);
	int iy0 = (int)(y0+.5);
	int iy1 = (int)(y1+.5);
        //TODO draw a line
//	ip.drawLine(ix0, iy0, ix1, iy1);
    }
  
    /** This routine gets a Graphics (and probably Graphics2D)
     *  object with which the processor can write
     *  on the current state of the image.
     */
    private Graphics getGraphics() {
	
	// Convert to byte if needed.
	if (! (ip instanceof ByteProcessor) ) {
        ip = new ByteProcessor(ip.createImage());
	}
	byte[] pixels = (byte[]) ip.getPixels();
	
	
        bi  = new BufferedImage(nx, ny, BufferedImage.TYPE_BYTE_INDEXED, icm);	
	int [] buf = new int[nx];
	
	// Copy the data into the buffer.
	for (int i = 0; i < ny; i++) {
	    //create ABGR pixel array
	    for( int j = 0; j < nx; j++) {
		int a = pixels[i*nx+j] & 0xFF;
		buf[j] = (0xFF << 24) | (a << 16) | (a << 8) | a;
	    }
	    //set pixels
	    bi.setRGB(0, i, nx, 1, buf, 0, nx);
	} 
	return bi.getGraphics();
    }
    
    private double[] imgSmooth(double[] img, String smooth) {
	double[] ximg = img.clone();
	try {
	    int n = Integer.parseInt(smooth);
	    BoxSmoother.smooth(ximg, nx, ny, 1, n, n);
	} catch (Exception e) {
	    System.err.println("  Error in quicklook postprocessing smoothing");
	}
	return ximg;
    }


    // This method returns a buffered image with the contents of an image
    public static BufferedImage toBufferedImage(java.awt.Image image) {
        if (image instanceof BufferedImage) {
            return (BufferedImage)image;
        }

        // This code ensures that all the pixels in the image are loaded
        image = new ImageIcon(image).getImage();

        // Determine if the image has transparent pixels; for this method's
        // implementation, see Determining If an Image Has Transparent pixels
        boolean hasAlpha = false;

        // Create a buffered image with a format that's compatible with the screen
        BufferedImage bimage = null;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            // Determine the type of transparency of the new buffered image
            int transparency = Transparency.OPAQUE;
            if (hasAlpha) {
                transparency = Transparency.BITMASK;
            }

            // Create the buffered image
            GraphicsDevice gs = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            bimage = gc.createCompatibleImage(
                    image.getWidth(null), image.getHeight(null), transparency);
        } catch (HeadlessException e) {
            // The system does not have a screen
        }

        if (bimage == null) {
            // Create a buffered image using the default color model
            int type = BufferedImage.TYPE_INT_RGB;
            if (hasAlpha) {
                type = BufferedImage.TYPE_INT_ARGB;
            }
            bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
        }

        // Copy image to buffered image
        Graphics g = bimage.createGraphics();

        // Paint the image onto the buffered image
        g.drawImage(image, 0, 0, null);
        g.dispose();

        return bimage;
    }

}
