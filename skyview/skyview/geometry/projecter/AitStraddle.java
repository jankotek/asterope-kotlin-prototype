package skyview.geometry.projecter;

/** Handle the straddling of the Aitoff projection
 *  when a figure extends accross the lon=180 lin.
 */
class AitStraddle extends CarStraddle {
   
   AitStraddle(Ait inProj) {
       super(inProj);
       doClip = false;
   }
}
