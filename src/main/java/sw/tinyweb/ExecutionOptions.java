package sw.tinyweb;

/**
 * How can servlets be executed?
 * 
 * @author $Author: $
 * @version $Revision: $
 * 
 * @see TinyWebServletConfig
 */
public enum ExecutionOptions
{

	/** Servlet does NOT require a separate thread for execution. */
	NO_THREAD,
	
	/** Servlet requires a separate thread for execution. */
	REQUIRES_THREAD

}
