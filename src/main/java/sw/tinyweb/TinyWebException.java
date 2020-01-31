package sw.tinyweb;

/**
 * TinyWeb exception.
 * 
 * @author $Author: $
 * @version $Revision: $
 */
public class TinyWebException extends Exception
{
	private static final long serialVersionUID = -4828564365190691512L;

	private final int errorCode;

	/**
	 * Constructor.
	 * 
	 * @param aCode The HTTP status code
	 * @param aMsg The error message
	 */
	public TinyWebException(int aCode, String aMsg)
	{
		super( aMsg );
		this.errorCode = aCode;
	}
	
	/**
	 * Constructor.
	 * 
	 * @param aCode The HTTP status code
	 * @param aMsg The error message
	 * @param aCause The reason
	 */
	public TinyWebException(int aCode, String aMsg, Throwable aCause)
	{
		super( aMsg, aCause );
		this.errorCode = aCode;
	}

	/** @return HTTP status code */
	public int getErrorCode()
	{
		return this.errorCode;
	}
	
}
