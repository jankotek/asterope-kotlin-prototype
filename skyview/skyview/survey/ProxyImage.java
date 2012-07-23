package skyview.survey;

/** This class defines a proxy image.  The proxy has an
 *  approximate WCS which may be used to determine where the
 *  image is to used for sampling, but it must be replaced by
 *  the real image before the sampling is done.
 *  of a set of pixel values and a WCS describing the
 *  pixel coordinates.
 */

import skyview.geometry.WCS;
import skyview.geometry.Transformer;
import skyview.geometry.TransformationException;

public class ProxyImage extends Image {

    /** The image that we are proxying for.
     *  If this is not null, we simple forward requests to this.
     */
    private Image realImage;
    
    /** The approximate image */
    private Image proxyImage;
    
    /** The image we are currently using */
    private Image currentImage;
    
    /** An image factory that can create the real image */
    private ImageFactory fac;
    
    /** The string the image factory needs to create the real image */
    private String spell;
    
    /** Get the name of the image */
    public String getName() {
	return currentImage.getName();
    }
    
    /** set the name of the image */
    protected void setName(String name) {
	currentImage.setName(name);
    }
    
    /** Set the factory that is used to create the real images */
    public void setFactory(ImageFactory imFac) {
	fac = imFac;
    }
    
    /** Construct a WCS */
    public ProxyImage (String spell, WCS wcs, int width, int height, int depth)
      throws TransformationException {
        this.spell = spell;
	proxyImage   = new Image(null, wcs, width, height, depth);
	currentImage = proxyImage;
    }
    
    /** Get the WCS associated with the image. */
    public WCS getWCS() {
	return currentImage.getWCS();
    }
    
    /** Get a pixels data associated with the image. */
    public double  getData(int npix) {
	return currentImage.getData(npix);
    }
    
    /** Get the data as an array */
    public double[] getDataArray() {
	return currentImage.getDataArray();
    }
    
    /** Set the Data associated with the image.
     */
    public void setData(int npix, double newData) {
	currentImage.setData(npix, newData);
    }
    
    /** Clear the data array */
    public void clearData() {
	currentImage.clearData();
    }
    
    /** Set the data array */
    public void setDataArray(double[] newData) {
	currentImage.setDataArray(newData);
    }
	
    /** Get the transformation to the pixel coordinates of the image */
    public Transformer getTransformer() {
	return currentImage.getTransformer();
    }
    
    /** Get the width of the image */
    public int getWidth() {
	return currentImage.getWidth();
    }
    
    /** Get the height of the image */
    public int getHeight() {
	return currentImage.getHeight();
    }
    
    /** Get the number of planes in the image */
    public int getDepth() {
	return currentImage.getDepth();
    }
    
    
    /** Get the center position of the given output pixel */
    public double[] getCenter(int npix) {
	return currentImage.getCenter(npix);
    }
    
    /** Get the corners of the given output pixel */
    public double[][] getCorners(int npix) {
	return currentImage.getCorners(npix);
    }
    
    /** Make sure the image is read for detailed use.
      * Replace the proxy with the real image */
    public void validate() {
	if (realImage == null) {
	    realImage    = fac.factory(spell);
	}
	currentImage = realImage;
    }
    
    /** Is this currently a fully validated image? */
    public boolean valid() {
	return realImage != null;
    }
    
    /** Get the current 'real' image.
     */
    public Image getBaseImage() {
	return currentImage;
    }
}
	
