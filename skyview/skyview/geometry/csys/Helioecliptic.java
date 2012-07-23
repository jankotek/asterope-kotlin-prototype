package skyview.geometry.csys;


import static org.apache.commons.math3.util.FastMath.*;

/** A helioecliptic coordinate system at a given epoch.
 *  This gives a coordinate system where the Sun is at the
 *  center of the coordinate system.  We assume that the same epoch
 *  is to be used to get the position of the Sun and the basis
 *  Ecliptic coordinate system.
 */
public class Helioecliptic extends Ecliptic
  implements skyview.Component {
    
    private double epoch;
    
    /**
     * Get the name of the component.
     */
    public String getName() {
	return "H"+epoch;
    }
    
    /**
     * Get a description of the component.
     */
    public String getDescription() {
	return "A coordinate system with the equator along  the ecliptic and the Sun at the center. "+
	       "The position of the sun is inferred from the epoch.";
    }
 
    /** Get a coordinate system at a given epoch.
     *  @param epoch The desired epoch
     */
    public Helioecliptic(double epoch) {
	super(epoch, sunlong(epoch));
	this.epoch = epoch;
    }
    
    /** Find the ecliptic longitude of the Sun at a given epoch.
     *  Algorithm derived (and simplified) from the IDL Astronomy
     *  library sunpos (C.D. Pike, B. Emerson).
     *  @param Epoch (in years).
     */
    public static double sunlong(double epoch) {

       double dtor = 3.1415926535/180.0;
       // form time in Julian centuries from 1900.0
       // The original IDL seems to use 1999-12-31 12:00 UT at the nominal
       // start time...  Note that 2000-01-01 -> JD 2451544.5 

       //  orignal IDL: t = (jd - 2415020.0d)/36525.0d0
       
       double t = ((epoch-2000) * 365.25 + 2451544.5 - 2415020)/36525.;

       // form sun's mean longitude

       double l = (279.696678+((36000.768925*t) % 360))*3600;

       //  allow for ellipticity of the orbit (equation of centre)
       //  using the Earth's mean anomoly ME

       double  me = 358.475844 + ((35999.049750*t) % 360.0);
       double  ellcor  = (6910.1 - 17.2*t)*sin(me*dtor) + 72.3*sin(2.0*me*dtor);
       l += ellcor;

       // allow for the Venus perturbations using the mean anomaly of Venus MV

       double mv = 212.603219 + ((58517.803875*t) % 360) ;
       double  vencorr = 4.8 * cos((299.1017 + mv - me)*dtor) +
                  5.5 * cos((148.3133 +  2.0 * mv  -  2.0 * me )*dtor) + 
                  2.5 * cos((315.9433 +  2.0 * mv  -  3.0 * me )*dtor) + 
                  1.6 * cos((345.2533 +  3.0 * mv  -  4.0 * me )*dtor) + 
                  1.0 * cos((318.15   +  3.0 * mv  -  5.0 * me )*dtor);
       l += vencorr;

       //  Allow for the Mars perturbations using the mean anomaly of Mars MM

       double mm = 319.529425  +  (( 19139.858500 * t)  % 360 );
       double marscorr = 2.0 * cos((343.8883 -  2.0 * mm  +  2.0* me)*dtor ) + 
                         1.8 * cos((200.4017 -  2.0 * mm  + me) * dtor);
       l += marscorr;

       // Allow for the Jupiter perturbations using the mean anomaly of
       // Jupiter MJ

       double mj = 225.328328  +  (( 3034.6920239 * t)  %  360.0 );
       double jupcorr = 7.2 * cos(( 179.5317 - mj + me )*dtor) + 
                        2.6 * cos((263.2167  - mj ) *dtor) + 
                        2.7 * cos(( 87.1450  -  2.0 * mj  +  2.0 * me ) *dtor) + 
                        1.6 * cos((109.4933  -  2.0 * mj  +  me ) *dtor);
       l += jupcorr;

       // Allow for the Moons perturbations using the mean elongation of
       // the Moon from the Sun D

       double d = 350.7376814  + (( 445267.11422 * t)  %  360.0 );
       double mooncorr  = 6.5 * sin(d*dtor);
       l +=  mooncorr;

       // Allow for long period terms

       double longterm  = + 6.4 * sin(( 231.19  +  20.20 * t )*dtor);
       l  += longterm;
	
       l  =  ( l + 2592000.0)  %  1296000.0;
       double longmed = l/3600.0 * dtor;
	
       // Don't perform rest of calculation from Sunpos -- we've
       // got what we're after.
       return longmed;
    }
}
