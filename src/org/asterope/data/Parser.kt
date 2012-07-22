package org.asterope.data

import java.util.HashMap
import com.google.common.collect.ImmutableList
import java.util.Map
import com.google.common.base.Splitter
import javax.swing.plaf.basic.BasicInternalFrameTitlePane.SystemMenuBar
import com.google.common.collect.Lists
import java.util.ArrayList
import org.asterope.util.*;
import java.util.LinkedHashMap


/**
* Parse tabular data files (CSV etc)
*
*  @author Jan Kotek
*/

object Parser{



    fun parseFixedWidth(line:String, columns: ImmutableList<ParserColumn>):Map<String, ParserCell>{
        val ret = LinkedHashMap<String, ParserCell>();
        for (c  in columns){
            val v =
                    if (c.beginIndex>=line.length()) ""
                    else if (c.endIndex>=line.length) line.substring(c.beginIndex);
                    else line.substring(c.beginIndex, c.endIndex);
            ret.put(c.name, ParserCell(c.name, v.trim(), c.unit))
        }

        return ret;
    }

    fun parseADCColumnDefinition(readmeFileContent: String, position:Int = 0 ):ImmutableList<ParserColumn>{

        val ret = ArrayList<ParserColumn>();;

        //extract column definition
        val cols = readmeFileContent
                .split("Byte-by-byte Description of file")
                .get(position+1)
                .split("--------------------------------------------------------------------------------")
                .get(2)

        for (l in Splitter.on("\n")!!
                .omitEmptyStrings()!!
                .split(cols)!!){
            if (l==null) continue

            val endIndex = l.substring(5,9).replace(" ","").trim()
            val endIndex2 = if (endIndex == "") -1 else (Integer.valueOf(endIndex)!!);
            val beginIndex = l.substring(0,4).replace(" ","").trim()
            var beginIndex2 = if (beginIndex == "") -1 else (Integer.valueOf(beginIndex)!!-1)
            if (beginIndex2 == -1 && endIndex2!=-1)
                beginIndex2 = endIndex2;

            val format = l.substring(9,16).trim()
            val unit = l.substring(16,23).trim()
            val name = l.substring(23,35).trim()
            val desc = l.substring(35).trim()

            ret.add(
                    ParserColumn(
                        name=name,
                        unit = unit,
                        format = format,
                        desc = desc,
                        beginIndex = beginIndex2,
                        endIndex = endIndex2
                    )
            )

        }

        return ImmutableList.copyOf(ret)!!;
    }


}

class ParserCell(val name:String,val value:String,val  unit:String?) : Case(){

    fun toAngle():Angle =
    when (unit){
        "deg" ->value.toBigDecimal().arcDegree();
        "mas" ->value.toBigDecimal().miliArcSecond();
        else ->  throw ParserException("Unknown unit $unit in column $name")
    }

}

class ParserColumn(val name:String, val unit:String, val format:String="", val desc:String="",
                          val beginIndex:Int=-1, val endIndex:Int=-1) : Case(){

}

class ParserException(msg:String):RuntimeException(msg){}
