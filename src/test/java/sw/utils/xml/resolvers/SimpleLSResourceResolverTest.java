package sw.utils.xml.resolvers;

import junit.framework.TestCase;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.EntityResolver;

import sw.utils.xml.DOMDocument;

/**
 * <code>SimpleLSResourceResolver</code> test suite.
 * 
 * <p>
 * Tests:
 * <ol>
 * <li>Finds resource
 * <li>Cannot find resource
 * <li>URI manipulation
 * </ol>
 * </p>
 * 
 * @author $Author: $ 
 * @version $Revision: $
 */
public class SimpleLSResourceResolverTest extends TestCase
{
	/**
	 * Finds a resource.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testFindsResource()
	throws Exception
	{
		final DOMDocument doc = new DOMDocument( "testdoc" );
		
		final EntityResolver eresolver = new ClassPathEntityResolver();
		final SimpleLSResourceResolver dresolver = new SimpleLSResourceResolver( doc.getOwnerDocument(), eresolver );
		final LSInput i = dresolver.resolveResource("type", "namespaceURI", "publicId", "http://localhost/note.xsd", "http://localhost" );
		assertNotNull( i );
		assertNotNull( i.getByteStream() );
		assertNull( i.getCharacterStream() );
		assertEquals( "publicId", i.getPublicId() );
		assertEquals( "http://localhost/note.xsd", i.getSystemId() );
		assertEquals( "http://localhost", i.getBaseURI() );
		i.getByteStream().close();
	}
	
	/**
	 * Fails to find a resource.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testCannotFindResource()
	throws Exception
	{
		final DOMDocument doc = new DOMDocument( "testdoc" );
		
		final EntityResolver eresolver = new ClassPathEntityResolver();
		final SimpleLSResourceResolver dresolver = new SimpleLSResourceResolver( doc.getOwnerDocument(), eresolver );
		final LSInput i = dresolver.resolveResource("type", "namespaceURI", "publicId", "bad-resource", "http://localhost" );
		assertNull( i );
	}

	/**
	 * URI manipulation.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testGetAbsoluteURI()
	throws Exception
	{
		final SimpleLSResourceResolver dresolver = new SimpleLSResourceResolver( (DOMImplementation)null, null );
		assertEquals( "http://localhost/f.xml", dresolver.getAbsoluteURI( "http://localhost", "http://localhost/f.xml") );
		assertEquals( "http://localhost/f.xml", dresolver.getAbsoluteURI( "http://localhost", "f.xml") );		
		assertEquals( "http://localhost/./f.xml", dresolver.getAbsoluteURI( "http://localhost", "./f.xml") );		
	}

}
