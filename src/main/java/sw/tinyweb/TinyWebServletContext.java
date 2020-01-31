package sw.tinyweb;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;

import sw.tinyweb.utils.IteratorEnumeration;

/**
 * A single HTTP application context.
 * 
 * @author $Author: $
 * @version $Revision: $
 */
public class TinyWebServletContext implements ServletContext
{
	private static final Logger LOGGER = Logger.getLogger( TinyWebServletContext.class );
	
	private static final Map<String,String> MIME_TYPES = new HashMap<String, String>();
	static
	{
		MIME_TYPES.put( "pdf",	"application/pdf" );

		MIME_TYPES.put( "bmp",	"image/bmp" );
		MIME_TYPES.put( "gif",	"image/gif" );
		MIME_TYPES.put( "jpg",	"image/jpeg" );
		MIME_TYPES.put( "png",	"image/png" );
		MIME_TYPES.put( "svg",	"image/svg+xml" );
		
		MIME_TYPES.put( "css",	"text/css" );
		MIME_TYPES.put( "htm",	"text/html" );
		MIME_TYPES.put( "html",	"text/html" );
		MIME_TYPES.put( "js",	"text/javascript" );
		MIME_TYPES.put( "xml",	"text/xml" );
	}

	private final Map<String, Object> attributes = new HashMap<String, Object>();
	private final List<ServletContextAttributeListener> contextAttributeListeners = new ArrayList<ServletContextAttributeListener>();
	private final long createdTime;
	private final String displayName;
	private final Map<String, String> initParams = new HashMap<String, String>();
	private final String serverInfo;
	private final File webContentHome;
	
	/**
	 * Constructor.
	 * 
	 * @param aDisplayName The context display name
	 * @param aServerInfo The server information, eg. name and version
	 * @param aWebContentHome The location of the folder containing web content
	 */
	public TinyWebServletContext(String aDisplayName, String aServerInfo, File aWebContentHome)
	{
		this.createdTime = Calendar.getInstance().getTimeInMillis();
		this.displayName = aDisplayName;
		this.serverInfo = aServerInfo;
		this.webContentHome = aWebContentHome;
	}
	
	@Override
	public Object getAttribute(String aName)
	{
		return this.attributes.get( aName );
	}

	@Override
	public Enumeration<String> getAttributeNames()
	{
		return new IteratorEnumeration<String>( this.attributes.keySet() );
	}

	@Override
	public ServletContext getContext(String aURL)
	{
		return null; // application contexts not supported
	}

	@Override
	public String getContextPath()
	{
		return ""; // root context
	}

	@Override
	public String getInitParameter(String aName)
	{
		return this.initParams.get( aName );
	}

	@Override
	public Enumeration<String> getInitParameterNames()
	{
		return new IteratorEnumeration<String>( this.initParams.keySet() );
	}

	@Override
	public int getMajorVersion()
	{
		return 2; // servlet API 2.5
	}

	@Override
	public String getMimeType(String aFile)
	{
		String mtype = null;

	    final int ipos = aFile.lastIndexOf(".");
	    if ( ipos > 0 )
	    {
		    final String ext = aFile.substring(ipos+1).toLowerCase();
		    mtype = MIME_TYPES.get(ext);
	    }

	    return (mtype != null) ? mtype : "text/plain";
	}

	@Override
	public int getMinorVersion()
	{
		return 5; // servlet API 2.5
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String aName)
	{
		return TinyWebRequestDispatcherFactory.getInstance().createRequestDispatcherByName( aName );
	}

	@Override
	public String getRealPath(String aPath)
	{
		return aPath; // path mapping not supported
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String aPath)
	{
		return TinyWebRequestDispatcherFactory.getInstance().createRequestDispatcherByPath( aPath );
	}

	@Override
	public URL getResource(String aPath) throws MalformedURLException
	{
		String p = aPath;
		if ( aPath.startsWith("/") )
		{
			p = aPath.substring( 1 );
		}
		
		final File f = new File( this.webContentHome, p );
		if ( f.exists() )
		{
			return f.toURI().toURL();
		}
		
		final URL url = this.getClass().getResource( aPath );
		if ( url == null )
		{
			LOGGER.warn("Cannot find resource " + aPath);
		}
		
		return url;
	}
	
	@Override
	public InputStream getResourceAsStream(String aPath)
	{
		try
		{
			final URL url = this.getResource( aPath );
			if ( url != null )
			{
				return url.openStream();
			}
		}
		catch (IOException e)
		{
			LOGGER.error( "Cannot open resource " + aPath );
		}
		return null;
	}

	/** @return when was this context created? */
	public long getCreatedTime()
	{
		return this.createdTime;
	}
	
	/**
	 * When was the stated resource last modified?
	 * 
	 * <p>Returns {@link #getCreatedTime() context creation time} when the
	 * resource has no last-modified timestamp associated with it.
	 * </p>
	 * 
	 * @param aPath The resource
	 * @return the time or -1 (resource not found)
	 * @throws MalformedURLException when valid URLs cannot be created
	 * 
	 * @see #getResource(String)
	 */
	public long getResourceLastModified(String aPath) throws MalformedURLException
	{
		final URL url = this.getResource( aPath );
		if ( url == null )
		{
			// invalid resource
			return -1;
		}
		
		if ("file".equals(url.getProtocol()))
		{
			// last-modified timestamp available
			final File f = new File(url.getFile());
			return f.lastModified();
		}
		
		// last-modified timestamp not available
		return this.getCreatedTime();
	}

	@Override
	public Set<String> getResourcePaths(String aPath)
	{
		File folder;
		
		if ( "/".equals(aPath) )
		{
			folder = this.webContentHome;
		}
		else
		{
			String p = aPath;
			if ( aPath.startsWith("/") )
			{
				p = aPath.substring( 1 );
			}
			
			folder = new File( this.webContentHome, p );
		}

		final File[] subFolders = folder.listFiles( new FileFilter()
		{	
			@Override
			public boolean accept(File aPathname)
			{
				return aPathname.isDirectory();
			}
		});
		
		final TreeSet<String> results = new TreeSet<String>();
		for ( File f : subFolders )
		{
			results.add( f.getPath() );
		}
		
		return results;
	}

	@Override
	public String getServerInfo()
	{
		return this.serverInfo;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated As of Java Servlet API 2.1, no direct replacement
	 */
	@Deprecated
	@Override
	public Servlet getServlet(String aName) throws ServletException
	{
		throw new UnsupportedOperationException( "ServletContext.getServlet() not supported" );
	}

	@Override
	public String getServletContextName()
	{
		return this.displayName;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated As of Java Servlet API 2.1, no direct replacement
	 */
	@Deprecated
	@Override
	public Enumeration<String> getServletNames()
	{
		throw new UnsupportedOperationException( "ServletContext.getServletNames() not supported" );
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated As of Java Servlet API 2.0, no direct replacement
	 */
	@Deprecated
	@Override
	public Enumeration<Servlet> getServlets()
	{
		throw new UnsupportedOperationException( "ServletContext.getServlets() not supported" );
	}

	@Override
	public void log(String aMsg)
	{
		LOGGER.info( aMsg );
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated As of Java Servlet API 2.1, replaced by {@link #log(String, Throwable)}
	 */
	@Deprecated
	@Override
	public void log(Exception aError, String aMsg)
	{
		throw new UnsupportedOperationException( "ServletContext.log(Exception, String) not supported" );
	}

	@Override
	public void log(String aMsg, Throwable aError)
	{
		LOGGER.error( aMsg, aError );
	}

	@Override
	public void removeAttribute(String aName)
	{
		this.attributes.remove( aName );
		this.fireAttributeRemoved( aName );
	}

	@Override
	public void setAttribute(String aName, Object aValue)
	{
		if ( aValue == null )
		{
			this.removeAttribute( aName );
		}
		else
		{
			final boolean isReplace = this.attributes.containsKey( aName );

			this.attributes.put( aName, aValue );

			if ( isReplace )
			{
				this.fireAttributeReplaced( aName, aValue );
			}
			else
			{
				this.fireAttributeAdded( aName, aValue );
			}
		}
	}

	/**
	 * Add a new listener.
	 * 
	 * @param aListener The listener
	 */
	public void addServletContextAttributeListener(ServletContextAttributeListener aListener)
	{
		this.contextAttributeListeners.add( aListener );
	}
	
	/**
	 * Add new listeners.
	 * 
	 * @param aListeners The listeners
	 */
	public void addServletContextAttributeListener(List<ServletContextAttributeListener> aListeners)
	{
		this.contextAttributeListeners.addAll( aListeners );
	}

	/**
	 * Remove the stated listener.
	 * 
	 * @param aListener The listener
	 */
	public void removeServletContextAttributeListener(ServletContextAttributeListener aListener)
	{
		this.contextAttributeListeners.remove( aListener );
	}
	
	/**
	 * Inform listeners that an attribute has been added.
	 * 
	 * @param aName The attribute name
	 * @param aValue The attribute value
	 */
	protected void fireAttributeAdded(String aName, Object aValue)
	{
		final ServletContextAttributeEvent evt = new ServletContextAttributeEvent( this, aName, aValue );
		for ( ServletContextAttributeListener l : this.contextAttributeListeners )
		{
			l.attributeAdded( evt );
		}
	}

	/**
	 * Inform listeners that an attribute has been removed.
	 * 
	 * @param aName The attribute name
	 */
	protected void fireAttributeRemoved(String aName)
	{
		final ServletContextAttributeEvent evt = new ServletContextAttributeEvent( this, aName, null );
		for ( ServletContextAttributeListener l : this.contextAttributeListeners )
		{
			l.attributeRemoved( evt );
		}
	}

	/**
	 * Inform listeners that an attribute has been replaced.
	 * 
	 * @param aName The attribute name
	 * @param aValue The new value
	 */
	protected void fireAttributeReplaced(String aName, Object aValue)
	{
		final ServletContextAttributeEvent evt = new ServletContextAttributeEvent( this, aName, aValue );
		for ( ServletContextAttributeListener l : this.contextAttributeListeners )
		{
			l.attributeReplaced( evt );
		}
	}

}
