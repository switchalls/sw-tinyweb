package sw.tinyweb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import sw.tinyweb.io.HttpChunkedInputStream;
import sw.tinyweb.io.HttpHeaderReader;
import sw.tinyweb.utils.HttpHeaderUtils;
import sw.tinyweb.utils.IteratorEnumeration;
import sw.tinyweb.utils.LanguageTag;

/**
 * A single HTTP request.
 * 
 * <p>Supports -
 * <ol>
 * <li>HTTP GET -
 * <pre>
 *     GET /index.html?userid=joe&password=guessme HTTP/1.1
 *     Host: www.mysite.com
 *     User-Agent: Mozilla/4.0
 * </pre>
 * 
 * <li>HTTP POST (data from) -
 * <pre>
 *     POST /login.jsp HTTP/1.1
 *     Content-Length: 27
 *     Content-Type: application/x-www-form-urlencoded
 *     
 *     userid=joe&password=guessme
 * </pre>
 * 
 * <li>HTTP POST (chunked data) -
 * <pre>
 *     POST /login.jsp HTTP/1.1
 *     Content-Type: chunked
 *     
 *     182&lt;CRLF>
 *     &lt;0x182 bytes&gt;&lt;CRLF>
 *     0&lt;CRLF>
 *     &lt;CRLF>
 * </pre>
 * </ol>
 * </p>
 * 
 * <p>Usage:
 * <pre>
 *     final {@link java.net.Socket Socket} socket = ...
 *
 *     final {@link ServletResponse} response = ...
 *     
 *     final TinyWebRequest r = new TinyWebRequest();
 *     r.setLocalAddress( ({@link InetSocketAddress}) socket.getLocalAddr() );
 *     r.setRemoteAddress( ({@link InetSocketAddress}) socket.getRemoteAddr() );
 *     r.setServletResponse( response );
 *     r.initRequest( socket.getInputStream() );
 *     
 *     final String contextPath = ""; <font style="color:green">// root context</font>
 *     final String servletPath = null;
 *     
 *     <font style="color:green">// match request URL to application context and servlet paths</font>
 *     
 *     r.setContextPath( contextPath );
 *     r.setServletPath( servletPath );
 * </pre>
 * </p>
 * 
 * @author $Author: $
 * @version $Revision: $
 */
public class TinyWebRequest implements HttpServletRequest
{
	/** Empty array. */
	public static final String[] NO_PARAMS = new String[0];

	private static final Logger LOGGER = Logger.getLogger( TinyWebRequest.class );
		
	private final Map<String, Object> attributes = new HashMap<String, Object>();
	private String characterEncoding;
	private int contentLength;
	private String contextPath;
	private List<Cookie> cookies;
	private InetSocketAddress localAddress;
	private final Map<String, Object> headers = new HashMap<String, Object>();
	private InputStream inputStream;
	private String method;
	private final Map<String, String[]> parameterMap = new HashMap<String, String[]>();
	private String protocol;
	private InetSocketAddress remoteAddress;
	private String requestedSessionId;
	private URL requestURL;
	private String servletPath;
	private ServletResponse servletResponse;
	private TinyWebSession session;
	
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
	public String getAuthType()
	{
		return null; // TinyWebServer does not implement authentication
	}

	@Override
	public String getCharacterEncoding()
	{
		return (this.characterEncoding != null) ? this.characterEncoding : "UTF-8";
	}

	@Override
	public int getContentLength()
	{
		return this.contentLength;
	}

	@Override
	public String getContentType()
	{
		return this.getHeader("Content-Type");
	}

	@Override
	public String getContextPath()
	{
		return this.contextPath;
	}

	@Override
	public Cookie[] getCookies()
	{
		if ( this.cookies == null )
		{
			return null;
		}
		
		final Cookie[] c = new Cookie[ this.cookies.size() ];
		this.cookies.toArray( c );
		return c;
	}

	@Override
	public long getDateHeader(String aName)
	{
        final String header = this.getHeader(aName);
        if ( header == null )
        {
            return -1L;
        }
        
        try
        {
        	return HttpHeaderUtils.getInstance().parseDate(header).getTime();
        }
        catch (TinyWebException e)
        {
        	throw new IllegalArgumentException("Cannot parse date: " + header);
        }
	}

	@Override
	public Enumeration<String> getHeaderNames()
	{
		return new IteratorEnumeration<String>( this.headers.keySet() );
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getHeader(String aName)
	{
		final Object header = this.headers.get( aName);

		if ( header instanceof List<?> )
		{
			return ((List<String>)header).get(0);
		}
		
		if ( header instanceof String )
		{
			return (String) header;
		}
		
		return (header != null) ? header.toString() : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Enumeration<String> getHeaders(String aName)
	{		
		final Object header = this.headers.get( aName);
		
		if ( header instanceof Collection<?> )
		{
			return new IteratorEnumeration<String>( (Collection<String>)header );
		}

		final List<String> results = new ArrayList<String>();
		if ( header instanceof String )
		{
			results.add( (String)header );
		}
		else if ( header != null )
		{
			results.add( header.toString() );
		}
		
		return new IteratorEnumeration<String>( results );
	}

	@Override
	public int getIntHeader(String aName)
	{
        final Object header = this.getHeader( aName );
        if ( header != null )
        {
	        if ( !(header instanceof Number) )
	        {
	            throw new NumberFormatException("Invalid integer header: "+aName);        	
	        }
	        
	        final Number n = (Number) header;
	        return n.intValue();
        }
        return -1;
	}

	@Override
	public ServletInputStream getInputStream()
	{
		return new ServletInputStream()
		{	
			@Override
			public int read() throws IOException
			{
				return inputStream.read();
			}
		};
	}

	@Override
	public String getLocalAddr()
	{
		final String s = this.localAddress.getAddress().getHostAddress();
		return ("0.0.0.0".equals(s)) ? "localhost" : s;
	}

	@Override
	public String getLocalName()
	{
		return this.localAddress.getHostName();
	}

	@Override
	public int getLocalPort()
	{
		return this.localAddress.getPort();
	}

	@Override
	public Locale getLocale()
	{
		return this.getLocales().nextElement();
	}

	@Override
	public Enumeration<Locale> getLocales()
	{
		// create unique list of languages tags
		//
		// NB. Headers may contain multiple Accept-Language fields

		final List<LanguageTag> languages = new ArrayList<LanguageTag>();		
		for ( final Enumeration<String> i = this.getHeaders("Accept-Language");  i.hasMoreElements();)
		{
			this.addLanguage( languages, i.nextElement() );
		}
		
		// sort list into decreasing preference order (item 0 = highest)
		
		Collections.sort( languages, new Comparator<LanguageTag>()
		{
			@Override
			public int compare(LanguageTag aTag, LanguageTag aOther)
			{
				if ( aTag.getQualityLevel() == aOther.getQualityLevel() )
				{
					return 0;
				}
				
				return (aTag.getQualityLevel() > aOther.getQualityLevel()) ? -1 : 1;
			}	
		});
		
		// list locales in decreasing preference order (item 0 = highest)
		
		final List<Locale> locales = new ArrayList<Locale>();
		for ( LanguageTag language : languages )
		{
			locales.add( language.getLocale() );
		}
		
		if ( locales.isEmpty() )
		{
			locales.add( Locale.getDefault() );
		}
		
		return new IteratorEnumeration<Locale>( locales );
	}

	/**
	 * Extract language tags from the stated header text.
	 * 
	 * @param aList The list of unique languages
	 * @param aText The header text
	 * @throws IllegalArgumentException when the language tags cannot be extracted
	 */
	protected void addLanguage(List<LanguageTag> aList, String aText)
	throws IllegalArgumentException
	{
		final List<LanguageTag> tags = HttpHeaderUtils.getInstance().parseLanguages( aText );			
		for ( LanguageTag lt : tags )
		{
			final LanguageTag old = this.findLanguage( aList, lt.getLanguage() );
			if ( old == null )
			{
				aList.add( lt );
			}
			else if ( old.getQualityLevel() < lt.getQualityLevel() )
			{
				aList.remove( old );
				aList.add( lt );
			}
		}
	}
	
	/**
	 * Find the language.
	 * 
	 * @param aLanguages The list of languages
	 * @param aLanguage The RFC 1766 language tag
	 * @return the tag
	 */
	protected LanguageTag findLanguage(List<LanguageTag> aLanguages, String aLanguage)
	{
		for ( LanguageTag lt : aLanguages )
		{
			if ( lt.getLanguage().equals(aLanguage) )
			{
				return lt;
			}
		}
		return null;
	}
	
	@Override
	public String getMethod()
	{
		return this.method;
	}

	@Override
	public String getParameter(String aName)
	{
		final String[] values = this.parameterMap.get( aName );
		return (values != null) ? values[0] : null;
	}

	@Override
	public Enumeration<String> getParameterNames()
	{
		return new IteratorEnumeration<String>( this.parameterMap.keySet() );
	}

	@Override
	public Map<String, String[]> getParameterMap()
	{
		return this.parameterMap;
	}

	@Override
	public String[] getParameterValues(String aName)
	{
		final String[] values = this.parameterMap.get( aName );
		return (values != null) ? values : NO_PARAMS;
	}

	@Override
	public String getPathInfo()
	{
		// Returns any extra path information associated with the URL the
		// client sent when it made this request.
		//
		// The extra path information follows the servlet path but precedes
		// the query string and will start with a "/" character.
		
		final String p = this.requestURL.getPath();
		if ( this.servletPath != null )
		{
			final int ipos = p.indexOf( this.servletPath );
			if ( ipos > -1 )
			{
				return p.substring( ipos + this.servletPath.length() );
			}
		}
		return p;
	}

	@Override
	public String getPathTranslated()
	{
        return this.getPathInfo(); // path translation not supported
	}

	@Override
	public String getProtocol()
	{
		return this.protocol;
	}

	@Override
	public String getQueryString()
	{
		return this.requestURL.getQuery();
	}

	@Override
	public BufferedReader getReader()
	throws IOException
	{
		return new BufferedReader( new InputStreamReader(this.inputStream) );
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated As of Version 2.1, replaced by {@link javax.servlet.ServletContext#getRealPath(String)}
	 */
	@Deprecated
	@Override
	public String getRealPath(String aPath)
	{
        throw new UnsupportedOperationException("HttpServletRequest.getRealPath() not supported");
	}

	@Override
	public String getRemoteAddr()
	{
		return this.remoteAddress.getAddress().getHostAddress();
	}

	@Override
	public String getRemoteHost()
	{
		return this.remoteAddress.getHostName();
	}

	@Override
	public int getRemotePort()
	{
        return this.remoteAddress.getPort();
	}

	@Override
	public String getRemoteUser()
	{
        throw new UnsupportedOperationException("HttpServletRequest.getRemoteUser() not supported");
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String aPath)
	{
		final TinyWebRequestDispatcherFactory factory = TinyWebRequestDispatcherFactory.getInstance();
		
		if ( aPath.startsWith("/") )
		{
			return factory.createRequestDispatcherByPath( aPath );
		}
		
		final String newPath = this.createAbsolutePath( this.getServletPath(), aPath );
		return factory.createRequestDispatcherByPath( newPath );
	}

	/**
	 * Create a new absolute path.
	 * 
	 * @param aCurrentLocation The current location
	 * @param aRelativePath The relative path
	 * @return the new path
	 */
	protected String createAbsolutePath(String aCurrentLocation, String aRelativePath)
	{
		final Stack<String> location = new Stack<String>();
		final StringBuffer newPath = new StringBuffer();

		StringTokenizer tokeniser = new StringTokenizer( aCurrentLocation, "/" );
		while ( tokeniser.hasMoreTokens() )
		{
			location.push( tokeniser.nextToken( ) );
		}
		
		tokeniser = new StringTokenizer( aRelativePath, "/" );
		while ( tokeniser.hasMoreTokens() )
		{
			final String token = tokeniser.nextToken();
			
			if ( newPath.length() > 0 )
			{
				newPath.append( "/" );
				newPath.append( token );
			}
			else if ( ".".equals(token) )
			{
				// ignore
			}
			else if ( "..".equals(token) )
			{
				// path can go outside servlet context
				if ( location.size() > 0 )
				{
					location.pop();
				}
			}
			else
			{
				while ( location.size() > 0 )
				{
					newPath.insert( 0, location.pop() );
					newPath.insert( 0, "/" );
				}

				newPath.append( "/" );
				newPath.append( token );
			}
		}
		
		if ( newPath.length() < 1 )
		{
			newPath.append( "/" );
		}
		
		return newPath.toString();
	}
	
	@Override
	public String getRequestURI()
	{
		// Returns the part of this request's URL from the protocol name
		// up to the query string in the first line of the HTTP request.
		//
		// The web container does not decode this String. For example:
		// 	First line of HTTP request			Returned Value
		// 	POST /some/path.html HTTP/1.1		/some/path.html
		// 	GET http://foo.bar/a.html HTTP/1.0	/a.html
		// 	HEAD /xyz?a=b HTTP/1.1				/xyz

		return (this.requestURL != null) ? this.requestURL.getPath() : null;
	}

	@Override
	public StringBuffer getRequestURL()
	{
		final StringBuffer url = new StringBuffer();
		final String scheme = this.getScheme();
		final int port = this.getServerPort();

		url.append(scheme);
		url.append("://");
		url.append(this.getServerName());
		
		if (	(scheme.equals("http") && (port != 80))
			 || (scheme.equals("https") && (port != 443)) )
		{
			url.append(':');
			url.append(port);
		}
		
		return url.append(getRequestURI());
	}

	@Override
	public String getRequestedSessionId()
	{
		return this.requestedSessionId;
	}

	@Override
	public String getScheme()
	{
		return this.requestURL.getProtocol();
	}

	@Override
	public String getServerName()
	{
		final String host = this.requestURL.getHost();
		if ( "".equals(host) )
		{
			return this.getLocalAddr();
		}
		return host;
	}

	@Override
	public int getServerPort()
	{
		return this.localAddress.getPort();
	}

	@Override
	public String getServletPath()
	{
		return this.servletPath;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see #getSession(boolean)
	 */
	@Override
	public HttpSession getSession()
	{
		return this.getSession( true );
 	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see TinyWebSessionManager
	 */
	@Override
	public HttpSession getSession(boolean aCreate)
	{
		if ( (this.session == null) && (this.requestedSessionId != null) )
		{
			this.session = TinyWebSessionManager.getInstance().findSession(
				this.requestedSessionId
			);
			
			if ( this.session == null )
			{
				LOGGER.warn( "Cannot find request HTTP session " + this.requestedSessionId );
			}
		}

		if ( (this.session == null) && aCreate )
		{
			if ( (this.servletResponse != null) && this.servletResponse.isCommitted() )
			{
				throw new IllegalStateException( "Cannot create session after response committed" );
			}

			this.session = TinyWebSessionManager.getInstance().createSession();
		}
		
		if ( this.session != null )
		{
			this.session.sessionAccessed();
		}
		
		return this.session;
	}

	@Override
	public Principal getUserPrincipal()
	{
        throw new UnsupportedOperationException("HttpServletRequest.getUserPrincipal() not supported");
	}

	@Override
	public boolean isRequestedSessionIdFromCookie()
	{
		return (this.findCookie(TinyWebSession.SESSION_ID) != null);
 	}

	@Override
	public boolean isRequestedSessionIdFromURL()
	{
		return !this.isRequestedSessionIdFromCookie();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @deprecated As of Version 2.1, replaced by {@link #isRequestedSessionIdFromURL()}
	 */
	@Deprecated
	@Override
	public boolean isRequestedSessionIdFromUrl()
	{
	    throw new UnsupportedOperationException("HttpServletRequest.isRequestedSessionIdFromUrl() not supported");
	}

	@Override
	public boolean isRequestedSessionIdValid()
	{
		return (this.requestedSessionId != null) && (this.getSession(false) != null);
	}

	@Override
	public boolean isSecure()
	{
	    return "https".equals(this.getScheme());
	}

	@Override
	public boolean isUserInRole(String aUser)
	{
	    throw new UnsupportedOperationException("HttpServletRequest.isUserInRole() not supported");
	}

	@Override
	public void removeAttribute(String aName)
	{
		this.attributes.remove( aName );
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
			this.attributes.put( aName, aValue );
		}
	}

	@Override
	public void setCharacterEncoding(String aEncoding)
	throws UnsupportedEncodingException
	{
		this.characterEncoding = aEncoding;
	}

	/**
	 * Add a new cookie.
	 * 
	 * @param aCookie The cookie
	 */
	public void addCookie(Cookie aCookie)
	{
		if ( this.cookies == null )
		{
			this.cookies = new ArrayList<Cookie>();
		}
		
		this.cookies.add( aCookie );

		if ( TinyWebSession.SESSION_ID.equals(aCookie.getName()) )
		{
			this.requestedSessionId = aCookie.getValue();
		}
	}
	
	/**
	 * Find the stated cookie.
	 * 
	 * @param aName The cookie identifier
	 * @return the cookie or null (not found)
	 */
	public Cookie findCookie(String aName)
	{
		for ( Cookie c : this.cookies )
		{
			if ( c.getName().equals(aName) )
			{
				return c;
			}
		}
		return null;
	}

	/**
	 * Add a new HTTP header value.
	 * 
	 * @param aName The name
	 * @param aValue The value
	 */
	@SuppressWarnings("unchecked")
	public void addHeader(String aName, String aValue)
	{
		final Object header = this.headers.get( aName );
		if ( header instanceof Collection<?> )
		{
			((Collection<String>)header).add( aValue );
		}
		else if ( header instanceof String )
		{
			final Collection<String> strings = new ArrayList<String>();
			strings.add( (String)header );
			strings.add( aValue );
		}
		else // if ( header == null )
		{
			this.headers.put( aName, aValue );
		}
	}
	
	/**
	 * Add a new GET/POST parameter.
	 * 
	 * @param aName The name
	 * @param aValue The value
	 */
	public void addParameter(String aName, String aValue)
	{
		if ( TinyWebSession.SESSION_ID.equals(aName) )
		{
			this.requestedSessionId = aValue;
		}
		else
		{
			final String[] oldList = this.parameterMap.get( aName );
			if ( oldList == null )
			{
				this.parameterMap.put( aName, new String[]{aValue} );				
			}
			else
			{
				final String[] newList = new String[ oldList.length+1 ];
				System.arraycopy( oldList, 0, newList, 0, oldList.length );
				newList[ oldList.length ] = aValue;
				
				this.parameterMap.put( aName, newList );				
			}
		}
	}
	
	/** @param aLen The new value or -1 (length unknown) */
	public void setContentLength(int aLen)
	{
		this.contentLength = aLen;
	}
	
	/**
	 * What portion of the request URL path identified the application context?
	 * 
	 * @param aPath The value
	 */
	public void setContextPath(String aPath)
	{
		this.contextPath = aPath;
	}

	/**
	 * Set local port details.
	 * 
	 * @param aAddr The address information
	 * 
	 * @see #getLocalAddr()
	 * @see #getLocalName()
	 * @see #getLocalPort()
	 */
	public void setLocalAddress(InetSocketAddress aAddr)
	{
		this.localAddress = aAddr;
	}
		
	/**
	 * Initialise the request from the stated input stream.
	 * 
	 * @param aIn The input stream
	 * @throws IOException when the header information cannot be read
	 * @throws TinyWebException when the header content is rejected
	 * 
	 * @see #parseHeaderLine(String)
	 */
	public void initRequest(InputStream aIn)
	throws IOException, TinyWebException
	{
		this.inputStream = aIn;
		
        final HttpHeaderReader reader = new HttpHeaderReader( aIn );
        
        String s;
        while ( (s = reader.readLine()) != null )
        {
        	if ( "".equals(s) )
        	{
        		// end of header
        		break;
        	}
        	this.parseHeaderLine( s );
        }

        if ( LOGGER.isDebugEnabled() )
		{
        	LOGGER.debug( "HTTP "+this.getMethod() );
	        LOGGER.debug( "Request URI: "+this.getRequestURI() );

	        LOGGER.debug( "Headers..." );
        	for ( final Enumeration<String> e = this.getHeaderNames();  e.hasMoreElements();)
 	        {
        		final String name = e.nextElement();
	        	LOGGER.debug( name + ": " + this.getHeader(name) );
	        }

	        if ( this.cookies != null )
	        {
		        LOGGER.debug( "Cookies..." );
	        	for ( Cookie c : this.cookies )
	        	{
	        		LOGGER.debug( c.getName() + " = " + c.getValue() );
	        	}
	        }
	        
	        LOGGER.debug( "Parameters..." );
        	for ( final Enumeration<String> e = this.getParameterNames();  e.hasMoreElements();)
	        {
        		final String name = e.nextElement();
	        	LOGGER.debug( name + ": " + this.getParameter(name) );
	        }
		}
		
        if ( this.method == null )
        {
        	throw new TinyWebException( HttpStatusCodes.METHOD_NOT_ALLOWED, "Missing HTTP request method" );				
        }
        
        if ( this.requestURL == null )
        {
        	throw new TinyWebException( HttpStatusCodes.BAD_REQUEST, "Missing HTTP request URL" );				
        }

        this.setCharacterEncoding( this.getHeader("Character-Encoding") );

		final String clen = this.getHeader("Content-Length");
		if ( clen != null )
		{
			this.setContentLength( Integer.parseInt(clen) );
		}
		else
		{
			this.setContentLength( -1 );
		}

		final String transferEncoding = this.getHeader("Transfer-Encoding");
		if ( "application/x-www-form-urlencoded".equals(this.getContentType()) )
		{
			final String params = reader.read( this.getContentLength() );
			this.parseRequestUrlParameters( params );
		}
		else if ( "chunked".equals(transferEncoding) )
		{
			this.inputStream = new HttpChunkedInputStream( reader );
		}
	}

	/**
	 * Set remote port details.
	 * 
	 * @param aAddr The address information
	 * 
	 * @see #getRemoteAddr()
	 * @see #getRemoteHost()
	 * @see #getRemotePort()
	 */
	public void setRemoteAddress(InetSocketAddress aAddr)
	{
		this.remoteAddress = aAddr;
	}

	/**
	 * What request are we processing?
	 * 
	 * @param aURL The request
	 */
	public void setRequestURL(URL aURL)
	{
		this.requestURL = aURL;
	}

	/**
	 * What portion of the request URL path identified the servlet?
	 * 
	 * @param aPath The value
	 */
	public void setServletPath(String aPath)
	{
		this.servletPath = aPath;
	}

	/**
	 * What is creating the response for this request?
	 * 
	 * @param aResponse The HTTP response creator
	 */
	public void setServletResponse(ServletResponse aResponse)
	{
		this.servletResponse = aResponse;
	}

	/** Close the output stream. */
	public void closeStream()
	{
		try
		{
			this.inputStream.close();
		}
		catch (IOException e)
		{
			LOGGER.error("Cannot close HTTP request stream", e);
		}
	}

	/**
	 * Change the servlet path.
	 * 
	 * @param aPath The new path
	 */
	public void changeServletPath(String aPath)
	{
		final StringBuffer sb = this.getRequestURL();
		final int ipos = sb.indexOf( this.servletPath );
		sb.replace( ipos, ipos + this.servletPath.length(), aPath );
		
		try
		{
			this.setRequestURL( new URL(sb.toString()) );
			this.setServletPath( aPath );
		}
		catch (MalformedURLException e)
		{
			LOGGER.error( "Invalid URL: " + sb );
		}
	}
	
	/**
	 * Parse a single line of text.
	 * 
	 * @param aText The text to be processed
	 * @throws TinyWebException when the header content is rejected
	 * 
	 * @see #getMethod()
	 * @see #getProtocol()
	 * @see #addCookie(Cookie)
	 * @see #addHeader(String, String)
	 * @see HttpHeaderUtils
	 */
	protected void parseHeaderLine(String aText)
	throws TinyWebException
	{
		if ( this.getMethod() == null )
		{
			if ( !aText.startsWith("GET") && !aText.startsWith("POST") )
			{
				throw new TinyWebException( HttpStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: "+aText );				
			}
			
			final String[] args = aText.split(" "); // split around white-space
			if ( args.length != 3 )
			{
				throw new TinyWebException( HttpStatusCodes.BAD_REQUEST, "Invalid HTTP request: "+aText );
			}
			
			if ( !"HTTP/1.1".equals(args[2]) )
			{
				throw new TinyWebException( HttpStatusCodes.HTTP_VERSION_NOT_SUPPORTED, "HTTP version not supported: "+args[2] );				
			}
			
			this.method = args[0];
			this.protocol = args[2];
			this.parseRequestUrl( args[1] );
		}
		else
		{
			final int ipos = aText.indexOf(':');
			if ( ipos < 0 )
			{
				throw new TinyWebException( HttpStatusCodes.BAD_REQUEST, "Invalid HTTP header: "+aText );
			}
			
			final String name = aText.substring( 0, ipos ).trim();
			final String value = aText.substring( ipos+1 );
			
			if ( "Cookie".equals(name) )
			{		
				final Cookie c = HttpHeaderUtils.getInstance().parseCookie( value );
				this.addCookie( c );
			}
			else
			{
				this.addHeader( name, value.trim() );
			}
		}
	}
	
	/**
	 * Extract the <code>urlPath</code> and <code>requestParam</code>(s)
	 * from the stated URL.
	 * 
	 * @param aUrl The URL
	 * @throws IllegalArgumentException when the URL is rejected
	 * 
	 * @see #getQueryString()
	 * @see #getScheme()
	 * @see #getServerName()
	 * @see #getServerPort()
	 * @see #parseRequestUrlParameters(String)
	 */
	protected void parseRequestUrl(String aUrl)
	throws IllegalArgumentException
	{
		// Aside - URL(String) requires a protocol
		
		String url = aUrl;
		if ( url.indexOf(':') < 0 )
		{
			url = "http:" + url;
		}
		
		try
		{
			this.setRequestURL( new URL(url) );
		}
		catch (MalformedURLException e)
		{
			throw new IllegalArgumentException( "Invalid URL: "+url );
		}
		
		if ( this.requestURL.getQuery() != null )
		{
			this.parseRequestUrlParameters( this.requestURL.getQuery() );
		}
	}

	/**
	 * Extract all request parameters from the stated URL.
	 * 
	 * <p>For example, <code>userid=joe&password=guessme</code>
	 * </p>
	 * 
	 * @param aParams The URL parameter list
	 * @throws IllegalArgumentException when the parameters are rejected
	 */
	protected void parseRequestUrlParameters(String aParams)
	throws IllegalArgumentException
	{
		final String[] args = aParams.split("&");		
		for ( int i=0;  i < args.length;  i++ )
		{
			final int ipos = args[i].indexOf('=');
			if ( ipos < 0 )
			{
				throw new IllegalArgumentException( "Invalid URL parameter: "+args[i] );
			}
			
			final String name = args[i].substring( 0, ipos );
			final String value = args[i].substring( ipos+1 );
			
			try
			{
				this.addParameter(
					URLDecoder.decode(name,"UTF-8"),
					URLDecoder.decode(value,"UTF-8")
				);
			}
			catch (UnsupportedEncodingException e)
			{
				// should never happen
				LOGGER.error( "UTF-8 encoding not supported", e );
			}
		}
	}
	
}
