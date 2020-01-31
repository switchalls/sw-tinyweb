package sw.utils;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.Test;

/**
 * <code>URLHelperTest</code> test suite.
 * 
 * <p>
 * Tests:
 * <ol>
 * <li>UTF-8 encode
 * <li>UTF-8 decode
 * <li>Finding parent paths for <code>jar:</code> URLs
 * <li>Finding parent paths for non <code>jar:</code> URLs
 * <li><code>file:</code> URL construction
 * <li><code>http:</code> URL construction
 * <li><code>jar:</code> URL construction
 * </ol>
 * </p>
 * 
 * @author $Author: $ 
 * @version $Revision: $
 */
public class URLHelperTest
{
	/**
	 * Test string for URL encoders.
	 * 
	 * <p>
	 * URL encoding will not change the following characters:
	 * a-z, A-Z, 0-9, '.', '*' and '_'.
	 * </p>
	 */
	private static final String TEST_STRING = "azAZ09.-*_ ()#@+%";
	
	@Test
	public void testUTF8Encode() throws Exception
	{
		assertEquals( "azAZ09.-*_+%28%29%23%40%2B%25", URLHelper.w3cEncodeString(TEST_STRING) );
	}
	
	@Test
	public void testUTF8Decode() throws Exception
	{
		assertEquals( TEST_STRING, URLHelper.decodeString("azAZ09.-*_+%28%29%23%40%2B%25") );
		assertEquals( TEST_STRING, URLHelper.decodeString("azAZ09.%2D*_%20%28%29%23%40%2B%25") );
	}

	@Test
	public void testGetParentURL_File() throws Exception
	{
		assertEquals( "file:/a/b/c", URLHelper.getParentURL( new URL("file:/a/b/c/doc.xml") ) );
		assertEquals( "file:/a/b/c/data", URLHelper.getParentURL( new URL("file:/a/b/c/data/doc.xml") ) );
	}
	
	@Test
	public void testGetParentURL_Jar() throws Exception
	{
		assertEquals( "jar:file:/a/b/c!/", URLHelper.getParentURL( new URL("jar:file:/a/b/c!/doc.xml") ) );
		assertEquals( "jar:file:/a/b/c!/data", URLHelper.getParentURL( new URL("jar:file:/a/b/c!/data/doc.xml") ) );
	}

	@Test
	public void testNewURL_File() throws Exception
	{
		assertEquals( "file:/a/b/c/d",       URLHelper.newURL( new URL("file:/a/b"),  "/c/d"      ).toExternalForm() );
		assertEquals( "file:/a/b/c/d",       URLHelper.newURL( new URL("file:/a/b"),  "c/d"       ).toExternalForm() );
		assertEquals( "file:/a/b/c/d",       URLHelper.newURL( new URL("file:/a/b/"), "/c/d"      ).toExternalForm() );
		assertEquals( "file:/a/b/c/d",       URLHelper.newURL( new URL("file:/a/b/"), "c/d"       ).toExternalForm() );
		assertEquals( "file:/a/b/./c/d",     URLHelper.newURL( new URL("file:/a/b/"), "./c/d"     ).toExternalForm() );
		assertEquals( "file:/a/b/../c/d",    URLHelper.newURL( new URL("file:/a/b/"), "../c/d"    ).toExternalForm() );
		assertEquals( "file:/a/b/../../c/d", URLHelper.newURL( new URL("file:/a/b/"), "../../c/d" ).toExternalForm() );
	}
	
	@Test
	public void testNewURL_Http() throws Exception
	{
		assertEquals( "http:/www.sw.com/a/b/c/d",       URLHelper.newURL( new URL("http:/www.sw.com/a/b"),  "/c/d"      ).toExternalForm() );
		assertEquals( "http:/www.sw.com/a/b/c/d",       URLHelper.newURL( new URL("http:/www.sw.com/a/b"),  "c/d"       ).toExternalForm() );
		assertEquals( "http:/www.sw.com/a/b/c/d",       URLHelper.newURL( new URL("http:/www.sw.com/a/b/"), "/c/d"      ).toExternalForm() );
		assertEquals( "http:/www.sw.com/a/b/c/d",       URLHelper.newURL( new URL("http:/www.sw.com/a/b/"), "c/d"       ).toExternalForm() );
		assertEquals( "http:/www.sw.com/a/b/./c/d",     URLHelper.newURL( new URL("http:/www.sw.com/a/b/"), "./c/d"     ).toExternalForm() );
		assertEquals( "http:/www.sw.com/a/b/../c/d",    URLHelper.newURL( new URL("http:/www.sw.com/a/b/"), "../c/d"    ).toExternalForm() );
		assertEquals( "http:/www.sw.com/a/b/../../c/d", URLHelper.newURL( new URL("http:/www.sw.com/a/b/"), "../../c/d" ).toExternalForm() );
	}
	
	@Test
	public void testNewURL_Jar() throws Exception
	{
		assertEquals( "jar:file:/a/b/!/c/d", URLHelper.newURL( new URL("jar:file:/a/b/!/c"),  "/d"      ).toExternalForm() );
		assertEquals( "jar:file:/a/b/!/c/d", URLHelper.newURL( new URL("jar:file:/a/b/!/c"),  "d"       ).toExternalForm() );
		assertEquals( "jar:file:/a/b/!/c/d", URLHelper.newURL( new URL("jar:file:/a/b/!/c/"), "/d"      ).toExternalForm() );
		assertEquals( "jar:file:/a/b/!/c/d", URLHelper.newURL( new URL("jar:file:/a/b/!/c/"), "d"       ).toExternalForm() );
		assertEquals( "jar:file:/a/b/!/c/d", URLHelper.newURL( new URL("jar:file:/a/b/!/c/"), "./d"     ).toExternalForm() );
		assertEquals( "jar:file:/a/b/!/d",   URLHelper.newURL( new URL("jar:file:/a/b/!/c/"), "../d"    ).toExternalForm() );
		assertEquals( "jar:file:/a/b/!/d",   URLHelper.newURL( new URL("jar:file:/a/b/!/c/"), "../../d" ).toExternalForm() );
	}
}
