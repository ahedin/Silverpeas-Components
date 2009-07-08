/*--- formatted by Jindent 2.1, (www.c-lab.de/~jindent) ---*/

/*
 * JobDomainPeasException.java
 */

package com.stratelia.silverpeas.infoLetter;

import com.stratelia.webactiv.util.exception.SilverpeasTrappedException;

/**
 * Class declaration
 *
 *
 * @author
 */
public class InfoLetterPeasTrappedException extends SilverpeasTrappedException
{

    /**
     * Constructor declaration
     * 
     * 
     * @param callingClass
     * @param errorLevel
     * @param message
     * 
     * @see
     */
    public InfoLetterPeasTrappedException(String callingClass, int errorLevel, String message)
    {
        super(callingClass, errorLevel, message);
    }

    /**
     * Constructor declaration
     * 
     * 
     * @param callingClass
     * @param errorLevel
     * @param message
     * @param extraParams
     * 
     * @see
     */
    public InfoLetterPeasTrappedException(String callingClass, int errorLevel, String message, String extraParams)
    {
        super(callingClass, errorLevel, message, extraParams);
    }

    /**
     * Constructor declaration
     * 
     * 
     * @param callingClass
     * @param errorLevel
     * @param message
     * @param nested
     * 
     * @see
     */
    public InfoLetterPeasTrappedException(String callingClass, int errorLevel, String message, Exception nested)
    {
        super(callingClass, errorLevel, message, nested);
    }

    /**
     * Constructor declaration
     * 
     * 
     * @param callingClass
     * @param errorLevel
     * @param message
     * @param extraParams
     * @param nested
     * 
     * @see
     */
    public InfoLetterPeasTrappedException(String callingClass, int errorLevel, String message, String extraParams, Exception nested)
    {
        super(callingClass, errorLevel, message, extraParams, nested);
    }

    /**
     * Method declaration
     * 
     * 
     * @return
     * 
     * @see
     */
    public String getModule()
    {
        return "infoLetter";
    }

}