package sw.utils.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import sw.utils.xml.handlers.SimpleErrorHandler;
import sw.utils.xml.resolvers.EmptyStreamEntityResolver;
import sw.utils.xml.resolvers.SimpleLSResourceResolver;
import sw.utils.xml.resolvers.SimpleURIResolver;

/**
 * DOM implementation of a <code>XMLDocument</code>.
 *
 * @author Stewart Witchalls
 * @version 4.0
 */
public class DOMDocument implements XMLDocument
{
	private static final Logger LOGGER = Logger.getLogger( DOMDocument.class );
    private static final String MISSING_ATTRIBUTE = "Cannot find attribute";
    private static final String MISSING_CDATA = "Cannot find any CDATA sections";
    private static final String MISSING_COMMENT = "Cannot find any comment blocks";
    private static final String MISSING_ELEMENT = "Cannot find element";

    private boolean coalescing;
	private EntityResolver entityResolver;
	private Node rootNode;
	private NamespaceContext namespaceContext;
	private String schemaURL;
	private boolean useRegexPatternMatching;
	private String validationType = XMLDocument.NO_VALIDATION;

	/** Default constructor. */
    public DOMDocument()
    {    	
        // do nothing
    }

    /**
	 * Constructor.
     * 
	 * @param aDoc The document's DOM model
	 */
	public DOMDocument(Document aDoc)
	{
		this( aDoc.getDocumentElement() );
	}

    /**
	 * Constructor.
     * 
	 * @param aRootNode The document root
	 */
	public DOMDocument(Node aRootNode)
	{
		this.rootNode = aRootNode;
	}

	/**
     * Constructor.
     * 
	 * @param aRootTag The name of the root element
	 * @throws ParserConfigurationException when the DOM cannot be created
	 */
	public DOMDocument(String aRootTag)
	throws ParserConfigurationException
	{
		final DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = fact.newDocumentBuilder();
		final Document doc = builder.newDocument();
		this.rootNode = doc.createElement(aRootTag);
		doc.appendChild(this.rootNode);
	}

	/**
	 * Constructor.
	 *
	 * @param aNamespaceURI The namespace URI or null (not required)
	 * @param aPublicId The public-id or null (not required)
	 * @param aSystemId The system-id or null (not required)
	 * @param aRootTag The name of the root element
	 * @throws ParserConfigurationException when the DOM cannot be created
	 */
	public DOMDocument(
            String aNamespaceURI,
            String aPublicId,
            String aSystemId,
            String aRootTag)
	throws	ParserConfigurationException
	{
		final DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = fact.newDocumentBuilder();
		final DOMImplementation dfact = builder.getDOMImplementation();

		final DocumentType dt = dfact.createDocumentType(
			aRootTag, aPublicId, aSystemId
		);

		final Document doc = dfact.createDocument( aNamespaceURI, aRootTag, dt );
		this.rootNode = doc.getDocumentElement();
	}

	/** {@inheritDoc} */
	@Override
	public boolean isValidated()
	{
		return (this.validationType != XMLDocument.NO_VALIDATION);
	}
	
	/** {@inheritDoc} */
	@Override
	public String getDocumentName()
	{
		return this.getRootNode().getNodeName();
	}

	/**
	 * Convert <code>CDATA</code> nodes to <code>Text</code> nodes and append
	 * them to the adjacent (if any) text node?
	 * 
	 * <p>If false, some <code>xsl:value-of</code> implementations will
	 * not include <code>CDATA</code> content in the selection.
	 * </p>
	 * 
	 * <p>Default is false.
	 * </p>
	 * 
	 * @return true when yes
	 */
	public boolean isCoalescing()
	{
		return this.coalescing;
	}
	
	/** @param aFlag The new value */
	public void setCoalescing(boolean aFlag)
	{
		this.coalescing = aFlag;
	}

	/** @return the external resource resolver (can be null) */
	public EntityResolver getEntityResolver()
	{
		return this.entityResolver;
	}

	/** {@inheritDoc} */
	@Override
	public void setEntityResolver(EntityResolver aResolver)
	{
		this.entityResolver = aResolver;
	}

	/** {@inheritDoc} */
	@Override
	public String getPublicId()
	{
		final DocumentType dtype = this.getOwnerDocument().getDoctype();
		return (dtype != null) ? dtype.getPublicId() : null;
	}

	/** {@inheritDoc} */
	@Override
	public String getSystemId()
	{
		final DocumentType dtype = this.getOwnerDocument().getDoctype();
		return (dtype != null) ? dtype.getSystemId() : null;
	}

	/** {@inheritDoc} */
	@Override
	public String getSchemaURL()
	{
		return this.schemaURL;
	}

	/** {@inheritDoc} */
	@Override
	public String getValidationType()
	{
		return this.validationType;
	}

	/**
	 * Get the namespace context to be used by all XPath searches.
	 * 
	 * @return the context or null (use default context)
	 */
	public NamespaceContext getNamespaceContext()
	{
		return this.namespaceContext;
	}

	/** @param aContext The new context or null */
	public void setNamespaceContext(NamespaceContext aContext)
	{
		this.namespaceContext = aContext;
	}

	/**
	 * Should regex be used when matching string content?
	 * 
	 * @return true when yes
	 * 
	 * @see #matchNodes(Node, Node)
     * @since 3.0
	 */
	public boolean getUseRegexPatternMatching()
	{
		return this.useRegexPatternMatching;
	}
	
	/** @param aFlag The new value */
	public void setUseRegexPatternMatching(boolean aFlag)
	{
		this.useRegexPatternMatching = aFlag;
	}
	
	/** {@inheritDoc} */
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
     */
    public void loadDocument(
            String aDocumentName,
            InputSource aXmlSource,
            boolean aValidateXmlContent)
    throws	IOException,
    		ParserConfigurationException,
    		SAXException
    {
        this.validationType = (aValidateXmlContent) ? DTD_VALIDATION : NO_VALIDATION;
        this.schemaURL = null;

		final DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
		fact.setCoalescing( this.isCoalescing() );
		fact.setNamespaceAware( true );
		fact.setValidating( aValidateXmlContent );

		EntityResolver resolver = this.getEntityResolver();
		if ( (resolver == null) && (aValidateXmlContent == false) )
		{
			resolver = new EmptyStreamEntityResolver();
		}

		final DocumentBuilder builder = fact.newDocumentBuilder();
		builder.setEntityResolver( resolver );
		builder.setErrorHandler( new SimpleErrorHandler() );

		final Document doc = builder.parse( aXmlSource );
		this.rootNode = doc.getDocumentElement();    	
    }
	
	/** {@inheritDoc} */
	@Override
	public void loadDocumentUsingW3CSchema(
           String aDocumentName,
           InputStream aXmlContent,
           String aSchemaURL)
	throws IOException,
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
     * @param aDocumentName The document's name
     * @param aXmlSource The document's content
     * @param aSchemaURL The W3C schema
     * @throws IOException when the stream cannot be read
     * @throws ParserConfigurationException when the XML parser cannot be created
     * @throws SAXException when the contents fail validation
     */
	public void loadDocumentUsingW3CSchema(
           String aDocumentName,
           InputSource aXmlSource,
           String aSchemaURL)
	throws IOException,
           ParserConfigurationException,
           SAXException
	{
		this.loadDocument( aDocumentName, aXmlSource, false );

		this.validationType = XMLDocument.W3C_SCHEMA_VALIDATION;
		this.schemaURL = aSchemaURL;

		this.validateUsingW3CSchema( aSchemaURL );
	}

	/**
	 * Validate this document using a W3C schema.
	 * 
     * <p>If no <code>entityResolver</code> has been provided, load the schema
     * using <code>SchemaFactory.newSchema(URL)</code>.
     * </p>
     * 
	 * @param aSchemaURL The W3C schema
	 * @throws IOException when the schema cannot be read
	 * @throws SAXException when the document does not match the schema
	 */
	public void validateUsingW3CSchema(String aSchemaURL)
	throws IOException, SAXException
	{
		final EntityResolver resolver = this.getEntityResolver();
		Schema schema = null;

		final SchemaFactory sfact = SchemaFactory.newInstance(
			XMLConstants.W3C_XML_SCHEMA_NS_URI
		);

		if ( resolver == null )
		{
			schema = sfact.newSchema( new URL(aSchemaURL) );
		}
		else
		{			
			final LSResourceResolver dresolver = new SimpleLSResourceResolver( this.getOwnerDocument(), resolver );
			sfact.setResourceResolver( dresolver );

			final Source s = XMLHelper.getInstance().newStreamSource( resolver, aSchemaURL );
			schema = sfact.newSchema( s );
		}

		final Validator validator = schema.newValidator();
		validator.setErrorHandler( new SimpleErrorHandler() );
	    validator.validate( this.getDOMSource() );
	}

	/**
	 * Write this XML document to the stated output stream.
	 * 
	 * @param aStream The output stream
	 * @param aIncludeHeader True when a XML header should be included
	 * @throws TransformerException when the document cannot be written
	 */
	public void writeDocument(OutputStream aStream, boolean aIncludeHeader)
	throws TransformerException
	{
		final TransformerFactory fact = TransformerFactory.newInstance();
		final Transformer trans = fact.newTransformer();
		final Document doc = this.getOwnerDocument();

		final EntityResolver resolver = this.getEntityResolver();
		if ( resolver != null )
		{
			trans.setURIResolver( new SimpleURIResolver(resolver) );
		}

		if ( aIncludeHeader == false )
		{
			trans.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
		}
		else if ( this.getRootNode() == doc.getDocumentElement() )
		{
			// If we're writing the whole document, add document-type settings

			trans.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );

// Document.getXmlEncoding() only exists in Xalan 2.7.0 API
//
//			if ( doc.getXmlEncoding() != null )
//			{
//				trans.setOutputProperty( OutputKeys.ENCODING, doc.getXmlEncoding() );
//			}

			if ( this.getPublicId() != null )
			{
				trans.setOutputProperty( OutputKeys.DOCTYPE_PUBLIC, this.getPublicId() );
			}			

			if ( this.getSystemId() != null )
			{
				trans.setOutputProperty( OutputKeys.DOCTYPE_SYSTEM, this.getSystemId() );
			}
		}

		final StreamResult sresult = new StreamResult( aStream );
		trans.transform( this.getDOMSource(), sresult );
	}

	/**
	 * Does this document contain the path?
	 * 
	 * @param aXPath The path
	 * @return true when yes
	 * @throws XPathExpressionException when the document cannot be searched
	 */
    public boolean contains(String aXPath)
    throws XPathExpressionException
    {
        return this.contains( this.getRootNode(), aXPath );
    }

    /**
	 * Does this document contain the path?
     * 
     * @param aAxis The node to be searched
     * @param aXPath The path
	 * @return true when yes
	 * @throws XPathExpressionException when the document cannot be searched
     */
    public boolean contains(Node aAxis, String aXPath)
	throws XPathExpressionException
	{
    	final NodeList nodes = this.listNodes( aAxis, aXPath );
    	return (nodes.getLength() > 0);
	}

    /**
     * Does the node contain a child of the stated type?
     * 
     * @param aNode The node to be searched
     * @param aType The node type
     * @return true when yes
     * 
     * @see #countChildNodesByType(Node, int)
     */
	public boolean containsNode(Node aNode, int aType)
	{
		final int count = this.countChildNodesByType( aNode, aType );
		return (count > 0);
	}

    /**
     * How many children of the stated type does the node contain?
     * 
     * @param aNode The node to be searched
     * @param aType The node type
     * @return the number
     */
	public int countChildNodesByType(Node aNode, int aType)
	{
		int count = 0;
		
		final NodeList nodes = aNode.getChildNodes();
		for ( int i=0;  i < nodes.getLength();  i++ )
		{
			final Node n = nodes.item( i );
			if ( n.getNodeType() == aType )
			{
				count++;
			}
		}
		
		return count;
	}

	/**
	 * Evaluate the xpath as a numeric value.
	 * 
     * @param aAxis The node to be searched
	 * @param aXPath The path
	 * @return the result
	 * @throws XPathExpressionException when the path cannot be evaluated
	 */
	public boolean evaluateBoolean(Node aAxis, String aXPath)
	throws XPathExpressionException
	{
		final XPathExpression expr = this.createXPathExpression( aXPath );
		final Boolean result = (Boolean) expr.evaluate( aAxis, XPathConstants.BOOLEAN );
		return result.booleanValue();
	}

	/**
	 * Evaluate the xpath as a numeric value.
	 * 
	 * @param aXPath The path
	 * @return the result
	 * @throws XPathExpressionException when the path cannot be evaluated
	 */
	public boolean evaluateBoolean(String aXPath)
	throws XPathExpressionException
	{
		return this.evaluateBoolean( this.getRootNode(), aXPath );
	}

	/**
	 * Evaluate the xpath as a boolean test.
	 * 
     * @param aAxis The node to be searched
	 * @param aXPath The path
	 * @return the numeric result
	 * @throws XPathExpressionException when the path cannot be evaluated
	 */
	public double evaluateNumber(Node aAxis, String aXPath)
	throws XPathExpressionException
	{
		final XPathExpression expr = this.createXPathExpression( aXPath );
		final Double result = (Double) expr.evaluate( aAxis, XPathConstants.NUMBER );
		return result.doubleValue();
	}

	/**
	 * Evaluate the xpath as a boolean test.
	 * 
	 * @param aXPath The path
	 * @return the numeric result
	 * @throws XPathExpressionException when the path cannot be evaluated
	 */
	public double evaluateNumber(String aXPath)
	throws XPathExpressionException
	{
		return this.evaluateNumber( this.getRootNode(), aXPath );
	}

	/**
	 * Evaluate the xpath as a string value.
	 * 
     * @param aAxis The node to be searched
	 * @param aXPath The path
	 * @return the string value
	 * @throws XPathExpressionException when the path cannot be evaluated
	 */
	public String evaluateString(Node aAxis, String aXPath)
	throws XPathExpressionException
	{
		final XPathExpression expr = this.createXPathExpression( aXPath );
		return (String) expr.evaluate( aAxis, XPathConstants.STRING );
	}

	/**
	 * Evaluate the xpath as a string value.
	 * 
	 * @param aXPath The path
	 * @return the string value
	 * @throws XPathExpressionException when the path cannot be evaluated
	 */
	public String evaluateString(String aXPath)
	throws XPathExpressionException
	{
		return this.evaluateString( this.getRootNode(), aXPath );
	}

	/** @return the W3C Document */
	public Document getOwnerDocument()
	{
		return this.getRootNode().getOwnerDocument();
	}

	/** @return the current document's root node */
	public Node getRootNode()
	{
		return this.rootNode;
	}

	/**
	 * Find the element.
	 * 
     * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @return the element or null (not found)
	 * @throws XPathExpressionException when the document cannot be searched
	 */
	public Element findElement(Node aAxis, String aXPath)
	throws XPathExpressionException
	{
		final NodeList nodes = this.listNodes( aAxis, aXPath );
		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
			if ( n instanceof Element )
			{
				return (Element) n;
			}
		}
		return null;
	}

	/**
	 * Find the nodes.
	 * 
     * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @return the <code>NodeList</code> or null when it does not exist
	 * @throws XPathExpressionException when the document cannot be searched
	 */
	public NodeList listNodes(Node aAxis, String aXPath)
	throws XPathExpressionException
	{
		final XPathExpression expr = this.createXPathExpression( aXPath );
		return (NodeList) expr.evaluate( aAxis, XPathConstants.NODESET );
	}

	/**
	 * Get the attribute's value.
	 * 
	 * @param aNode The node that contains the attribute
	 * @param aAttrName The attribute
	 * @return the attribute's value
	 * @throws XMLException when the attribute cannot be found
	 */
	public String getAttribute(Node aNode, String aAttrName)
	throws XMLException
	{
		// Alternative algorithm is to cast aNode to org.w3c.dom.Element
		// and use org.w3c.dom.Element#getAttribute(String).
        //
        // But, the following will handle any node capable of supporting
        // attributes.

		final Node attr = aNode.getAttributes().getNamedItem( aAttrName );
		if ( attr == null )
		{
			final String msg = MISSING_ATTRIBUTE + ", node=[" + aNode.getNodeName() + "], attribute=[" + aAttrName + "]";
			throw new XMLException( msg );
		}

		return attr.getNodeValue();
	}

	/**
	 * Get the attribute's value.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @param aAttrName The attribute
	 * @return the attribute's value
	 * @throws XPathExpressionException when the document cannot be searched
	 * @throws XMLException when the attribute cannot be found
	 */
	public String getAttribute(Node aAxis, String aXPath, String aAttrName)
	throws XPathExpressionException, XMLException
	{
		final NodeList nodes = this.listNodes( aAxis, aXPath );
		final StringBuffer buf = new StringBuffer();
		int count = 0;

		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node attr = nodes.item(i).getAttributes().getNamedItem( aAttrName );
			if ( attr != null )
			{
				buf.append( attr.getNodeValue() );
				count++;
			}
		}

		if ( count < 1 )
		{
			final String msg = MISSING_ATTRIBUTE + ", axis=[" + aAxis.getNodeName() + "], xpath=[" + aXPath + "], attribute=[" + aAttrName + "]";
			throw new XMLException( msg );
		}

		return buf.toString();
	}

	/**
	 * Combine the content of all child <code>COMMENT_NODE</code> nodes
	 * into a single string.
	 * 
	 * @param aNode The node
	 * @return the comment
	 * @throws XMLException when no comments are found
	 */
	public String getComment(Node aNode)
	throws XMLException
	{
		final StringBuffer buf = new StringBuffer();
		int count = 0;

		final NodeList nodes = aNode.getChildNodes();
		for ( int i=0;  i < nodes.getLength();  i++ )
		{
			final Node n = nodes.item( i );
			if ( n.getNodeType() == Node.COMMENT_NODE )
			{
				buf.append( n.getNodeValue() );
				count++;
			}
		}

		if ( count < 1 )
		{
			final String msg = MISSING_COMMENT + ", node=[" + aNode.getNodeName() + "]";
			throw new XMLException( msg );
		}

		return buf.toString();
	}

	/**
	 * Combine the content of all found <code>COMMENT_NODE</code> nodes
	 * into a single string.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @return the comment
	 * @throws XPathExpressionException when the document cannot be searched
	 * @throws XMLException when no comments are found
	 */
	public String getComment(Node aAxis, String aXPath)
	throws XPathExpressionException, XMLException
	{
		final StringBuffer buf = new StringBuffer();
		int count = 0;
		
		final NodeList nodes = this.listNodes( aAxis, aXPath );
		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
			if ( this.containsNode(n, Node.COMMENT_NODE) )
			{
				buf.append( getComment(n) );
				count++;
			}
		}

		if ( count < 1 )
		{
			final String msg = MISSING_COMMENT + ", axis=[" + aAxis.getNodeName() + "], xpath=[" + aXPath + "]";
			throw new XMLException( msg );
		}

		return buf.toString();
	}

	/**
	 * Combine the content of all child <code>CDATA_SECTION_NODE</code> nodes
	 * into a single string.
	 * 
	 * @param aNode The node
	 * @return the CDATA content
	 * @throws XMLException when no CDATA sections are found
	 */
	public String getCDATAContent(Node aNode)
	throws XMLException
	{
		final StringBuffer buf = new StringBuffer();
		int count = 0;
		
		final NodeList children = aNode.getChildNodes();
		for ( int i=0;  i < children.getLength();  i++ )
		{
			final Node c = children.item( i );
			if ( c.getNodeType() == Node.CDATA_SECTION_NODE )
			{
				buf.append( ((CDATASection)c).getData() );
				count++;
			}
		}

		if ( count < 1 )
		{
			final String msg = MISSING_CDATA + ", node=[" + aNode.getNodeName() + "]";
			throw new XMLException( msg );
		}

		return buf.toString();
	}

	/**
	 * Combine the content of all found <code>CDATA_SECTION_NODE</code> nodes
	 * into a single string.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @return the CDATA content
	 * @throws XPathExpressionException when the document cannot be searched
	 * @throws XMLException when no CDATA sections are found
	 */
	public String getCDATAContent(Node aAxis, String aXPath)
	throws XPathExpressionException, XMLException
	{
		final StringBuffer buf = new StringBuffer();
		int count = 0;

		final NodeList nodes = this.listNodes( aAxis, aXPath );
		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
			if ( this.containsNode(n, Node.CDATA_SECTION_NODE) )
			{
				buf.append( getCDATAContent(n) );
				count++;
			}
		}

		if ( count < 1 )
		{
			final String msg = MISSING_CDATA + ", axis=[" + aAxis.getNodeName() + "], xpath=[" + aXPath + "]";
			throw new XMLException( msg );
		}

		return buf.toString();
	}
	
	/**
	 * Combine the content of all child <code>TEXT_NODE</code> nodes
	 * into a single string.
	 * 
	 * @param aNode The node
	 * @return the text
	 * @throws XMLException when the query cannot be performed
	 */
	public String getNodeText(Node aNode)
	throws XMLException
	{
		final StringBuffer buf = new StringBuffer();

		final NodeList children = aNode.getChildNodes();
		for ( int i=0;  i < children.getLength();  i++ )
		{
			final Node c = children.item( i );
			if ( c.getNodeType() == Node.TEXT_NODE )
			{
				buf.append( ((Text)c).getData() );
			}
		}

		return buf.toString();
	}

	/**
	 * Combine the content of all found <code>TEXT_NODE</code> nodes
	 * into a single string.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @return the text
	 * @throws XPathExpressionException when the document cannot be searched
	 * @throws XMLException when a runtime error occurs
	 */
	public String getNodeText(Node aAxis, String aXPath)
	throws XPathExpressionException, XMLException
	{
		final StringBuffer buf = new StringBuffer();

		final NodeList nodes = this.listNodes( aAxis, aXPath );
		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
			if ( this.containsNode(n, Node.TEXT_NODE) )
			{
				buf.append( getNodeText(n) );
			}
		}

		return buf.toString();
	}

	/**
     * Combine all child <code>CDATA_SECTION_NODE</code> and
     * <code>TEXT_NODE</code> nodes into a single string.
	 *
	 * <p>Attributes cannot be accessed using this method.
	 * </p>
	 * 
	 * <p><b>Warning -</b> If using JDK 1.5 (or above), this method can
	 * be replaced with <code>Node.getTextContent</code>.
	 * </p>
	 *  
	 * @param aNode The node
	 * @return the content
	 * @throws XMLException when the query cannot be performed
	 */
	public String getNodeValue(Node aNode)
	throws XMLException
	{
		final StringBuffer buf = new StringBuffer();

		final NodeList nodes = aNode.getChildNodes();
		for ( int i=0;  i < nodes.getLength();  i++ )
		{
			final Node n = nodes.item( i );
			switch ( n.getNodeType() )
			{
			case Node.CDATA_SECTION_NODE:
			case Node.TEXT_NODE:
				buf.append( n.getNodeValue() );
				break;

			default:
				break;
			}
		}

		return buf.toString();
	}

	/**
	 * Combine the textual content of all found nodes into a single string.
	 * 
	 * <p>Only <code>CDATA_SECTION_NODE</code> and <code>TEXT_NODE</code> nodes
	 * are assumed to contain textual content.
	 * </p>
	 * 
	 * @param aXPath The search path from the document root
	 * @return the text
	 * @throws XPathExpressionException when the document cannot be searched
	 * @throws XMLException when a runtime error occurs
	 */
    public String getNodeValue(String aXPath)
    throws XPathExpressionException, XMLException
    {
        return this.getNodeValue( this.getRootNode(), aXPath );
    }
    
	/**
	 * Combine the textual content of all found nodes into a single string.
	 * 
	 * <p>Only <code>CDATA_SECTION_NODE</code> and <code>TEXT_NODE</code> nodes
	 * are assumed to contain textual content.
	 * </p>
	 * 
	 * <p>Attribute content cannot be accessed using this method.
	 * </p>
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @return the text
	 * @throws XPathExpressionException when the document cannot be searched
	 * @throws XMLException when a runtime error occurs
	 */
	public String getNodeValue(Node aAxis, String aXPath)
	throws XPathExpressionException, XMLException
	{
		final StringBuffer buf = new StringBuffer();

		final NodeList nodes = this.listNodes( aAxis, aXPath );
		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
			if (	this.containsNode(n, Node.CDATA_SECTION_NODE)
                 || this.containsNode(n, Node.TEXT_NODE) )
			{
				buf.append( this.getNodeValue(n) );
			}
		}

		return buf.toString();
	}

	/**
	 * Get the stated element.
	 * 
	 * @param aXPath The search path from the document root
	 * @return the element
	 * @throws XPathExpressionException when the document cannot be searched
	 * @throws XMLException when no nodes are matched
	 */
	public Element getElement(String aXPath)
	throws XPathExpressionException, XMLException
	{
		return this.getElement( this.getRootNode(), aXPath );
	}

	/**
	 * Get the stated element.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @return the element
	 * @throws XPathExpressionException when the document cannot be searched
	 * @throws XMLException when no nodes are matched
	 */
	public Element getElement(Node aAxis, String aXPath)
	throws XPathExpressionException, XMLException
	{
		final Element e = this.findElement( aAxis, aXPath );
		if ( e == null )
		{
			final String msg = MISSING_ELEMENT + ", axis=[" + aAxis.getNodeName() + "], xpath=[" + aXPath + "]";
			throw new XMLException( msg );
		}
		return e;
	}

	/**
	 * Get the stated element.
	 * 
	 * @param aNode The parent node
	 * @param aPos The index of the required element node (0..n-1)
	 * @return the element
	 * @throws XMLException when no nodes are matched
	 */
	public Element getElementAt(Node aNode, int aPos)
	throws XMLException
	{
		final NodeList nodes = aNode.getChildNodes();
		int eindex = aPos;

		for ( int i=0;  i < nodes.getLength()  &&  eindex > -1;  i++ )
		{
			final Node n = nodes.item( i );
			if ( n.getNodeType() == Node.ELEMENT_NODE )
			{
				if ( eindex == 0 )
				{
					return (Element)n;
				}
				eindex--;
			}
		}

		final String msg = MISSING_ELEMENT + ", node=[" + aNode.getNodeName() + "], pos=[" + aPos + "]";
		throw new XMLException( msg );
	}

	/**
	 * Get the stated element.
	 * 
	 * @param aXPath The search path from the document root
	 * @param aPos The index of the required element node (0..n-1)
	 * @return the element
	 * @throws XPathExpressionException when the document cannot be searched
	 * @throws XMLException when no nodes are matched
	 */
	public Element getElementAt(String aXPath, int aPos)
	throws XPathExpressionException, XMLException
	{
		return this.getElementAt( this.getRootNode(), aXPath, aPos );
	}

	/**
	 * Get the stated element.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @param aPos The index of the required element node (0..n-1)
	 * @return the element
	 * @throws XPathExpressionException when the document cannot be searched
	 * @throws XMLException when no nodes are matched
	 */
	public Element getElementAt(Node aAxis, String aXPath, int aPos)
	throws XPathExpressionException, XMLException
	{
		final NodeList nodes = this.listNodes( aAxis, aXPath );
		if ( nodes.getLength() > 0 )
		{
			return this.getElementAt( nodes.item(0), aPos );
		}

		final String msg = MISSING_ELEMENT + ", axis=[" + aAxis.getNodeName() + "], xpath=[" + aXPath + "], pos=[" + aPos + "]";
		throw new XMLException( msg );
	}

	/**
	 * Add a <code>CDATA_SECTION_NODE</code>.
	 * 
	 * @param aNode The parent node
	 * @param aValue The CDATA content
	 */
	public void appendCDATASection(Node aNode, String aValue)
	{
		final CDATASection n = aNode.getOwnerDocument().createCDATASection( aValue );
		aNode.appendChild( n );
	}

	/**
	 * Add a <code>CDATA_SECTION_NODE</code> to all found nodes.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @param aValue The CDATA content
	 * @return the list of found elements
	 * @throws XPathExpressionException when the document cannot be searched
	 */
    public Collection<Node> appendCDATASection(
	        Node aAxis,
	        String aXPath,
	        String aValue)
	throws  XPathExpressionException
	{
		final NodeList nodes = this.listNodes( aAxis, aXPath );
        final Collection<Node> changed = new ArrayList<Node>();

        for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
			this.appendCDATASection( n, aValue );
			changed.add( n );
		}

		return changed;
	}

	/**
	 * Add a <code>COMMENT_NODE</code>.
	 * 
	 * @param aNode The parent node
	 * @param aValue The comment
	 */
    public void appendComment(Node aNode, String aValue)
    {
        final Comment n = aNode.getOwnerDocument().createComment( aValue );
        aNode.appendChild( n );
    }

	/**
	 * Add a <code>COMMENT_NODE</code> to all found nodes.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @param aValue The comment
	 * @return the list of found elements
	 * @throws XPathExpressionException when the document cannot be searched
	 */
    public Collection<Node> appendComment(Node aAxis, String aXPath, String aValue)
	throws XPathExpressionException
	{
		final NodeList nodes = this.listNodes( aAxis, aXPath );
		final Collection<Node> changed = new ArrayList<Node>();

		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
			appendComment( n, aValue );
			changed.add( n );
		}

		return changed;
	}

	/**
	 * Add a new element.
	 * 
	 * @param aNode The parent node
	 * @param aElementName The element's name
	 * @return the new element
	 */
	public Element appendElement(Node aNode, String aElementName)
	{
		Node n = aNode;
        if ( n == null )
        {
            n = this.getRootNode();
        }
        
        final Element e = n.getOwnerDocument().createElement( aElementName );
		n.appendChild( e );
		return e;
	}

	/**
	 * Add a new element to all found nodes.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @param aElementName The element's name
	 * @return the list of found elements
	 * @throws XPathExpressionException when the document cannot be searched
	 */
	public Collection<Node> appendElement(
            Node aAxis,
            String aXPath,
            String aElementName)
	throws  XPathExpressionException
	{
		final NodeList nodes = this.listNodes( aAxis, aXPath );
		final Collection<Node> changed = new ArrayList<Node>();

		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
			this.appendElement( n, aElementName );
			changed.add( n );
		}

		return changed;
	}

	/**
	 * Add a <code>TEXT_NODE</code>.
	 * 
	 * @param aNode The parent node
	 * @param aValue The text
	 */
	public void appendText(Node aNode, String aValue)
	{
		final Text n = aNode.getOwnerDocument().createTextNode( aValue );
		aNode.appendChild( n );
	}

	/**
	 * Add a <code>TEXT_NODE</code> to all found nodes.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @param aValue The text
	 * @return the list of found elements
	 * @throws XPathExpressionException when the document cannot be searched
	 */
	public Collection<Node> appendText(Node aAxis, String aXPath, String aValue)
	throws XPathExpressionException
	{
		final NodeList nodes = this.listNodes( aAxis, aXPath );
		final Collection<Node> changed = new ArrayList<Node>();

		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
			this.appendText( n, aValue );
			changed.add( n );
		}

		return changed;
	}

	/**
	 * Remove all attributes.
	 * 
	 * @param aNode The parent node
	 * @return the number of attributes removed
	 */
	public int removeAttribute(Node aNode)
	{
		final NamedNodeMap attrs = aNode.getAttributes();
		final int count = attrs.getLength();
		
		while (attrs.getLength() > 0)
		{
			final Node n = attrs.item( attrs.getLength()-1 );
			attrs.removeNamedItem( n.getNodeName() );
		}
		
		return count;
	}

	/**
	 * Remove all attributes from all found nodes.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @return the list of affected elements
	 * @throws XPathExpressionException when the document cannot be searched
	 */
	public Collection<Node> removeAttributes(Node aAxis, String aXPath)
	throws XPathExpressionException
	{
		final NodeList nodes = this.listNodes( aAxis, aXPath );
		final Collection<Node> changed = new ArrayList<Node>();

		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
			if ( this.removeAttribute(n) > 0 )
			{
                changed.add( n );
			}
		}

		return changed;
	}

	/**
	 * Remove all child <code>CDATA_SECTION_NODE</code>.
	 * 
	 * @param aNode The parent node
	 * @return the number of removed nodes
	 */
	public int removeCDATAContent(Node aNode)
	{
		return this.removeNodeByType( aNode, Node.CDATA_SECTION_NODE );
	}

	/**
	 * Remove all child <code>CDATA_SECTION_NODE</code> from all found nodes.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @return the list of affected elements
	 * @throws XPathExpressionException when the document cannot be searched
	 */
	public Collection<Node> removeCDATAContent(Node aAxis, String aXPath)
	throws XPathExpressionException
	{
		final NodeList nodes = this.listNodes( aAxis, aXPath );
		final Collection<Node> changed = new ArrayList<Node>();

		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
			if ( this.removeCDATAContent(n) > 0 )
			{
                changed.add( n );
			}
		}

		return changed;
	}

	/**
	 * Remove all child <code>COMMENT_NODE</code>.
	 * 
	 * @param aNode The parent node
	 * @return the number of removed nodes
	 */
	public int removeComment(Node aNode)
	{
		return this.removeNodeByType( aNode, Node.COMMENT_NODE );
	}

	/**
	 * Remove all child <code>COMMENT_NODE</code> from all found nodes.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @return the list of affected elements
	 * @throws XPathExpressionException when the document cannot be searched
	 */
	public Collection<Node> removeComment(Node aAxis, String aXPath)
	throws XPathExpressionException
	{
		final NodeList nodes = this.listNodes( aAxis, aXPath );
		final Collection<Node> changed = new ArrayList<Node>();

		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
			if ( this.removeComment(n) > 0 )
			{
                changed.add( n );
			}
		}

		return changed;
	}

	/**
	 * Remove all elements.
	 * 
	 * @param aNode The parent node
	 * @param aElementName The element's name
	 * @return the number of affected elements
	 */
	public int removeElementByName(Node aNode, String aElementName)
	{
		final NodeList nodes = aNode.getChildNodes();
		int count = 0;

		for ( int i=0;  i < nodes.getLength();  i++ )
		{
			final Node n = nodes.item( i );
            if (	(n.getNodeType() == Node.ELEMENT_NODE)
                 && (n.getNodeName().equals(aElementName)) )
			{
				aNode.removeChild( n );
				count++;
			}
		}

		return count;
	}
 
	/**
	 * Remove all child elements.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @param aElementName The element to be removed
	 * @return the list of affected elements
	 * @throws XPathExpressionException when the document cannot be searched
	 */
	public Collection<Node> removeElementByName(
			Node aAxis,
			String aXPath,
			String aElementName)
	throws  XPathExpressionException
	{
		final NodeList nodes = this.listNodes( aAxis, aXPath );
		final Collection<Node> changed = new ArrayList<Node>();

		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
			if ( this.removeElementByName(n, aElementName) > 0 )
			{
                changed.add( n );
			}
		}

		return changed;
	}

	/**
	 * Remove all child nodes of the stated type.
	 * 
	 * @param aNode The parent node
	 * @param aNodeType The node type
	 * @return the number of children removed
	 */
	public int removeNodeByType(Node aNode, int aNodeType)
	{
		final NodeList nodes = aNode.getChildNodes();
		int count = 0;

		for ( int i=0;  i < nodes.getLength();  i++ )
		{
			final Node n = nodes.item( i );
			if ( n.getNodeType() == aNodeType )
			{
				aNode.removeChild( n );
				count++;
				i--;
			}
		}

		return count;
	}

	/**
	 * Replace the node's textual value.
	 * 
	 * @param aNode The parent node
	 * @param aValue The new text
	 */
	public void replaceText(Node aNode, String aValue)
	{
		removeNodeByType( aNode, Node.TEXT_NODE );
		appendText( aNode, aValue );
	}

	/**
	 * Change the attribute's value.
	 * 
	 * @param aNode The parent element
	 * @param aAttrName The attribute's name
	 * @param aValue The new value
	 */
	public void setAttribute(Element aNode, String aAttrName, String aValue)
	{
		// org.w3c.dom.Attr nodes cannot be added as child nodes.
		// Must use setAttribute().

		aNode.setAttribute( aAttrName, aValue );
	}

	/**
	 * Change the attribute on all found elements.
	 * 
	 * @param aAxis The node to be searched
	 * @param aXPath The search path
	 * @param aAttrName The attribute's name
	 * @param aValue The new value
	 * @return the list of affected elements
	 * @throws XPathExpressionException when the document cannot be searched
	 */
    public Collection<Node> setAttribute(
            Node aAxis,
            String aXPath,
            String aAttrName,
            String aValue)
    throws  XPathExpressionException
    {
		final NodeList nodes = this.listNodes( aAxis, aXPath );
		final Collection<Node> changed = new ArrayList<Node>();

		for ( int i=0;  i < nodes.getLength();  i++)
		{
			final Node n = nodes.item( i );
            if ( n.getNodeType() == Node.ELEMENT_NODE )
            {
                setAttribute( (Element)n, aAttrName, aValue );
                changed.add( n );
            }
        }

        return changed;
    }

	/**
	 * Tranform the XML file using an XSL stylesheet.
	 *
	 * @param aXslStylesheet The XSL stylesheet
	 * @param aParameters Transformation parameters (can be null)
	 * @param aOutput The output stream
	 * @throws TransformerException when the transformation fails
	 */
	public void transform(String aXslStylesheet, Map<String,Object> aParameters, Writer aOutput)
	throws TransformerException
	{
		final Templates tmpl = XMLHelper.getInstance().newTransformerTemplate(
			this.getEntityResolver(), aXslStylesheet
		);
		this.transform( tmpl, aParameters, aOutput );
	}
    
	/**
	 * Transform this document using a cached transformer.
	 * 
	 * @param aTmpl The cached XSL stylesheet
	 * @param aParameters Transformation parameters (can be null)
	 * @param aOutput The output stream
	 * @throws TransformerException when the transformation fails
	 */
	public void transform(Templates aTmpl, Map<String,Object> aParameters, Writer aOutput)
	throws TransformerException
	{		
		final Transformer trans = aTmpl.newTransformer();
		XMLHelper.getInstance().setTransformerParameters( trans, aParameters );
		
		final EntityResolver resolver = this.getEntityResolver();
		if ( resolver != null )
		{
			trans.setURIResolver( new SimpleURIResolver(resolver) );
		}

		final Result result = new StreamResult( aOutput );
		trans.transform( this.getDOMSource(), result );
	}

	/**
	 * Compare XML attributes.
	 * 
	 * <p>Attribute ordering is ignored.
	 * </p>
	 * 
	 * @param aNode The first node
	 * @param aOther The second node
	 * @return true when both sets contain the same attributes
	 * @throws XMLException when the query cannot be performed
     * @since 3.0
	 * 
	 * @see #getUseRegexPatternMatching()
	 * @see #matchStrings(String, String)
	 */
	public boolean matchAttributes(Node aNode, Node aOther)
	throws XMLException
	{
		final NamedNodeMap attrs = aNode.getAttributes();
		final NamedNodeMap other_attrs = aOther.getAttributes();
		
		if ( attrs.getLength() != other_attrs.getLength() )
		{
			LOGGER.info("Attribute count mismatch: " + aNode.getNodeName());
			return false;
		}
		
		for ( int i=0;  i < attrs.getLength();  i++)
		{
			final Attr attr = (Attr) attrs.item(i);
			final Attr other = (Attr) other_attrs.getNamedItem( attr.getName() );
			
			if ( other == null )
			{
				LOGGER.info("Cannot find attribute: " + aOther.getNodeName() + "@" + attr.getName());				
				return false;
			}
			
			if ( !this.matchStrings(attr.getValue(), other.getValue()) )
			{
				LOGGER.info("Attribute value mismatch: " + aNode.getNodeName() + "@" + attr.getName());				
				return false;
			}
		}
		
		return true;
	}

	/**
	 * Compare XML nodes.
	 * 
	 * <p>Compares ...
	 * <ol>
	 * <li>Node type
	 * <li>Attributes
	 * <li>CDATA content
	 * <li>Text content
	 * <li>Children
	 * </ol>
	 * </p>
	 * 
	 * @param aNode The first node
	 * @param aOther The second node
	 * @return true when both nodes contain the same attributes and content
	 * @throws XMLException when the query cannot be performed
     * @since 3.0
	 * 
	 * @see #getCDATAContent(Node)
	 * @see #getNodeText(Node)
	 * @see #getUseRegexPatternMatching()
	 * @see #matchAttributes(Node, Node)
	 * @see #matchStrings(String, String)
 	 */
	public boolean matchNodes(Node aNode, Node aOther)
	throws XMLException
	{
		if ( aNode.getNodeType() != aOther.getNodeType() )
		{
			LOGGER.info("Node type mismatch: " + aNode.getNodeName());
			return false;
		}

		if ( !aNode.getNodeName().equals(aOther.getNodeName()) )
		{
			LOGGER.info("Node name mismatch: " + aNode.getNodeName() + " != " + aOther.getNodeName());
			return false;
		}

		if ( !this.matchAttributes(aNode, aOther) )
		{
			LOGGER.info("Attributes mismatch: " + aNode.getNodeName());				
			return false;
		}

		if ( this.containsNode(aNode, Node.CDATA_SECTION_NODE) )
		{
			try
			{
				final String other = this.getCDATAContent(aOther);
				if ( !this.matchStrings(this.getCDATAContent(aNode), other) )
				{
					LOGGER.info("CDATA mismatch: " + aNode.getNodeName());
					return false;
				}
			}
			catch (XMLException e)
			{
				LOGGER.info("Cannot find CDATA: " + aNode.getNodeName());
				return false;
			}
		}
		
		if ( !this.matchStrings(this.getNodeText(aNode), this.getNodeText(aOther)) )
		{
			LOGGER.info("Node text mismatch: " + aNode.getNodeName());
			return false;
		}			

		// only compare element children
		
		int ecount = this.countChildNodesByType( aNode, Node.ELEMENT_NODE );
		if ( ecount != this.countChildNodesByType(aOther, Node.ELEMENT_NODE) )		
		{
			LOGGER.info("Node child count mismatch: " + aNode.getNodeName());
			return false;
		}
		
		final NodeList nlist = aNode.getChildNodes();
		int i = 0;

		final NodeList nlist_other = aOther.getChildNodes();
		int j = 0;
		
		for (;  ecount  > 0;  ecount-- )
		{
			Node n = nlist.item(i++);
			while ( n.getNodeType() != Node.ELEMENT_NODE )
			{
				n = nlist.item(i++);
			}
			
			Node n_other = nlist_other.item(j++);
			while ( n_other.getNodeType() != Node.ELEMENT_NODE )
			{
				n_other = nlist_other.item(j++);
			}

			if ( !this.matchNodes(n, n_other) )
			{
				LOGGER.info("Node content mismatch: " + aNode.getNodeName());
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Compare two strings.
	 * 
	 * <p>Sets the {@link Pattern#DOTALL} option, allowing '.' to match
	 * newline characters.
	 * </p>
	 * 
	 * @param aValue The first string
	 * @param aOther The second string
	 * @return true when the strings match
     * @since 3.0
	 * 
	 * @see #getUseRegexPatternMatching()
	 */
	protected boolean matchStrings(String aValue, String aOther)
	{
		if ( this.getUseRegexPatternMatching() )
		{
			final Pattern p = Pattern.compile( aOther, Pattern.DOTALL );
			final Matcher m = p.matcher( aValue );
			if ( !m.find() )
			{
				return false;
			}
		}
		else if ( !aValue.equals(aOther) )
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Create and compile a new <code>XPathExpression</code>.
	 * 
	 * @param aXPath The expression to compile
	 * @return the new expr
	 * @throws XPathExpressionException when the expression cannot be created
	 */
	protected XPathExpression createXPathExpression(String aXPath)
	throws XPathExpressionException
	{
		final XPath xpath = XMLHelper.getInstance().newXPath();
		
		if ( this.getNamespaceContext() != null )
		{
			xpath.setNamespaceContext( this.getNamespaceContext() );
		}

		return xpath.compile( aXPath );
	}

	/**
	 * Create the <code>DOMSource</code> for this document.
	 * 
	 * @return the source
	 */
	protected DOMSource getDOMSource()
	{
		final Document d = this.getOwnerDocument();
		final Node r = this.getRootNode();
		if ( d.getDocumentElement() == r )
		{
			return new DOMSource( d );
		}
		return new DOMSource( r );
	}

}
