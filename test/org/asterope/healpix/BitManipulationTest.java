package org.asterope.healpix;

import junit.framework.TestCase;

/**
 * @author N Kuropatkin
 *
 */
public class BitManipulationTest extends TestCase {

	/**
	 * test Modulo
	 */
	public void testMODULO() {
		
		double a = 5.;
		double b = 3.;
		double mod = PixToolsUtils.MODULO(a,b);
		System.out.println("a="+a+" b="+b+" mod="+mod);
		a = -5.0;
		b = 3.0;
		mod = PixToolsUtils.MODULO(a,b);
		System.out.println("a="+a+" b="+b+" mod="+mod);
		a = 5.0;
		b = -3.0;
		mod = PixToolsUtils.MODULO(a,b);
		System.out.println("a="+a+" b="+b+" mod="+mod);
		a = -5.0;
		b = -3.0;
		mod = PixToolsUtils.MODULO(a,b);
		System.out.println("a="+a+" b="+b+" mod="+mod);
		a = 8.0;
		b = 5.0;
		mod = PixToolsUtils.MODULO(a,b);
		System.out.println("a="+a+" b="+b+" mod="+mod);
		a = -8.0;
		b = 5.0;
		mod = PixToolsUtils.MODULO(a,b);
		System.out.println("a="+a+" b="+b+" mod="+mod);
		a = 1.0;
		b = 4.0;
		mod = PixToolsUtils.MODULO(a,b);
		System.out.println("a="+a+" b="+b+" mod="+mod);
	}
}
