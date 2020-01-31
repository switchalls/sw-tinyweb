package sw.utils.xml.resolvers;

import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.xml.sax.EntityResolver;

import sw.utils.xml.XMLHelper;

/**
 * <code>SimpleURIResolver</code> test suite.
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
public class SimpleURIResolverTest extends TestCase
{
	/**
	 * Finds a resource.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testFindsResource()
	throws Exception
	{		
		final EntityResolver eresolver = new ClassPathEntityResolver();
		final SimpleURIResolver uresolver = new SimpleURIResolver( eresolver );
		final StreamSource s = (StreamSource) uresolver.resolve( "http://localhost/note.xsd", "http://localhost" );
		assertNotNull( s );
		assertNotNull( s.getInputStream() );
		assertNull( s.getReader() );
		assertNull( s.getPublicId() );
		assertEquals( "http://localhost/note.xsd", s.getSystemId() );
		XMLHelper.getInstance().closeStream( s );
	}
	
	/**
	 * Fails to find a resource.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testCannotFindResource()
	throws Exception
	{
		final EntityResolver eresolver = new ClassPathEntityResolver();
		final SimpleURIResolver uresolver = new SimpleURIResolver( eresolver );
		final StreamSource s = (StreamSource) uresolver.resolve( "bad-resource", "http://localhost" );
		assertNull( s );
	}

	/**
	 * URI manipulation.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testGetAbsoluteURI()
	throws Exception
	{
		final SimpleURIResolver uresolver = new SimpleURIResolver( null );
		assertEquals( "http://localhost/f.xml", uresolver.getAbsoluteURI( "http://localhost", "http://localhost/f.xml") );
		assertEquals( "http://localhost/f.xml", uresolver.getAbsoluteURI( "http://localhost", "f.xml") );		
		assertEquals( "http://localhost/./f.xml", uresolver.getAbsoluteURI( "http://localhost", "./f.xml") );		
	}

}
