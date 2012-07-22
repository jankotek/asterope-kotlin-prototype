/**
 * This class defines the basics structural methods which all
 * Java classes are required to include.  These include
 * metadata and serialization requirements.
 */

package org.asterope.util;

public interface Component{
    
    /** Get the name of this component. */
    public abstract String getName();
    
    /** Get the description of this component. */
    public abstract String getDescription();
}
