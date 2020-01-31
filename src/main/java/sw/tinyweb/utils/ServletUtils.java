package sw.tinyweb.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * {@link javax.servlet.Servlet} utilities.
 *
 * @author $Author: $
 * @version $Revision: $
 */
public class ServletUtils
{	
	private static final String HTTP_URL_FORMAT = "http://%s:%s%s/%s;jsessionid=%s";
	private static final Logger LOGGER = Logger.getLogger( ServletUtils.class );

	private static final HashMap<String,String> MIME_TYPES = new HashMap<String,String>();
	static
	{
		MIME_TYPES.put( "pdf", "application/pdf" );
		MIME_TYPES.put( "bmp", "image/bmp" );
		MIME_TYPES.put( "gif", "image/gif" );
		MIME_TYPES.put( "jpg", "image/jpg" );
		MIME_TYPES.put( "png", "image/png" );
		MIME_TYPES.put( "svg", "image/svg+xml" );
		MIME_TYPES.put( "xml", "text/xml" );
	}

	/**
	 * Turn off page caching.
	 * 
	 * @param aResp The HTTP response
	 */
	public static void disablePageCaching(HttpServletResponse aResp)
	{
		aResp.setDateHeader("Expires", 1);

		// HTTP 1.0
		aResp.setHeader("Pragma", "no-cache");

		// HTTP 1.1
		//
		// IE uses "no-cache"
		// Firefox uses "no-store"
		//
		aResp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
	}

	/**
	 * Copy the contents of the input stream to the stated output stream.
	 * 
	 * @param aIn The input
	 * @param aOut The output
	 * @throws IOException when data cannot be read or written
	 */
	public static void copyContent(InputStream aIn, OutputStream aOut)
	throws IOException
	{
		final byte[] buf = new byte[1024];
		int n;
		
		while ( (n = aIn.read(buf)) > -1 )
		{
			aOut.write( buf, 0, n );
		}
	}

	/**
	 * Copy the contents of the input stream to the HTTP response.
	 * 
	 * @param aIn The input
	 * @param aResp The output
	 * @throws IOException when data cannot be read or written
	 */
	public static void copyContent(InputStream aIn, ServletResponse aResp)
	throws IOException
	{
		copyContent( aIn, aResp.getOutputStream() );
	}

	/**
	 * Read the contents of the input stream into a string.
	 * 
	 * @param aIn The input
	 * @return the contents
	 * @throws IOException when data cannot be read
	 */
	public static String readContent(InputStream aIn)
	throws IOException
	{
		final StringBuffer sb = new StringBuffer();
		final byte[] buf = new byte[1024];
		int n;
		
		while ( (n = aIn.read(buf)) > -1 )
		{
			sb.append( new String(buf, 0, n) );
		}
		
		return sb.toString();
	}

	/**
	 * Close the stated stream.
	 * 
	 * @param aIn The stream (can be null)
	 */
	public static void closeStream(InputStream aIn)
	{
		try
		{
			if ( aIn != null )
			{
				aIn.close();
			}
		}
		catch (Exception e)
		{
			// ignore
		}
	}

	/**
	 * Get the MIME-TYPE associated with the stated resource.
	 * 
	 * <p>Uses the file extension, eg. <code>XYZ.gif</code> =
	 * <code>image/gif</code>.
	 * </p>
	 * 
	 * <p>Defaults to <code>text/plain</code> for unrecognised
	 * file extensions.
	 * </p>
	 * 
	 * @param aPath The resource
	 * @return the mime type
	 */
	public static String getMimeType(String aPath)
	{
		String mtype = null;

	    final int ipos = aPath.indexOf(".");
	    if ( ipos > 0 )
	    {
		    final String ext = aPath.substring(ipos+1).toLowerCase();
		    mtype = MIME_TYPES.get(ext);
	    }

	    return (mtype != null) ? mtype : "text/plain";
	}

    /**
     * Generate the correct URL for the stated action.
     * 
     * <p>URL format: <b>http://</b> &lt;server name&gt;
     * <b>:</b> &lt;server port&gt; &lt;context path&gt;
     * <b>/</b> &lt;action name&gt;
     * <b>;</b> jsessionid= &lt;session id&gt;
     * </p>
     * 
     * @param aReq The HTTP request
     * @param aActionName The target action
     * @param aSessionId The identifier for this HTTP session
     * @return the URL
     */
    public static String generateUrl(
    		HttpServletRequest aReq,
    		String aActionName,
    		String aSessionId) 
    {
    	String cpath = aReq.getContextPath();
    	if ( cpath == null )
    	{
    		cpath = "";
    	}

        final String url = String.format(
        	HTTP_URL_FORMAT, aReq.getServerName(), aReq.getServerPort(),
        	cpath, aActionName, aSessionId
        );    	
        
    	LOGGER.debug( "Generated URL: {}"+url );
        return url;
    }

    /**
     * Generate the correct URL for the stated action.
     * 
     * @param aReq The HTTP request
     * @param aActionName The target action
     * @param aSessionId The identifier for this HTTP session
     * @param aParams The URL parameters
     * @return the URL
     * 
     * @see #generateUrl(HttpServletRequest, String, String)
     */
    public static String generateUrl(
    		HttpServletRequest aReq,
    		String aActionName,
    		String aSessionId,
    		String aParams) 
    {
    	final String baseUrl = generateUrl( aReq, aActionName, aSessionId );
       	final int ipos = baseUrl.indexOf( ";jsessionid" );

    	final StringBuffer sb = new StringBuffer( baseUrl.substring(0, ipos) );        
    	if ( sb.indexOf("?") < 0 )
    	{
    		sb.append( "?" );
    	}
    	else
    	{
    		sb.append( "&" );
    	}
    	
     	
    	sb.append( aParams );
    	sb.append( baseUrl.substring(ipos) );
    	
    	LOGGER.debug( "Generated URL with parameters: " + sb );
        return sb.toString();
    }

    /**
     * Find the identifier for the current HTTP session.
     * 
     * <p>If a <code>requestedSessionId</code> has been set on the HTTP
     * request, use it. Otherwise, find the associated <code>JSESSIONID</code>
     * cookie.
     * </p>
     * 
     * @param aReq The HTTP request
     * @return the session id or null (not found)
     * 
     * @see HttpServletRequest#getRequestedSessionId()
     * @see HttpServletRequest#getCookies()
     */
    public static String getSessionId(HttpServletRequest aReq)
    {
    	// has one been set on the HTTP request?

    	if ( aReq.getRequestedSessionId() != null )
    	{
        	LOGGER.debug( "Using requested-sessionid as JSESSIONID: " + aReq.getRequestedSessionId() );
	    	return aReq.getRequestedSessionId();
    	}

    	// if not, search cookies

    	final Cookie[] cookies = aReq.getCookies();
    	if ( cookies != null )
    	{
	    	for ( int i=0;  i < cookies.length;  i++)
	    	{
	    		if ( cookies[i].getName().equals("JSESSIONID") )
	    		{
    	        	LOGGER.debug( "Found JSESSIONID cookie: " + cookies[i].getValue() );
	    			return cookies[i].getValue();
	    		}
	    	}
    	}

    	// if no cookies, give up! 
    	return null;
    }

}
