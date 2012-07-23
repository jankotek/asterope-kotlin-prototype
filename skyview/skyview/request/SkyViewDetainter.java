package skyview.request;
import java.util.regex.*;
import java.net.URLEncoder;
import java.net.MalformedURLException; 
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/** This class defines string patterns and rules that are used to do taint
 *  checking for Skyview CGI calls.
 *  A hash map of acceptable character patterns is created upon construction
 *  and are matched against user input associated with each CGI parameter.
 *  If the pattern for a CGI parameter is not found the input is checked
 *  for characters that are not allowed.  Some parameters such as survey 
 *  name are checked against a pattern of allowed characters and then 
 *  checked again to make sure certain sequesnced of allowed characters are 
 *  not present (eg. "<script>")
 */

public class SkyViewDetainter extends Detainter {
    
   /* Constructor builds pattern map*/
   public SkyViewDetainter ()  {
     super();

     /* Define CGI input/acceptable character pattern mapping */
     super.addPattern("catalogName", 
	Pattern.compile("[\\sA-Za-z0-9,_\\(\\)\\-\\/\\.\\[\\]a\\(\\)\\+\\&\\?\\:]+"));
    
     super.addPattern("surveyName", 
	Pattern.compile("[\\sA-Za-z0-9,_\\(\\)\\-\\<\\>\\/\\.\\:]+"));
    
    super.addPattern("alphaNumeric",
	Pattern.compile("[\\sA-Za-z0-9,_\\-\\.\\+\\(\\)\\/]+"));

    super.addPattern("string", 
	Pattern.compile("[\\sA-Za-z_\\-\\(\\)\\/,]+"));

    super.addPattern("file", 
	Pattern.compile("[\\sA-Za-z0-9_\\-\\/\\.\\+]+"));

    super.addPattern("url", 
	Pattern.compile("[\\s,A-Za-z0-9_\\-\\/\\.\\+\\:\\#\\%\\?]+"));

    super.addPattern("numeric", 
	Pattern.compile("[\\s0-9\\.\\-\\+]+"));

    super.addPattern("scientificnotation", 
	Pattern.compile("[e\\s0-9\\.\\-\\+,]+"));

    super.addPattern("default_range", 
	Pattern.compile("[\\sDdEeFfAaUuLlTt\\s0-9\\.\\-\\+,]+"));

    super.addPattern("numericRange", 
	Pattern.compile("[\\s0-9\\.\\-\\+,]+"));

    super.addPattern("position", 
	Pattern.compile("[\\sA-Za-z0-9,'_\\-\\.\\+:\\&\\[\\]\\*\\\"]+"));

    super.addPattern("script", Pattern.compile("<\\s*script\\s*>"));

   }

   /* ------------------------------------------------------------------- */
   /* Validates SkyView user input based on form element name             */
   public boolean validate(String name, String input) throws Exception {	
      //System.err.print(name + "->" + input);

      if (name.equalsIgnoreCase("survey") 
			|| name.equalsIgnoreCase("imredd")
                	|| name.equalsIgnoreCase("imgreen")
                	|| name.equalsIgnoreCase("imgree")
                	|| name.equalsIgnoreCase("imblue")
                	|| name.equalsIgnoreCase("contour")
                	|| name.equalsIgnoreCase("contours")) {
         //--- Survey name allows many more characters than most other input
         //--- values so check for allowed values and then check for 
         //--- combinations of allowed values that are invalid
         //System.err.println("  --using survey pattern");
         if (! isValid(super.getPattern("surveyName"), name, input)) {
            return false;
         }
         //System.err.println("  --using script pattern");
         return checkInvalid(super.getPattern("script"), name, input);

      } else if (name.equalsIgnoreCase("catalog")
                	|| name.equalsIgnoreCase("catlog")
                	|| name.equalsIgnoreCase("cataloglist")) {
         //--- Catalog allows many more characters than most other input
         //--- values (eg., URLs) so check for allowed values and then check 
         //--- for combinations of allowed values that are invalid
         //System.err.println("  --using catalog pattern");
         if (! isValid(super.getPattern("catalogName"), name, input)) {
            return false;
         }
         //System.err.println("  --using script pattern");
         return checkInvalid(super.getPattern("script"), name, input);

      } else if (name.equalsIgnoreCase("position")
			|| name.equalsIgnoreCase("scoord")
			|| name.equalsIgnoreCase("vcoord")){
         //--- Coordinates
         //System.err.println("  --using position pattern");
         return isValid(super.getPattern("position"), name, input);

      } else if (name.equalsIgnoreCase("smooth") 
		|| name.equalsIgnoreCase("rotation") 
		|| name.equalsIgnoreCase("catalogradius") 
		|| name.equalsIgnoreCase("contoursmooth") 
		|| Pattern.compile("^.*BIN|STR|END$").matcher(name).find()  
		|| name.equalsIgnoreCase("contourmin") 
		|| name.equalsIgnoreCase("contournumber") 
		|| name.equalsIgnoreCase("is_smooth") 
		|| name.equalsIgnoreCase("is_rotation") 
		|| name.equalsIgnoreCase("is_degtext") 
		|| name.equalsIgnoreCase("savebysurvey") 
		|| name.equalsIgnoreCase("contourmax")){
         //--- Numbers 
         //System.err.println("  --using numeric pattern");
         return isValid(super.getPattern("numeric"),  name, input);

      } else if (name.equalsIgnoreCase("filename") 
			|| name.equalsIgnoreCase("file") 
			|| name.equalsIgnoreCase("surveymanifest") 
			|| name.equalsIgnoreCase("lut") 
			|| name.equalsIgnoreCase("cache") 
			|| name.equalsIgnoreCase("outputroot") 
			|| name.equalsIgnoreCase("webrootpath") 
			|| name.equalsIgnoreCase("lutcbarpath") 
			|| name.equalsIgnoreCase("htmlwriter") 
			|| name.equalsIgnoreCase("rgbwriter") 
			|| name.equalsIgnoreCase("surveytemplate") 
			|| name.equalsIgnoreCase("nullimagedir") 
			|| name.equalsIgnoreCase("descriptionxslt") 
			|| name.equalsIgnoreCase("galleryxslt") 
			|| name.equalsIgnoreCase("headertemplate") 
			|| name.equalsIgnoreCase("rgbtemplate") 
			|| name.equalsIgnoreCase("xmlroot") 
			|| name.equalsIgnoreCase("surveysheader") 
			|| name.equalsIgnoreCase("footertemplate") 
			|| name.equalsIgnoreCase("output")){
         //--- File name
         //System.err.println("  --using filename pattern");
         return isValid(super.getPattern("file"), name, input);

      } else if (name.equalsIgnoreCase("localurl")
			||  name.equalsIgnoreCase("catalogurl")) {
         //--- url
         //System.err.println("  --using url pattern for " + name);
         return isValid(super.getPattern("url"), name, input);

      } else if (name.equalsIgnoreCase("catalogfilter") ) {
         //--- may be redundant
         //System.err.println("  --using script pattern");
         return checkInvalid(super.getPattern("script"), name, input);

      } else if (name.equalsIgnoreCase("plotcolor")
			|| name.equalsIgnoreCase("catalogids")
			|| name.equalsIgnoreCase("nwindow")
			|| name.equalsIgnoreCase("submit")
			|| name.equalsIgnoreCase("namres")
			|| name.equalsIgnoreCase("resolver")){
         //--- Miscellaneous string input values
         //System.err.println("  --using string pattern");
         return isValid(super.getPattern("string"), name, input);

      } else if (name.equalsIgnoreCase("resamp") 
			|| name.equalsIgnoreCase("sampler") 
			|| name.equalsIgnoreCase("projection")
			|| name.equalsIgnoreCase("spcoordinates")
			|| name.equalsIgnoreCase("coordinates")
			|| name.equalsIgnoreCase("equinox")
			|| name.equalsIgnoreCase("equinx")
			|| name.equalsIgnoreCase("pxlcnt")
			|| name.equalsIgnoreCase("deedger")
			|| name.equalsIgnoreCase("coordselect")
			|| name.equalsIgnoreCase("interface")
			|| name.equalsIgnoreCase("grid")
			|| name.equalsIgnoreCase("griddd")
			|| name.equalsIgnoreCase("gridd")
			|| name.equalsIgnoreCase("gridlabels")
			|| name.equalsIgnoreCase("cs")
			|| name.equalsIgnoreCase("invert")
			|| name.equalsIgnoreCase("float")
			|| name.equalsIgnoreCase("compress")
			|| name.equalsIgnoreCase("cont_scaling")
			|| name.equalsIgnoreCase("scaling")
			|| name.equalsIgnoreCase("surveyfinder")
			|| name.equalsIgnoreCase("settingsupdaters")
			|| name.equalsIgnoreCase("finalpostprocessor")
			|| name.equalsIgnoreCase("quicklook")
			|| name.equalsIgnoreCase("nullimages")
			|| name.equalsIgnoreCase("return")
			|| name.equalsIgnoreCase("coltab")
			|| name.equalsIgnoreCase("nofits")
			|| name.equalsIgnoreCase("iscaln")
                        || name.equalsIgnoreCase("maproj")){
         //--- Alpha numeric input values
         //System.err.println("  --using alphaNumeric pattern");
         return isValid(super.getPattern("alphaNumeric"), name, input);

      } else if (name.equalsIgnoreCase("size") 
			|| name.equalsIgnoreCase("imscale")
                        || name.equalsIgnoreCase("sfactr")){
         //--- Numeric number or range of numbers
         //System.err.println("  --using numericrange pattern");
         return ( isValid(super.getPattern("default_range"), name, input));

      } else if (name.equalsIgnoreCase("pixelx") 
			|| name.equalsIgnoreCase("pixely") 
			|| name.equalsIgnoreCase("pixels")
			|| name.equalsIgnoreCase("iosmooth")
			|| name.equalsIgnoreCase("ebins")) {
         //--- Numeric number or range of numbers
         //System.err.println("  --using numericrange pattern");
         return isValid(super.getPattern("numericRange"), name, input);

      } else {
         //--- Everything else
         System.err.println("  --using negative pattern for " + name +
		"-->"+input);
         return checkInvalid(super.getPattern("negative"), name, input);
      }

   }

   /* ------------------------------------------------------------------- */
   /* Validates input using appropriate pattern                           */
   public boolean isValid(Pattern pattern, String name, String input ) 
							   throws Exception {	
      if ((input != null && ! input.equals("") && ! input.equals("null")) 
                         && ! pattern.matcher(input).matches()){
         System.err.println("*Input validation failed for " +
		name +"==>" + input + ", pattern=" + pattern.pattern());
         String msg ="Input value";

         if (input !=null) {msg += " for " + name;}  

         msg += " is not valid";
         throw new Exception(msg);
      }

      return true;
   }

   /* Validates input by checking for invalid characters */
   public boolean checkInvalid(Pattern pattern, String name, String input ) 
							   throws Exception {	
      if ( pattern.matcher(input).find()) {
         System.err.println("*Input validation failed for " +
		name +"==>" + input + ", pattern=" + pattern.pattern());
         String msg ="Input value";
         if (name !=null) {msg += " for " +name;}  

         msg += " is not valid";
         throw new Exception(msg);
     }  

     return true;
   }
}
