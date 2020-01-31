package sw.utils.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.apache.log4j.Logger;
import org.dom4j.io.SAXContentHandler;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import sw.utils.xml.handlers.SimpleErrorHandler;
import sw.utils.xml.resolvers.SimpleLSResourceResolver;

/**
 * SAX implementation of a <code>XMLDocument</code>.
 *
 * <p>A JAXP SAX wrapper for condensing all SAX callbacks into two ...
 * <ol>
 * <li>{@link ContentHandler#elementStart(String, String, Attributes)}
 * <li>{@link ContentHandler#elementEnd(String, String, String, Attributes)}
 * </ol>
 * </p>
 * 
 * <p>Attributes can be filtered by {@link #addExcludedNamespaceURI(String) excluding namespaces}.
 * When excluded, they are removed from attribute lists.
 * </p>
 * 
 * <p>Elements can be filtered by excluding namespaces and/or
 * {@link #addExcludedPath(String) defining} Regex patterns that are matched
 * against element XPaths. Excluding an element will cancel all callbacks
 * for it and its children. In affect, the parser behaves as though
 * the element never existed.
 * </p>
 * 
 * <p>If the SAX parser supports {@link org.xml.sax.ext.LexicalHandler lexical handlers},
 * whitespace surrounding CDATA blocks will be removed. This feature can be
 * disabled by overriding {@link #startCDATA(XmlValueBuilder)}.
 * </p>
 * 
 * @author Stewart Witchalls
 * @version 5.0
 */
public class SAXDocument implements XMLDocument
{
	/** SAX parser property name. */
	public static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";

	/** Initial size of all <code>StringBuilder</code>(s) for default-content values. */
	public static final int DEFAULT_VALUE_SIZE = 64;

	private static final String BAD_REGEX_PATTERN = "Invalid regex pattern";
	private static final Logger LOGGER = Logger.getLogger( SAXDocument.class );	
	private static final Attributes NO_ATTRIBUTES = new AttributesImpl();

    /**
     * Simplified SAX content handler for parsing XML documents.
     * 
     * <p>Only inform the user of <code>elementStart</code> and
     * <code>elementEnd</code> events. None of the other SAX
     * events (eg. <code>character</code>) are necessary for
     * element analysis.
     * </p>
     * 
     * <p>All names (including attributes) are fully qualified.
     * </p>
     */
    public interface ContentHandler
    {
        /**
         * Element started event.
         * 
         * @param aXPath The path from the document root
         * @param aElementName The element
         * @param aAttrs The XML attributes
         * @throws SAXException when an error should abort parsing
         */
        void elementStart(String aXPath, String aElementName, Attributes aAttrs)
        throws SAXException;

        /**
         * Element closed event.
         * 
         * <p>SAX parser's do not differentiate between padding
         * and real textual values. The user may need to check that
         * <code>aValue</code> contains more than white-space.
         * </p>
         *
         * @param aXPath The path from the document root
         * @param aElementName The element
         * @param aValue The default content
         * @param aAttrs The XML attributes
         * @throws SAXException when an error should abort parsing
         */
        void elementEnd(String aXPath, String aElementName, String aValue, Attributes aAttrs)
        throws SAXException;
    }

    /** SAX parser listener. */
    public class DefaultContentHandler extends DefaultHandler
    {
        /**
         * {@inheritDoc}
         *
         * <p>Any derived class overriding this method must remember to call
         * the super class version.
         * </p>
         */
    	@Override
        public void startDocument() throws SAXException
        {
    		documentName = null;
    		publicId = null;
    		systemId = null;
        }

        /**
         * {@inheritDoc}
		 *
         * <p>Called when a namespace is declared.
         * </p>
         * 
         * @see SAXDocument#isExcludedNamespaceURI(String)
         * @see SAXDocument#addExcludedPath(String)
         */
    	@Override
        public void startPrefixMapping(String aPrefix, String aURI)
        throws SAXException
        {
        	if ( isExcludedNamespaceURI(aURI) )
        	{
        		if ( excludedAttrNamespaces == null )
        		{
        			excludedAttrNamespaces = new ArrayList<String>();
        			excludedPathNamespaces = new ArrayList<String>();
        		}
        		
        		excludedAttrNamespaces.add( aPrefix+":" );
        		excludedPathNamespaces.add( "/"+aPrefix+":" );        		
        	}
        }
        
        /**
         * {@inheritDoc}
         * 
         * @see SAXDocument#createFilteredAttrList(String, Attributes)
         * @see SAXDocument#fireElementStart(String, String, Attributes)
         * @see SAXDocument#isExcludedPath(String)
         */
    	@Override
        public void startElement(
                String aUri,
                String aLocalName,
                String aQualifiedName,
                Attributes aAttrs)
        throws  SAXException
        {    	
            if (documentName == null)
            {
                documentName = aQualifiedName;
            }
            
            // Create the current XPath
            
            final String xpath = createElementXPath( aQualifiedName );

            // Clone the org.w3c.dom.Attributes list, filtering out
            // attributes with excluded namespace prefixes.
            //
            // NB. Some SAX parsers will re-use the same object
            // for different nodes??!!

            Attributes attrs = NO_ATTRIBUTES;
            if ( aAttrs.getLength() > 0 )
            {
            	attrs = createFilteredAttrList( xpath, aAttrs );  
            }
            
            elementAttrStack.push(attrs);

            // Create the default-content buffer

            elementValueStack.push( createXmlValueBuilder(DEFAULT_VALUE_SIZE) );
     
        	if ( !isExcludedPath(xpath) )
            {
            	fireElementStart(xpath, aQualifiedName, attrs);
            }
        	else
        	{
        		LOGGER.debug("elementStart(" + xpath + "): excluded");        		    		
        	}
        }

        /**
         * {@inheritDoc}
         *
         * @see SAXDocument#fireElementEnd(String, String, String, Attributes)
         */
    	@Override
        public void endElement(String aUri, String aLocalName, String aQualifiedName)
        throws SAXException
        {
            final String xpath = elementPathStack.pop();
            final Attributes attrs = elementAttrStack.pop();
            final XmlValueBuilder value = elementValueStack.pop();

        	if ( !isExcludedPath(xpath) )
            {
            	fireElementEnd(xpath, aQualifiedName, freeXmlValueBuilder(value), attrs);
            }
        	
    		freeAttributes( attrs );
        }

    	@Override
        public void characters(char[] aBuf, int aOffset, int aLen)
        throws SAXException
        {
            final XmlValueBuilder value = elementValueStack.peek();           
            if ( !value.isUnparsedData() || value.inCDATABlock() )
        	{
            	// either no CDATA blocks present or inside one

            	value.append( aBuf, aOffset, aLen );            		
        	}
            else if ( !this.isWhitespace(aBuf, aOffset, aLen) )
            {
            	// parsed data outside CDATA block
            	
         		value.append( aBuf, aOffset, aLen );
            }
        }
    	
        /**
         * {@inheritDoc}
         *
         * <p>{@link org.xml.sax.DTDHandler#notationDecl(String, String, String)}
         * should be called whenever a DTD declaration is encountered. However,
         * during testing, the Apache Xerces 2.x parser does not do this??!!
         * The only alternative is to trap the resolve-DTD request.
         * </p>
         *
         * <p>Id(s) are passed as fully qualified URI(s).
         * </p>
         */
    	@Override
        public InputSource resolveEntity(String aPublicId, String aSystemId)
        {
        	if ( publicId == null )
        	{
        		publicId = aPublicId;
        	}

        	if ( systemId == null )
        	{
        		systemId = aSystemId;
        	}

            try
            {
            	if ( entityResolver != null )
            	{
            		return entityResolver.resolveEntity( aPublicId, aSystemId );
            	}
            }
            catch (Throwable e)
            {
            	LOGGER.warn( "resolveEntity("+aPublicId+","+aSystemId+") error", e );
            }
            
        	// Tell the parser to use default behavior
        	return null;
        }

        /**
         * {@inheritDoc}
         *
         * <p>Apache claims that <code>org.xml.sax.helpers.DefaultHandler</code>
         * should propagate errors. However, testing has shown that Xerces 2.x
         * does not??!!
         * </p>
         */
    	@Override
        public void error(SAXParseException aError)
        throws SAXException
        {
            throw new SAXException(aError.getMessage());
        }

        /**
         * {@inheritDoc}
         *
         * <p>Apache claims that <code>org.xml.sax.helpers.DefaultHandler</code>
         * should propagate errors. However, testing has shown that Xerces 2.x
         * does not??!!
         * </p>
         */
    	@Override
        public void fatalError(SAXParseException aError)
        throws SAXException
        {
            throw new SAXException(aError.getMessage());
        }
    	
    	/**
    	 * Does the stated data contain only whitespace?
    	 * 
    	 * @param aBuf The data container
    	 * @param aOffset The location of the first data character in the array
    	 * @param aLen The data length
    	 * @return true if yes
    	 */
    	protected boolean isWhitespace(char[] aBuf, int aOffset, int aLen)
    	{
    		for ( int i=0;  i < aLen;  i++)
    		{
    			if ( !Character.isWhitespace(aBuf[aOffset + i]) )
    			{
    				return false;
    			}
    		}
    		return true;
    	}
    }

    /** SAX parser listener. */
    public class DefaultLexicalHandler extends DefaultHandler2
    {
    	/**
    	 * {@inheritDoc}
    	 * 
    	 * @see SAXDocument#startCDATA(XmlValueBuilder)
    	 */
    	@Override
    	public void startCDATA()
    	{    		
            final XmlValueBuilder value = elementValueStack.peek();
            SAXDocument.this.startCDATA( value );
    	}
    	
    	@Override
    	public void endCDATA()
    	{
    		final XmlValueBuilder value = elementValueStack.peek();
            value.endCDATA();
    	}    	
    }
    
    /**
     * Helper class for constructing XML element values.
     * 
     * <p>Wraps and behaves like a {@link StringBuilder}.
     * </p>
     * 
     * <p><i>Aside - {@link StringBuilder} is final and therefore
     * cannot be extended.
     * </p>
     * 
     * <p>Remembers when CDATA blocks have been encountered.
     * </p>
     */
    public class XmlValueBuilder
    {
    	private final StringBuilder builder;
    	private boolean inCDATABlock;
    	private boolean unparsedData;
    	
    	/**
    	 * Constructor.
    	 * 
    	 * @param aInitialSize The initial buffer size
    	 */
    	public XmlValueBuilder(int aInitialSize)
    	{
    		this.builder = new StringBuilder( aInitialSize );
    	}

    	/**
    	 * Append the data.
    	 * 
    	 * @param aChar The data
    	 * @return this object
    	 */
    	public XmlValueBuilder append(char aChar)
    	{
    		this.builder.append( aChar );
    		return this;
    	}

    	/**
    	 * Append the data.
    	 * 
    	 * @param aString The data
    	 * @return this object
    	 */
    	public XmlValueBuilder append(String aString)
    	{
    		this.builder.append( aString );
    		return this;
    	}
    	
    	/**
    	 * Append the data.
    	 * 
    	 * @param aBuf The data container
    	 * @param aOffset The location of the first data character in the array
    	 * @param aLen The data length
    	 * @return this object
    	 */
    	public XmlValueBuilder append(char[] aBuf, int aOffset, int aLen)
    	{
    		this.builder.append( aBuf, aOffset, aLen );
    		return this;
    	}

    	/**
    	 * Change the data length.
    	 * 
    	 * @param aNewLength The new length
    	 */
    	public void setLength(int aNewLength)
    	{
    		this.builder.setLength( aNewLength );
    	}

    	@Override
    	public String toString()
    	{
    		return this.builder.toString();
    	}

    	/** Reset this instance back to a virgin state. */
    	public void reset()
    	{
    		this.builder.setLength( 0 );
    		this.inCDATABlock = false;
    		this.unparsedData = false;
    	}
    	
    	/** @return true when we are inside a CDATA block */
    	public boolean inCDATABlock()
    	{
    		return this.inCDATABlock;
    	}

    	/** @return true when one or more CDATA blocks have been encountered */
    	public boolean isUnparsedData()
    	{
    		return this.unparsedData;
    	}
    	
    	/** @return true if the value only contains whitespace */
    	public boolean isWhitespace()
    	{
    		for ( int i=0;  i < this.builder.length();  i++)
    		{
    			if ( !Character.isWhitespace(this.builder.charAt(i)) )
    			{
    				return false;
    			}
    		}
    		return true;
    	}
    	
    	/** Callback for when a CDATA block is entered. */
    	public void startCDATA()
    	{
    		this.unparsedData = true;
    		this.inCDATABlock = true;
    	}
    	
    	/** Callback for when a CDATA is exited. */
    	public void endCDATA()
    	{
    		this.inCDATABlock = false;
    	}
    }
    
    private final List<ContentHandler> contentHandlerList = new ArrayList<ContentHandler>();
    private String documentName;
    private EntityResolver entityResolver;
    private List<String> excludedNamespaceURIs;
    private List<Pattern> excludedPathPatterns;
    private String publicId;
    private String systemId;
    private String schemaURL;
    private String validationType;

    /** Current node's XML attributes. */
    private final Stack<Attributes> elementAttrStack = new Stack<Attributes>();

    /** Current node's XPath. */
    private final Stack<String> elementPathStack = new Stack<String>();

    /** Current node's value. */
    private final Stack<XmlValueBuilder> elementValueStack = new Stack<XmlValueBuilder>();
    
    /**
     * List of attribute name patterns describing namespaces to be excluded.
     * @see SAXContentHandler#startPrefixMapping(String, String)
     */
    private List<String> excludedAttrNamespaces;

    /**
     * List of XPath patterns describing namespaces to be excluded.
     * 
     * @see SAXContentHandler#startPrefixMapping(String, String)
     */
    private List<String> excludedPathNamespaces;

	@Override
    public boolean isValidated()
    {
		return (this.validationType != XMLDocument.NO_VALIDATION);
    }

	@Override
    public String getDocumentName()
    {
        return this.documentName;
    }

	/** @return the external resource resolver (can be null) */
	public EntityResolver getEntityResolver()
	{
		return this.entityResolver;
	}

	@Override
	public void setEntityResolver(EntityResolver aResolver)
	{
		this.entityResolver = aResolver;
	}

	@Override
    public String getPublicId()
    {
        return this.publicId;
    }

	@Override
    public String getSystemId()
    {
        return this.systemId;
    }

	@Override
	public String getSchemaURL()
	{
		return this.schemaURL;
	}

	@Override
	public String getValidationType()
	{
		return this.validationType;
	}

	/**
	 * Add a content handler.
	 * 
	 * @param aHandler The handler
	 */
    public void addContentHandler(ContentHandler aHandler)
    {
        this.contentHandlerList.add( aHandler );
    }

    /**
     * Remove the content handler.
     * 
     * @param aHandler The handler
     */
    public void removeContentHandler(ContentHandler aHandler)
    {
        this.contentHandlerList.remove( aHandler );
    }

    /**
     * The element path to be excluded.
     * 
     * <p>Callbacks are not issued for excluded paths or any of their
     * children. In effect, the parser behaves as though the element
     * never existed.
     * </p>
     * 
     * <p><i>Aside:</i> Whilst nodes can be excluded, it is not possible
     * to apply the same technique to create a <em>only include</em> list.
     * Why? Exclusion works by matching a child element. You keep all
     * parent elements until an element becomes excluded. Then you discard
     * it and all children. However, you cannot determine when to keep a
     * parent node until you have one of its children. For example, the
     * pattern <code>.&#42;/D$</code> will match the child
     * <code>/A/B/C/D</code>, but not the parent <code>/A/B</code>.
     * </p>
     * 
     * @param aRegexPattern The pattern
     * @throws SAXException when the pattern is invalid
     */
    public void addExcludedPath(String aRegexPattern)
    throws SAXException
    {
    	if ( this.excludedPathPatterns == null )
    	{
    		this.excludedPathPatterns = new ArrayList<Pattern>();
    	}
    	
    	try
    	{
			this.excludedPathPatterns.add( Pattern.compile(aRegexPattern) );
    	}
    	catch (PatternSyntaxException e)
    	{
    		throw new SAXException( BAD_REGEX_PATTERN+", pattern=["+aRegexPattern+"]", e );
    	}
    }
    
    /** Remove all excluded paths. */
    public void removeExcludedPaths()
    {
    	this.excludedPathPatterns = null;
    }
    
    /**
     * Does the XML name start with an excluded namespace?
     * 
     * <p>For example, exclude <code>xsi:noNamespaceSchemaLocation</code>
     * when the namespace
     * <code>xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"</code>
     * is being excluded.
     * </p>
     * 
     * @param aName The XML name
     * @return true when yes
     */
    public boolean isExcludedAttr(String aName)
    {
    	// Aside - "for (int i=...)" is quicker than "for ( T p : list )"

    	if ( this.excludedAttrNamespaces != null )
    	{
    		for ( int i = this.excludedAttrNamespaces.size() - 1;  i >= 0;  i--)
    		{
    			if ( aName.startsWith(this.excludedAttrNamespaces.get(i)) )
    			{
    				return true;
    			}
    		}    		
    	}
    	return false;
    }

    /**
     * Should this element path be excluded?
     * 
     * @param aXPath The path to be tested
     * @return true when the element path is excluded
     */
    public boolean isExcludedPath(String aXPath)
    {
    	// Aside - "for (int i=...)" is quicker than "for ( T p : list )"
    	
    	if ( this.excludedPathNamespaces != null )
    	{
    		for ( int i = this.excludedPathNamespaces.size() - 1;  i >= 0;  i--)
    		{
    			if ( aXPath.indexOf(this.excludedPathNamespaces.get(i)) > -1 )
    			{
    				return true;
    			}    			
    		}
    	}

    	if ( this.excludedPathPatterns != null )
    	{
    		for ( int i = this.excludedPathPatterns.size() - 1;  i >= 0;  i--)
	    	{
    			final Pattern p = this.excludedPathPatterns.get(i);
	    		final Matcher m = p.matcher( aXPath );
	    		if ( m.matches() )
	    		{
	    			return true;
	    		}
	    	}
    	}
    	
    	return false;
    }
    
    /**
     * Exclude the namespace.
     * 
     * <p>Users exclude namespaces by defining their URI.
     * Any attribute and/or element using the URI's declared
     * prefix will be excluded.
     * </p>
     * 
     * <p>NB. XML allows the user to declare their own prefixes for
     * a namespace URI. So, we have to use the URI to identify what
     * prefix has been declared and then base all filtering on that
     * declared prefix.
     * </p>
     * 
     * @param aURI The URI
     */
    public void addExcludedNamespaceURI(String aURI)
    {
    	if ( this.excludedNamespaceURIs == null )
    	{
    		this.excludedNamespaceURIs = new ArrayList<String>();
    	}
    	this.excludedNamespaceURIs.add( aURI );   	
    }
    
    /** Remove all excluded URIs. */
    public void removeNamespaceURIs()
    {
    	this.excludedNamespaceURIs = null;
    }

    /**
     * Should this namespace URI be excluded?
     *  
     * @param aURI The namespace URI
     * @return true when the namespace is excluded
     */
    public boolean isExcludedNamespaceURI(String aURI)
    {
    	return (this.excludedNamespaceURIs != null)
    		 ? this.excludedNamespaceURIs.contains(aURI)
    		 : false;    	
    }
    
    /**
     * Inform all listeners that a new element has been found.
     * 
     * @param aXPath The path from the document root
     * @param aElementName The element
     * @param aAttrs The XML attributes
     * @throws SAXException when any parser wants to stop the parser
     */
    public void fireElementStart(String aXPath, String aElementName, Attributes aAttrs)
    throws SAXException
    {
    	if (this.contentHandlerList.size() == 1)
    	{
    		final ContentHandler h = this.contentHandlerList.get(0);
			this.fireElementStart( aXPath, aElementName, aAttrs, h );    		
    	}
    	else
    	{
			for ( ContentHandler h : this.contentHandlerList )
			{
				this.fireElementStart( aXPath, aElementName, aAttrs, h );
	        }
    	}
    }

    private void fireElementStart(
    		String aXPath,
    		String aElementName,
    		Attributes aAttrs,
    		ContentHandler aHandler)
    throws  SAXException
    {
        try
        {
        	aHandler.elementStart( aXPath, aElementName, aAttrs );
        }
        catch (SAXException e)
        {
            throw e;
        }
        catch (Throwable e)
        {
        	LOGGER.error( "ContentHandler.elementStart() exception...", e );
        }
    }
    
    /**
     * Inform all listeners that the current element has been closed.
     * 
     * @param aXPath The path from the document root
     * @param aElementName The element
     * @param aValue The default content
     * @param aAttrs The XML attributes
     * @throws SAXException when any parser wants to stop the parser
     */
    public void fireElementEnd(
            String aXPath,
            String aElementName,
            String aValue,
            Attributes aAttrs)
    throws  SAXException
    {
    	if (this.contentHandlerList.size() == 1)
    	{
    		final ContentHandler h = this.contentHandlerList.get(0);
			this.fireElementEnd( aXPath, aElementName, aValue, aAttrs, h );
    	}
    	else
    	{
			for ( ContentHandler h : this.contentHandlerList )
	 		{
				this.fireElementEnd( aXPath, aElementName, aValue, aAttrs, h );
			}
    	}
    }
    
    private void fireElementEnd(
            String aXPath,
            String aElementName,
            String aValue,
            Attributes aAttrs,
            ContentHandler aHandler)
    throws  SAXException
    {
        try
        {
        	aHandler.elementEnd( aXPath, aElementName, aValue, aAttrs );
        }
        catch (SAXException e)
        {
            throw e;
        }
        catch (Throwable e)
        {
        	LOGGER.error( "ContentHandler.elementEnd() exception...", e );
        }    	
    }
    
    @Override
    public void loadDocument(
    		String aDocumentName,
    		InputStream aXmlContent,
    		boolean aValidateXmlContent)
    throws	IOException,
    		ParserConfigurationException,
    		SAXException
    {
		final InputSource isource = new InputSource(aXmlContent);
    	this.loadDocument( aDocumentName, isource, aValidateXmlContent );
    }
    
    /**
     * Load the XML document.
     *
     * <p>Assumes DTD based validation.
     * </p>
     * 
     * @param aDocumentName The document id
     * @param aXmlSource The XML content
     * @param aValidateXmlContent True when the parser should validate the XML
     * @throws IOException when the stream cannot be read
     * @throws ParserConfigurationException when the XML parser cannot be created
     * @throws SAXException when the contents fail validation
     * 
     * @see #createSAXContentHandler()
     * @see #createSAXLexicalHandler()
     */
    public synchronized void loadDocument(
    		String aDocumentName,
    		InputSource aXmlSource,
    		boolean aValidateXmlContent)
    throws	IOException,
    		ParserConfigurationException,
    		SAXException
    {
    	this.reset();
    	
        this.documentName = aDocumentName;
        this.validationType = (aValidateXmlContent) ? DTD_VALIDATION : NO_VALIDATION;
        this.schemaURL = null;
        
		final SAXParser parser = XMLHelper.getInstance().newSAXParser(
			aValidateXmlContent, true
		);
		
		try
		{
			parser.setProperty( LEXICAL_HANDLER_PROPERTY, this.createSAXLexicalHandler() );			
		}
		catch (SAXException e)
		{
			// lexical handlers not supported
		}
		
		EntityResolver defaultResolver = null;
		try
		{
			if ( !aValidateXmlContent && (this.getEntityResolver() == null) )
			{
				defaultResolver = parser.getXMLReader().getEntityResolver();

				// Use default resolver when no custom one installed
				this.setEntityResolver( defaultResolver );
			}
			
			parser.parse( aXmlSource, this.createSAXContentHandler() );
		}
		finally
		{
			if ( defaultResolver != null )
			{
				this.setEntityResolver( null );
			}
		}
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see #loadDocumentUsingW3CSchema(String, InputSource, String)
     */
	@Override
    public void loadDocumentUsingW3CSchema(
            String aDocumentName,
            InputStream aXmlContent,
            String aSchemaURL)
    throws  IOException,
            ParserConfigurationException,
            SAXException
    {
		final InputSource isource = new InputSource(aXmlContent);
		this.loadDocumentUsingW3CSchema( aDocumentName, isource, aSchemaURL );
    }

    /**
     * Load the XML document.
     *
     * <p>Validate the XML content using the stated W3C schema.
     * </p>
     * 
     * <p>If no <code>entityResolver</code> has been provided, load the schema
     * using <code>SchemaFactory.newSchema(URL)</code>.
     * </p>
     * 
     * @param aDocumentName The document's name
     * @param aXmlSource The document's content
     * @param aSchemaURL The W3C schema
     * @throws IOException when the stream cannot be read
     * @throws ParserConfigurationException when the XML parser cannot be created
     * @throws SAXException when the contents fail validation
     * 
     * @see #createSAXContentHandler()
     * @see #createSAXErrorHandler()
     * @see #createSAXLexicalHandler()
     */
    public synchronized void loadDocumentUsingW3CSchema(
            String aDocumentName,
            InputSource aXmlSource,
            String aSchemaURL)
    throws  IOException,
            ParserConfigurationException,
            SAXException
    {
    	this.reset();
    	
        this.documentName = aDocumentName;
        this.validationType = XMLDocument.W3C_SCHEMA_VALIDATION;
        this.schemaURL = aSchemaURL;

		final XMLHelper xhelper = XMLHelper.getInstance();
		Schema schema = null;

		final SchemaFactory sfact = SchemaFactory.newInstance(
    		XMLConstants.W3C_XML_SCHEMA_NS_URI
        );

		if ( this.getEntityResolver() == null )
		{
			schema = sfact.newSchema( new URL(aSchemaURL) );
		}
		else
		{			
			final DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
			final DocumentBuilder builder = fact.newDocumentBuilder();
			final LSResourceResolver dresolver = new SimpleLSResourceResolver(
				builder.getDOMImplementation(), this.getEntityResolver()
			);

			sfact.setResourceResolver( dresolver );

			final Source s = xhelper.newStreamSource(
				this.getEntityResolver(), aSchemaURL
			);
			
			schema = sfact.newSchema( s );
		}

        final ValidatorHandler vh = schema.newValidatorHandler();
        vh.setErrorHandler( this.createSAXErrorHandler() );
        vh.setContentHandler( this.createSAXContentHandler() );

		final SAXParser parser = xhelper.newSAXParser( false, true );
		final XMLReader reader = parser.getXMLReader();
		reader.setContentHandler( vh );
        
		try
		{
			reader.setProperty( LEXICAL_HANDLER_PROPERTY, this.createSAXLexicalHandler() );			
		}
		catch (SAXException e)
		{
			// lexical handlers not supported
		}

		EntityResolver defaultResolver = null;
		try
		{
			if ( this.getEntityResolver() == null )
			{
				defaultResolver = reader.getEntityResolver();

				// Use default resolver when no custom one installed
				this.setEntityResolver( defaultResolver );
			}
				        
	        reader.parse( aXmlSource );
		}
		finally
		{
			if ( defaultResolver != null )
			{
				this.setEntityResolver( null );
			}
		}
    }
    
    /** Reset the reader back to a virgin state. */
    public void reset()
    {
		this.documentName = null;
		this.publicId = null;
		this.schemaURL = null;
		this.systemId = null;
		this.validationType = NO_VALIDATION;

        this.excludedAttrNamespaces = null;
        this.excludedPathNamespaces = null;

        this.elementPathStack.clear();
        
        while ( this.elementAttrStack.size() > 0 )
		{
    		this.freeAttributes( this.elementAttrStack.pop() );
		}

        while ( this.elementValueStack.size() > 0 )
		{
			this.freeXmlValueBuilder( this.elementValueStack.pop() );
		}
    }

    /**
     * Create a filtered version of the attribute list.
     * 
     * <p>Filter out excluded namespace prefixes.
     * </p>
     * 
     * @param aXPath The element
     * @param aAttrs The XML attributes
     * @return the filtered XML attributes
     */
    protected Attributes createFilteredAttrList(String aXPath, Attributes aAttrs)
    {
    	// Some SAX parsers recycle the attributes passed into their callbacks.
    	// Therefore, must always clone the list.

    	final AttributesImpl copy = this.createAttributes();
    	for ( int i=0;  i < aAttrs.getLength();  i++ )
        {        	
        	final String qname = aAttrs.getQName( i );
        	if ( !this.isExcludedAttr(qname) )
        	{
        		copy.addAttribute(
        			aAttrs.getURI(i), aAttrs.getLocalName(i),
        			qname, aAttrs.getType(i), aAttrs.getValue(i)
        		);
        	}
        	else
        	{
    			LOGGER.debug("elementStart("+aXPath+"): attribute ["+qname+"] excluded");        		
         	}
        }
        return copy;
    }

    /**
     * Create the full XPath for the element.
     * 
     * <p>Push the full XPath onto <code>elementPathStack</code>.
     * </p>
     * 
     * @param aElementName The element name
     * @return the element's full XPath
     */
    protected String createElementXPath(String aElementName)
    {
    	String parentPath = "";
        if ( this.elementPathStack.size() > 0 )
        {
        	parentPath = this.elementPathStack.peek();
        }
    	
        final XmlValueBuilder sb = this.createXmlValueBuilder(
        	parentPath.length() + aElementName.length() + 1
        );
        
        sb.append( parentPath )
          .append( '/' )
          .append( aElementName );

        final String xpath = this.freeXmlValueBuilder( sb );
        this.elementPathStack.push( xpath );
        return xpath;
    }

    /** @return the new handler */
    protected ErrorHandler createSAXErrorHandler()
    {
    	return new SimpleErrorHandler();
    }

    /** @return the new handler */
    protected DefaultHandler createSAXContentHandler()
    {
    	return new DefaultContentHandler();
    }

    /** @return the new handler */
    protected LexicalHandler createSAXLexicalHandler()
    {
    	return new DefaultLexicalHandler();
    }
    
    /**
     * Treat the XML value as unparsed data.
     * 
     * <p>If "parsed data" proceeding first CDATA block contains
     * only whitespace, remove it.
     * </p>
     * 
     * @param aValue The parsed data
     */
	protected void startCDATA(XmlValueBuilder aValue)
	{
		if ( !aValue.isUnparsedData() && aValue.isWhitespace() )
		{
			// whitespace before first CDATA block

			aValue.setLength( 0 );
		}
        
		aValue.startCDATA();
	}

    /**
     * Optimization - Recycle XML attribute containers.
     */
    
    private final List<AttributesImpl> unusedAttributes = new LinkedList<AttributesImpl>();
    
    private AttributesImpl createAttributes()
    {
    	final int count = this.unusedAttributes.size();
    	if ( count > 0 )
    	{
    		return this.unusedAttributes.remove( count-1 );    		
    	}
        return new AttributesImpl();    	
    }
    
    private void freeAttributes(Attributes aAttrs)
    {
    	if ( aAttrs != NO_ATTRIBUTES )
    	{
    		final AttributesImpl p = (AttributesImpl) aAttrs;
    		p.clear();
    		
    		this.unusedAttributes.add( p );
    	}
    }

    /**
     * Optimization - Recycle string buffers.
     */

    private final List<XmlValueBuilder> unusedXmlValueBuilders = new LinkedList<XmlValueBuilder>();
    
    private XmlValueBuilder createXmlValueBuilder(int aInitialSize)
    {
    	final int count = this.unusedXmlValueBuilders.size();
    	if ( count != 0 )
    	{
			return this.unusedXmlValueBuilders.remove( count-1 );
    	}    	
        return new XmlValueBuilder( (aInitialSize > DEFAULT_VALUE_SIZE) ? aInitialSize : DEFAULT_VALUE_SIZE );
    }

    private String freeXmlValueBuilder(XmlValueBuilder aBuffer)
    {
    	final String s = aBuffer.toString();
    	aBuffer.reset();
    	
		this.unusedXmlValueBuilders.add( aBuffer );
		return s;
    }

}
