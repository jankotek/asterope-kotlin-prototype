package skyview.geometry.projecter;

import skyview.geometry.Projecter;

/** Handle the straddling of the TOAST and TEA projections
 * derived for the Octahedral geometries.
 * Assuming the standard pole in the center of the image,
 * these projection straddle when when a figure crosse the
 * LON=0,90,180 or 270 with a negative latitude.
 */
class OctaStraddle extends CarStraddle {
   
   OctaStraddle(double size, Projecter inProj) {
       super(inProj);
       doClip = true;
       clipXMin = -size;
       clipYMin = -size;
       clipXMax = size;
       clipYMax = size;
   }
   
}
