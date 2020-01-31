package sw.utils.xml;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import sw.test.JunitTestHelper;
import sw.utils.xml.resolvers.ClassPathEntityResolver;
import sw.utils.xml.resolvers.SingleLocationEntityResolver;

/**
 * <code>SAXDocument</code> test suite.
 * 
 * <p>Tests:
 * <ol>
 * <li>Load document.
 * <li>DTC validation.
 * <li>W3C schema validation.
 * <li>Entity resolvers.
 * <li>Multiple content handlers.
 * <li>Namespace based filtering.
 * <li>Element filtering.
 * </ol>
 * </p>
 * 
 * @author $Author: $ 
 * @version $Revision: $
 */
public class SAXDocumentTest extends TestCase
{
	private static final String FINDME = "FINDME.txt";

	/** Helper class. */
	public class SimpleContentHandler implements SAXDocument.ContentHandler
	{
		/** Property value. */
		public Map<String, String> properties = new HashMap<String, String>();

		@Override
        public void elementStart(String aXPath, String aElementName, Attributes aAttrs)
        throws SAXException
        {
        	// do nothing
        }
		
	    @Override
	    public void elementEnd(String aXPath, String aElementName, String aValue, Attributes aAttrs)
	    throws SAXException
	    {
	    	properties.put( aElementName, aValue );
	    }
	}
	
	/** Helper class. */
	public class FilteringContentHandler implements SAXDocument.ContentHandler
	{
		/** Number of elements using the <code>accept</code> namespace. */
		public int acceptCount = 0;

		/** Number of elements using the <code>ignore</code> namespace. */
		public int ignoreCount = 0;

		/** Number of elements using the <code>xsi</code> namespace. */
		public int xsiCount = 0;
		
		/** Property value. */
		public Map<String, String> properties = new HashMap<String, String>();
		
        @Override
        public void elementStart(String aXPath, String aElementName, Attributes aAttrs)
        throws SAXException
        {
        	// do nothing
        }

        @Override
        public void elementEnd(String aXPath, String aElementName, String aValue, Attributes aAttrs)
        throws SAXException
        {
    		if ( aXPath.indexOf("xsi:") > -1 )
        	{
        		xsiCount++;
        	}

        	if ( aXPath.indexOf("accept:") > -1 )
        	{
        		acceptCount++;
        	}

        	if ( aXPath.indexOf("ignore:") > -1 )
        	{
        		ignoreCount++;
        	}
        	
        	for ( int i=0;  i < aAttrs.getLength();  i++)
        	{
            	if ( aAttrs.getQName(i).startsWith("xsi:") )
            	{
            		xsiCount++;
            	}

            	if ( aAttrs.getQName(i).startsWith("accept:") )
            	{
            		acceptCount++;
            	}

            	if ( aAttrs.getQName(i).startsWith("ignore:") )
            	{
            		ignoreCount++;
            	}        		
        	}
        	
        	properties.put( aElementName, aValue );
        }
	};

	private File xmlHome;
	
	@Override
	public void setUp()
	throws Exception
	{		
        super.setUp();
        
    	// test setup
    	
        this.xmlHome = JunitTestHelper.getResourceParentFile( this.getClass(), FINDME );
	}
	
	/**
	 * Load document.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testLoadFile_NoValidation()
	throws Exception
	{
		final FileInputStream fin = new FileInputStream( new File(this.xmlHome, "dtd-note.xml") );
		try
		{
			final SimpleContentHandler chandler = new SimpleContentHandler();

			final SAXDocument sax = new SAXDocument();
			sax.addContentHandler( chandler );
			assertNull( sax.getEntityResolver() );
			
			sax.loadDocument( "tdoc", fin, false );

			assertEquals("note", sax.getDocumentName());
			assertNotNull(sax.getSystemId());
			assertTrue(sax.getSystemId().matches("^file:///(.*)/note\\.dtd$"));
			assertNull(sax.getPublicId());
			assertNull(sax.getSchemaURL());
			assertEquals(SAXDocument.NO_VALIDATION, sax.getValidationType());
			assertFalse(sax.isValidated());
						
			// whitespace retained for parsed data when no CDATA present

			String expected = "\n"
					+ "\t\tReminder\n"
					+ "\t";
			
			assertEquals( expected, chandler.properties.get("heading") );
			
			// only unparsed data retained when CDATA present
			
			expected =
					"\n\t\tDon't\n\t\t"	// parsed data
					+ " forget "		// CDATA block
					+ "\n\t\tme\n\t\t"	// parsed data
					+ " this weekend!";	// CDATA block
			
			assertEquals( expected, chandler.properties.get("body") );
		}
		finally
		{
			fin.close();
		}
	}
	
	/**
	 * DTD validation.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testLoadFile_DTD()
	throws Exception
	{
		final FileInputStream fin = new FileInputStream( new File(this.xmlHome, "dtd-note.xml") );
		try
		{
			final EntityResolver resolver = new SingleLocationEntityResolver(this.xmlHome.toURL());
			final SAXDocument sax = new SAXDocument();
			sax.setEntityResolver( resolver );
			sax.loadDocument( "tdoc", fin, true );

			assertEquals("note", sax.getDocumentName());
			assertNotNull(sax.getSystemId());
			assertTrue(sax.getSystemId().matches("^file:///(.*)/note\\.dtd$"));
			assertNull(sax.getPublicId());
			assertNull(sax.getSchemaURL());
			assertEquals(SAXDocument.DTD_VALIDATION, sax.getValidationType());
			assertTrue(sax.isValidated());
		}
		finally
		{
			fin.close();
		}
	}

	/**
	 * DTD validation.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testInvalidFile_DTD()
	throws Exception
	{
		final FileInputStream fin = new FileInputStream( new File(this.xmlHome, "dtd-bad-note.xml") );
		try
		{
			final EntityResolver resolver = new SingleLocationEntityResolver(this.xmlHome.toURL());
			final SAXDocument sax = new SAXDocument();
			sax.setEntityResolver( resolver );
			sax.loadDocument( "tdoc", fin, true );			
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
	 * W3C schema validation.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testLoadFile_Schema()
	throws Exception
	{
		final FileInputStream fin = new FileInputStream( new File(this.xmlHome, "w3c-note.xml") );
		try
		{			
			final URL schemaURL = new File(this.xmlHome,"note.xsd").toURL();
			final SAXDocument sax = new SAXDocument();
			sax.loadDocumentUsingW3CSchema( "tdoc", fin, schemaURL.toExternalForm() );

			assertEquals("note", sax.getDocumentName());
			assertNull(sax.getSystemId());
			assertNull(sax.getPublicId());
			assertTrue(sax.getSchemaURL().matches("^file:/(.*)/note.xsd$"));
			assertEquals(SAXDocument.W3C_SCHEMA_VALIDATION, sax.getValidationType());
			assertTrue(sax.isValidated());
		}
		finally
		{
			fin.close();
		}
	}

	/**
	 * W3C schema validation.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testBadFile_Schema()
	throws Exception
	{
		final FileInputStream fin = new FileInputStream( new File(this.xmlHome, "w3c-bad-note.xml") );
		try
		{
			final URL schemaURL = new File(this.xmlHome,"note.xsd").toURL();
			final SAXDocument sax = new SAXDocument();
			sax.loadDocumentUsingW3CSchema( "tdoc", fin, schemaURL.toExternalForm() );
			
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
	 * Entity resolvers.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testCustomEntityResolver()
	throws Exception
	{
		final FileInputStream fin = new FileInputStream( new File(this.xmlHome, "w3c-note.xml") );
		try
		{
			final ClassPathEntityResolver resolver = new ClassPathEntityResolver();
			resolver.setClassPath( new Class<?>[]{this.getClass()} );
			
			final SAXDocument sax = new SAXDocument();
			sax.setEntityResolver( resolver );
			sax.loadDocumentUsingW3CSchema( "tdoc", fin, "http://localhost/note.xsd" );
		}
		finally
		{
			fin.close();
		}
	}
	
	/**
	 * Multiple content handlers.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testMultipleContentHandlers()
	throws Exception
	{		
		final FilteringContentHandler chandler_1 = new FilteringContentHandler();
		final FilteringContentHandler chandler_2 = new FilteringContentHandler();

		// multiple handlers
		
		final SAXDocument sax = new SAXDocument();
		sax.addContentHandler( chandler_1 );
		sax.addContentHandler( chandler_2 );

		FileInputStream fin = new FileInputStream( new File(this.xmlHome, "nsfilter.xml") );
		try
		{
			sax.loadDocument( "tdoc", fin, false );			
		}
		finally
		{
			fin.close();
		}
		
		assertEquals( "xsiCount", 1, chandler_1.xsiCount );
		assertEquals( "acceptCount", 7, chandler_1.acceptCount );
		assertEquals( "ignoreCount", 7, chandler_1.ignoreCount );

		assertEquals( "xsiCount", 1, chandler_2.xsiCount );
		assertEquals( "acceptCount", 7, chandler_2.acceptCount );
		assertEquals( "ignoreCount", 7, chandler_2.ignoreCount );

		// one handler
		
		sax.removeContentHandler( chandler_1 );
		
		chandler_1.xsiCount = 0;
		chandler_1.acceptCount = 0;
		chandler_1.ignoreCount = 0;
		
		chandler_2.xsiCount = 0;
		chandler_2.acceptCount = 0;
		chandler_2.ignoreCount = 0;

		fin = new FileInputStream( new File(this.xmlHome, "nsfilter.xml") );
		try
		{
			sax.loadDocument( "tdoc", fin, false );			
		}
		finally
		{
			fin.close();
		}
		
		assertEquals( "xsiCount", 0, chandler_1.xsiCount );
		assertEquals( "acceptCount", 0, chandler_1.acceptCount );
		assertEquals( "ignoreCount", 0, chandler_1.ignoreCount );

		assertEquals( "xsiCount", 1, chandler_2.xsiCount );
		assertEquals( "acceptCount", 7, chandler_2.acceptCount );
		assertEquals( "ignoreCount", 7, chandler_2.ignoreCount );
	}
	
	/**
	 * Namespace based filtering.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testNamespaceFiltering()
	throws Exception
	{		
		final FilteringContentHandler chandler = new FilteringContentHandler();

		final SAXDocument sax = new SAXDocument();
		sax.addContentHandler( chandler );

		// add filters
		
		sax.addExcludedNamespaceURI( "http://www.w3.org/2001/XMLSchema-instance" );
		sax.addExcludedNamespaceURI( "http://www.sw.com/ignore" );

		FileInputStream fin = new FileInputStream( new File(this.xmlHome, "nsfilter.xml") );
		try
		{			
			sax.loadDocument( "tdoc", fin, false );
		}
		finally
		{
			fin.close();
		}

		assertEquals( "xsiCount", 0, chandler.xsiCount );
		assertEquals( "acceptCount", 7, chandler.acceptCount );
		assertEquals( "ignoreCount", 0, chandler.ignoreCount );

		// remove filters
		
		sax.removeNamespaceURIs();

		chandler.xsiCount = 0;
		chandler.acceptCount = 0;
		chandler.ignoreCount = 0;

		fin = new FileInputStream( new File(this.xmlHome, "nsfilter.xml") );
		try
		{			
			sax.loadDocument( "tdoc", fin, false );
		}
		finally
		{
			fin.close();
		}
		
		assertEquals( "xsiCount", 1, chandler.xsiCount );
		assertEquals( "acceptCount", 7, chandler.acceptCount );
		assertEquals( "ignoreCount", 7, chandler.ignoreCount );
		
		// whitespace retained for parsed data when no CDATA present
		
		assertEquals( " Reminder ", chandler.properties.get("heading") );
		
		// only unparsed data retained when CDATA present

		assertEquals( "Don't forget me this weekend!", chandler.properties.get("body") );
	}

	/**
	 * Element filtering.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testElementFiltering()
	throws Exception
	{
		final SimpleContentHandler chandler = new SimpleContentHandler();

		final SAXDocument sax = new SAXDocument();
		sax.addContentHandler( chandler );

		// add filters
		
		sax.addExcludedPath( ".*/body$" );
		
		FileInputStream fin = new FileInputStream( new File(this.xmlHome, "dtd-note.xml") );
		try
		{
			sax.loadDocument( "tdoc", fin, false );
		}
		finally
		{
			fin.close();
		}
			
		assertNull( chandler.properties.get("body") );
		
		// remove filters
		
		sax.removeExcludedPaths();
		
		fin = new FileInputStream( new File(this.xmlHome, "dtd-note.xml") );
		try
		{
			sax.loadDocument( "tdoc", fin, false );
		}
		finally
		{
			fin.close();
		}

		final String expected =
				"\n\t\tDon't\n\t\t"		// parsed data
				+ " forget "			// CDATA block
				+ "\n\t\tme\n\t\t"		// parsed data
				+ " this weekend!";		// CDATA block

		assertEquals( expected, chandler.properties.get("body") );
	}

}