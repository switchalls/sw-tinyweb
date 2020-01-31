package sw.utils.xml;

/**
 * XML related exception.
 * 
 * @author Stewart Witchalls
 * @version 1.0
 */
public class XMLException extends org.xml.sax.SAXException
{
	private static final long serialVersionUID = -1543385663786988077L;

	private Throwable cause = null;
    
    /**
     * Constructor.
     * 
     * @param aMsg The error message
     */
    public XMLException(String aMsg)
    {
        super( aMsg );
    }

    /**
     * Constructor.
     * 
     * @param aMsg The error message
     * @param aCause The reason this error occurred
     */
    public XMLException(String aMsg, Throwable aCause)
    {
        super( aMsg );
        this.cause = aCause;
    }
    
    /** @return the reason this error occurred */
    @Override
	public Throwable getCause()
    {
        return this.cause;
    }
}


