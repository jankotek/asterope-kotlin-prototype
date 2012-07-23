package skyview.geometry;

/** This class is thrown when an error
 *  occurs relating to transformations among
 *  frames.
 */
public class TransformationException extends Exception {
    public TransformationException(){
    }
    public TransformationException(String msg) {
	super(msg);
    }
}
