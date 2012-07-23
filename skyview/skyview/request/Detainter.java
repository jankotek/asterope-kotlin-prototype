package skyview.request;
import java.util.regex.*;
import java.util.HashMap;


public abstract class Detainter {
    
   /* mapping of CGI form parameter to acceptable characters of its value */
   private HashMap<String, Pattern> patterns = new HashMap<String, Pattern>();


   public Detainter () {
       addPattern("negative", Pattern.compile("[><;`\\$]"));
   }

   /*   ----------------------------------------------------------------- */
   /*   Add new pattern to list.  Throws exception if name already exists */
   public Pattern getPattern (String name)  throws Exception {
      if (patterns.containsKey(name)){
           return patterns.get(name);
      }
      return null;
   }

   /*   ----------------------------------------------------------------- */
   /*   Add new pattern to list.  Replaces pattern if it already exists   */
   public void addPattern (String name, Pattern pattern) {
      if (patterns.containsKey(name)){
         System.err.println(" *Replacing pattern " + "==>" + name);
      }
      patterns.put(name, pattern);
   }
    
   

   public abstract boolean validate(String name, String input) 
							throws Exception; 
  

}
