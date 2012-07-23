package skyview.data;

import java.util.logging.Level;
import java.util.logging.Logger;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import skyview.executive.Settings;
import skyview.geometry.Converter;
import skyview.geometry.Projecter;
import skyview.geometry.Scaler;
import skyview.geometry.TransformationException;
import skyview.geometry.Transformer;
import skyview.geometry.WCS;
import skyview.survey.Image;

/**
 *
 * @author tmcglynn
 */
public class NormedGaussSmoother extends WeightedSmoother {

    private double sigma;
    private Scaler sc;
    private Projecter proj;
    private int width;
    private int height;
    private int depth;
    private int block = width*height;

    void initialize(Image im) {
        try {
            sigma = Double.parseDouble(Settings.get("sigma", "2"));
            if (sigma <= 0) {
                sigma = 2;
            }

            sc = im.getWCS().getScaler().inverse();
            proj = im.getWCS().getProjection().getProjecter();
            width = im.getWidth();
            height = im.getHeight();
            depth  = im.getDepth();

        } catch (TransformationException ex) {
            System.err.println("Error getting projection information.");
        }
    }

    public void updateHeader(Header header) {
        try {
            header.insertComment(" ");
            header.insertComment(" Normed Gaussian Smoothing with sigma="+sigma);
            header.insertComment("  ");
        } catch (FitsException e) {
            System.err.println("FITS exception updating header");
        }
    }

    public String getName() {
        return "Normed Gaussian Smoother";
    }

    public String getDescription() {
        return "Symmtric gaussian smoothing with constant smoothing";
    }

    private double[] dpix;
    void updateWeights(int pix) {
        pix = pix % block;
        dpix[0]  = pix % width + 0.5;
        dpix[1]  = pix / width + 0.5;
        double[] pos    = sc.transform(dpix);
        double[] tissot = proj.tissot(pos[0], pos[1]);
        throw new IllegalArgumentException("Not available");
    }

}
