package skyview.data;

import skyview.executive.Key;
import skyview.process.Processor;
import skyview.executive.Settings;
import skyview.executive.SettingsUpdater;
import skyview.survey.Image;
import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;

import nom.tam.fits.Header;

/** Do a box car smoothing of an image */
public class BoxSmoother implements Processor {

    /** Width of box */
    private int nx = 1;
    
    /* Height of box */
    private int ny = 1;
    
    /** Width of image */
    private int width;
    /** Height of image */
    private int height;
    
    /** Depth of image */
    private int depth;

    /** Image data */
    private double[] data;
    
    /** Working data */
    private double[] xdata;
    
    public String getName() {
	return "Smoother("+nx+","+ny+")";
    }
    
    public String getDescription() {
	return "Box car smoother";
    }
    
    /** Use as a postprocessor */
    public void process(Image[] inputs, Image output,
			int[] selector, Sampler samp, DepthSampler dsamp) {
	
	String[] smoothPar = Settings.getArray(Key.smooth);
	try {
	    if (smoothPar.length == 1 && smoothPar[0].length() > 0) {
	        nx = Integer.parseInt(smoothPar[0]);
		ny = nx;
	    } else if (smoothPar.length > 1) {
		nx = Integer.parseInt(smoothPar[0].trim());
		ny = Integer.parseInt(smoothPar[1].trim());
	    } else {
	        nx = 3;
	        ny = 3;
	    }
	} catch (Exception e) {
	    System.err.println("Error parsing smooth parameters:"+Settings.get(Key.smooth));
	    return;
	}
	
        data   = output.getDataArray();
	width  = output.getWidth();
	height = output.getHeight();
	depth  = output.getDepth();
	if (depth <= 0) {
	    depth = 1;
	}
	smooth();
    }
    
    /** Smooth an image directly */
    public static void smooth(Image img, int boxWidth, int boxHeight) {
	smooth(img.getDataArray(), img.getWidth(), img.getHeight(), 
	       img.getDepth(), boxWidth, boxHeight
	       );
    }
    
    public static void smooth(double[] data, int imageWidth, int imageHeight,
			      int imageDepth, int boxWidth, int boxHeight) {
	
  
	BoxSmoother bs = new BoxSmoother();
	bs.data   = data;
	bs.width  = imageWidth;
	bs.height = imageHeight;
	bs.depth  = imageDepth;
	bs.nx     = boxWidth;
	bs.ny     = boxHeight;
	bs.smooth();
    }
	
    /** Smooth the current image according to the prescribed size of the box.
     *  When going over the edges of the box we re-use the edge pixels.
     */
    public void smooth() {
	
	if (nx <= 1 && ny <= 1) {
	    return;
	}
  	xdata = data.clone();
	
        int dx = nx/2;
	int dy = ny/2;
	
	int     xlim = width-1;
	int     ylim = height-1;
	boolean dtest = depth > 1;
        int     block = width*height;
	
	// Loops over image pixels.
	for (int y=0; y<height; y += 1) {
	    for (int x=0; x<width; x += 1) {
		// Loops over box pixels
	        for (int iy=-dy; iy<=dy; iy += 1) {
	            for (int ix=-dx; ix<=dx; ix += 1) {
			if (ix == 0 && iy == 0) {
			    continue;
			}
			
			int tx = x+ix;
			int ty = y+iy;
			
			if (tx < 0) {
			    tx = 0;
			} else if (tx >= xlim) {
			    tx = xlim;
			}
			if (ty < 0) {
			    ty = 0;
			} else if (ty > ylim) {
			    ty = ylim;
			}
			if (dtest) {
			    for (int z=0; z<depth; z += 1) {
				xdata[z*block+y*width+x] +=
				  data[z*block+ty*width+tx];
			    }
			} else {
			    xdata[y*width+x] += data[ty*width+tx];
			}
//			System.err.println("BS: to:"+x+","+y+"  from:"+tx+","+ty+" values "+data[ty*width+tx]+" to "+xdata[y*width+x]);
		    }
		}
	    }
	}
	
	// Normalize back to the original scaling.
	double div = (2*dx+1)*(2*dy+1);	
	for (int i=0; i<xdata.length; i += 1) {
	    xdata[i] /= div;
	}
	
	// Copy smoothed image back
	System.arraycopy(xdata, 0, data, 0, data.length);
    }
    
    /** Add information about the smoothing to the FITS header */
    public void updateHeader(Header h) {
	try {
	    h.insertHistory("");
	    h.insertHistory("Smoothed with BoxSmoother:"+this.getClass().getName());
	    h.insertHistory("    Box:"+nx+","+ny);
	    h.insertHistory("");
	} catch (Exception e) {}
    }
}
