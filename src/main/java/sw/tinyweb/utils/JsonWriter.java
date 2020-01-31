package sw.tinyweb.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Write XML document as a JSON stream.
 * 
 * @author $Author: $
 * @version $Revision: $
 */
public class JsonWriter
{
	private final String callback;
	private final PrintWriter writer;
		
	/**
	 * Constructor.
	 * 
	 * @param aResponse The HTTP response
	 * @param aCallback The callback method or null (return JSON stream)
	 * @throws IOException when the response writer cannot be created
	 * 
	 * @see #JsonWriter(PrintWriter, String)
	 * @see #configureOutputStream(HttpServletResponse)
	 */
	public JsonWriter(HttpServletResponse aResponse, String aCallback)
	throws IOException
	{
		this( aResponse.getWriter(), aCallback );
		this.configureOutputStream( aResponse );
	}

	/**
	 * Constructor.
	 * 
	 * <p>If <code>aCallback</code> defined, creates the
	 * <code>text/javascript</code> response
	 * <pre>    aCallback '(' &lt;data&gt; ');'</pre>
	 * Otherwise, creates an <code>application/json</code> response
	 * containing only &lt;data&gt;.
	 * </p>
	 * 
	 * @param aWriter The output stream
	 * @param aCallback The callback method or null (return JSON stream)
	 */
	public JsonWriter(PrintWriter aWriter, String aCallback)
	{
		this.callback = aCallback;
		this.writer = aWriter;
		
		if ( this.callback != null )
		{
			this.writer.print( this.callback );
			this.writer.print( "(" );
		}
	}

	/**
	 * Configure the HTTP response.
	 * 
	 * @param aResponse The HTTP response
	 * 
	 * @see HttpServletResponse#setCharacterEncoding(String)
	 * @see HttpServletResponse#setContentType(String)
	 */
	public void configureOutputStream(HttpServletResponse aResponse)
	{
		if ( this.callback != null )
		{
			aResponse.setCharacterEncoding( "UTF-8" );
			aResponse.setContentType( "text/javascript" );
		}
		else
		{
			aResponse.setCharacterEncoding( "UTF-8" );
			aResponse.setContentType( "application/json" );
		}
	}
	
	/** Close the output stream. */
	public void close()
	{
		if ( this.callback != null )
		{
			this.writer.println( ");" );
		}
		this.writer.close();
	}

	/** Flush the writer. */
	public void flush()
	{
		this.writer.flush();
	}
	
	/**
	 * Append the stated text.
	 * 
	 * @param aText The text
	 */
	public void print(String aText)
	{
		this.writer.print( aText );
	}
	
	/**
	 * Append the stated text.
	 * 
	 * @param aText The text
	 */
	public void println(String aText)
	{
		this.writer.println( aText );
	}

	/**
	 * Convert the XML document.
	 * 
	 * @param aXML The XML document
	 * @throws Exception when the document cannot be converted
	 * 
	 * @see #printXmlDocument(InputStream)
	 */
	public void printXmlDocument(String aXML)
	throws Exception
	{
		this.printXmlDocument( new ByteArrayInputStream(aXML.getBytes("UTF-8")) );
	}

	/**
	 * Convert the XML document.
	 * 
	 * @param aXML The XML document
	 * @throws Exception when the document cannot be converted
	 * 
	 * @see #print(DOMDocument, Node)
	 */
	public void printXmlDocument(InputStream aXML)
	throws Exception
	{
		final Document doc = new SAXBuilder().build(aXML);
		new XMLOutputter().output(doc, this.writer);
	}
		
	/**
	 * Get the JSON equivalent of the stated text.
	 * 
	 * <p>Auto wraps strings with double quotes.
	 * </p>
	 * 
	 * @param aText The text
	 * @return the JSON value
	 */
	protected String getJsonValue(String aText)
	{
		if ( this.isBoolean(aText) || this.isNumber(aText) )
		{
			return aText;
		}
		
		// escape reserved characters in Javascript strings
		
		final String s = aText.replaceAll("\"", "\\\"").replaceAll("\n", "\\n");
		return "\"" + s + "\"";
	}
	
	/**
	 * Can the text represent a JSON boolean value?
	 * 
	 * @param aText The text
	 * @return true when yes
	 */
	protected boolean isBoolean(String aText)
	{
		return "true".equals(aText) || "false".equals(aText);
	}
	
	/**
	 * Can the text represent a JSON numeric value?
	 * 
	 * @param aText The text
	 * @return true when yes
	 */
	protected boolean isNumber(String aText)
	{
		try
		{
			Double.parseDouble( aText );
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
}
