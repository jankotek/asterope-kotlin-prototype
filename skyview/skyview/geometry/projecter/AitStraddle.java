package skyview.geometry.projecter;

/** Handle the straddling of the Aitoff projection
 *  when a figure extends accross the Lon=180 lin.
 */
class AitStraddle extends CarStraddle {
   
   AitStraddle(Ait inProj) {
       super(inProj);
       doClip = false;
   }
}
