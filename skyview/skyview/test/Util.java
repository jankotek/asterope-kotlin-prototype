package skyview.test;


import skyview.executive.Settings;


import static org.junit.Assert.assertEquals;

import java.io.DataOutputStream;

public class Util {
    
    public static void regress(String key, double value, DataOutputStream os) {
	if (Settings.has(key)) {
	    try {
		double val = Double.parseDouble(Settings.get(key));
		System.err.println("  Comparing:"+key+" Expecting:'"+val+"'  Got: '"+value+"'");
		assertEquals("  Regression: " +key, value, val, 0.);
	    } catch (Exception e) {
		System.err.println("  Regression error:"+key+" "+value+" "+Settings.get("key"));
	    }
	} else {
	    System.err.println("  Missing value:"+key+" with value "+value);
	    writeNewSetting(os,key,value);
	}
    }
    static void writeNewSetting(DataOutputStream os, String key, double val) {
	try {
	    os.writeBytes(key+"="+val+"\n");
	    os.flush();
	} catch (Exception e){}
    }
    
}
