package skyview.process.expfinder;

import skyview.process.ExposureFinder;
import skyview.survey.Image;
import skyview.geometry.Sampler;

public class Null implements ExposureFinder {
    
    public void setImage(Image input, Image output, Sampler samp) {
    }
    
    public double getExposure(int pixel) {
	return 1;
    }
}
