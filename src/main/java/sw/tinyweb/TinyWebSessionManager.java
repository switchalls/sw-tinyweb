package sw.tinyweb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

/**
 * HTTP session manager.
 * 
 * @author $Author: $
 * @version $Revision: $
 * 
 * @see javax.servlet.http.HttpSession
 */
public class TinyWebSessionManager
{
	private static TinyWebSessionManager globalInstance;
	
	/** @return the global instance */
	public static TinyWebSessionManager getInstance()
	{
		if ( globalInstance == null )
		{
			globalInstance = new TinyWebSessionManager();
		}
		return globalInstance;
	}
	
	private final Map<String, TinyWebSession> sessions = new HashMap<String, TinyWebSession>();
	private ServletContext servletContext;
	private int nextSessionId = 0;
	
	/**
	 * Set the application context for all sessions.
	 * 
	 * @param aContext The context
	 */
	public void setServletContext(ServletContext aContext)
	{
		this.servletContext = aContext;
	}
	
	/**
	 * Find the stated session.
	 * 
	 * @param aId The identifier
	 * @return the session or null (not found)
	 */
	public TinyWebSession findSession(String aId)
	{
		return this.sessions.get( aId );
	}
	
	/**
	 * Create a new session.
	 * 
	 * @return the session
	 * 
	 * @see #setServletContext(ServletContext)
	 */
	public TinyWebSession createSession()
	{
		final String id = Integer.toString(++this.nextSessionId);
		final TinyWebSession s = new TinyWebSession( id, this.servletContext );
		this.sessions.put( s.getId(), s );
		return s;
	}
	
	/**
	 * Remove the stated session.
	 * 
	 * @param aId The session identifier
	 * @return the session or null (not found)
	 */
	public TinyWebSession removeSession(String aId)
	{
		return this.sessions.remove( aId );
	}
	
	/**
	 * Remove all sessions that have timed out.
	 */
	public void removeStaleSessions()
	{
		final List<String> copy = new ArrayList<String>();
		copy.addAll( this.sessions.keySet() );
		
		for ( String id : copy )
		{
			final TinyWebSession s = this.findSession( id );
			if ( s.isTimedOut() )
			{
				this.sessions.remove( id );
				s.invalidate();
			}
		}
	}

}
