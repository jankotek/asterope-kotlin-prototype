package skyview.geometry.projecter;

/** This class implements a tangent plane projection
 *  for the NEAT survey.  The NEAT projection is not
 *  a real tangent plane projection, since there is an extra
 *  distortion, but this projection will be used for proxy
 *  images where the distortion is not known to mark that
 *  we do not have a standard tangent plane projection.
 *  When the actual  image is downloaded, we can provide
 *  a combination of standard Tangent plane projecter and
 *  NEAT distorter to correctly transform coordinates.
 */

public class Xtn extends Tan {
    
}
