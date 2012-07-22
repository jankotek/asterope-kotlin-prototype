package org.asterope.data

import java.util.ArrayList
import java.util.TreeMap
import java.util.List
import org.asterope.healpix.LongRangeSet

/**
 * Object used to access stars from database bundled with Asterope
 *
 *  @author Jan Kotek
 */

open class StarDao{

    private val stars = ArrayList<Star>()

    private val areaIndex = TreeMap<Long, List<Star>>();

    fun addStar(star:Star){
        stars.add(star);
        val sublist = areaIndex.get(star.ipix) ?: ArrayList<Star>();
        sublist.add(star)
        areaIndex.put(star.ipix, sublist);
    }

    fun getStarsByArea(set:LongRangeSet):List<Star>{
        val ret = ArrayList<Star>();
        val iter = set.rangeIterator()!!;
        while (iter.moveToNext()){
            val submap = areaIndex.subMap(iter.first(), true, iter.last(),true)!!;
            for (sublist in submap.values()){
                ret.addAll(sublist)
            }
        }

        return ret;
    }


}
