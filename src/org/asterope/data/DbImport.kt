package org.asterope.data

import java.io.File
import org.asterope.Beans
import org.asterope.util.*

import java.util.List

/**
 * Imports internal database
 *
 *  @author Jan Kotek
 */
object DbImport{

    val  xhipReadme = File("cat/xhip.readme")

    val xhipDat = File("cat/xhip.dat.bz2")


    fun loadXhip():java.util.Iterator<Star>{
        val xhipColDef = Parser.parseADCColumnDefinition(DbImport.xhipReadme.readToString());


        val stars = DbImport.xhipDat.openReader().lineIterator().jj()
                .map{ Parser.parseFixedWidth(it, xhipColDef)}
                .map{ vals->

            val ra:Angle = vals.get("RAdeg")!!.toAngle();
            val de:Angle = vals.get("DEdeg")!!.toAngle();

            val pos = Vector3D(ra,de);
            val mag = Magnitude(1111);

            Star(pos,mag)
        }

        return stars;

    }

}


