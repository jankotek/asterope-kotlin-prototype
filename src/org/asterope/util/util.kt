package org.asterope.util

import com.google.common.base.Charsets
import com.google.common.io.Files
import com.google.common.base.Objects
import java.io.File
import java.io.Reader
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.FileInputStream
import org.itadaki.bzip2.BZip2InputStream
import java.util.zip.GZIPInputStream
import org.asterope.healpix.PixTools
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import java.math.BigDecimal

/**
 * Various utilities
 *
 *  @author Jan Kotek
 */


fun java.io.File.readToString():String{
    return Files.toString(this, Charsets.UTF_8)!!;
}

fun java.io.File.openReader():BufferedReader{
    return if (this.getName()!!.matches(".+\\.bz2$"))
        BufferedReader(InputStreamReader(BZip2InputStream(FileInputStream(this), false)))
    else if (this.getName()!!.matches(".+\\.gz$"))
        BufferedReader(InputStreamReader(GZIPInputStream(FileInputStream(this))))
    else
        Files.newReader(this,Charsets.UTF_8)!!;
}

fun String.toBigDecimal() = BigDecimal(this)


class jjIter<E>(val iter:jet.Iterator<E>) :java.util.Iterator<E>{
    public override fun next() = iter.next();
    public override fun remove() =  throw UnsupportedOperationException()
    public override fun hasNext() =  iter.hasNext;
}


fun <E> jet.Iterator<E>.jj():java.util.Iterator<E> = jjIter(this);



class Magnitude(val mili:Int){
    fun toString() = "Mag("+mili+")";
    fun equals(o:Any?) = o is Magnitude && o.mili == mili;
    fun hashCode() = mili;
}


open class Case: java.lang.Object() {

    override fun toString():String{
        var helper = Objects.toStringHelper(this.getClass())!!;

        for (field in this.getClass()!!.getDeclaredFields()){
            field!!.setAccessible(true)
            helper = helper.add(field!!.getName(), field!!.get(this))!!
        }

        return helper.toString()!!;
    }

    override fun equals(p0:Any?):Boolean {
        if (p0 ==null)
            return false;
        if (!(p0 is Object))
            return false;
        if (p0.getClass()!=getClass())
            return false;

        //compare field values on both objects
        for (field in this.getClass()!!.getDeclaredFields()){
            field!!.setAccessible(true)
            val v1 = field!!.get(this);
            val v2 = field!!.get(p0)
            if (v1!=v2) return false;
        }
        return true;
    }

    private var _hashCode:Int? = null;

    override fun hashCode():Int{
        if (_hashCode!=null) //cache hash code
            return _hashCode!!;

        var ret = 0;
        //combine all field using xors
        for (field in this.getClass()!!.getDeclaredFields()){
            field!!.setAccessible(true)
            val v1 = field!!.get(this);
            if(v1 is Object)
                ret = ret xor v1.hashCode()
        }
        _hashCode = ret;
        return ret;
    }
}
