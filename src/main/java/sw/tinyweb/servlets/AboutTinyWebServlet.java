package sw.tinyweb.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <code>TinyWeb</code> "about" page.
 * 
 * @author $Author: $
 * @version $Revision: $
 */
public class AboutTinyWebServlet extends HttpServlet
{
	private static final long serialVersionUID = -7954043928500435374L;

	@Override
	public void doGet(HttpServletRequest aRequest, HttpServletResponse aResponse)
	throws ServletException, IOException
	{
		aResponse.setCharacterEncoding( "UTF-8" );
		aResponse.setContentType( "text/html" );
		
		final PrintWriter writer = new PrintWriter( aResponse.getOutputStream(), true );
		writer.println("<html>" );
		writer.println("<head><title>TinyWeb - About</title></head>" );
		writer.println("<body>" );
		writer.println("TinyWeb - HTTP/1.1 javax.servlet container" );
		writer.println("<br/>" );
		writer.println("(c) Stewart Witchalls 2012" );
		writer.println("</body>" );
		writer.println("</html>" );
		writer.close();
	}

	@Override
	public void doPost(HttpServletRequest aRequest, HttpServletResponse aResponse)
	throws ServletException, IOException
	{
		this.doGet( aRequest, aResponse );
	}
	
}

