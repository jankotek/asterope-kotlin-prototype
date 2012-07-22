package org.asterope

import org.asterope.data.StarDao

/**
 * Simple inversion of control for Asterope
 *
 *  @author Jan Kotek
 */


class Beans{

    object starDao:StarDao()

}
