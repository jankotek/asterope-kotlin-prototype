package skyview.process;

import skyview.survey.Image;
import skyview.geometry.Sampler;

/** This interface should be implemented by objects that
 *  can calculate the exposure corresponding to a given pixel
 *  in an image.
 */

public interface ExposureFinder {

    /** Specify the input image for which we are
     *  going to get the exposure.
     */
    public void setImage (Image input, Image output, Sampler samp);

    /** Get the exposure for a given pixel.
     *  For 3-d images the exposure is assumed to be
     *  constant in the energy dimension.  This is probably not
     *  true, but we don't anticipate adding 3-d images much.
     */
    public double getExposure(int pixel);
}
