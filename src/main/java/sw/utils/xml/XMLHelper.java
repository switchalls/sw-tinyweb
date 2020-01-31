package sw.utils.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import sw.utils.xml.handlers.SimpleErrorHandler;
import sw.utils.xml.resolvers.EmptyStreamEntityResolver;
import sw.utils.xml.resolvers.SimpleURIResolver;

/**
 * Helper for manipulating XML documents.
 * 
 * @author Stewart Witchalls
 * @version 4.0
 */
public class XMLHelper
{
	private static final String CANNOT_FIND_RESOURCE = "Cannot load resource";
	private static final String CANNOT_READ_XSL_STYLESHEET = "Cannot read XSL stylesheet";

    /** Singleton instance */
	private static XMLHelper globalInstance;

	/**
	 * Get the global instance.
	 * 
	 * <p>This instance will be shared across all applications
	 * executing inside the same JVM.
	 * </p>
	 * 
	 * @return the singleton
	 */
	public static XMLHelper getInstance()
	{
		if ( globalInstance == null )
		{
			globalInstance = new XMLHelper();
		}
		return globalInstance;
	}

	/** @param aHelper The new global instance */
	public static void setInstance(XMLHelper aHelper)
	{
		globalInstance = aHelper;
	}

    /**
     * Create a string (based on the stated data) that can be safely
     * used inside XML files.
     * 
     * <p>All invalid characters are replaced with question mark.
     * </p>
     * 
     * <p>See <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">XML 1.0 valid characters</a>
     * for more details.
     * </p>
     * 
     * @param aData The data
     * @param aLen The length
     * @param aEncodingType The encoding type
     * @return the XML string
     * @throws UnsupportedEncodingException when the encoding type is not supported
     */
    public String createXmlSafeString(
    		byte[] aData,
    		int aLen,
    		String aEncodingType)
    throws	UnsupportedEncodingException
    {
    	final String s = new String( aData, 0, aLen, aEncodingType );
    	final StringBuffer sb = new StringBuffer();

    	for ( int i=0;  i < s.length();  i++)
    	{
    		char c = s.charAt(i);
    		if ( (c == 0x09) // horizontal tab
    				|| (c == 0x0a) // new line
    				|| (c == 0x0d) // carriage return
    				|| (c == 0x20) // whitespace
    				|| ((c > 0x20) && (c <= 0xd7ff))
    				|| ((c >= 0xe000) && (c <= 0xfffd))
    				|| ((c >= 0x10000) && (c <= 0x10FFFF)) )
			{				
    			// OK
			}
    		else
    		{
    			c = '?';
    		}

    		sb.append( c );
    	}

    	return sb.toString();
    }

    /**
     * Replace XML special characters with their common encoding.
     * 
     * <p>
     * CDATA blocks must not contain the character sequence ]]&gt;.
     * </p>
     * 
     * @param aContent The CDATA content
     * @return the encoded data
     */
	public String encodeCDATA(String aContent)
	{
		final StringBuffer sb = new StringBuffer( aContent );
		int ipos = 0;
		while ( (ipos = sb.indexOf("]]>", ipos)) > -1 )
		{
			sb.replace( ipos, ipos+3, "]]&gt;" );
		}
		return sb.toString();
	}

	/**
     * Replace XML special characters with their common encoding.
     * 
     * <p>
	 * Some XML characters have special meanings. If used as part of an
	 * element's data, XML parsers will become confused. So, we need to
	 * replace them recognised equivalents.
	 * </p>
	 * 
     * <p>
     * Mappings:
     * <ol>
     * <li>&amp; to &amp;amp;
     * <li>&lt; to &amp;lt;
     * <li>&gt; to &amp;gt;
     * <li>&quot; (double quote) to &amp;quot;
     * <li>&#39; (single quote) to &amp;#39;
     * </ol>
     * </p>
     * 
     * <p>
     * NB. Although <code>&amp;#39;</code> can be represented by the XML entity
     * <code>&amp;apos;</code>, not all browsers support the encoding.
     * </p>
     * 
     * @param aData The data to be encoded
     * @return the encoded data
     */
    public String encodeString(String aData)
    {
    	final StringBuffer sb = new StringBuffer();
    	for ( int i=0;  i < aData.length();  i++)
    	{
    		final char c = aData.charAt(i);
    		switch ( c )
    		{
    		case '&':
    			sb.append( "&amp;" );
    			break;
    		
    		case '<':
    			sb.append( "&lt;" );
    			break;

    		case '>':
    			sb.append( "&gt;" );
    			break;

    		case '"':
    			sb.append( "&quot;" );
    			break;

    		case '\'':
    			sb.append( "&#39;" );
    			break;

    		default:
    			sb.append( c );
    		}
    	}
    	return sb.toString();
    }

    /**
     * Create a new SAX parser.
     * 
     * <p>Turn off external resource resolution when not validating XML content.
     * </p>
     * 
     * @param aValidating True when validation is required
     * @param aNamespaceAware True when the parser is namespace aware
     * @return the parser
     * @throws ParserConfigurationException when the parser cannot be created
     * @throws SAXException when the parser cannot be created
     * @see EmptyStreamEntityResolver
     */
    public SAXParser newSAXParser(boolean aValidating, boolean aNamespaceAware)
    throws ParserConfigurationException, SAXException
    {
		final SAXParserFactory pfact = SAXParserFactory.newInstance();
		pfact.setValidating( aValidating );
		pfact.setNamespaceAware( aNamespaceAware );

		final SAXParser parser = pfact.newSAXParser();
		final XMLReader reader = parser.getXMLReader();
		reader.setErrorHandler(new SimpleErrorHandler()); 
		
		if ( !aValidating )
		{
			reader.setEntityResolver( new EmptyStreamEntityResolver() );
		}

		return parser;
    }

    /**
     * Create a new XPath expression.
     * 
     * @return the new xpath
     * @throws XPathExpressionException when the new expression cannot be created
     */
    public XPath newXPath()
    throws XPathExpressionException
    {
    	return XPathFactory.newInstance().newXPath();
    }

    /**
	 * Create a XSL stylesheet template.
	 * 
	 * <p>Uses <code>URL.openStream</code> when <code>aResolver</code> is null.
	 * </p>
	 * 
	 * @param aResolver The resolver (can be null)
	 * @param aXslStylesheet The XSL stylesheet
	 * @return the template
	 * @throws TransformerException when the template cannot be created
	 */
	public Templates newTransformerTemplate(
			EntityResolver aResolver,
			String aXslStylesheet)
	throws	TransformerException
	{
		StreamSource s = null;
		try
		{
			s = this.newStreamSource( aResolver, aXslStylesheet );

			final TransformerFactory tfact = TransformerFactory.newInstance();
			tfact.setURIResolver( new SimpleURIResolver(aResolver) );
			
			return tfact.newTemplates( s );
		}
		catch (Exception e)
		{
			throw new TransformerException( CANNOT_READ_XSL_STYLESHEET, e );			
		}
		finally
		{
			this.closeStream( s );
		}
	}
	
	/**
	 * Set the transformation parameters.
	 * 
	 * @param aTrans The transformer
	 * @param aParameters The parameters (can be null)
	 */
	public void setTransformerParameters(Transformer aTrans, Map<String,Object> aParameters)
	{
		if ( aParameters != null )
		{
			for (Map.Entry<String,Object> me : aParameters.entrySet())
			{
				final String pname = me.getKey();
				final Object pvalue = me.getValue();
				aTrans.setParameter( pname, pvalue );				
			}
		}		
	}

	/**
	 * SAX based transformation.
	 *
	 * @param aResolver The external resource resolver (can be null)
	 * @param aXmlContent The document content
	 * @param aValidateXmlContent True when content should be validated
	 * @param aTmpl The cached XSL stylesheet
	 * @param aParameters Transformation parameters (can be null)
	 * @param aOutput The output stream
	 * @throws ParserConfigurationException when the SAX parser cannot be configured
	 * @throws SAXException when the SAX parser encounters a problem
	 * @throws TransformerException when the transformation fails
	 */
	public void saxTransform(
			EntityResolver aResolver,
			InputStream aXmlContent,
			boolean aValidateXmlContent,
			Templates aTmpl,
			Map<String,Object> aParameters,
			Writer aOutput)
	throws	ParserConfigurationException,
			SAXException,
			TransformerException
	{
		final SAXTransformerFactory tfact = (SAXTransformerFactory)
			SAXTransformerFactory.newInstance();

// Xerces bug? - Factory URIResolver not copied across to Transformer
//
//		if ( aResolver != null )
//		{
//			tfact.setURIResolver( new DefaultURIResolver(aResolver) );
//		}

		final TransformerHandler thandler = tfact.newTransformerHandler( aTmpl );
		final Transformer trans = thandler.getTransformer();
		this.setTransformerParameters( trans, aParameters );

		if ( aResolver != null )
		{
			trans.setURIResolver( new SimpleURIResolver(aResolver) );
		}

		// NB. newSAXParser() installs an EmptyStreamEntityResolver when
		// validation == false

		final SAXParser parser = newSAXParser( aValidateXmlContent, true );
		final XMLReader reader = parser.getXMLReader();

		if ( aResolver != null )
		{
			reader.setEntityResolver( aResolver );
		}

		final InputSource isource = new InputSource( aXmlContent );
		final SAXSource xmlSource = new SAXSource( reader, isource );		
		final Result result = new StreamResult( aOutput );
		trans.transform( xmlSource, result );
	}

	/**
	 * Create a new <code>InputSource</code>.
	 * 
	 * <p>If <code>aResolver</code> is null, use <code>URL.openStream</code>.
	 * </p>
	 * 
	 * @param aResolver The entity resolver (can be null)
	 * @param aResourceURL The resource
	 * @return the new source
	 * @throws IOException when a resource cannot be read
	 * @throws SAXException when XML parsing should be stopped
	 */
	public InputSource newInputSource(EntityResolver aResolver, String aResourceURL)
	throws IOException, SAXException
	{
		final String systemId = aResourceURL;
		if ( aResolver == null )
		{
			final InputSource s = new InputSource();
			s.setByteStream( new URL(aResourceURL).openStream() );
			s.setPublicId( null );
			s.setSystemId( systemId );
			return s;
		}
		final InputSource isource = aResolver.resolveEntity( null, systemId );
		if ( isource == null )
		{
			final String msg = CANNOT_FIND_RESOURCE + ", url=["+systemId+"]";
			throw new IOException( msg );
		}
		return isource; 
	}

	/**
	 * Create a new <code>StreamSource</code>.
	 * 
	 * @param aSource The XML input source
	 * @return the new stream source
	 */
	public StreamSource newStreamSource(InputSource aSource)
	{
		final StreamSource s = new StreamSource();
		s.setInputStream( aSource.getByteStream() );
		s.setReader( aSource.getCharacterStream() );
		s.setPublicId( aSource.getPublicId() );
		s.setSystemId( aSource.getSystemId() );
		return s;
	}

	/**
	 * Create a new <code>StreamSource</code>.
	 *
	 * @param aResolver The entity resolver (can be null)
	 * @param aResourceURL The resource
	 * @return the new source
	 * @throws IOException when a resource cannot be read
	 * @throws SAXException when XML parsing should be stopped
	 *
	 * @see #newInputSource(EntityResolver, String)
	 */
	public StreamSource newStreamSource(EntityResolver aResolver, String aResourceURL)
	throws IOException, SAXException
	{
		final InputSource isource = this.newInputSource( aResolver, aResourceURL );
		return this.newStreamSource( isource );
	}

	/**
     * Close the XML input source.
     * 
     * @param aSource The source
     */
    public void closeStream(InputStream aSource)
	{
		try
		{
			if ( aSource != null )
			{
				aSource.close();
			}
		}
		catch (IOException e)
		{
			// do nothing
		}		
	}

    /**
     * Close the XML input source.
     * 
     * @param aSource The source
     */
    public void closeStream(Reader aSource)
	{
		try
		{
			if ( aSource != null )
			{
				aSource.close();
			}
		}
		catch (IOException e)
		{
			// do nothing
		}		
	}

    /**
     * Close the XML input source.
     * 
     * @param aSource The source (can be null)
     */
    public void closeStream(InputSource aSource)
	{
    	if ( aSource != null )
    	{
    		this.closeStream( aSource.getCharacterStream() );
    		this.closeStream( aSource.getByteStream() );
    	}
	}

    /**
     * Close the XML input source.
     * 
     * @param aSource The source (can be null)
     */
    public void closeStream(StreamSource aSource)
    {
    	if ( aSource != null )
    	{
	    	this.closeStream( aSource.getReader() );
	    	this.closeStream( aSource.getInputStream() );
    	}
	}

}
