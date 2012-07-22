package org.asterope.util

import org.asterope.healpix.PixTools
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import java.math.BigDecimal

/**
 * Various angle related utilities
 *
 *  @author Jan Kotek
 */


val HEALPIX_RESOLUTION = 20.arcMinute();
val HEALPIX_NSIDE = PixTools.GetNSide(HEALPIX_RESOLUTION.toArcSecond());
val HEALPIX_TOOLS = PixTools(HEALPIX_NSIDE)
val ARC_SECOND_TO_MICRO_ARC_SEC:Long = 1000000.toLong();

val ARC_MINUTE_TO_MICRO_ARC_SEC:Long = ARC_SECOND_TO_MICRO_ARC_SEC * 60
val ARC_DEGREE_TO_MICRO_ARC_SEC:Long = ARC_MINUTE_TO_MICRO_ARC_SEC * 60;
val MICRO_ARC_SEC_TO_RADIAN:Double = ARC_DEGREE_TO_MICRO_ARC_SEC.toDouble() * 180.toDouble() / Math.PI

val ARC_SECOND_TO_MICRO_ARC_SEC_BD = BigDecimal(ARC_SECOND_TO_MICRO_ARC_SEC)
val ARC_MINUTE_TO_MICRO_ARC_SEC_BD = BigDecimal(ARC_MINUTE_TO_MICRO_ARC_SEC)
val ARC_DEGREE_TO_MICRO_ARC_SEC_BD = BigDecimal(ARC_DEGREE_TO_MICRO_ARC_SEC)

val ARC_DEGREE_TO_RADIAN:Double = Math.PI / 180.0;
val ARC_RADIAN_TO_DEGREE:Double = 180.0/Math.PI;

val D2R = ARC_DEGREE_TO_RADIAN;
val R2D = ARC_RADIAN_TO_DEGREE;



fun Vector3D(ra:Angle, de:Angle) = Vector3D( ra.toRadian(), de.toRadian())


fun Vector3D.toIpix():Long = HEALPIX_TOOLS.vect2pix(this)




fun jet.Int.arcMinute() =  Angle(this * ARC_MINUTE_TO_MICRO_ARC_SEC)
fun jet.Int.arcDegree() =  Angle(this * ARC_DEGREE_TO_MICRO_ARC_SEC)
fun jet.Int.miliArcSecond() =  Angle(this * 1000.toLong())
fun jet.Int.arcSecond() =  Angle(this * ARC_SECOND_TO_MICRO_ARC_SEC)

fun jet.Double.arcMinute() =  Angle((this * ARC_MINUTE_TO_MICRO_ARC_SEC).toLong())
fun jet.Double.arcDegree() =  Angle((this * ARC_DEGREE_TO_MICRO_ARC_SEC).toLong())
fun jet.Double.miliArcSecond() =  Angle((this * 1000.toLong()).toLong())
fun jet.Double.arcSecond() =  Angle((this * ARC_SECOND_TO_MICRO_ARC_SEC).toLong())




fun BigDecimal.arcMinute() =  Angle(this.multiply(ARC_MINUTE_TO_MICRO_ARC_SEC_BD)!!.longValue());
fun BigDecimal.arcSecond() =  Angle(this.multiply(ARC_SECOND_TO_MICRO_ARC_SEC_BD)!!.longValue());
fun BigDecimal.arcDegree() =  Angle(this.multiply(ARC_DEGREE_TO_MICRO_ARC_SEC_BD)!!.longValue());
fun BigDecimal.miliArcSecond() =  Angle(this.multiply(BigDecimal.valueOf(1000))!!.longValue());


fun java.lang.Integer.arcSecond() = this.intValue().arcSecond();
fun java.lang.Integer.arcMinute() = this.intValue().arcMinute();
fun java.lang.Integer.arcDegree() = this.intValue().arcDegree();
fun java.lang.Integer.miliArcSecond() = this.intValue().miliArcSecond();




class Angle(val microArcSec:Long){
    fun toString() = "Angle("+microArcSec+")"
    fun equals(o:Any?) =  (o is Angle && o.microArcSec==microArcSec)
    fun hashCode():Int =  (microArcSec xor (microArcSec ushr 32)).toInt();

    fun toRadian():Double = MICRO_ARC_SEC_TO_RADIAN * microArcSec //TODO should be cached?
    fun toArcMinute():Double = microArcSec.toDouble() / ARC_MINUTE_TO_MICRO_ARC_SEC;
    fun toArcSecond():Double = microArcSec.toDouble() / ARC_SECOND_TO_MICRO_ARC_SEC;
}
