package skyview.survey;

/** This interface defines the role of something that
 *  can find a survey given a survey id.
 */


public interface SurveyFinder {

    /** Do we have this survey? */
    public abstract Survey find(String id);
    
    /** What are the IDs available? */
    public abstract String[] getSurveys();
    
}
