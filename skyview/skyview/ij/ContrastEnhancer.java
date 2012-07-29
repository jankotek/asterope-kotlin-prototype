package skyview.ij;


/** Implements ImageJ's Process/Enhance Contrast command. */
public class ContrastEnhancer{

	boolean classicEqualization;


	/**	
		Changes the tone curves of images. 
		It should bring up the detail in the flat regions of your image.
		Histogram Equalization can enhance meaningless detail and hide 
		important but small high-contrast features. This method uses a
		similar algorithm, but uses the square root of the histogram 
		values, so its effects are less extreme. Hold the alt key down 
		to use the standard histogram equalization algorithm.
		This code was contributed by Richard Kirk (rak@cre.canon.co.uk).
	*/ 	
	public void equalize(ImageProcessor ip) {
	
		int[] histogram = ip.getHistogram();

    	int max = 255;
		int 	range = 255;

		double sum;
		
		sum = getWeightedValue(histogram, 0);
		for (int i=1; i<max; i++)
			sum += 2 * getWeightedValue(histogram, i);
		sum += getWeightedValue(histogram, max);
		
		double scale = range/sum;
		int[] lut = new int[range+1];
		
		lut[0] = 0;
		sum = getWeightedValue(histogram, 0);
		for (int i=1; i<max; i++) {
			double delta = getWeightedValue(histogram, i);
			sum += delta;
			lut[i] = (int)Math.round(sum*scale);
			sum += delta;
		}
		lut[max] = max;
		
		ip.applyTable(lut);
	}

	private double getWeightedValue(int[] histogram, int i) {
		int h = histogram[i];
		if (h<2 || classicEqualization) return (double)h;
		return Math.sqrt((double)(h));
	}
	
}