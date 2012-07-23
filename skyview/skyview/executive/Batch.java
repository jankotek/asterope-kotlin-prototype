package skyview.executive;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class Batch {
    
    /** Run a series of image requests.
     *  Usage:  java skyview.executive.Batch file [key1=val1 key2=val2 ...]
     *  where each line of file contains  settings that supplement
     *  the those given on this command line. 
     */
    public static void main(String[] args) throws Exception {

        
        if (args.length < 1) {
	    System.err.println("Batch processing requested but no file specified.  Use '-' for STDIN");
	}
	String[] xargs = new String[args.length -1];
	System.arraycopy(args, 1, xargs, 0, xargs.length);
	Settings.addArgs(xargs);
	BufferedReader in;
	String file = args[0];
	
	if (file.equals("-")) {
	    in = new BufferedReader(new InputStreamReader(System.in));
	} else {
	    in = new BufferedReader(new FileReader(file));
	}
	
	Imager img = new Imager();
	int count = 0;
	String origOutput = Settings.get("output");
	while (true) {
	    Settings.save();
	    String line = in.readLine();
	    if (line == null) {
		break;
	    }
	    line = line.trim();
	    if (line.length() == 0  || line.startsWith("#") ) {
		continue;
	    }
	    args = line.split(" ");
	    for (int i=0; i<args.length; i += 1) {
		args[i] = args[i].trim();
	    }
	    try {
		Settings.addArgs(args);
		String currOutput = Settings.get("output");
		// Make sure each line has a distinct output setting.
		if (currOutput == null) {
		    Settings.put("output", "output"+count);
		} else if (currOutput.equals(origOutput)) {
		    Settings.put("output", origOutput+count);
		}
	        img.run();
	    } catch (Exception e) {
		System.err.println("Caught exception for line:"+line+"\n   "+e.getMessage());
		e.printStackTrace(System.err);
	    }
	    img.clearImageCache();
	    Settings.restore();
	    count += 1;
	}
    }
}
