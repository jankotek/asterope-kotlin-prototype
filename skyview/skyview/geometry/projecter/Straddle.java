package skyview.geometry.projecter;

/**
 * This class helps straddleable projection deal with the straddling.
 * A projection is straddleable if it is possible to take a region in the
 * sky defined by a set of vertices and when the boundaries of these points
 * are transformed into the new projection, the boundaries between consective
 * vertices may project into lines that go outside of the standard boundaries
 * of the projection.
 * E.g., in projection to Cartesian projection, 
 * a pixel that straddles the lon=180 degree
 * line will end up with some vertices to the right of the origin and some 
 * to the left and the proper line joining them is not one that goes through
 * the center of the image.
 * 
 * Straddling is dealt with by first breaking up the input vertices into
 * groups that are properly connectable.  E.g., in the Cartesian projection
 * the pixels with positive and negative values of x go into two groups.
 * We then recreate the pixel for each of the groups but substitute shadow
 * vertices for any vertex that belongs to one of the other groups.  E.g.,
 * for the Cartesian projection when we deal with the pixels with negative
 * longitudes, we use shadow pixels for the input pixels that have positive
 * longitude.  For each of the straddle components that we build up this
 * way we clip the component using at the boundaries of the standard region
 * of the projection.  These clipped elements are then sampled and added
 * together to give us the full input pixel.
 * 
 * The shadow verticess arise from the continuity of the projections at the
 * nominal boundaries.  E.g., suppose we are dealing in the Cartesian
 * projection and we want to sample a pixel with vertices 
 *    (179, 44), (179,46), (181, 46), (181, 44)
 * Since the Car projection is centered at 0,0 these will
 * correspond to points in the projection plane at  (179,44),(179,46),
 * (-179,46),(-179,44), since the nominal region in the projection
 * runs between longitudes -180 and 180.  We now create two straddle component
 * the first has vertices at (179,44),(179,46),(181,46),(181,44) and the
 * second at (-181,44),(-181,46),(-179,46),(-179,44).  These two straddle
 * components include the 'shadow' vertices needed to draw the pixel appropriately
 * on each side of the projection.  We clip these two straddle components to
 * the standard region of the projection (179,44),(179,46),(180,46),(180,44)
 * and (-180,44),(-180,46),(-179,46),(-179,44) to get two regions that we can
 * then sample normally.
 * 
 * Our example here has used the Car projection where there are generally just
 * two straddle regions.  This will generally be the case for the
 * Car and Ait projections.  The CSC projection can have three,
 * the staddle components are determined by the tiles that the vertex belongs
 * to.  Currently we treat the TOA and TEA projections as having just two
 * components, but given the discontinuities in the derivatives corresponding
 * to the octagon tile boundaries, one could treat them with up to four
 * straddle components.  Except in peculiar cases (i.e., very large pixels), there should be
 * no more than that. 
 * 
 * Ait straddling does not currently clip the straddle components,
 * this is addressed by the the checking for invalid pixels.
 */
abstract class Straddle {

    abstract boolean straddle(double[][] vertices);
    abstract double[][][] straddleComponents(double[][] vertices);
}
