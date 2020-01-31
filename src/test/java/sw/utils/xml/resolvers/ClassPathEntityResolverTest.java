package sw.utils.xml.resolvers;

import junit.framework.TestCase;

import org.xml.sax.InputSource;

import sw.utils.xml.XMLHelper;

/**
 * <code>ClassPathEntityResolver</code> test suite.
 * 
 * <p>
 * Tests:
 * <ol>
 * <li>Finds resource
 * <li>Cannot find resource
 * </ol>
 * </p>
 * 
 * @author $Author: $ 
 * @version $Revision: $
 */
public class ClassPathEntityResolverTest extends TestCase
{
	/**
	 * Find a resource.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testFindsResource()
	throws Exception
	{
		final ClassPathEntityResolver r = new ClassPathEntityResolver();		
		final InputSource isource = r.resolveEntity( null, "note.xsd" );
		assertNotNull( isource );
		XMLHelper.getInstance().closeStream( isource );
	}

	/**
	 * Fails to find a resource.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testCannotFindResource()
	throws Exception
	{
		final ClassPathEntityResolver r = new ClassPathEntityResolver();
		final InputSource s = r.resolveEntity( null, "bad-resource" );
		assertNull( s );
	}

	/**
	 * Extraction of filename from URI.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testGetFilename()
	throws Exception
	{
		final ClassPathEntityResolver r = new ClassPathEntityResolver();
		assertEquals( "f.xml", r.getFilename("f.xml") );
		assertEquals( "f.xml", r.getFilename("/f.xml") );
		assertEquals( "f.xml", r.getFilename("http://localhost/a/b/f.xml") );
	}

}
