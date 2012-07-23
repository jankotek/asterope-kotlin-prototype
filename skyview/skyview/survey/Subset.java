package skyview.survey;


import skyview.geometry.WCS;
import skyview.geometry.Scaler;
import skyview.geometry.Transformer;
import skyview.geometry.TransformationException;

import static org.apache.commons.math3.util.FastMath.*;

/** A image that is a subset of an existing image.  Generally
 *  this class is expected to be used when we break down a larger
 *  image into subimages for sampling purposes.  Any action that
 *  modifies the image should use the setData or setDataArray methods.
 *  Modifying the array returned by getDataArray will not modify the
 *  underlying image.  E.g., the BoxSmoother should not be run on
 *  the subset images but on the parent.
 */
public class Subset extends Image {
    
    /** The dimensions of the parent image. */
    private int         px,py,pz;
    
    /** The offset of the image within the parent */
    private int         x, y, z;
    
    /** The size of a plane is nx*ny */
    private int         plane;
    
    /** The size of the subset is nx*ny*nz */
    private int         total;
    
    /** The parent image. */
    private Image       parent;
    
    /** The width of the subset */
    private int width;
    
    /** Create a two-d subset of a (presumed) 2-d parent. */
    public Subset(Image parent, int x, int y, int nx, int ny) 
      throws TransformationException {
	this(parent, x, y, 0, nx, ny, 1);
    }
    
    /** Create a subset of a parent image. */
    public Subset(Image parent, int x, int y, int z, int nx, int ny, int nz) throws TransformationException {
    
	WCS wcs = parent.getWCS().addScaler(new Scaler(-x,-y, 1,0,0,1));
	setName(parent.getName() + " ("+x+","+y+","+z+")");
	initialize(parent.getDataArray(), wcs, nx, ny, nz);
	    
	this.width = nx;
	
	this.parent = parent;
	
	this.x = x;
	this.y = y;
	this.z = z;
	
	this.px = parent.getWidth();
	this.py = parent.getHeight();
	this.pz = parent.getDepth();
	  	
	this.plane = getWidth()*getHeight();
	this.total = plane*getDepth();
	
    }
    
    /** Get a description of the object */
    public String getDescription() {
	return "A subset of an existing image.";
    }
    
    private int getParentNpix(int npix) {
	
	if (npix < 0 || npix > total) {
	    return -1;
	}
	
	int tz = npix/plane;
	int ty = (npix%plane)/width;
	int tx = npix%width;
	
	return (tx + x) + (ty + y)*px + (tz + z)*(px*py);
    }
    
    /** Get a pixels data associated with the image. */
    public double  getData(int npix) {
	npix = getParentNpix(npix);
	if (npix < 0) {
	    return Double.NaN;
	} else {
	    return data[npix];
	}
    }
    
    /** Get the data as an array. Note that
     *  for an image this array can be used to modify the
     *  image, but that will not be true for the array returned here.
     */
    public double[] getDataArray() {
	
	double[] ndata = new double[total];
	
	int offset = 0;
	int width = getWidth();
	int height = getHeight();
	int depth  = getDepth();
	for (int tz=0; tz<depth; tz += 1) {
	    for (int ty=0; ty<height; ty += 1) {
		// Find the begining pixel for this row
		// in the parent array.  For that pixel the
		// X index in the subset is 0.
		int ppix = getParentNpix(ty*width + tz*plane);
		System.arraycopy(data, ppix, ndata, offset, width);
		offset += width;
	    }
	}
	
	return ndata;
    }
    
    /** Set the data associated with the image.
     */
    public void setData(int npix, double newData) {
	npix = getParentNpix(npix);
	if (npix >= 0) {  // Defer to the parent who knows if
	                  // we are accumulating.
	    parent.setData(npix, newData);
	}
    }
    
    /** Clear the data array */
    public void clearData() {
	
	int width = getWidth();
	int height = getHeight();
	int depth  = getDepth();
	double[] zeros = new double[width];
	
	for (int tz=0; tz<depth; tz += 1) {
	    for (int ty=0; ty<height; ty += 1) {
		// Find the begining pixel for this row
		// in the parent array.  For that pixel the
		// X index in the subset is 0.
		int ppix = getParentNpix(ty*width + tz*plane);
		System.arraycopy(zeros, 0, data, ppix, width);
	    }
	}
    }
    
    /** Set the data array */
    public void setDataArray(double[] newData) {
	
	if (newData.length != total) {
	    throw new IllegalArgumentException("Attempt to set subset data with wrong length array. Got:"+newData.length+" when expecting:"+total);
	}
	
	int offset = 0;
	int width  = getWidth();
	int height = getHeight();
	int depth  = getDepth();
	
	for (int tz=0; tz<depth; tz += 1) {
	    for (int ty=0; ty<height; ty += 1) {
		
		// Find the begining pixel for this row
		// in the parent array.  For that pixel the
		// X index in the subset is 0.
		int ppix = getParentNpix(ty*width + tz*plane);
		System.arraycopy(newData, offset, data, ppix, width);
		offset += width;
	    }
	}
    }
    
    /** Split an image into subsets.
     *  If the image is too small, just return it/
     */
    public static Image[] split(Image parent, int nx, int ny) throws TransformationException {
	
	int width  = parent.getWidth();
	int height = parent.getHeight();
	int depth  = parent.getDepth();
	
	int sx = (width-1)/nx + 1;
	int sy = (height-1)/ny + 1;
	if (sx > 1 || sy > 1) {
	    Subset[] subs = new Subset[sx*sy];
	    int cnt = 0;
	    for (int i=0; i<sx; i += 1) {
		int x = i*nx;
		int wx = min(nx, width-x);
		
		for (int j=0; j<sy; j += 1) {
		    int y = j*ny;
		    int wy = min(ny, height-y);
		    
		    subs[cnt] = new Subset(parent, x,y,0, wx,wy,depth);
		    cnt += 1;
		}
	    }
	    return subs;
	} else {
	    return new Image[]{parent};
	}
    }
    /** Split an index array into subarrays for each subset.
     *  If the image is too small, just return it/
     */
    public static int[][] split(Image parent, int[] index, int nx, int ny) {
	
	int width  = parent.getWidth();
	int height = parent.getHeight();
	int depth  = parent.getDepth();
	
	int sx = (width-1)/nx + 1;
	int sy = (height-1)/ny + 1;
	int[][] indices;
	if (sx > 1 || sy > 1) {
	    indices = new int[sx*sy][];
	    int cnt = 0;
	    for (int i=0; i<sx; i += 1) {
		int x = i*nx;
		int wx = min(nx, width-x);
		
		for (int j=0; j<sy; j += 1) {
		    int y = j*ny;
		    int wy = min(ny, height-y);
		    int[] sub = new int[wx*wy];
		    int offset = 0;
		    for (int ay=y; ay<y+wy; ay += 1) {
			System.arraycopy(index, x+ay*width, sub, offset, wx);
			offset += wx;
		    }
		    indices[cnt] = sub;
		    cnt += 1;
		}
	    }
	} else {
	    indices = new int[1][];
	    indices[0]  = index;
	}
	return indices;
    }
}
	
