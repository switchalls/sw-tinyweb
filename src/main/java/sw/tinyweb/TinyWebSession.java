package sw.tinyweb;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;

import org.apache.log4j.Logger;

import sw.tinyweb.utils.IteratorEnumeration;

/**
 * A single HTTP session.
 * 
 * @author $Author: $
 * @version $Revision: $
 */
@SuppressWarnings("deprecation")
public class TinyWebSession implements HttpSession
{
	/** Cookie identifier. */
	public static final String SESSION_ID = "jsessionid";

	private static final Logger LOGGER = Logger.getLogger( TinyWebSession.class );
	
	private final Map<String, Object> attributes = new HashMap<String, Object>();
	private final long creationTime;
	private final String id;
	private long lastAccessedTime;
	private int maxInactiveInterval = (30 * 60); // 30 minutes
	private final ServletContext servletContext;
	private boolean sessionValid = true;
	
	/**
	 * Constructor.
	 * 
	 * @param aId The session identifier
	 * @param aContext The application context
	 */
	public TinyWebSession(String aId, ServletContext aContext)
	{
		this.creationTime = System.currentTimeMillis();
		this.id = aId;
		this.lastAccessedTime = creationTime;
		this.servletContext = aContext;
	}
	
	@Override
	public Object getAttribute(String aName)
	{
		this.checkSessionValidity();
		return this.attributes.get( aName );
	}

	@Override
	public Enumeration<String> getAttributeNames()
	{
		this.checkSessionValidity();
		return new IteratorEnumeration<String>( this.attributes.keySet() );
	}

	@Override
	public long getCreationTime()
	{
		this.checkSessionValidity();
		return this.creationTime;
	}

	@Override
	public String getId()
	{
		return this.id;
	}

	@Override
	public long getLastAccessedTime()
	{
		this.checkSessionValidity();
		return this.lastAccessedTime;
	}

	@Override
	public int getMaxInactiveInterval()
	{
		return this.maxInactiveInterval;
	}

	@Override
	public ServletContext getServletContext()
	{
		return this.servletContext;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated As of Version 2.1, no direct replacement
	 */
	@Deprecated
	@Override
	public HttpSessionContext getSessionContext()
	{
		throw new UnsupportedOperationException( "HttpSession.getSessionContext() not supported" );
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated As of Version 2.2, replaced by {@link #getAttribute(String)}
	 */
	@Deprecated
	@Override
	public Object getValue(String aName)
	{
		throw new UnsupportedOperationException( "HttpSession.getValue() not supported" );
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated As of Version 2.2, replaced by {@link #getAttributeNames()}
	 */
	@Deprecated
	@Override
	public String[] getValueNames()
	{
		throw new UnsupportedOperationException( "HttpSession.getValueNames() not supported" );
	}

	@Override
	public void invalidate()
	{
		this.checkSessionValidity();
		LOGGER.info( "Invalidating HTTP session " + this.id );
		this.sessionValid = false;		
	}

	@Override
	public boolean isNew()
	{
		return false; // not implemented
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated As of Version 2.2, replaced by {@link #setAttribute(String, Object)}
	 */
	@Deprecated
	@Override
	public void putValue(String aName, Object aValue)
	{
		throw new UnsupportedOperationException( "HttpSession.putValue() not supported" );
	}

	@Override
	public void removeAttribute(String aName)
	{
		this.checkSessionValidity();
		
		final Object value = this.attributes.remove( aName );
		if ( value instanceof HttpSessionBindingListener )
		{
			final HttpSessionBindingEvent evt = new HttpSessionBindingEvent( this, aName );
			((HttpSessionBindingListener)value).valueUnbound( evt );
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated As of Version 2.2, replaced by {@link #removeAttribute(String)}
	 */
	@Deprecated
	@Override
	public void removeValue(String aName)
	{
		throw new UnsupportedOperationException( "HttpSession.removeValue() not supported" );
	}

	@Override
	public void setAttribute(String aName, Object aValue)
	{
		this.checkSessionValidity();		
		this.removeAttribute( aName );
		
		if ( aValue != null )
		{
			this.attributes.put( aName, aValue );
			
			if ( aValue instanceof HttpSessionBindingListener )
			{
				final HttpSessionBindingEvent evt = new HttpSessionBindingEvent( this, aName );
				((HttpSessionBindingListener)aValue).valueBound( evt );
			}
		}
	}

	@Override
	public void setMaxInactiveInterval(int aSeconds)
	{
		this.maxInactiveInterval = aSeconds;
	}

	/**
	 * Has this session timed out?
	 * 
	 * @return true when yes
	 * 
	 * @see #getLastAccessedTime()
	 * @see #getMaxInactiveInterval()
	 */
	public boolean isTimedOut()
	{
		if ( this.getMaxInactiveInterval() == -1 )
		{
			return false;
		}
		
		final long timeIdle = (System.currentTimeMillis() - this.getLastAccessedTime()) / 1000;
		final long maxIdle = this.getMaxInactiveInterval();
		return ( timeIdle > maxIdle );
	}
	
	/**
	 * Tag this session as being used.
	 * 
	 * @see #getLastAccessedTime()
	 */
	public void sessionAccessed()
	{
		this.lastAccessedTime = System.currentTimeMillis();
	}
	
	/**
	 * Is this session still valid?
	 * 
	 * @throws IllegalStateException when the session is not valid
	 */
	protected void checkSessionValidity()
	throws IllegalStateException
	{
		if ( !this.sessionValid )
		{
			throw new IllegalStateException( "HTTP session "+this.getId()+" has been invalidated" );
		}
	}

}
