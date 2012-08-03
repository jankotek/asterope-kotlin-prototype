package skyview.survey;


import skyview.executive.Key;
import skyview.executive.Settings;
import static org.apache.commons.math3.util.FastMath.*;

/** This class generates DSS2 image spells. */
public class DSS2ImageGenerator implements ImageGenerator {
    
    public void getImages(double ra, double dec, double size, java.util.ArrayList<String> list) {
	
	size       += Double.parseDouble(Settings.get(Key.ImageSize));
	
	// This string is used to distinguish the various DSS2 surveys.
	String dssPrefix = Settings.get(Key.DSS2Prefix);
	if (dssPrefix == null) {
	    dssPrefix = "";
	}
	
	// Note that the last parameter is key since all of this depends
	// upon consistent indexing.
	double[][] data = surveyCenters(ra, dec, size, 0.25);
	int n = data[0].length;
	
	for (int i=0; i<data[0].length; i += 1) {
	    int sz = 18;
	    double delta = abs(abs(dec)-90);
	    if (delta < 8) {
		sz = 28;
	    }
	    // This is the part of the spell that varies with the image.
	    // The rest is supplied in the spell prefix and suffix.
	    String spell = "h="+sz+"&w="+sz+
	                   "&ra="+data[0][i] + "&dec=" +  data[1][i] +                 // Part of URL
	                   ","+dssPrefix+"img"+(int)(data[2][i])+".fits.gz,"      +   // File name
	                   data[0][i]+","+data[1][i];                                 // RA and Dec
	    list.add(spell);
	}
    }
  
    /** Inherited from IDL and traces of IDL's array orientation remain */
    private double[][] surveyCenters(double inRA, double inDec, double inSize, double delta) {
	

        int    ndec   = (int) (180./delta+.5) + 1;

        double ddec   = 180./(ndec-1);

        delta  = toRadians(delta);
        ddec   = toRadians(ddec);

        double[] decs   = new double[ndec];
	int[]    nra    = new int[ndec];
	
	int count = 0;
	
	for (int i=0; i<ndec; i += 1) {
	    decs[i] = -PI/2 + ddec*i;
	    nra[i]  = (int) (2*PI/delta * sin(PI/2 - decs[i])) + 1;
	    count += nra[i];
	}
	
	inRA   = toRadians(inRA);
	inDec  = toRadians(inDec);
	inSize = toRadians(inSize);
	
	// This starts with the maximum possible number
	double[] ra0   = new double[count];
	double[] dec0  = new double[count];
	double[] ind0  = new double[count];  // This will be integer valued.
	
	int outCount = 0;
	int index = 0;
	
        for (int iDec = 0; iDec < ndec; iDec += 1) {
	    
            int    nr  = nra[iDec];
	    double dRA  = 2*PI/nr;
	    double cDec = decs[iDec];
	    
	    // Skip if Dec is too far off.
	    if (abs(cDec - inDec) < inSize) {
	    
	        for (int jRA = 0; jRA < nr; jRA += 1) {
		    double cRA = jRA*dRA;
		
		    if (skyview.geometry.Util.sphdist(cRA,cDec, inRA,inDec) < inSize) {
		        ra0[outCount]  = toDegrees(cRA);
		        dec0[outCount] = toDegrees(cDec);
		        ind0[outCount] = index + jRA;
		        outCount += 1;
		    }
	        }
	    }
	    index += nr;
	}
	
	double[] ra1  = new double[outCount];
	double[] dec1 = new double[outCount];
	double[] ind1 = new double[outCount];
	
	System.arraycopy(ra0,  0, ra1,  0, outCount);
	System.arraycopy(dec0, 0, dec1, 0, outCount);
	System.arraycopy(ind0, 0, ind1, 0, outCount);
	
	return new double[][] {ra1, dec1, ind1};
    }
}
