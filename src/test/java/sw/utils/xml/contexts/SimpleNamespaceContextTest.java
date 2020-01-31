package sw.utils.xml.contexts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.XMLConstants;

import sw.utils.xml.contexts.SimpleNamespaceContext;

import junit.framework.TestCase;

/**
 * <code>SimpleNamespaceContext</code> test suite.
 * 
 * @author $Author: $ 
 * @version $Revision: $
 */
public class SimpleNamespaceContextTest extends TestCase
{
	/**
	 * Get the namespace URI associated with a single prefix.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testNamespaceURI()
	throws Exception
	{
		// default state

		final SimpleNamespaceContext nc = new SimpleNamespaceContext();
		assertNotNull( nc.getNamespaces() );
		assertEquals( 0, nc.getNamespaces().size() );
		
		// unknown namespace

		assertEquals( XMLConstants.NULL_NS_URI, nc.getNamespaceURI("xxx") );

		// known namespace
		
		nc.getNamespaces().put( "xxx", "http://xxx" );
		assertEquals( "http://xxx", nc.getNamespaceURI("xxx") );
	}

	/**
	 * Get the prefix associated with a single namespace URI.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testPrefix()
	throws Exception
	{
		final SimpleNamespaceContext nc = new SimpleNamespaceContext();

		// unknown namespace

		assertEquals( null, nc.getPrefix("http://xxx") );

		// known namespace
		
		nc.getNamespaces().put( "xxx", "http://xxx" );
		assertEquals( "xxx", nc.getPrefix("http://xxx") );		
	}

	/**
	 * List all prefixes associated with a single namespace URI.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testPrefixes()
	throws Exception
	{
		final HashMap<String,String> mappings = new HashMap<String,String>();
		mappings.put( "xxx", "http://xxx" );
		mappings.put( "yyy", "http://xxx" );

		final SimpleNamespaceContext nc = new SimpleNamespaceContext();
		nc.setNamespaces( mappings );
		
		// unknown namespace

		final ArrayList<String> prefixes = new ArrayList<String>();
		for ( final Iterator<String> i = nc.getPrefixes("http://yyy");  i.hasNext();)
		{
			prefixes.add( i.next() );
		}

		assertEquals( 0, prefixes.size() );

		// known namespace

		for ( final Iterator<String> i = nc.getPrefixes("http://xxx");  i.hasNext();)
		{
			prefixes.add( i.next() );
		}

		assertEquals( 2, prefixes.size() );
		assertTrue( prefixes.contains("xxx") );
		assertTrue( prefixes.contains("yyy") );
	}

}
