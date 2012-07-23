package skyview.data;

import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import skyview.executive.Settings;
import skyview.survey.Image;
import org.apache.commons.math3.special.Erf;

/**
 *
 * @author tmcglynn
 */
public class GaussSmoother extends WeightedSmoother {

    private double sigma;

    void initialize(Image output) {
        double sigma = Double.parseDouble(Settings.get("sigma", "2"));
        if (sigma <= 0) {
            sigma = 2;
        }

        int sz = (int) Math.ceil(sigma);
        // Go out 3 sigma to both sides and then add in a center pixel.
        int dim = 2*(3*sz) + 1;
  
        double[] weights = new double[dim*dim];
        double sum = 0;
        int pos = 0;
        int pix = 0;
        double div = 1./Math.sqrt(2)/sigma;

        sz = dim/2;
  
        double py = -(sz+0.5)*div;
        for (double iy= -sz; iy <= sz; iy += 1) {
            double px = -(sz+0.5)*div;
            double line =  Erf.erf(py,py+div);

            for (double ix = -sz; ix <= sz; ix += 1) {
                // Erf goes from -1 to 1, so we need to normalize.
                double val = 0.25*line*Erf.erf(px,px+div);
                weights[pix] = val;
                sum         += val;
                pix         += 1;
                px          += div;
            }
            py += div;
        }
        if (sum > 1.000000001) {
            System.err.println("Warning: Gaussian weights exceed unity");
        }
        if (sum < 0.99) {
            System.err.println("Warning: Gaussian weights significantly less than 1");
        }

        if (sum != 0) {
            for (int i=0; i<dim*dim; i += 1) {
                weights[i] /= sum;
            }
        }

        setSmoothSize(dim, dim);
        setWeights(weights);

//        pix = 0;
//        System.err.println("The unnormalized sum of all weights was:"+sum);
//        for (int iy=0; iy<dim; iy += 1) {
//            for (int ix=0; ix<dim; ix += 1) {
//                System.err.printf(" %12.8f", weights[pix]);
//                pix += 1;
//            }
//            System.out.println();
//        }
    }
    
    public void updateHeader(Header header) {
        try {
            header.insertComment(" ");
            header.insertComment(" Gaussian Smoothing with sigma="+sigma);
            header.insertComment("  ");
        } catch (FitsException e) {
            System.err.println("FITS exception updating header");
        }
    }

    public String getName() {
        return "Basic Gaussian Smoother";
    }

    public String getDescription() {
        return "Symmtric gaussian smoothing with constant smoothing";
    }

    void updateWeights(int pix) {
        return;
    }

}
