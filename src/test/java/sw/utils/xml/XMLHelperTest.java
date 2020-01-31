package sw.utils.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;

import junit.framework.TestCase;

import org.xml.sax.EntityResolver;

import sw.test.JunitTestHelper;
import sw.utils.xml.resolvers.ClassPathEntityResolver;

/**
 * <code>XMLHelper</code> test suite.
 * 
 * <p>
 * Tests:
 * <ol>
 * <li>XML string encoding
 * <li>SAX based transformations
 * </ol>
 * </p>
 * 
 * @author $Author: $ 
 * @version $Revision: $
 */
public class XMLHelperTest extends TestCase
{
	private static final String FINDME = "FINDME.txt";

	private File xmlHome;

	/**
	 * Test configuration.
	 * 
	 * <p>
	 * Called for each individual test.
	 * </p>
	 * 
	 * @throws Exception when the test should be aborted
	 */
	@Override
	public void setUp()
	throws Exception
	{		
        super.setUp();
        this.xmlHome = JunitTestHelper.getResourceParentFile( this.getClass(), FINDME );
	}

	/**
	 * Check XML string creation.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testXmlSafeStrings()
	throws Exception
	{
		final XMLHelper xhelper = XMLHelper.getInstance();
		assertEquals( " \t\r\n??", xhelper.createXmlSafeString(new byte[]{' ','\t','\r','\n',0x00,0x04}, 6, "ASCII") );
		assertEquals( "AB-@[", xhelper.createXmlSafeString(new byte[]{'A','B','-','@','['}, 5, "ASCII") );
	}

	/**
	 * Check XML string encoding.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testEncodeCDATA()
	throws Exception
	{
		final XMLHelper xhelper = XMLHelper.getInstance();
		assertEquals( "<![CDATA[", xhelper.encodeCDATA("<![CDATA[") );
		assertEquals( "]]&gt;", xhelper.encodeCDATA("]]>") );
		assertEquals( "<![CDATA[]]&gt;", xhelper.encodeCDATA("<![CDATA[]]>") );
		assertEquals( "<![CDATA[&<>\"'[]]]&gt;", xhelper.encodeCDATA("<![CDATA[&<>\"'[]]]>") );		
		assertEquals( "<![CDATA[<![CDATA[]]&gt;]]&gt;", xhelper.encodeCDATA("<![CDATA[<![CDATA[]]>]]>") );		
	}

	/**
	 * Check XML string encoding.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testEncodeString()
	throws Exception
	{
		final XMLHelper xhelper = XMLHelper.getInstance();

		final String[][] testStrings = new String[][] {
			{ "&&", "&amp;&amp;" },
			{ "<<", "&lt;&lt;" },
			{ ">>", "&gt;&gt;" },
			{ "\"\"", "&quot;&quot;" },
			{ "''", "&#39;&#39;" },
			{ "[[", "[[" }, // not encoded
			{ "]]", "]]" }, // not encoded
		};
		
		for ( int i=0;  i < testStrings.length;  i++ )
		{
			final String estr = xhelper.encodeString(testStrings[i][0]);
			assertEquals( "encoded", testStrings[i][1], estr );
		}
	}

	/**
	 * Transform a <code>note</code> into a <code>postit</code>.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testSAXTransform_NoValidation()
	throws Exception
	{
		this.doNote2PostitTransform( null, false );
	}

	/**
	 * Transform a <code>note</code> into a <code>postit</code>.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testSAXTransform_DTD()
	throws Exception
	{
		final ClassPathEntityResolver resolver = new ClassPathEntityResolver();
		resolver.setClassPath( new Class<?>[]{this.getClass()} );
		
		this.doNote2PostitTransform( resolver, true );
	}

	/**
	 * Transform <code>dtd-note.xml</code> using <code>note-2-postit.xsl</code>.
	 * 
	 * @param aResolver The entity resolver (can be null)
	 * @param aValidateXmlContent True when content should be validated
	 * @throws Exception when the transformation fails
	 */
	private void doNote2PostitTransform(EntityResolver aResolver, boolean aValidateXmlContent)
	throws Exception
	{
		final XMLHelper xhelper = XMLHelper.getInstance();

		final FileInputStream fin = new FileInputStream( new File(this.xmlHome,"dtd-note.xml") );
		try
		{
			final File xslFile = new File(this.xmlHome, "note-2-postit.xsl");
			assertNotNull( "Cannot find note-2-postit.xsl", xslFile );

			final ByteArrayOutputStream bout = new ByteArrayOutputStream();
			xhelper.saxTransform(
				aResolver,
				fin,
				aValidateXmlContent,
				xhelper.newTransformerTemplate(aResolver, xslFile.toURL().toExternalForm()),
				null, // no properties
				new PrintWriter(bout)
			);
			
			assertTrue( "bout.size", (bout.size() > 0) );
			
			final DOMDocument dom = new DOMDocument();
			dom.setEntityResolver( aResolver );
			dom.loadDocument( "tdoc", new ByteArrayInputStream(bout.toByteArray()), false );
		}
		finally
		{
			fin.close();
		}
	}

}