package sw.utils.xml.resolvers;

import java.io.InputStream;
import java.net.URL;

import org.apache.log4j.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import sw.utils.URLHelper;

/**
 * <code>EntityResolver</code> that can retrieve resources from a single
 * resource location.
 * 
 * @author Stewart Witchalls
 * @version 1.0
 */
public class SingleLocationEntityResolver implements EntityResolver
{
	private static final Logger LOGGER = Logger.getLogger( SingleLocationEntityResolver.class );
	
	private final URL baseURL;
	
	/**
	 * Constructor.
	 * 
	 * @param aBaseURL The location of all resources
	 */
	public SingleLocationEntityResolver(URL aBaseURL)
	{
		this.baseURL = aBaseURL;
	}
	
	/** @return the single location of all resources */
	public URL getBaseURL()
	{
		return this.baseURL;
	}
	
	@Override
	public InputSource resolveEntity(String aPublicId, String aSystemId)
	{
		if ( LOGGER.isDebugEnabled() )
		{
			LOGGER.debug( "resolveEntity("+aPublicId+","+aSystemId+")" );
		}
		
		try
		{			
			// If no DTD location has been stated (inside the XML file),
			// the systemId will be set to the browser's current working
			// directory (CWD) + required DTD

			final URL docURL = URLHelper.newURL( this.getBaseURL(), getResourceName(aSystemId) );
			final InputStream fin = docURL.openStream();
			return new InputSource( fin );
		}
		catch (Throwable e)
		{
			if ( LOGGER.isDebugEnabled() )
			{
				LOGGER.debug( "Cannot find systemId resource", e ); // with stack trace
			}
		}
		
		// Resource could not be found!
		//
		// Return null so that the parser will implement default behaviour.

		return null;
	}
	
	/**
	 * Extract the resource's file name from its URL.
	 * 
	 * @param aURL The URL for the resource
	 * @return the name
	 */
	protected String getResourceName(String aURL)
	{
		// Parser CWD would be systemId.substring(0,pos)

		final int pos = aURL.lastIndexOf("/");
		return (pos < 0) ? aURL : aURL.substring( pos+1 );
	}	
}
