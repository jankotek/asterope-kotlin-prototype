package skyview.geometry;


import skyview.data.CoordinateFormatter;

/** This class represents a position in the sky.  This
 *  class is used to pass a position that may be represented in different
 * frames in different parts of a program.  However since it creates
 * CoordinateSystem objects for each transformation it should not be
 * used to do coordinate transformations for large arrays of positions.
 */
public class Position {
    
    /** The J2000 coordinates */
    private double[] coords = new double[2];;
    
    /** The original system coordinates */
    private double[] orig;
    
    /** The original coordinate system */
    private String origFrame;
    
    
    /** Define a position object in the standard (J2000) frame */
    public Position(double l, double b) throws TransformationException {
	this (l, b, "J2000");
    }
    
    /** Define a position object used a specified frame */
    public Position(double l, double b, String frame) throws TransformationException {
	
	orig = new double[2];
	orig[0] = l;
	orig[1] = b;
	origFrame = frame;
	
	if (frame == null || frame.toUpperCase().equals("J2000")) {
	    
	    coords[0]  = l;
	    coords[1]  = b;
	    
	} else {
	    
	    CoordinateSystem csys = CoordinateSystem.factory(frame);
	    double[] unit         = Util.unit(Math.toRadians(l), Math.toRadians(b));
	    Converter conv        = new Converter();
	    
	    if (csys.getRotater() != null) {
		conv.add(csys.getRotater().inverse());
	    }
	    if (csys.getSphereDistorter() != null) {
		conv.add(csys.getSphereDistorter().inverse());
	    }
	    double[] j2000Unit = conv.transform(unit);
	    double[] j2000C    = Util.coord(j2000Unit);
	    
	    coords[0] = Math.toDegrees(j2000C[0]);
	    coords[1] = Math.toDegrees(j2000C[1]);
	}
    }
    
    /** Get the coordinates in the standard (J2000) frame.
      * Used to be called getPosition.
      */
    public double[] getCoordinates() throws TransformationException {
	return getCoordinates("J2000");
    }
    
    /** Get the coordinates in a specified frame.
      * Used to be called getPosition.
      */
    public double[] getCoordinates(String frame) throws TransformationException {
	
	
	if (frame == null || frame.toUpperCase().equals("J2000")) {
	    return coords;
	    
	} else {
	    
	    // Return data in the original coordinate from
	    // without loss of precision due to transformations.
	    if (frame.equals(origFrame)) {
	        return orig.clone();
	    }

	    CoordinateSystem csys = CoordinateSystem.factory(frame);
	    double[] unit         = Util.unit(Math.toRadians(coords[0]), Math.toRadians(coords[1]));
	    Converter conv        = new Converter();
	    
	    if (csys.getSphereDistorter() != null) {
		conv.add(csys.getSphereDistorter());
	    }
	    
	    if (csys.getRotater() != null) {
		conv.add(csys.getRotater());
	    }
	    
	    double[] xUnit   = conv.transform(unit);
	    double[] xCoords = Util.coord(xUnit);
	    xCoords[0] = Math.toDegrees(xCoords[0]);
	    xCoords[1] = Math.toDegrees(xCoords[1]);
	    
	    // Make sure longitude is within range.
	    if (xCoords[0] < 0) {
		xCoords[0] += 360;
	    } else if (xCoords[0] >= 360) {
		xCoords[0] -= 360;
	    }
	    
	    return xCoords;
	}
    }
    
    public String formatted(String coords, int precision, boolean sexagesimal) {
	CoordinateFormatter cf = new CoordinateFormatter();
	double[] vals = null;
	try {
	    vals = getCoordinates(coords);
	} catch(Exception e) {
	    return null;
	}
	String value;
	if (sexagesimal) {
	    if (coords.charAt(0) == 'B' || coords.charAt(0) == 'J') {
		vals[0] /= 15;
	    }
	    cf.setZeroFill(true);
	    value = cf.sexagesimal(vals[0], precision)+", "+
	            cf.sexagesimal(vals[1], precision);
	} else {
	    value = cf.decimal(vals[0], precision)+", "+cf.decimal(vals[1], precision);
	}
	return value;
    }
    
    /** Test the Position class */
    public static void main(String[] args) throws Exception {
	
	double l     = Double.parseDouble(args[0]);
	double b     = Double.parseDouble(args[1]);
	
	Position p   = new Position(l, b, args[2]);
	double[] out = p.getCoordinates(args[3]);
	
	System.out.println("Output:"+(out[0])+ ","+(out[1]));
    }
				  
}
