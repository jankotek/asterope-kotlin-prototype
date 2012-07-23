package skyview.executive;

/** This interface defines an object
 *  that would like to take process the settings
 *  before processing begins.  E.g., this could
 *  be used to implement translation of settings
 *  from other languages.
 */
public interface SettingsUpdater {
    
    /** Update the global settings */
    public abstract void updateSettings();
}
