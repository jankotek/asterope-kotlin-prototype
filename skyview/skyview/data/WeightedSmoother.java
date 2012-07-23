package skyview.data;

import skyview.process.Processor;
import skyview.executive.Settings;
import skyview.executive.SettingsUpdater;
import skyview.survey.Image;
import skyview.geometry.Sampler;
import skyview.geometry.DepthSampler;

import nom.tam.fits.Header;

/** Smooth an image using a rectangular array of weights */
public abstract class WeightedSmoother implements Processor {

    /** Width of box */
    private int nx = 1;
    
    /* Height of box */
    private int ny = 1;
    
    /** Width of image */

    private int width;
    /** Height of image */
    private int height;

    private int depth;

    /** Image data */
    private double[] data;
    
    /** Current weigths */
    private double[] weights;

    protected void setSmoothSize(int nx, int ny) {
        this.nx = nx;
        this.ny = ny;
    }

    abstract void initialize(Image output);

    abstract void updateWeights(int pix);

    double[] getWeights() {
        return weights;
    }

    void setWeights(double[] newWeights) {
        weights = newWeights;
    }
    
    /** Use as a postprocessor */
    public void process(Image[] inputs, Image output,
			int[] selector, Sampler samp, DepthSampler dsamp) {

        initialize(output);
        data   = output.getDataArray();
	width  = output.getWidth();
	height = output.getHeight();
	depth  = output.getDepth();
	if (depth <= 0) {
	    depth = 1;
	}
	smooth();
    }
    
    /** Smooth the current image according to the prescribed size of the box.
     *  When going over the edges of the box we re-use the edge pixels.
     */
    public void smooth() {

        // Keep track of the coordinate systems:
        //    x,y,z  -- pixel indices in the image we are smoothing
        //    width,height,depth -- dimensions of the image we are smoothing
        //    pix    -- pointer to the image pixel at x,y,z
        //    nx,ny  -- size of the smoothing kernel (should be odd
        //              but we do not need nx==ny)
        //    px,py  -- pixel indices within the smoothing kernel, but these
        //              are indexed relative to the center of the kernel
        //              so they may have negative values
        //    tx,ty  -- nx/2,ny/2 (truncated) so  -tx <= px <= tx, ...

        int block = height*width;

        // Loop over depth
        // This could cost more if we have an expensive updateWeights
        // and we have a 3-d image, but simplifies things
        // otherwise.  It also uses up less temporary space.

        for (int z = 0; z<depth; z += 1) {
            double[] xdata = new double[width*height];
            int pix = block*z;

            // Loops over image pixels.
            for (int y = 0; y < height; y += 1) {
                for (int x = 0; x < width; x += 1) {

                    updateWeights(pix);
                    
                    // It is assumed that nx and ny are odd.
                    int ty = ny/2;
                    int tx = nx/2;

                    // Loop over the weighted filter
                    // Note that the size of the filter is not
                    // guaranteed to be constant from input pixel to input pixel.
                    //   Note that px,py range from -sz to +sz except
                    //   when we are at the edge of the input image.
                    //   Since we nx/ny are odd the filter is symmetric
                    //   around the current pixel.
                    for (int py = Math.max(-ty, -y); py <= Math.min(ty, height-y-1); py += 1) {
                        for (int px = Math.max(-tx, -x); px <= Math.min(tx, width-x-1); px += 1) {
                            xdata[(y+py) * width + x+px] +=
                               weights[(py+ty) * nx + (px+tx)] * data[pix];
                        }
                    }
                    pix += 1;
                }
            }
    	    // Copy smoothed image back (for the current z value)
	    System.arraycopy(xdata, 0, data, z*block, block);
        }
    }
}
