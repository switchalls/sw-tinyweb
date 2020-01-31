package sw.tinyweb.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import junit.framework.TestCase;

/**
 * <code>JsonWriter</code> test suite.
 * 
 * <p>Test scenarios...
 * <ol>
 * <li>Typical usage.
 * <li>Property values.
 * <li>Records.
 * </ol>
 * </p>
 * 
 * @author $Author: $ 
 * @version $Revision: $
 */
public class JsonBuilderTest extends TestCase
{
	/**
	 * Typical usage.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testTypicalUsage()
	throws Exception
	{
		final JsonBuilder builder = new JsonBuilder();
		builder.openRecord("record_1");
		builder.setProperty("long_1", 1L);
		builder.setProperty("long_2", 2L);
		builder.openRecord("record_2");
		builder.closeRecord();
		builder.closeRecord();
		
		final String expected = "{'record_1': {'long_1':1,'long_2':2,'record_2': {}}}";
		assertEquals(expected, builder.toString());
	}

	/**
	 * Append content verbatim.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testGenericAppend()
	throws Exception
	{
		final JsonBuilder builder = new JsonBuilder();
		builder.append("hello");
		
		assertEquals("{hello}", builder.toString());
	}

	/**
	 * Property values.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testProperties_Date()
	throws Exception
	{
		final Date d = new Date();
		
		final JsonBuilder builder = new JsonBuilder();
		builder.setProperty("d", d);
		
		final String json = builder.toString();
		assertTrue( json.startsWith("{'d':'") );
		assertTrue( json.endsWith("Z'}") );
		
		final SimpleDateFormat fmt = new SimpleDateFormat(JsonBuilder.ISO8641_DATE_FORMAT);
		fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
				
		final Date copy = fmt.parse( json.substring(6, json.length()-2) );
		assertEquals(d, copy);
	}

	/**
	 * Property values.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testProperties_Double()
	throws Exception
	{
		final JsonBuilder builder = new JsonBuilder();
		builder.setProperty("d", 2.2D);
		
		assertEquals("{'d':2.2}", builder.toString());
	}

	/**
	 * Property values.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testProperties_String()
	throws Exception
	{
		final JsonBuilder builder = new JsonBuilder();
		builder.setProperty("s", "a'b'c");
		
		assertEquals("{'s':'a\'b\'c'}", builder.toString());
	}

}
