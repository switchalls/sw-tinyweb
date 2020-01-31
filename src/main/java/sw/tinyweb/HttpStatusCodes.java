package sw.tinyweb;

import javax.servlet.http.HttpServletResponse;

/**
 * HTTP status codes returned by TinyWeb.
 * 
 * <p>See status code <a href="http://en.wikipedia.org/wiki/List_of_HTTP_status_codes">definitions</a>.
 * </p>
 * 
 * @author $Author: $
 * @version $Revision: $
 */
public class HttpStatusCodes
{
	/** 2xx Success. */
	public static final int OK = HttpServletResponse.SC_OK;

	/** 4xx Client error. */
	public static final int BAD_REQUEST = HttpServletResponse.SC_BAD_REQUEST;
	/** 4xx Client error. */
	public static final int METHOD_NOT_ALLOWED = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
	
	/** 5xx Server error. */
	public static final int HTTP_VERSION_NOT_SUPPORTED = HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED;
	/** 5xx Server error. */
	public static final int INTERNAL_SERVER_ERROR = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
	/** 5xx Server error. */
	public static final int NOT_IMPLEMENTED = HttpServletResponse.SC_NOT_IMPLEMENTED;
	
}
