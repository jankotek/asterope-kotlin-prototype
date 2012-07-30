package skyview.request;

import skyview.executive.Key;
import skyview.executive.Settings;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** This class provides a utility function that transforms strings
 *  to replace embedded settings with the current values of the setting.
 */
public class SettingsMatcher {
   
    private static Pattern settingPattern = Pattern.compile("\\$\\w+");

    /** Replace all settings variables with their values.
     *  This function takes the input string and looks for all
     *  settings that are embedded in the string and replaces those
     *  settings with the value of that setting.  E.g., given
     *  a string "The survey is $survey and the scale is $scale."
     *  the method will look for the settings survey and scale
     *  and in the global Settings and replace these substrings
     *  with their current values.  E.g., the previous might return
     *  with "The survey is Digitized Sky Survey and the scale is 0.00133."
     *  If a setting has multiple values, only the first is used.
     *  If no value is found for the setting, then the text is
     *  left unchanged.
     */
    public static String replaceSettings(String input) {
	
	Matcher match = settingPattern.matcher(input);
	int     offset = 0;
	StringBuffer sb = new StringBuffer();
	
	while (match.find()) {
	    match.appendReplacement(sb, getSetting(match.group()));
	}
	match.appendTail(sb);
	return new String(sb);
    }
    
    private static String getSetting(String key) {
	if (key.length() < 2) {
	    return key;
	}
	
        String val = Settings.get(Key.valueOfIgnoreCase(key.substring(1)));
	if (val != null) {
	    return val;
	} else {
	    return "\\"+key;
	}
    }
    
    public static void main(String[] args) {
	System.out.println("Input=\n"+args[0]+"\n");
	System.out.println("Output=\n"+replaceSettings(args[0]));
    }
}
		
		
	
	
	
