package sw.utils.xml.handlers;

import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Simple error handler for a SAX based parser.
 * 
 * <p>Logs exceptions.
 * </p>
 * 
 * @author Stewart Witchalls
 * @version 1.0
 */
public class SimpleErrorHandler implements ErrorHandler
{
	private static final Logger LOGGER = Logger.getLogger( SimpleErrorHandler.class );
	
    @Override
	public void warning(SAXParseException aError)
    throws SAXException
    {
    	final String msg = getErrorMessage(aError);
        LOGGER.warn(msg);
    }

    @Override
	public void error(SAXParseException aError)
    throws SAXException
    {
    	final String msg = getErrorMessage(aError);
        LOGGER.error(msg);
        throw new SAXException(aError);
    }

    @Override
	public void fatalError(SAXParseException aError)
    throws SAXException
    {
    	final String msg = getErrorMessage(aError);
        LOGGER.fatal(msg);
        throw new SAXException(aError);
    }

	/**
	 * Generate the human readable error message for the stated SAX error.
	 * 
	 * @param aError The error
	 * @return the message
	 */
    public String getErrorMessage(SAXParseException aError)
    {
        return "XML parse error, cause=[" + aError.getMessage()
            + "], line=[" + aError.getLineNumber()
            + "], col=[" + aError.getColumnNumber()
            + "]";
    }    
}

