package skyview.ops;

import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

/**
 * This class is intended to be used to clean up directories which
 * accumulate files during SkyView operations.  E.g., we might have
 * java -cp skyview.jar /tmp.shared/tempspace/fits time=1440 \
 *    largetime=120 large=100 recordfile=/skyview/logs/mskstat \
 *    email=skyview@host.net mailcmd=/skyview/htdocs/cgi/cgifiles/mailSender.pl
 *
 * This cleans up all files after a day and files larger than 100 MB after 2 hours.
 * It appends information from SkyView FITS files files in a log files and
 * sends an E-mail (using the specified Perl script) to a skyview user.
 * It might be run hourly using a cron job.
 * @author tmcglynn
 */
public class DirScan {
    
    private static List<String> wantedKeys = new ArrayList<String>();
    private static List<Integer> keyType  = new ArrayList<Integer>();

    private static final int X_STRING = 0;
    private static final int X_INT    = 1;
    private static final int X_FLOAT  = 2;
    private static int skFits = 0;
    private static int smFits  = 0;
    private static int skImg  = 0;
    private static int smImg   = 0;
    private static int other   = 0;
    private static Map<String,Integer> keyTypes = new HashMap<String, Integer>();
    private static long epoch         = new java.util.Date().getTime()/1000;

    // Define which fields we want to extract from the FITS file.
    // The first is handled a little specially.
    static {
        wantedKeys.add("SURVEY");
        keyTypes.put("SURVEY", X_STRING);
        wantedKeys.add("CTYPE1");
        keyTypes.put("CTYPE1", X_STRING);
        wantedKeys.add("NAXIS1");
        keyTypes.put("NAXIS1", X_INT);
        wantedKeys.add("CRVAL1");
        keyTypes.put("CRVAL1", X_FLOAT);
        wantedKeys.add("CRVAL2");
        keyTypes.put("CRVAL2", X_FLOAT);
        // Ask for Y axis since it is normally positive.
        // We put this last to be compatible with earlier
        // mskstat files which do not include the image scale.
        wantedKeys.add("CDELT2");
        keyTypes.put("CDELT2", X_FLOAT);
    }

    public static void main(String[] args) throws Exception {
	
	String directory = ".";
        String email   = null;
        String mailcmd = null;
	
	long   largeFile = 100*1024*1024L;
	
	int    standardFileDuration = 8*3600;
	int    largeFileDuration    = 8*3600;
	String recordFile = null;
	
	if (args.length == 0) {
	    System.err.println(
              "Usage: DirScan dir [keyword=val ...]\n" +
              "  dir   directory to be scanned.\n" +
              "  time=[480]      Minutes before all files become eligible for deletion.\n" +
              "  largetime=[480] Minutes before large files become eligible for deletion.\n"+
              "  large=[100]     Mininum size (MB) of a large file.\n" +
              "  recordFile=     File to record results in.\n" +
              "  email=          Email account to send summary to.\n" +
              "  mailCmd=        Perl E-mail cmd.  (e.g., /skyview/htdocs/cgi-bin/cgifiles/mailSender.pl)\n");

	    System.exit(0);
	}
	
	if (args.length > 0) {
	    directory = args[0];
	}
	
	for (int i=1; i<args.length; i += 1) {
	    
	    String[] parse = args[i].split("=", 2);
	    if (parse.length != 2) {
		System.err.println("Unknown argument: "+args[i]);
	    } else {
		String key = parse[0].toLowerCase();
		String val = parse[1];
	    
	        if (key.equals("time") ) {
		    int t = Integer.parseInt(val);
		    standardFileDuration = t*60;
		    largeFileDuration    = t*60;
		} else if (key.equals("largetime")) {
		    int t = Integer.parseInt(val);
		    largeFileDuration    = t*60;
		} else if (key.equals("large")) {
		    int t = Integer.parseInt(val);
		    largeFile = 1024L*1024L*t;
		} else if (key.equals("recordfile")) {
		    recordFile = val;
                } else if (key.equals("email")) {
                    email = val;
                } else if (key.equals("mailcmd")) {
                    mailcmd = val;
		      
		} else {
		    System.err.println("Unknown key for:"+args[i]);
		}
	    }
	}
	
	File f = new File(directory);
	if (!f.exists()) {
	    System.err.println("Scan directory "+directory+" does not exist.");
	    System.exit(1);
	}

	
	PrintStream recordOutput = null;
	if (recordFile != null) {
	    recordOutput = new java.io.PrintStream(new java.io.FileOutputStream(recordFile, true));
            appendDate(recordOutput);
	}	    
	
	if (!f.isDirectory()) {
	    System.err.println("Location "+directory+" is not a directory.");
	}
	
	java.io.File[] files = f.listFiles();
	for (java.io.File file: files) {
	    processFile(file, standardFileDuration, largeFile, largeFileDuration, recordOutput);
	}
        if (recordFile != null) {
            report(recordOutput, email, mailcmd);
            appendDate(recordOutput);
        }
    }

    static void report(PrintStream out, String email, String mailcmd) {
        String msg = "# Deleted\n" +
                     "#    SkyView FITS files: "+skFits+"\n"+
                     "#    SkyView QL files:   "+skImg+"\n"+
                     "#    SkyMorph FITS files:"+smFits+"\n"+
                     "#    SkyMorph QL files:  "+smFits+"\n"+
                     "#    Other files:        "+other+"\n"+
                     "#\n";
        if (email != null  && mailcmd != null) {

            try {
                String subject = "SkyView Cleanup at:" + new Date();
                ProcessBuilder pb = new ProcessBuilder("/usr1/local/bin/perl",
                        mailcmd, subject, email, "cleanup@skyview");
                Process proc = pb.start();
                java.io.BufferedOutputStream os =
                        new java.io.BufferedOutputStream(proc.getOutputStream());
                os.write(msg.getBytes());
                os.close();

            } catch (Exception e) {
            }
        }
        out.print(msg);
    }

    static void appendDate(PrintStream writer) {
        Date d = new Date();
        writer.println("Purge at: "+d);
    }

    static void processFile(java.io.File f, int time, long large, int timeLarge, PrintStream writer) throws Exception {
	
	String name   = f.getName();
	if (name.equals(".") || name.equals("..")) {
	    return;
	}
	
	String suffix = getSuffix(name);
        if (suffix.equals("gz")) {
	    String xname = name.substring(0, name.length()-3);
	    suffix = getSuffix(xname);
	}
	long fileEpoch = f.lastModified()/1000;
	long size      = f.length();
 	if (epoch - fileEpoch > time || (size > large && (epoch-fileEpoch > timeLarge))) {

	    if (suffix.equals("fits")) {
	        processFits(f, name, writer);
            } else if (suffix.equals("gif") || suffix.equals("jpeg")  || suffix.equals("jpg")) {
	        processGraphic(name);
	    } else {
	        processGeneral(name);
	    }

            // The following statement should be uncommented when we go into production.
            // f.delete();
        }
    }
    
    static String getSuffix(String name) {
	if (name.lastIndexOf(".") > 0) {
	    return name.substring(name.lastIndexOf(".")+1);
	} else {
	    return "";
	}
    }
    
    static void processGraphic(String name) {
        if (skymorph(name)) {
            smImg += 1;
        } else {
            skImg += 1;
        }
    }
    
    static void processFits(java.io.File f, String name, PrintStream writer) {
        if (skymorph(name)) {
            smFits += 1;
        } else {
            skFits += 1;
            if (writer != null) {
	        recordFits(f, writer);
	    }
        }
    }
    
    static void processGeneral(String name) {
        other += 1;
    }

    static boolean skymorph(String name) {
        return name.indexOf("sm") >= 0;
    }

    static void recordFits(File file, PrintStream writer) {

        boolean wrote = false;
        Fits f = null;
        try {
            f     = new Fits(file);
            Header hdr = f.readHDU().getHeader();
            String sep = "";
            String format = "%20s";
            for (String key: wantedKeys) {
                String val = null;
                switch (keyTypes.get(key)) {
                    case X_STRING:
                      val = hdr.getStringValue(key);
                      if (val != null && val.length() > 20) {
                         val = val.substring(0, 5) + "..." + val.substring(val.length()-12);
                      }
                      break;
                    case X_INT:
                        val = hdr.getLongValue(key)+"";
                        break;
                    case X_FLOAT:
                        val = hdr.getDoubleValue(key)+"";
                        if (val != null && val.length() > 15) {
                            val = val.substring(0,15);
                        }
                        break;
                    default:
                        System.err.println("*** Unspecified type for key:"+key);
                }
                writer.format(format, val);
                writer.print(sep);
                sep = "  ";
                format="%15s";
                wrote = true;
            }
        } catch (Exception e) {
        } finally {
            if (f != null) {
                try {
                    f.getStream().close();
                } catch (Exception e) {
                }
            }
        }
        if (wrote) {
            writer.println();
        }
    }
}