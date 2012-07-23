/**
  * Sample using interpolating splines.
  * Based on routines by:
  *	Philippe Thevenaz
  *	Bldg. BM-Ecublens 4.137
  *	CH-1015 Lausanne
  *----------------------------------------------------------------------------
  *	phone (CET):	        +41(21)693.51.61
  *	fax:			+41(21)693.37.01
  *	RFC-822:		philippe.thevenaz@epfl.ch
  *
  * 
  */

package skyview.geometry.sampler;

import skyview.survey.Image;

public class Spline extends skyview.geometry.Sampler {
    
    private double[] zout = new double[2];
    
    private int      xmin = 0;
    private int      ymin = 0;
    
    public String getName() {
	return "Spline "+splineDegree+" sampler";
    }
    
    public String getDescription() {
	return "Sample using splines of the given order";
    }
    
    /** The order of the spline */
    private  int      splineDegree = 3;
    
    /** Set the order */
    public void setOrder(int order) {
	if (order < 2 || order > 5) {
	    throw new Error("Invalid order for spline (should be between 2 and 5)");
	}
	this.splineDegree = order;
    }
      
    /** A copy of the image that is transformed into
     *  the spline coefficients
     */
    private  double[] image;
    
    /** Construct a  sample of a given degree.  Probably
      *  should override the setImage functions and such
      *  since we don't handle 3-D images yet.
     **/
    public void setInput(Image inImage) {
	
	if (inImage.getDepth() > 1) {
	    throw new Error("Spline Sampler cannot handle 3-D images");
	}
	
	super.setInput(inImage);  // This will extract the fields of inImage
	
	if (bounds == null) {
            this.image  = new double[inImage.getWidth()*inImage.getHeight()];
	    for (int i=0; i<image.length; i += 1) {
	        image[i] = inImage.getData(i);
	    }
	 
	} else {
	    getBoundedInput(inImage, bounds);
	}
	 
	samplesToCoefficients();
	 
    }
    
    /** 
     * Build a spline sampler based on only part of the input image.
     */
    private void getBoundedInput(Image inImage, int[] bounds) {
	
	/* We have the limits we're looking for in the output image,
	 * but we need to transform that to limits in the input image.
	 */
	
	double xi0=1.e10,xie=-1.e10, yi0=1.e10,yie=-1.e10;
	
	for (int i=0; i<bounds.length; i += 1) {
	    
	    double[] zin = outImage.getCenter(bounds[i]);
	    trans.transform(zin, zout);
	    if (zout[0] < xi0) {
		xi0 = zout[0];
	    }
	    if (zout[0] > xie) {
		xie = zout[0];
	    }
	    if (zout[1] < yi0) {
		yi0 = zout[1];
	    }
	    if (zout[1] > yie) {
		yie = zout[1];
	    }
	}
	double dx = xie-xi0;
	double dy = yie-yi0;
	
	xi0 -= .05*dx + splineDegree;
	xie += .05*dx + splineDegree;
	yi0 -= .05*dy + splineDegree;
	yie += .05*dy + splineDegree;
	
	int ixi0 = (int) Math.floor(xi0);
	int ixie = (int) Math.ceil(xie);
	int iyi0 = (int) Math.floor(yi0);
	int iyie = (int) Math.ceil(yie);
	
	if (ixi0 < 0) {
	    ixi0 = 0;
	}
	if (iyi0 < 0) {
	    iyi0 = 0;
	}
	if (ixie >= inWidth) {
	    ixie = inWidth-1;
	}
	if (iyie >= inHeight) {
	    iyie = inHeight-1;
	}
	
	int oldWidth = inWidth;
	
	this.inWidth  = ixie-ixi0+1;
	this.inHeight = iyie-iyi0+1;
	this.image  = new double[inWidth*inHeight];
	this.xmin    = ixi0;
	this.ymin    = iyi0;
	
	int outOff = 0;
	
	for (int y=0; y < inHeight; y += 1) {
	    int inOff = (y+ymin)*oldWidth + xmin;
	    for (int x=0; x < inWidth; x += 1) {
	        image[outOff] = inImage.getData(inOff);
		outOff  += 1;
		inOff   += 1;
	    }
	}
    }
	 
       
     /** Transform the image to spline coefficients on the image copy.
      * Note that except for
      * variables visible outside a method, these routines use
      * Thevanez' original variable names which sometimes
      * violated Java conventions.
      */
     private  int samplesToCoefficients () {
    
    	double[]  Line;
    	double[]  Pole = new double[2];
    	int	  NbPoles;
    
    	/* recover the poles from a lookup table */
    	switch (splineDegree) {
	 case 2:
	    			NbPoles = 1;
	    			Pole[0] = Math.sqrt(8.0) - 3.0;
	    			break;
	 case 3:
	    			NbPoles = 1;
	    			Pole[0] = Math.sqrt(3.0) - 2.0;
	    			break;
	 case 4:
	    			NbPoles = 2;
	    			Pole[0] = Math.sqrt(664.0 - Math.sqrt(438976.0)) + Math.sqrt(304.0) - 19.0;
	    			Pole[1] = Math.sqrt(664.0 + Math.sqrt(438976.0)) - Math.sqrt(304.0) - 19.0;
	    			break;
	 case 5:
	    			NbPoles = 2;
	    			Pole[0] = Math.sqrt(135.0 / 2.0 - Math.sqrt(17745.0 / 4.0)) + Math.sqrt(105.0 / 4.0)
	      				- 13.0 / 2.0;
	    			Pole[1] = Math.sqrt(135.0 / 2.0 + Math.sqrt(17745.0 / 4.0)) - Math.sqrt(105.0 / 4.0)
	      				- 13.0 / 2.0;
	    			break;
	 default:
	    			System.err.println("Invalid spline degree\n");
	    			return(1);
	}
    
    	/* convert the image samples into interpolation coefficients */
    	/* in-place separable process, along x */
    	Line = new double[inWidth];
    
    	for (int y = 0; y < inHeight; y++) {
	     getRow(image, y, Line, inWidth);
	     convertToInterpolationCoefficients(Line, inWidth, Pole, NbPoles, 1.e-14);
	     putRow(image, y, Line, inWidth);
	}
    
    	/* in-place separable process, along y */
    	Line = new double[inHeight];
    
    	for (int x = 0; x < inWidth; x++) {
	     getColumn(image, inWidth, x, Line, inHeight);
	     convertToInterpolationCoefficients(Line, inHeight, Pole, NbPoles, 1.e-14);
	     putColumn(image, inWidth, x, Line, inHeight);
	}
    
    	return(0);
    } /* end SamplesToCoefficients */
    

    private void convertToInterpolationCoefficients 
      (
	double	c[],		/* input samples --> output coefficients */
	int	DataLength,	/* number of samples or coefficients */
	double	z[],		/* poles */
	int	NbPoles,	/* number of poles */
	double	Tolerance	/* admissible relative error */
      )
    { /* begin ConvertToInterpolationCoefficients */
    
    	double	Lambda = 1.0;
    
    	/* special case required by mirror boundaries */
    	if (DataLength == 1) {
	    return;
	}
	
    	/* compute the overall gain */
    	for (int k = 0; k < NbPoles; k++) {
	     Lambda = Lambda * (1.0 - z[k]) * (1.0 - 1.0 / z[k]);
	}
	
    	/* apply the gain */
    	for (int n = 0; n < DataLength; n++) {
	     c[n] *= Lambda;
	}
	
    	/* loop over all poles */
    	for (int k = 0; k < NbPoles; k++) {
	     /* causal initialization */
	     c[0] = initialCausalCoefficient(c, DataLength, z[k], Tolerance);
	     /* causal recursion */
	     for (int n = 1; n < DataLength; n++) {
		 c[n] += z[k] * c[n - 1];
	     }
	     /* anticausal initialization */
	     c[DataLength - 1] = initialAntiCausalCoefficient(c, DataLength, z[k]);
	     /* anticausal recursion */
	     for (int n = DataLength - 2; 0 <= n; n--) {
		 c[n] = z[k] * (c[n + 1] - c[n]);
	     }
	}
    } /* end ConvertToInterpolationCoefficients */

    /*--------------------------------------------------------------------------*/
    private  double initialCausalCoefficient
  				(
				 					double	c[],		/* coefficients */
				 					int	DataLength,	/* number of coefficients */
				 					double	z,			/* actual pole */
				 					double	Tolerance	/* admissible relative error */
				 				)
  
    { /* begin InitialCausalCoefficient */
    
    	double	Sum, zn, z2n, iz;
    
    	/* this initialization corresponds to mirror boundaries */
    	int Horizon = DataLength;
	
    	if (Tolerance > 0.0) {
	    Horizon = (int)Math.ceil(Math.log(Tolerance) / Math.log(Math.abs(z)));
	}
    	if (Horizon < DataLength) {
	    		/* accelerated loop */
	    zn = z;
	    Sum = c[0];
	    for (int n = 1; n < Horizon; n++) {
		Sum += zn * c[n];
		zn *= z;
	    }
	    return(Sum);
	}
    	else {
	    /* full loop */
	    zn = z;
	    iz = 1.0 / z;
	    z2n = Math.pow(z, (double)(DataLength - 1));
	    Sum = c[0] + z2n * c[DataLength - 1];
	    z2n *= z2n * iz;
	    for (int n = 1; n <= DataLength - 2; n++) {
		 Sum += (zn + z2n) * c[n];
		 zn *= z;
		 z2n *= iz;
	    }
	    return(Sum / (1.0 - zn * zn));
	}
    } /* end InitialCausalCoefficient */



    /*--------------------------------------------------------------------------*/
    private void getColumn
  				(
				 					double[] Image,		/* input image array */
				 					int	 Width,		/* width of the image */
				 					int	 x,		/* x coordinate of the selected line */
				 					double[] Line,		/* output linear array */
				 					int	 Height		/* length of the line */
				 				)
  
    { /* begin GetColumn */
    
    	for (int y = 0; y < Height; y++) {
	     Line[y] = Image[y*Width+x];
	}
    } /* end GetColumn */

    /*--------------------------------------------------------------------------*/
    private void getRow
  				(
				 					double[]  Image,		/* input image array */
				 				        int	  y,			/* y coordinate of the selected line */
				 					double[]  Line,	      	 	/* output linear array */
				 					int	  Width		        /* length of the line */
				 				)
  
    { /* begin GetRow */
    
	
	System.arraycopy(Image, y*Width, Line, 0, Width);
    } /* end GetRow */

    /*--------------------------------------------------------------------------*/
    private double initialAntiCausalCoefficient
  				(
				 					double	c[],		/* coefficients */
				 					int	DataLength,	/* number of samples or coefficients */
				 					double	z			/* actual pole */
				 				)
  
    { /* begin InitialAntiCausalCoefficient */
    
    	/* this initialization corresponds to mirror boundaries */
    	return((z / (z * z - 1.0)) * (z * c[DataLength - 2] + c[DataLength - 1]));
    } /* end InitialAntiCausalCoefficient */


    /*--------------------------------------------------------------------------*/
    private void putColumn
  				(
				 				        double[] Image,		/* output image array */
				 					int	 Width,		/* width of the image */
				 					int	 x,			/* x coordinate of the selected line */
				 					double	 Line[],		/* input linear array */
				 					int	 Height		/* length of the line and height of the image */
				 				)
  
    { /* begin PutColumn */
    
    
    	for (int y = 0; y < Height; y++) {
	    Image[y*Width+x] = Line[y];
	}
    } /* end PutColumn */

     /*--------------------------------------------------------------------------*/
     private void putRow
  				(
				 				        double[] Image,		/* output image array */
				 					int      y,			/* y coordinate of the selected line */
				 					double	 Line[],		/* input linear array */
				 					int	 Width		/* length of the line and width of the image */
				 				)
  
    { /* begin PutRow */
	
	System.arraycopy(Line, 0, Image, y*Width, Width);
    
    } /* end PutRow */



    private double[]	 xWeight= new double[6];
    private double[]     yWeight=new double[6];
    private int[]	 xIndex= new int[6];
    private int[]        yIndex=new int[6];

    public double interpolatedValue(
  				 					double	x,			/* x coordinate where to interpolate */
				 					double	y			/* y coordinate where to interpolate */
				 				)
  
    { /* begin InterpolatedValue */
    
    	int	 Width2 = 2 * inWidth - 2, Height2 = 2 * inHeight - 2;
    	double	 interpolated;
    	double	 w, w2, w4, t, t0, t1;
	int i,j;
    
    	/* compute the interpolation indexes */
    	if (splineDegree % 2  != 0) {
	    i = (int)Math.floor(x) - splineDegree / 2;
	    j = (int)Math.floor(y) - splineDegree / 2;
	    
	    for (int k = 0; k <= splineDegree; k++) {
		 xIndex[k] = i++;
		 yIndex[k] = j++;
	    }
	}
    	else {
	    i = (int)Math.floor(x + 0.5) - splineDegree / 2;
	    j = (int)Math.floor(y + 0.5) - splineDegree / 2;
	    for (int k = 0; k <= splineDegree; k++) {
		xIndex[k] = i++;
		yIndex[k] = j++;
	    }
	}
    
    	/* compute the interpolation weights */
    	switch (splineDegree) {
	 case 2:
	      /* x */
	      w = x - (double)xIndex[1];
	      xWeight[1] = 3.0 / 4.0 - w * w;
	      xWeight[2] = (1.0 / 2.0) * (w - xWeight[1] + 1.0);
	      xWeight[0] = 1.0 - xWeight[1] - xWeight[2];
	      /* y */
	      w = y - (double)yIndex[1];
	      yWeight[1] = 3.0 / 4.0 - w * w;
	      yWeight[2] = (1.0 / 2.0) * (w - yWeight[1] + 1.0);
	      yWeight[0] = 1.0 - yWeight[1] - yWeight[2];
	      break;
	 case 3:
	      /* x */
	      w = x - (double)xIndex[1];
	      xWeight[3] = (1.0 / 6.0) * w * w * w;
	      xWeight[0] = (1.0 / 6.0) + (1.0 / 2.0) * w * (w - 1.0) - xWeight[3];
	      xWeight[2] = w + xWeight[0] - 2.0 * xWeight[3];
	      xWeight[1] = 1.0 - xWeight[0] - xWeight[2] - xWeight[3];
	      /* y */
	      w = y - (double)yIndex[1];
	      yWeight[3] = (1.0 / 6.0) * w * w * w;
	      yWeight[0] = (1.0 / 6.0) + (1.0 / 2.0) * w * (w - 1.0) - yWeight[3];
	      yWeight[2] = w + yWeight[0] - 2.0 * yWeight[3];
	      yWeight[1] = 1.0 - yWeight[0] - yWeight[2] - yWeight[3];
	      break;
	 case 4:
	      /* x */
	      w = x - (double)xIndex[2];
	      w2 = w * w;
	      t = (1.0 / 6.0) * w2;
	      xWeight[0] = 1.0 / 2.0 - w;
	      xWeight[0] *= xWeight[0];
	      xWeight[0] *= (1.0 / 24.0) * xWeight[0];
	      t0 = w * (t - 11.0 / 24.0);
	      t1 = 19.0 / 96.0 + w2 * (1.0 / 4.0 - t);
	      xWeight[1] = t1 + t0;
	      xWeight[3] = t1 - t0;
	      xWeight[4] = xWeight[0] + t0 + (1.0 / 2.0) * w;
	      xWeight[2] = 1.0 - xWeight[0] - xWeight[1] - xWeight[3] - xWeight[4];
	      /* y */
	      w = y - (double)yIndex[2];
	      w2 = w * w;
	      t = (1.0 / 6.0) * w2;
	      yWeight[0] = 1.0 / 2.0 - w;
	      yWeight[0] *= yWeight[0];
	      yWeight[0] *= (1.0 / 24.0) * yWeight[0];
	      t0 = w * (t - 11.0 / 24.0);
	      t1 = 19.0 / 96.0 + w2 * (1.0 / 4.0 - t);
	      yWeight[1] = t1 + t0;
	      yWeight[3] = t1 - t0;
	      yWeight[4] = yWeight[0] + t0 + (1.0 / 2.0) * w;
	      yWeight[2] = 1.0 - yWeight[0] - yWeight[1] - yWeight[3] - yWeight[4];
	      break;
	 case 5:
	      /* x */
	      w = x - (double)xIndex[2];
	      w2 = w * w;
	      xWeight[5] = (1.0 / 120.0) * w * w2 * w2;
	      w2 -= w;
	      w4 = w2 * w2;
	      w -= 1.0 / 2.0;
	      t = w2 * (w2 - 3.0);
	      xWeight[0] = (1.0 / 24.0) * (1.0 / 5.0 + w2 + w4) - xWeight[5];
	      t0 = (1.0 / 24.0) * (w2 * (w2 - 5.0) + 46.0 / 5.0);
	      t1 = (-1.0 / 12.0) * w * (t + 4.0);
	      xWeight[2] = t0 + t1;
	      xWeight[3] = t0 - t1;
	      t0 = (1.0 / 16.0) * (9.0 / 5.0 - t);
	      t1 = (1.0 / 24.0) * w * (w4 - w2 - 5.0);
	      xWeight[1] = t0 + t1;
	      xWeight[4] = t0 - t1;
	      /* y */
	      w = y - (double)yIndex[2];
	      w2 = w * w;
	      yWeight[5] = (1.0 / 120.0) * w * w2 * w2;
	      w2 -= w;
	      w4 = w2 * w2;
	      w -= 1.0 / 2.0;
	      t = w2 * (w2 - 3.0);
	      yWeight[0] = (1.0 / 24.0) * (1.0 / 5.0 + w2 + w4) - yWeight[5];
	      t0 = (1.0 / 24.0) * (w2 * (w2 - 5.0) + 46.0 / 5.0);
	      t1 = (-1.0 / 12.0) * w * (t + 4.0);
	      yWeight[2] = t0 + t1;
	      yWeight[3] = t0 - t1;
	      t0 = (1.0 / 16.0) * (9.0 / 5.0 - t);
	      t1 = (1.0 / 24.0) * w * (w4 - w2 - 5.0);
	      yWeight[1] = t0 + t1;
	      yWeight[4] = t0 - t1;
	      break;
	}
    
    	/* apply the mirror boundary conditions */
    	for (int k = 0; k <= splineDegree; k++) {
	    
	    xIndex[k] = (inWidth == 1) ? (0) : ((xIndex[k] < 0) ?
						  (-xIndex[k] - Width2 * ((-xIndex[k]) / Width2))
						: (xIndex[k] - Width2 * (xIndex[k] / Width2)));
	    if (inWidth <= xIndex[k]) {
		xIndex[k] = Width2 - xIndex[k];
	    }
	    
	    yIndex[k] = (inHeight == 1) ? (0) : ((yIndex[k] < 0) ?
						   (-yIndex[k] - Height2 * ((-yIndex[k]) / Height2))
						 : (yIndex[k] - Height2 * (yIndex[k] / Height2)));
	    if (inWidth <= yIndex[k]) {
		yIndex[k] = Height2 - yIndex[k];
	    }
	}
    
    	/* perform interpolation */
    	interpolated = 0.0;
    	for (j = 0; j <= splineDegree; j++) {
	    
	     w = 0.0;
	     for (i = 0; i <= splineDegree; i++) {
		 w += xWeight[i] * image[yIndex[j]*inWidth + xIndex[i]];
	     }
	     interpolated += yWeight[j] * w;
	}
    
    	return(interpolated);
    }

    public void sample(int pix) {
	
	
	// Find the X and Y at this point.
	double[] zin = outImage.getCenter(pix);
	trans.transform(zin, zout);
	
	/*
	 * What's that 0.5 doing in the next two statements?
	 * 
	 * This is not the FITS/non-FITS issue.  In our coordinate
	 * system the first pixel spans the range 0-1, so the center of
	 * that pixel is at 0.5.  This is the 'sample' point for this
	 * pixel.  However most samplers assume that the
	 * sampled values are at the integral values, so we need to
	 * shift this down to 0.
	 */
	
	double x = zout[0]-0.5 - xmin;
	double y = zout[1]-0.5 - ymin;
	
	if (x < 0 || x > inWidth || y < 0 || y > inHeight) {
	    return;
	} else {
	    // Remember this only handles 2-d
	    outImage.setData(pix, interpolatedValue(x,y));
	}
    }
    
}
