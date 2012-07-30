package skyview.executive;


import java.util.*;
import java.util.regex.Pattern;

/** This class defines a singleton where SkyView preferences/settings
 *  Testchange...
 *  can be set and gotten from anywhere in the system.
 *  A setting is simply a key=string value.  When there
 *  is to be more than one value for the key it should be
 *  specified as string1,string2,string3.  A comma
 *  is not allowed as a character within a setting.
 *  Keys are case insensitive.<p>
 *  When specified in the command line Settings may sometimes be set 
 *  with just the keyword.  This is treated as equivalent to key=1.
 */
public class Settings {

    /** The hashmap storing the settings */
    private static HashMap<Key,String> single = new HashMap<Key,String>();
    
    /** A stack of saved versions of the settings. */
    private static Stack<HashMap<Key,String>> backup = new Stack<HashMap<Key,String>>();
    
    /** Used to split the hashmap */
    private static Pattern comma = Pattern.compile(",");
    
    /** Used to split the hashmap */
    private static Pattern equal = Pattern.compile("=");
    
    static {
	initializeSettings();
    }
    
    // Initialize the settings.
    // First look for the file indicated in
    // the SKYVIEW_SETTINGS environment variable.
    // Then try the file skyview.settings.
    // Then try the resource skyview.settings.
    // 
    static void initializeSettings() {
	
	String settingsFile = System.getenv("SKYVIEW_SETTINGS");
	
	if (settingsFile == null) {
	    settingsFile = "skyview.settings";
	}
	updateFromFile(settingsFile);
    }
    
    /** Try to read settings from a file */
    public static void updateFromFile(String settingsFile) {
	
	java.io.BufferedReader ir = null;
	
	try {
	    java.io.InputStream is =  skyview.survey.Util.getResourceOrFile(settingsFile);
		
	    if (is != null) {
		ir = new java.io.BufferedReader( new java.io.InputStreamReader(is));
	    }
	    
	} catch (Exception e) {
	    // Just continue...
	    System.err.println("Exception noted when attempting to open settings file/resource:"+settingsFile+"\n"+e);
	    ir = null;
	}
	
	if (ir != null) {
	    readFile(ir);
	} else {
	    System.err.println("Unable to find settings. Continuing with defaults.");
	}
    }
    
    static void readFile(java.io.BufferedReader ir) {
	try {
	    Pattern eq = Pattern.compile("=");
	    String line;
	    while ((line = ir.readLine()) != null) {
		
		line = line.trim();
		    
		if (line.length() < 2 || line.charAt(0) == '#') {
		    continue;
		}
		    
		String[] parse = eq.split(line, 2);
		    
		if (parse.length > 2) {
		    System.err.println("Unparseable line in input settings:\n   "+line);
		    continue;
		}
		String val = "1";
		if (parse.length == 2) {
		    val = parse[1];
		}
		char first = val.charAt(0);
		if (first == '$') {
		    // Look for the environment variable and use it.
		    val = System.getenv(val.substring(1));
		    if  (val == null) {
			continue;
		    }
		}
		put(Key.valueOfIgnoreCase(parse[0]), val);
	    }
	} catch (Exception e) {
	    System.err.println("Exception caught parsing settings:\n"+e);
	}
    }
    
    /** Add settings from a list of arguments.  This is probably the
     *  argument list given to main, but needn't be.
     */
    public static void addArgs(String[] args) {
	
    	for (String arg: args) {
	    
	    // Java seems sometimes to leave newline on last character
	    // of an argument list... This is probably a bug somewhere.
	    if (arg.length() > 0 && arg.charAt(arg.length()-1) == 13) {
		arg = arg.substring(0,arg.length()-1);
	    }
	    if (arg.length() == 0) {
		continue;
	    }
	  
	    String[] tokens=equal.split(arg, 2);
	    
	    if (tokens.length == 2) {
		String key = tokens[0];
		put(Key.valueOfIgnoreCase(key), tokens[1]);
		
	    } else {
		put(Key.valueOfIgnoreCase(arg), "1");
	    }
	}
    }
    
    /** Don't allow anyone else to create a settings object. */
    private Settings() {
    }
    
    /** Get a value corresponding to the key */
    public static String get(Key key) {
	if (key == null) {
	    return null;
	}
	return  single.get(key);
    }
    
    /** Get a values corresponding to a key or the default */
    public static String get(Key key, String dft) {
	String gt = get(key);
	if (gt == null) {
	    return dft;
	} else {
	    return gt;
	}
    }
    
    /** Get the values corresponding to a key as an array of strings.  Returns
      * null rather than a 0 length array if the value is not set.
      */
    public static String[] getArray(Key key) {
	String gt = get(key);
	if (gt == null) {
	    return new String[0];
	} else {
	    return comma.split(gt);
	}
    }
    
    /** This method works like put except that
     *  it does not add a pair if the keys is in the _nullvalues setting
     *  or if the Setting is already set (unless it is set to the
     *  special value "default")
     */
    public static void suggest(Key key, String value) {
	if (Settings.has(key)  && !"default".equalsIgnoreCase(Settings.get(key))) {
	    return;
	}
	if (Settings.has(Key._nullvalues)) {
	    String[] keys = Settings.getArray(Key._nullvalues);
	    for (String nullKey: keys) {
		if (nullKey.equals(key)) {
		    return;
		}
	    }
	}
	Settings.put(key, value);
    }
    
    /** Save a key and value */
    public static void put(Key key, String value) {

    if(key == null) throw new IllegalArgumentException("null key");

	if (value == null) {
	    value = "1";
	}
	if (value.equals("null")) {
	    Settings.add(Key._nullvalues, key.name()); //TODO key was put as value here, not sure what it is it for
	    single.remove(key);
	    return;
	}
	
	if (value.length() > 1 && (value.charAt(0) == '\'' || value.charAt(0) == '"')) {
	    char last = value.charAt(value.length()-1);
	    if (value.charAt(0) == last) {
		value = value.substring(1,value.length()-1);
	    }
	}
	
	single.put(key, value);
    }
    
    /** Save the current state of the settings for a later restoration */
    public static void save() {
	backup.push(single);
	single = (HashMap<Key,String>) single.clone();
    }
    
    /** Add a setting to a list -- but only if it is
     *  not already in the list.
     */
    public static void add(Key key, String value) {
	
	// If we try to add a null it's OK if it's the only
	// value, but we can't add it to a list sensibly.
	if (value == null) {
	    if (!Settings.has(key)) {
		Settings.put(key, value);
	    }
	    return;
	}
	
	String[] oldVals = Settings.getArray(key);
	
	// If the old value is an explicit null just replace it.
        if (oldVals.length == 1 && oldVals[0].equals("null")) {
	    Settings.put(key,value);
	    return;
	}
	
	String newValue = "";
	String comma    = "";
	for (int i=0; i<oldVals.length; i += 1) {
	    if (oldVals[i].equals(value)) {
	        return;
	    }
	    newValue += comma + oldVals[i];
	    comma = ",";
	}
	newValue += comma+value;
	Settings.put(key, newValue);
    }
    
    /** Check if the given key has been set */
    public static boolean has(Key key) {
	return single.containsKey(key);
    }
    
    /** Return the array of keys in the current settings */
    public static Key[] getKeys() {
	return single.keySet().toArray(new Key[0]);
    }
    
    /** Restore a previously saved state. */
    public static void restore() {
	
	if (!backup.empty()) {
	    single = backup.pop();
        } else {
	    System.err.println("Attempt to restore Settings ignored: No previous state saved.");
        }
    }
    
    /** Give a copy of the current settings and pop the stack */
    public static HashMap<Key, String> pop() {
	if (backup.size() > 0) {
	    HashMap<Key,String> curr = single;
	    restore();
	    return curr;
	} else {
	    System.err.println("Error: Attempt to pop Settings, but stack is empty");
	    return single;
	}
    }
    
    public static void push(HashMap<Key,String> top) {
	save();
	single = top;
    }


    /** print current settings, used for debugging*/
    public static void printSettings(){
        List keys = new ArrayList();
        for(Key k:single.keySet()){
            keys.add(k.name().toLowerCase());
        }

        Collections.sort(keys);
        System.out.println("Skyview settings:");
        for(Object k: keys){
            Key key = Key.valueOfIgnoreCase((String)k);
            System.out.println("   "+key+" = "+single.get(key));
        }
    }
}
