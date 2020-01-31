package sw.tinyweb;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;

import org.apache.log4j.Logger;

/**
 * {@link RequestDispatcher} factory.
 * 
 * @author $Author: $
 * @version $Revision: $
 */
public class TinyWebRequestDispatcherFactory
{
	private static TinyWebRequestDispatcherFactory globalInstance;
	
	/** @return the global instance */
	public static TinyWebRequestDispatcherFactory getInstance()
	{
		if ( globalInstance == null )
		{
			globalInstance = new TinyWebRequestDispatcherFactory();
		}
		return globalInstance;
	}
	
	private static final Logger LOGGER = Logger.getLogger( TinyWebRequestDispatcherFactory.class );
	
	private TinyWebServer server;
	
	/**
	 * Create a new dispatcher.
	 * 
	 * @param aName The name
	 * @return the dispatcher or null (not found)
	 */
	public RequestDispatcher createRequestDispatcherByName(String aName)
	{
		try
		{
			Servlet targetServlet = null;
			if ( this.server != null )
			{
				targetServlet = this.server.createServletByName( this.server.getRootContext(), aName );
			}
			
			if ( targetServlet != null )
			{
				return new TinyWebRequestDispatcher( targetServlet, "/" );
			}
		}
		catch (Exception e)
		{
			LOGGER.error( "Cannot create request dispatcher", e);
		}
		return null;
	}

	/**
	 * Create a new dispatcher.
	 * 
	 * @param aPath The servlet URL
	 * @return the dispatcher
	 */
	public RequestDispatcher createRequestDispatcherByPath(String aPath)
	{
		try
		{
			Servlet targetServlet = null;
			if ( this.server != null )
			{
				targetServlet = this.server.createServletByPath( this.server.getRootContext(), aPath );
			}
			
			if ( targetServlet != null )
			{
				return new TinyWebRequestDispatcher( targetServlet, aPath );
			}
		}
		catch (Exception e)
		{
			LOGGER.error( "Cannot create request dispatcher", e);
		}
		return null;
	}
	
	/** @param aServer The new value */
	public void setServer(TinyWebServer aServer)
	{
		this.server = aServer;
	}
	
}
