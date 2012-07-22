package org.asterope.data

import junit.framework.TestCase
import kotlin.test.assertEquals

class DbImportTest:TestCase(){


    fun test_count_xhip_stars(){
        val stars = DbImport.loadXhip();

        assertEquals(117955, stars.toList().size())
    }

}