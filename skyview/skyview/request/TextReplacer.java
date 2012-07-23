package skyview.request;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.FileReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/** This class provides a utility function that transforms strings
 *  to replace embedded settings with the current values of the setting.
 */
public class TextReplacer extends Reader {

    /** Look for instances of $xx or ${xx} */
    private static Pattern settingPattern = Pattern.compile("\\$\\{(\\w+)\\}");

    private Map<String,String> fields;
    private String             currentLine = "";
    private int                offsetWithinLine = 1;
    private BufferedReader     bufInput;
    private boolean            eof = false;


    /**
     * Create an object where we'll filter an input stream
     * replacing variables with values.
     * @param fields   The map from variable keys to output values.
     * @param input    The input Reader to be filtered.
     */
    public TextReplacer(Map<String,String> fields, Reader input) throws IOException {
        this.bufInput = new BufferedReader(input);
        this.fields = fields;
    }

    private void updateLine() throws java.io.IOException {
        if (eof) {
            throw new EOFException("Read past EOF");
        }
        String line = bufInput.readLine();
        if (line == null) {
            eof = true;
        } else {
        }

        if (line != null) {
            currentLine = replace(line);
        } else {
            currentLine = null;
        }
        offsetWithinLine = 0;
    }

    public String readLine() throws IOException {

        if (currentLine == null) {
            return null;
        } else if (eol()) {
            updateLine();
        }

        if (currentLine == null) {
            return null;
        } else {
            offsetWithinLine = currentLine.length();
            return currentLine.substring(offsetWithinLine);
        }
    }

    public boolean ready() throws IOException {
        return !eol() || super.ready();
    }

    public int read() throws IOException {
        if (currentLine == null) {
            return -1;
        }
        if (eol()) {
            updateLine();
            return '\n';
        }
        offsetWithinLine += 1;
        return currentLine.charAt(offsetWithinLine-1);
    }

    public long skip(long val) throws IOException {
        int cnt = 0;
        for (int i=0; i<val; i += 1) {
            int v = read();
            if (v == -1) {
                break;
            }
            cnt += 1;
        }
        return cnt;
    }

    public int read(char[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    public int read(char[] buf, int offset, int len) throws IOException {

        int cnt = 0;
        for (int i=0; i<len; i += 1) {
            int v = read();
            if (v < 0) {
                if (cnt == 0) {
                    return -1;
                } else {
                    return cnt;
                }
            }
            buf[offset+i] = (char) v;
            cnt += 1;
        }
        return cnt;
    }

    private boolean eol() {
        return offsetWithinLine >= currentLine.length();
    }


    /** Replace all settings variables with their values.
     *  This function takes the current line and looks for all
     *  variabless that are embedded in the string and replaces those
     *  variables with their value.  E.g., given
     *  a string "The survey is $survey and the scale is $scale."
     *  the method will replace the string $survey with the specifid
     *  value of survey and $scale with the specified value of scale.
     *  If an unmatched variable is found, the entire line is returned as a "".
     *  This allows one to control which lines are returned by the
     *  presence or absence of variables.
     *
     */
    public String replace(String input) {
	
	Matcher match = settingPattern.matcher(input);
	int     offset = 0;
	StringBuffer sb = new StringBuffer();
	
	while (match.find()) {
            String fld = match.group(1);
            String repl = fields.get(fld);
            if (repl == null) {
                return "";
            }
	    match.appendReplacement(sb, repl);
	}
	match.appendTail(sb);
	return sb.toString();
    }

    /** Usage: TextReplacer  filename key=val [key=val] ...*/
    public static void main(String[] args) throws Exception {
        Reader rdr = new FileReader(args[0]);
        Map<String,String> fields = new HashMap<String,String>();
        for (int i=1; i<args.length; i += 1) {
            String[] elements = args[i].split("=", 2);
            fields.put(elements[0], elements[1]);
        }
        BufferedReader xr = new BufferedReader(new TextReplacer(fields, rdr));
        String line = null;
        while ( (line = xr.readLine())!= null) {
            System.out.println(line);
        }
    }

    public void close() throws IOException {
        bufInput.close();
    }
}