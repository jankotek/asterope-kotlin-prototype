package org.asterope.data

import junit.framework.TestCase

import com.google.common.collect.ImmutableList
import kotlin.test.*
import org.asterope.util.*;
import java.io.Reader
import org.itadaki.bzip2.BZip2InputStream
import java.io.FileInputStream
import java.io.InputStreamReader


class ParserTest:TestCase(){

    fun test_parse_fixed_width(){
        val columns = ImmutableList.of(
            ParserColumn(name ="test",unit= "d",beginIndex = 3, endIndex=4)
        )!!;

        val line = "1234567890"

        val cells = Parser.parseFixedWidth(line, columns)

        assertTrue(cells.containsKey("test"));
        assertEquals("4",cells.get("test")?.value);

    }

    fun test_parse_ADS_col_desc(){
        val h =  Parser
                    .parseADCColumnDefinition( DbImport.xhipReadme.readToString())
                    .get(0)

        assertEquals("HIP", h.name)
        assertEquals("---", h.unit)
        assertEquals("I6",h.format)
        assertEquals("Hipparcos identifier", h.desc)
        assertEquals(0, h.beginIndex)
        assertEquals(6,h.endIndex)
    }

    fun test_parse_ADS_row(){
        val cols = Parser .parseADCColumnDefinition( DbImport.xhipReadme.readToString())
        val out = DbImport.xhipDat.openReader();

        val line = out.readLine()!!;

        val dat= Parser.parseFixedWidth(line,cols);

        assertEquals(1, dat.get("HIP")!!.value.toInt());
        assertEquals("",dat.get("Comp")!!.value );
        assertEquals("F3 V",dat.get("SpType")!!.value );

    }

}