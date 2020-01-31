package sw.tinyweb.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sw.utils.xml.DOMDocument;
import sw.utils.xml.XMLException;

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
		final DOMDocument doc = new DOMDocument();
		doc.loadDocument( "XML", aXML, false );
		
		this.print( doc, doc.getRootNode() );
	}
	
	/**
	 * Convert the XML document into a JSON stream.
	 * 
	 * @param aDoc The XML document
	 * @param aNode The node inside the XML document
	 * @throws XMLException when a problem occurs
	 */
	public void print(DOMDocument aDoc, Node aNode)
	throws XMLException
	{		
		this.writer.print( "{" );

		// write attributes
		
		final NamedNodeMap attrs = aNode.getAttributes();
		for ( int i=0;  i < attrs.getLength();  i++)
		{
			if ( i > 0 )
			{
				this.writer.print(",");
			}
			
			final Node n = attrs.item(i);
			this.writer.print("\"");
			this.writer.print(n.getNodeName());
			this.writer.print("\":");
			this.writer.print(this.getJsonValue(n.getNodeValue()));
		}
		
		boolean needsComma = (attrs.getLength() > 0);

		// write node value
	
		String nodeText;
		if ( aDoc.containsNode(aNode, Node.CDATA_SECTION_NODE) )
		{
			nodeText = aDoc.getCDATAContent(aNode);
		}
		else
		{
			nodeText = aDoc.getNodeText(aNode);			
		}
		
		if ( nodeText.length() > 0 )
		{
			if ( needsComma )
			{
				this.writer.print(",");
			}
			
			needsComma = true;

			this.writer.print("\"nodeText\":");
			this.writer.print(this.getJsonValue(nodeText));
		}
		
		// write children
		
		final NodeList nl = aNode.getChildNodes();

		final Map<String, Integer> childCounters = new HashMap<String, Integer>();
		for ( int i=0;  i < nl.getLength();  i++)
		{
			final Node n = nl.item(i);
			if ( n instanceof Element )
			{
				Integer childCount = childCounters.get( n.getNodeName() );
				if ( childCount == null )
				{
					childCount = 1; 
				}
				else
				{
					childCount = childCount + 1;
				}
				
				childCounters.put( n.getNodeName(), childCount );
			}
		}
		
		for ( String childName : childCounters.keySet() )
		{
			final Integer childCount = childCounters.get(childName);
			
			if ( needsComma )
			{
				this.writer.print(",");
			}

			needsComma = true;
			
			this.writer.print("\"");
			this.writer.print(childName);
			this.writer.print("\":");

			if ( childCount > 1 )
			{
				this.writer.print("[");				
			}

			// do any children contain more than CDATA sections or Text nodes?
			
			int attrCount = 0;
			int elementCount = 0;
			
			for ( int i=0;  i < nl.getLength();  i++)
			{
				final Node n = nl.item(i);
				if ( childName.equals(n.getNodeName()) )
				{
					attrCount += n.getAttributes().getLength();
					elementCount += aDoc.countChildNodesByType( n, Node.ELEMENT_NODE );
				}
			}
			
			int itemCount = 0;
			for ( int i=0;  i < nl.getLength();  i++)
			{
				final Node n = nl.item(i);

				if ( !childName.equals(n.getNodeName()) )
				{
					continue;
				}
				
				if ( itemCount++ > 0 )
				{
					this.writer.print(",");						
				}

				if ( (attrCount > 0) || (elementCount > 0) )
				{
					this.print( aDoc, n );
				}
				else
				{
					if ( aDoc.containsNode(n, Node.CDATA_SECTION_NODE) )
					{
						nodeText = aDoc.getCDATAContent(n);
					}
					else
					{
						nodeText = aDoc.getNodeText(n);			
					}
					
					this.writer.print(this.getJsonValue(nodeText));
				}
			}

			if ( childCount > 1 )
			{
				this.writer.print("]");
			}
		}
		
		this.writer.print( "}" );
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
