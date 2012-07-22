package org.asterope.data

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import org.asterope.util.*

/**
 * Star on sky
 *
 *  @author Jan Kotek
 */

class Star(
        val pos:Vector3D,
        val mag:Magnitude
){
    val ipix:Long = pos.toIpix()
}
