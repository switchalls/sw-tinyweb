package sw.tinyweb;

import javax.servlet.http.Cookie;

/**
 * TinyWeb cookie.
 * 
 * <p>Provides additional attributes over standard cookie definition.
 * </p>
 * 
 * @author $Author: $
 * @version $Revision: $
 */
public class TinyWebCookie extends Cookie
{
	private boolean httpOnly;
	
	/**
	 * Constructor.
	 * 
	 * @param aName The cookie name
	 * @param aValue The cookie value
	 */
	public TinyWebCookie(String aName, String aValue)
	{
		super( aName, aValue );
	}
	
	/** @return false when this cookie can be used by cross-domain requests */
	public boolean isHttpOnly()
	{
		return this.httpOnly;
	}
	
	/** @param aFlag The new value */
	public void setHttpOnly(boolean aFlag)
	{
		this.httpOnly = aFlag;
	}

}
