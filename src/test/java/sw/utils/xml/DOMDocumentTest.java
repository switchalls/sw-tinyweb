package sw.utils.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import junit.framework.TestCase;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import sw.test.JunitTestHelper;
import sw.utils.xml.contexts.SimpleNamespaceContext;
import sw.utils.xml.resolvers.ClassPathEntityResolver;
import sw.utils.xml.resolvers.SingleLocationEntityResolver;

/**
 * <code>DOMDocument</code> test suite.
 * 
 * <p>Tests:
 * <ol>
 * <li>Creating and populating a DOM
 * <li>Using a <code>NamespaceContext</code>
 * <li>DOM storage
 * <li>Find attribute
 * <li>Find CDATA content
 * <li>Find comments
 * <li>Find elements
 * <li>Remove attribute
 * <li>Remove CDATA content
 * <li>Remove comments
 * <li>Remove elements
 * <li>Remove element content
 * <li>Replace element content
 * <li><code>XPath</code> evaluation
 * <li>DTD validation
 * <li>W3C schema validation
 * <li>Write out a DOM
 * <li>XSLT transformations
 * <li>Using entity resolvers
 * </ol>
 * </p>
 * 
 * <p>
 * Most tests use {@link MockDOMDocument}.
 * </p>
 * 
 * @author $Author: $ 
 * @version $Revision: $
 */
public class DOMDocumentTest extends TestCase
{
	private static final String FINDME = "FINDME.txt";

	private File xmlHome;

	@Override
	public void setUp()
	throws Exception
	{		
        super.setUp();        
        this.xmlHome = JunitTestHelper.getResourceParentFile( this.getClass(), FINDME );
	}

	/**
	 * Create a DOM.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testCreateDom()
	throws Exception
	{
		// No public-id etc

		final MockDOMDocument tdoc = new MockDOMDocument();
		tdoc.assertValidContent();
		assertNull( tdoc.getPublicId() );
		assertNull( tdoc.getSystemId() );
		assertNull( tdoc.getRootNode().getNamespaceURI() );

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		tdoc.writeDocument( out, true );
		String xml = new String(out.toByteArray());
		assertTrue( xml.indexOf("<!DOCTYPE") < 0 ); // No DOCTYPE written because no id(s)

		out = new ByteArrayOutputStream();
		tdoc.writeDocument( out, false );
		xml = new String(out.toByteArray());
		assertTrue( xml.indexOf("<!DOCTYPE") < 0 ); // no header

		// With public-id etc

		final DOMDocument doc = new DOMDocument( null, "publicId", "systemId", "Junit" );
		doc.appendElement( doc.getRootNode(), "r1" );

		assertEquals( "publicId", doc.getPublicId() );
		assertEquals( "systemId", doc.getSystemId() );
		assertNull( doc.getRootNode().getNamespaceURI() );
		assertTrue( doc.contains("/Junit/r1") );
		assertTrue( doc.contains("r1") );

		out = new ByteArrayOutputStream();
		doc.writeDocument( out, true );
		xml = new String(out.toByteArray());
		assertTrue( xml.indexOf("<!DOCTYPE Junit PUBLIC \"publicId\" \"systemId\">") > -1 );

		out = new ByteArrayOutputStream();
		doc.writeDocument( out, false );
		xml = new String(out.toByteArray());
		assertTrue( xml.indexOf("<!DOCTYPE") < 0 ); // no header
	}

	/**
	 * Using a <code>NamespaceContext</code>.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testNamespaceContext()
	throws Exception
	{
		final DOMDocument doc = new DOMDocument( "http://foo.bar.com", null, null, "Junit" );
		assertEquals( "http://foo.bar.com", doc.getRootNode().getNamespaceURI() );
		doc.appendElement( doc.getRootNode(), "r1" );

		// No default namespace prefix defined for this DOM, so must use a
		// NamespaceContext to map namespace prefixes to namespaces.
		// Otherwise, cannot find root element usign a XPath.

		doc.setNamespaceContext( new SimpleNamespaceContext("foo", "http://foo.bar.com") );
		assertTrue( doc.contains("/foo:Junit/r1") );

		// Elements below root do not have a namespace, so don't need
		// a namespace prefix for them.

		assertTrue( doc.contains("//r1") );		
	}

	/**
	 * Can we find attributes?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testFindAttribute()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();

		final Element r1 = d.getElement("r1");
		assertEquals("attr-value", d.getAttribute(r1, "r1-attr"));
		
		try
		{
			final String s = d.getAttribute(r1, "xx-attr");
			fail("getAttribute(bad attribute) worked: "+s);
		}
		catch (XMLException e)
		{
			// correct
		}

		assertEquals("attr-value", d.getAttribute(d.getRootNode(), "r1", "r1-attr"));

		try
		{
			final String s = d.getAttribute(d.getRootNode(), "r1", "xx-attr");
			fail("getAttribute(bad attribute) worked: "+s);
		}
		catch (XMLException e)
		{
			// correct
		}
	}

	/**
	 * Can we remove attributes?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testRemoveAttributes()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();
		
		List<Node> removed = (List<Node>) d.removeAttributes(d.getRootNode(), "r1");
		assertEquals(1, removed.size());
		
		final Node n = removed.get(0);
		assertEquals("r1", n.getNodeName());

		// no attributes
		
		removed = (List<Node>) d.removeAttributes(d.getRootNode(), "r2");
		assertEquals(0, removed.size());

		// invalid xpath
		
		removed = (List<Node>) d.removeAttributes(d.getRootNode(), "does-not-exist");
		assertEquals(0, removed.size());
	}

	/**
	 * Can we find CDATA content?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testFindCDATAContent()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();

		final Element r3 = d.getElement("r3");
		final Element e4_1 = d.getElement( "r4/e4.1" );
		assertEquals( "e4.1-CDATA", d.getCDATAContent(e4_1) );

		try
		{
			final String s = d.getCDATAContent(r3);
			fail("getCDATAContent(no CDATA content) worked: "+s);
		}
		catch (XMLException e)
		{
			// correct
		}

		assertEquals( "e4.1-CDATA", d.getCDATAContent(d.getRootNode(), "r4/e4.1") );

		try
		{
			final String s = d.getCDATAContent(d.getRootNode(), "r3");
			fail("getCDATAContent(no CDATA content) worked: "+s);
		}
		catch (XMLException e)
		{
			// correct
		}
	}

	/**
	 * Can we remove CDATA content?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testRemoveCDATAContent()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();
		
		List<Node> removed = (List<Node>) d.removeCDATAContent(d.getRootNode(), "r4/e4.1");
		assertEquals(1, removed.size());
		
		final Node n = removed.get(0);
		assertEquals("e4.1", n.getNodeName());

		// no CDATA
		
		removed = (List<Node>) d.removeCDATAContent(d.getRootNode(), "r4");
		assertEquals(0, removed.size());

		// invalid xpath
		
		removed = (List<Node>) d.removeCDATAContent(d.getRootNode(), "does-not-exist");
		assertEquals(0, removed.size());
	}

	/**
	 * Can we find comments?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testFindComment()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();
		
		final Element r3 = d.getElement("r3");
		final Element e4_1 = d.getElement("r4/e4.1");

		assertEquals( "r3-comment", d.getComment(r3) );

		try
		{
			final String s = d.getComment(e4_1);
			fail("getComment(no comments) worked: "+s);
		}
		catch (XMLException e)
		{
			// correct
		}

		assertEquals( "r3-comment", d.getComment(d.getRootNode(), "r3") );

		try
		{
			final String s = d.getComment(d.getRootNode(), "r4/e4.1");
			fail("getComment(no comments) worked: "+s);
		}
		catch (XMLException e)
		{
			// correct
		}
	}

	/**
	 * Can we remove comments?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testRemoveComment()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();
		
		List<Node> removed = (List<Node>) d.removeComment(d.getRootNode(), "r3");
		assertEquals(1, removed.size());
		
		final Node n = removed.get(0);
		assertEquals("r3", n.getNodeName());

		// no comments
		
		removed = (List<Node>) d.removeComment(d.getRootNode(), "r4/e4.1");
		assertEquals(0, removed.size());

		// invalid xpath
		
		removed = (List<Node>) d.removeComment(d.getRootNode(), "does-not-exist");
		assertEquals(0, removed.size());
	}

	/**
	 * Can we evaluate boolean XPaths?
	 * 
	 * @throws ParserConfigurationException when the DOM cannot be created
	 * @throws TransformerException when the DOM cannot be created
	 * @throws XPathExpressionException when the DOM cannot be searched
	 */
	public void testEvaluateBoolean()
	throws ParserConfigurationException, TransformerException, XPathExpressionException
	{
		final MockDOMDocument d = new MockDOMDocument();

		// There is only one "r3" element
		assertTrue( "r3", d.evaluateBoolean("r3") );
		assertTrue( "count(r3)", d.evaluateBoolean("count(r3)") );

		// Only { "r2", "r4" } have a "find-this" child whose value is "find-me"
		assertTrue( "*[find-this = 'find-me']", d.evaluateBoolean("*[find-this = 'find-me']") );

		// No elements have "not-me"
		assertFalse( "*[find-this = 'not-me']", d.evaluateBoolean("*[find-this = 'not-me']") );
	}

	/**
	 * Can we evaluate numeric XPaths?
	 * 
	 * @throws ParserConfigurationException when the DOM cannot be created
	 * @throws TransformerException when the DOM cannot be created
	 * @throws XPathExpressionException when the DOM cannot be searched
	 */
	public void testEvaluateNumber()
	throws ParserConfigurationException, TransformerException, XPathExpressionException
	{
		final MockDOMDocument d = new MockDOMDocument();

		// There is only one "r3" element
		assertEquals( 1.0, d.evaluateNumber("count(r3)") );

		// Only { "r2", "r4" } have a "find-this" child whose value is "find-me"
		assertEquals( 2.0, d.evaluateNumber("count(*[find-this = 'find-me'])") );
	}

	/**
	 * Can we evaluate string XPaths?
	 * 
	 * @throws ParserConfigurationException when the DOM cannot be created
	 * @throws TransformerException when the DOM cannot be created
	 * @throws XPathExpressionException when the DOM cannot be searched
	 */
	public void testEvaluateString()
	throws ParserConfigurationException, TransformerException, XPathExpressionException
	{
		final MockDOMDocument d = new MockDOMDocument();

		// Only { "r2", "r4" } have a "find-this" child whose value is "find-me"
		assertEquals( "*[find-this = 'find-me']", "find-me", d.evaluateString("*[find-this = 'find-me']") );
	}

	/**
	 * Can we find nodes?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testFindNodes()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();

		// Find { "r6"  } by xpath

		NodeList nodes = d.listNodes(d.getRootNode(), "r6");		
		assertEquals( "r6", 3, nodes.getLength() );

		// Find { "r2", "r4", "r5"  } by existence of child element

		nodes = d.listNodes(d.getRootNode(), "*[find-this]");
		assertEquals( "*[find-this]", 3, nodes.getLength() );

		// Find { "r2", "r4"  } by child value

		nodes = d.listNodes(d.getRootNode(), "*[find-this = 'find-me']");
		assertEquals( "*[find-this = 'find-me']", 2, nodes.getLength() );

		// Find { "r5"  } by child value

		nodes = d.listNodes(d.getRootNode(), "*[find-this != 'find-me']");
		assertEquals( "*[find-this != 'find-me']", 1, nodes.getLength() );

		// Find { "r4/e4.1/e4.1.1" } by child value

		nodes = d.listNodes(d.getRootNode(), "*/*/*[find-this = 'find-me']");
		assertEquals( "*/*/*[find-this='find-me']", 1, nodes.getLength() );

		// Find { "r3" }  by existence of attribute

		nodes = d.listNodes(d.getRootNode(), "*[@r3-attr]");
		assertEquals( "*[@r3-attr]", 1, nodes.getLength() );
	}

	/**
	 * Can we get elements by child index?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testFindElement()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();
				
		assertNotNull( "r2", d.findElement(d.getRootNode(), "r2") );
		assertNotNull( "r4/e4.1/e4.1.1", d.findElement(d.getRootNode(), "r4/e4.1/e4.1.1") );

		assertNotNull( "r2", d.getElement(d.getRootNode(), "r2") );
		assertNotNull( "r4/e4.1/e4.1.1", d.getElement(d.getRootNode(), "r4/e4.1/e4.1.1") );

		// findElement() should return null when it cannot find any elements
		
		assertNull( "does-not-exist", d.findElement(d.getRootNode(), "does-not-exist") );
		
		// getElement() should throw an error when it cannot find any elements
		
		try
		{
			final Element e = d.getElement( d.getRootNode(), "does-not-exist" );
			fail( "found does-not-exist: " + e.getNodeName() );
		}
		catch (XMLException e)
		{
			// correct
		}
	}

	/**
	 * Can we get elements by child index?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testElementAt()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();
		
		final Element e1_1 = d.getElement("r1/e1.1");
		final Element c1 = d.getElementAt(e1_1, 0);
		assertEquals("e1.1.1", c1.getNodeName());
		Element c2 = d.getElementAt(e1_1, 1);
		assertEquals("e1.1.2", c2.getNodeName());

		// position > child number
		
		try
		{
			final Element enode = d.getElementAt( "r1", 1 );
			fail( "getElementAt(position > child#): " + enode.getNodeName() );
		}
		catch (XMLException e)
		{
			// correct
		}

		// xpath versions
		
		c2 = d.getElementAt("r1/e1.1", 1);
		assertEquals("e1.1.2", c2.getNodeName());
		
		try
		{
			final Element enode = d.getElementAt("does-not-exist", 1);
			fail( "getElementAt(invalid xpath): " + enode.getNodeName() );
		}
		catch (XMLException e)
		{
			// correct
		}
	}

	/**
	 * Can we remove elements?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testRemoveElement()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();

		final List<Node> removed = (List<Node>) d.removeElementByName(d.getRootNode(), "r4", "e4.1");
		assertEquals(1, removed.size());
		
		final Node n = removed.get(0);
		assertEquals( "r4", n.getNodeName() );
		
		assertNull( d.findElement(d.getRootNode(), "r4/e4.1") );

		final int removedCount = d.removeElementByName(d.getRootNode(), "r4");
		assertEquals(1, removedCount);
		
		assertNull( d.findElement(d.getRootNode(), "r4/e4.1") );
	}
	
	/**
	 * What happens when invalid <code>XPath</code>s are used?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testInvalidXPath()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();
		try
		{
			final Element enode = d.getElement( "does-not-exist" );
			fail( "found element" + enode.getNodeName() );
		}
		catch (XMLException e)
		{
			// correct
		}
	}

	/**
	 * Does <code>getNodeText</code> work as expected?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testNodeText()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();

		final Element e4_1_1 = d.getElement( "r4/e4.1/e4.1.1" );
		assertEquals( "e4.1.1-text", d.getNodeText(e4_1_1) );

		assertEquals( "e4.1.1-text", d.getNodeText(d.getRootNode(), "r4/e4.1/e4.1.1") );
	}

	/**
	 * Does <code>getNodeValue</code> work as expected?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testNodeValue()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();

		assertEquals( "e4.1-CDATA", d.getNodeValue("r4/e4.1") );

		assertEquals( "e4.1.1-text", d.getNodeValue("r4/e4.1/e4.1.1") );
		assertEquals( "r6-text-1-2-3,r6-text-2,r6-text-3", d.getNodeValue("r6") );
		
		// Find by attribute will return the attribute owner's text, not the
		// attribute value itself!
		
		assertEquals( "*[@r3-attr]", "r3-text", d.getNodeValue("*[@r3-attr]") );

		// Only { "r2", "r4", "r5" } have a "find-this" child
		//
		// NB. "r5" may be found before "r4"??!!
		
		final String nvalue = d.getNodeValue("*/find-this");
		if ( nvalue.equals("find-mefind-meignore") == false )
		{
			assertEquals( "*/find-this", "find-meignorefind-me", nvalue );
		}
	}

	/**
	 * Can we replace text?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testReplaceText()
	throws Exception
	{
		final MockDOMDocument d = new MockDOMDocument();
		
		assertEquals( "r6-text-1-2-3,r6-text-2,r6-text-3", d.getNodeValue("r6") );
		
		final Element r6 = d.getElement("r6");
		d.replaceText(r6, "new-text");

		// NB. There are 3 "r6" nodes; replaceText() only changes value on first instance found
		assertEquals( "new-text,r6-text-2,r6-text-3", d.getNodeValue("r6") );
	}

	/**
	 * Can the DOM store itself?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testStoreDOM()
	throws Exception
	{
		final MockDOMDocument dout = new MockDOMDocument();

		final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
		dout.writeDocument( out, true );

		//printXMLFile( out.toByteArray() );

		final MockDOMDocument din = new MockDOMDocument();

		final ByteArrayInputStream xmlContent = new ByteArrayInputStream( out.toByteArray() );
		din.loadDocument( "Junit", xmlContent, false );
		din.assertValidContent();
	}

	/**
	 * Can we load a XML file without using validation?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testLoadFile_NoValidation()
	throws Exception
	{
		FileInputStream fin = new FileInputStream( new File(this.xmlHome,"dtd-note.xml") );
		try
		{
			final DOMDocument d = new DOMDocument();
			assertNull( d.getEntityResolver() );
			d.loadDocument( "tdoc", fin, false );
			
			assertFalse(d.isCoalescing());
			assertFalse(d.isValidated());
			assertEquals("note", d.getDocumentName());
			assertEquals("none", d.getValidationType());
			assertNull(d.getSchemaURL());
			
			fin.close();
			fin = new FileInputStream( new File(this.xmlHome,"dtd-note.xml") );
			assertNull( d.getEntityResolver() );
			d.loadDocument( "tdoc", fin, false );			
		}
		finally
		{
			fin.close();
		}
	}

	/**
	 * Load a valid DTD controlled file.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testLoadFile_DTD()
	throws Exception
	{
		// load document with DTD
		
		FileInputStream fin = new FileInputStream( new File(this.xmlHome,"dtd-note.xml") );
		try
		{
			final DOMDocument d = new DOMDocument();
			d.setEntityResolver( new SingleLocationEntityResolver(this.xmlHome.toURL()) );
			d.loadDocument( "tdoc", fin, true );			
			
			assertFalse(d.isCoalescing());
			assertTrue(d.isValidated());
			assertEquals("note", d.getDocumentName());
			assertEquals("dtd", d.getValidationType());
			assertNull(d.getSchemaURL());
		}
		finally
		{
			fin.close();
		}
		
		// cannot find DTD (no entity resolver)
		
		fin = new FileInputStream( new File(this.xmlHome,"dtd-note.xml") );
		try
		{
			final DOMDocument d = new DOMDocument();
			assertNull( d.getEntityResolver() );
			d.loadDocument( "tdoc", fin, true );
			fail("loadDocument(no entity resolver) worked");
		}
		catch (FileNotFoundException e)
		{			
			assertTrue( e.getMessage().indexOf("note.dtd (No such file or directory)") > -1 );
		}
		finally
		{
			fin.close();
		}

		// validation off, therefore does not matter if we cannot find DTD
		
		fin = new FileInputStream( new File(this.xmlHome,"dtd-note.xml") );
		try
		{
			final DOMDocument d = new DOMDocument();
			assertNull( d.getEntityResolver() );
			d.loadDocument( "tdoc", fin, false );
			
			assertFalse(d.isCoalescing());
			assertFalse(d.isValidated());
			assertEquals("note", d.getDocumentName());
			assertEquals("none", d.getValidationType());
			assertNull(d.getSchemaURL());
		}
		finally
		{
			fin.close();
		}
	}

	/**
	 * Try to load an invalid DTD controlled file.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testBadFile_DTD()
	throws Exception
	{
		final FileInputStream fin = new FileInputStream( new File(this.xmlHome,"dtd-bad-note.xml") );
		try
		{
			final DOMDocument d = new DOMDocument();
			d.setEntityResolver( new SingleLocationEntityResolver(this.xmlHome.toURL()) );
			d.loadDocument( "tdoc", fin, true );			
			fail( "Loaded dtd-bad-note.xml" );
		}
		catch (SAXException e)
		{
			// correct
		}
		finally
		{
			fin.close();
		}
	}

	/**
	 * Load a valid W3C schema controlled file.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testLoadFile_Schema()
	throws Exception
	{
		final FileInputStream fin = new FileInputStream( new File(this.xmlHome,"w3c-note.xml") );
		try
		{
			final URL schemaURL = new File(this.xmlHome,"note.xsd").toURL();
			final DOMDocument d = new DOMDocument();
			d.loadDocumentUsingW3CSchema( "tdoc", fin, schemaURL.toExternalForm() );

			assertFalse(d.isCoalescing());
			assertTrue(d.isValidated());
			assertEquals("note", d.getDocumentName());
			assertEquals("w3cSchema", d.getValidationType());
			assertNotNull(d.getSchemaURL());
			assertTrue(d.getSchemaURL().endsWith("note.xsd"));
		}
		finally
		{
			fin.close();
		}
	}

	/**
	 * Try to load an invalid W3C schema controlled file.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testBadFile_Schema()
	throws Exception
	{
		final FileInputStream fin = new FileInputStream( new File(this.xmlHome,"w3c-bad-note.xml") );
		try
		{
			final URL schemaURL = new File(this.xmlHome,"note.xsd").toURL();
			final DOMDocument d = new DOMDocument();
			d.loadDocumentUsingW3CSchema( "tdoc", fin,  schemaURL.toExternalForm() );
			
			fail( "Loaded w3c-bad-note.xml" );
		}
		catch (SAXException e)
		{
			// correct
		}
		finally
		{
			fin.close();
		}
	}

	/**
	 * Set an entity resolver.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testCustomEntityResolver()
	throws Exception
	{
		final FileInputStream fin = new FileInputStream( new File(this.xmlHome,"w3c-note.xml") );
		try
		{
			final ClassPathEntityResolver resolver = new ClassPathEntityResolver();
			resolver.setClassPath( new Class<?>[]{this.getClass()} );
			
			final URL schemaURL = new URL("http://localhost/note.xsd");
			final DOMDocument d = new DOMDocument();
			d.setEntityResolver( resolver );
			d.loadDocumentUsingW3CSchema( "tdoc", fin, schemaURL.toExternalForm() );
		}
		finally
		{
			fin.close();
		}
	}

	/**
	 * Write a DOM.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testWrite()
	throws Exception
	{
		final FileInputStream fin = new FileInputStream( new File(this.xmlHome,"dtd-note.xml") );
		try
		{
			final DOMDocument d = new DOMDocument();
			d.setEntityResolver( new SingleLocationEntityResolver(this.xmlHome.toURL()) );
			d.loadDocument( "tdoc", fin, true );
			
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			final PrintStream ps = new PrintStream( bout );
			
			// TODO (SW) - DOMDocument needs a mechanism for defining DTD declaration when writing XML
			
			ps.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes() );
			ps.write( "<!DOCTYPE note SYSTEM \"note.dtd\">".getBytes() );
			d.writeDocument( ps, false );
			
			assertTrue( bout.size() > 0 );
			
			d.loadDocument( "tdoc", new ByteArrayInputStream(bout.toByteArray()), true );
		}
		finally
		{
			fin.close();
		}
	}

	/**
	 * Transform a <code>note</code> into a <code>postit</code>.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testTransform()
	throws Exception
	{
		final FileInputStream fin = new FileInputStream( new File(this.xmlHome,"dtd-note.xml") );
		try
		{
			final DOMDocument d = new DOMDocument();
			d.setEntityResolver( new SingleLocationEntityResolver(this.xmlHome.toURL()) );
			d.loadDocument( "tdoc", fin, true );
			
			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			d.transform(
				new File(this.xmlHome,"note-2-postit.xsl").toURL().toExternalForm(),
				null,
				new PrintWriter(bout)
			);
			
			assertTrue( "bout.size", (bout.size() > 0) );

			d.loadDocument( "tdoc", new ByteArrayInputStream(bout.toByteArray()), true );
		}
		finally
		{
			fin.close();
		}
	}

	/**
	 * Write out an empty DOM.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testWriteEmptyDocument()
	throws Exception
	{
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final DOMDocument doc = new DOMDocument( "testDoc" );
		doc.writeDocument( out, false );
		
		assertEquals( "dpc.writeDocument", "<testDoc/>", new String(out.toByteArray()) );
	}
	
	/**
	 * Can we compare nodes?
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testCompareNodes()
	throws Exception
	{
		final DOMDocument d = new MockDOMDocument();
		final DOMDocument other = new MockDOMDocument();
		
		// whole document
		
		assertFalse( d.getUseRegexPatternMatching() );
		assertTrue( d.matchNodes(d.getRootNode(), d.getRootNode()) );

		d.setUseRegexPatternMatching(true);
		
		assertTrue( d.matchNodes(d.getRootNode(), d.getRootNode()) );
		
		// node type mismatch
		
		final Element r1 = d.getElement("r1");

		final NodeList attrs = d.listNodes(d.getRootNode(), "r1/@r1-attr");
		assertEquals( 1, attrs.getLength() );
		
		d.setUseRegexPatternMatching(false);
		
		assertFalse( d.matchNodes(r1, attrs.item(0)) );

		// node name

		assertFalse( d.matchNodes(r1, d.getElement("r2")) );

		// attribute value mismatch
		
		r1.getAttributeNode("r1-attr").setValue("myValue");
		
		assertFalse( d.matchNodes(r1, other.getElement("r1")) );

		// attribute count mismatch

		other.getElement("r1").removeAttribute("r1-attr");
		
		assertFalse( d.matchNodes(r1, other.getElement("r1")) );
						
		// same count but different attributes

		other.getElement("r1").setAttribute("xxx", "xxx");
		
		assertFalse( d.matchNodes(r1, other.getElement("r1")) );

		// CDATA value mismatch
		
		final Element e4_1 = d.getElement("r4/e4.1");
		d.removeCDATAContent(e4_1);
		d.appendCDATASection(e4_1, ".*");

		assertFalse( d.matchNodes(other.getElement("r4/e4.1"), e4_1) );
		
		// Regex matching
		
		d.setUseRegexPatternMatching( true );
		
		assertTrue( d.matchNodes(other.getElement("r4/e4.1"), e4_1) );

		// cannot find CDATA in "other" node
		
		d.removeCDATAContent(e4_1);

		d.setUseRegexPatternMatching(false);
		
		assertFalse( d.matchNodes(other.getElement("r4/e4.1"), e4_1) );
		
		// Text value mismatch

		final Element e4_2 = d.getElement("r4/e4.2");
		d.replaceText(e4_2, ".*");

		d.setUseRegexPatternMatching(false);
		
		assertFalse( d.matchNodes(other.getElement("r4/e4.2"), e4_2) );

		// Regex matching
		
		d.setUseRegexPatternMatching(true);
		assertTrue( d.matchNodes(other.getElement("r4/e4.2"), e4_2) );
		
		d.replaceText(e4_2, "^[0-9].*");
		
		assertFalse( d.matchNodes(other.getElement("r4/e4.2"), e4_2) );
		
		// child count mismatch

		other.removeElementByName(other.getRootNode(), "r4", "e4.2");
		
		assertFalse( d.matchNodes(d.getElement("r4"), other.getElement("r4")) );
		
		// child content mismatch
				
		assertFalse( d.matchNodes(d.getRootNode(), other.getRootNode()) );		
	}
	
	/*
	private void printXMLFile(byte[] aData)
	throws java.io.IOException
	{
		java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream( aData );
		byte ioBuf[] = new byte[1024];
		int len;
		while ( (len = in.read(ioBuf)) > -1 )
			System.out.print( new String(ioBuf,0,len) );
	}
	*/
}