package skyview.ij;

import java.awt.Font;

import skyview.executive.Settings;
import skyview.geometry.Transformer;
import skyview.geometry.CoordinateSystem;
import skyview.geometry.Projection;
import skyview.geometry.Converter;
import skyview.geometry.Scaler;
import skyview.geometry.WCS;

import java.io.BufferedReader;
import java.io.FileReader;

import ij.ImagePlus;

import java.util.ArrayList;

/** Draw lines specified in a user file. */
public class Drawer {
    
    IJProcessor proc;
    double x0;
    double y0;
    double a00 = 1, a01=0, a10=0, a11 =1;
    double scale = 1;
    double offX  = 0, offY = 0;
    boolean colorSet = false;
    
    boolean cont  = false;
    double  lastx = -1;
    double  lasty = -1;
    
    int     pointSize = 12;
    
    Converter cnv = null;
    WCS wcs;
    double angScale;
    double currentAngle = 0;
    double initialAngle = 0;
    
    int recursion = 0;
    
    
    Drawer(IJProcessor ij, int nx, int ny, WCS wcs) {
	proc     = ij;
	x0       = nx/2.;
	y0       = ny/2.;
	this.wcs = wcs;
	angScale = wcs.getScale();
	if (Settings.has("drawAngle")) {
	    try {
	        initialAngle = Double.parseDouble(Settings.get("drawAngle"));
	    } catch (Exception e) {
		System.err.println("  Unable to parse plot angle:"+Settings.get("plotAngle"));
	    }
	}
	reset();
    }
    
    // Add in a rotation.
    void rotate(String angle) {
	try {
	    double xangle = Math.toRadians(Double.parseDouble(angle));
	    currentAngle += xangle;
	    a00 =  a11 = Math.cos(currentAngle);
	    a01 = -Math.sin(currentAngle);
	    a10 = -a01;
	} catch (Exception e) {
	    System.err.println("  Draw error: rotate:"+angle);
	}
    }
    
    // Add in an offset
    void offset(String val) {
	String[] offsets = val.split("\\s+");
	boolean OK = false;
	if (offsets.length == 2) {
	    try {
		
	        double x = Double.parseDouble(offsets[0]);
	        double y = Double.parseDouble(offsets[1]);
		offX += x;
		offY += y;
	        OK = true;
	    } catch (Exception e) {
	    }
	} else if (offsets.length == 1  && offsets[0].length() == 2) {
	    char c1 = offsets[0].charAt(0);
	    char c2 = offsets[0].charAt(1);
	    OK = true;
	    if (c1 == '+') {
		offX += x0;
	    } else if (c1 == '-') {
		offX -= x0;
	    }
	    if (c2 == '+') {
		offY += y0;
	    } else if (c2 == '-') {
		offY -= y0;
	    }
	}
	if (!OK) {
	    System.err.println("  Draw Error adding offset:"+val);
        }
    }
    
    void reset() {
	
	// Set offset to 0
	offX  = 0;
	offY  = 0;
	
	// Set scale to 1
	scale = 1;
	
	// Set angle to 0
	a00 = 1;
	a11 = 1;
	a01 = 0;
	a10 = 0;
	currentAngle = 0;
	rotate(""+initialAngle);
	
	
	// No special projection
	cnv = null;
	
	// Setting colors uses up space in the color table so we don't do this
	// unless required.
	if (colorSet) {
	    color("white");
	}
	thick("1");
	font("12");
	
    }
    
    void drawFile(String file) {
	recursion += 1;
	if (recursion < 10) {
	    ArrayList<String> input = new ArrayList<String>();
	    int readCount = -1;
	    try {
	        BufferedReader bf = new BufferedReader(new FileReader(file));
	        readCount = 0;
	        String line;
	        while ((line=bf.readLine()) != null) {
		    input.add(line);
		    readCount += 1;
	        }
	    } catch (Exception e) {
	        System.err.println("  Error reading draw file:"+file+" at line "+readCount);
	    }
	    draw(input.toArray(new String[0]));
	} else {
	    System.err.println("  Draw error: Reached recusion limit on:"+file);
	}
	recursion -= 1;
    }
    
    void drawCommands() {
	String[] cmds = Settings.getArray("draw");
	draw(cmds);
    }
    
    public void draw(String[] cmds) {
	
	String  cmd;
	for (int i=0; i<cmds.length; i += 1) {
	    
	    cmd = cmds[i].trim();
	    if (cmd.length() == 0 || cmd.charAt(0) == '#') {
		cont = false;
		continue;
	    }
	    
	    String[] flds = cmd.split("\\s+", 2);
	    String key = flds[0].toLowerCase();
	
	    // Only command with a single field.
	    if (key.equals("reset")) {
		reset();
		cont = false;
		continue;
	    }
	    
	    if (flds.length == 1) {
		System.err.println("  Empty command in draw ("+i+") :"+cmd);
		continue;
	    }
	    
	    String val = flds[1];
	    
	    if (key.equals("scale")) {
	        scale(val);
		cont = false;
		
	    } else if (key.equals("project")) {
		project(val);
		cont = false;	    
	    
	    } else if (key.equals("file")) {
		drawFile(val);
		cont = false;
		
	    } else if (key.equals("offset")) {
		offset(val);
		cont = false;
		
	    } else if (key.equals("color")) {
		color(val);
	        // Don't change cont
		     
	    } else if (key.equals("rotate")) {
		rotate(val);
		cont = false;
	     
	    } else if (key.equals("thick")) {
		thick(val);
	        // Don't change cont
	
	    } else if (key.equals("font")) {
		font(val);
		cont = false;
		
		
	    } else if (key.equals("text")) {
		text(val);
		cont = false;
		
	    } else if (key.equals("circle")) {
		circle(val);
	    
	    } else {
		line(key, val);
	    }
	}
    }
    
    void font(String val) {
	
	String[] fields = val.split("\\s+");
	int pnt = pointSize;
	try {
	    pnt = Integer.parseInt(fields[0]);
	    String font = "SansSerif";
	    if (fields.length > 1) {
		font = fields[1];
	    }
	    
	    proc.getImageProcessor().setFont(new Font(font, Font.PLAIN, pointSize));
	} catch (Exception e) {
	    System.err.println("  Draw: Unable to set font:"+val);
	}
	pointSize = pnt;
    }
    
    void project(String val) {	
	
	try {
	    String[] fields = val.split("\\s+");
	    String proj = fields[0];
	    String csys = null;
	    
	    if (fields.length > 1) {
		csys = fields[1];
	    }
	    
	    Projection pj = null;
	    double sign = 1;
	    
	    // If a user wishes to enter coordinates, then
	    // treat this as a Cartesian projection, but
	    // note that the user will not have flipped
	    // the X-direction.
	    if (proj.equalsIgnoreCase("Coo")) {
		proj = "Car";
		sign = -1;
	    }
		
	    if (Projection.fixedPoint(proj) == null) {
		double[] c = new double[]{x0,y0};
		double[] p = new double[3];
		Converter cvx = wcs.inverse();
		cvx.transform(c, p);
		pj = new Projection(val, skyview.geometry.Util.coord(p));
	    } else {
		pj = new Projection(proj);
	    }
	    cnv = new Converter();
	    
	    // Scale positions to radians.
	    cnv.add(new Scaler(0,0, -sign*angScale, 0, 0, angScale));
	    
	    // Invert the projection.  Since we need to
	    // ask for inverses we need to explicitly confirm
	    // that the distorter and rotater are not null.
	    // 
	    // There's always a projecter so we don't need to check
	    // that this is null (projecters and deprojecters change
	    // the dimensions of the point so they can't be null).
	    if (pj.getDistorter() != null) {
		cnv.add(pj.getDistorter().inverse());
	    }
	    cnv.add(pj.getProjecter().inverse());
	    if (pj.getRotater() != null) {
		cnv.add(pj.getRotater().inverse());
	    }
	    
	    if (csys != null) {
		CoordinateSystem cs = CoordinateSystem.factory(csys);
		if (cs == null) {
		    System.err.println("  Drawer: Invalid coordinate system ignored:"+csys);
		    csys = null;
		}
		if (cs.getSphereDistorter() != null) {
		    cnv.add(cs.getSphereDistorter().inverse());
		}
		if (cs.getRotater() != null) {
		    cnv.add(cs.getRotater().inverse());
		}
	    }
	    
	    if (csys != null) {
		// Template is in a specific coordinate system.
		cnv.add(wcs.getCoordinateSystem().getRotater());
		cnv.add(wcs.getCoordinateSystem().getSphereDistorter());
	    }
	    
	    // So far cnv converts from the plot plane back to the current coordinate system.
	    // We don't care about the coordinate system in the WCS -- the templates are coordinates
	    // system independent, but we do need to
	    // deal with the projection and scaler to get back to pixel coordinates.
	    // 
	    Projection wp = wcs.getProjection();
	    cnv.add(wp.getRotater());
	    cnv.add(wp.getProjecter());
	    cnv.add(wcs.getScaler());
	    
	} catch (Exception e) {
	    System.err.println("  Unable to set drawing projection:"+val+"\nError is:"+e);
	}
    }
	
	
    void thick(String val) {
	try {
	    proc.getImageProcessor().setLineWidth(Integer.parseInt(val));
	} catch (Exception e) {
	    System.err.println("  Draw error setting thickness:"+val);
	}
    }
	
    
    void color(String input) {
	colorSet = true;
	proc.setColor(input);
    }
    
    /** Scale can be specified as pixels or in degrees, minutes or seconds.
     *  When specified in angular measurements the WCS scale of the output
     *  image is used.
     */
    void scale(String val) {
	
	char last = val.charAt(val.length()-1);
	double factor = 1;
	
	if (last == '"') {
	    factor = Math.toRadians(1/3600.);
	} else if (last == '\'') {
	    factor = Math.toRadians(1/60.);
	} else if (last == 'd' || last == 'D') {
	    factor = Math.toRadians(1);
	}
	if (factor != 1) {
	    val = val.substring(0,val.length()-1);
	    factor = factor/angScale;
	}
	try {
	    scale = factor*Double.parseDouble(val);
	} catch (Exception e) {
	    System.err.println("  Draw error: Invalid scale:"+val);
	}
    }
    
    void circle(String val) {
	boolean OK = false;
	try {
	    String[] fields = val.split("\\s+");
	    
	    if (fields.length == 3) {
		double[] pnt = getPoint(Double.parseDouble(fields[0]),
	                                Double.parseDouble(fields[1]));
		double r = Double.parseDouble(fields[2]);
	        r = r*scale;
	    
		int diam = (int) (r+0.5);
		if (!Double.isNaN(pnt[0])) { 
		    proc.getImageProcessor().drawOval((int)(x0+pnt[0]-diam/2+0.5), (int)(y0-(pnt[1]+diam/2+0.5)), diam, diam);
		}
		OK = true;
	    }
	} catch (Exception e) {
	}
	if (!OK) {
	    System.err.println("  Draw error in circle:"+val);
	}
    }
    
    double[]  getPoint(double xIn, double yIn) {
        double tx = (xIn+offX)*scale;
	double ty = (yIn+offY)*scale;
	
        double xOut = a00*tx + a01*ty;
	double yOut = a10*tx + a11*ty;
	
	double[] pnt = new double[] {xOut, yOut};
	
	// If we put in a projection then we do an explicit
	// transformation from the plot plane to the sphere back
	// to the image plane.
	if (cnv != null) {
	    cnv.transform(pnt, pnt);
	    pnt[0] -= x0;
	    pnt[1] -= y0;
	}
	
	return pnt;
    }
	
    void line(String p1, String p2) {
	try {
	    
	    double[] pnt = getPoint(Double.parseDouble(p1), Double.parseDouble(p2));
						
	    if (cont && !Double.isNaN(pnt[0]) && !Double.isNaN(lastx)) {
	        proc.drawLine( (lastx  + x0),    (y0-lasty),
			       (pnt[0] + x0),   (y0-pnt[1]));
	    }
	    
	    cont  = true;
	    lastx = pnt[0];
	    lasty = pnt[1];
	    
        } catch (Exception e) {
	    System.err.println("  Draw error on segment:"+p1+" "+p2);
	    cont = false;
        }
    }
    
    void text(String val) {
	try {
	    String[] fields = val.split("\\s+", 4);
	    if (fields.length != 4) {
		return;
	    }
	    double[] pnt  = getPoint(Double.parseDouble(fields[0]),
	                             Double.parseDouble(fields[1]));
	    double ang = Double.parseDouble(fields[2]);
	    
	    if (!Double.isNaN(pnt[0])) {
	        proc.plotString(fields[3], x0+pnt[0], y0+pnt[1], Math.toRadians(-(Math.toDegrees(currentAngle)+ang)));
	    }
	    
	} catch (Exception e) {
	    System.err.println("  Draw error on text:"+val);
	}
    }
}
