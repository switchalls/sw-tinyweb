package sw.tinyweb.utils;

import sw.tinyweb.utils.XMLBuilder;
import sw.utils.xml.XMLException;
import junit.framework.TestCase;

/**
 * <code>XMLBuilder</code> test suite.
 * 
 * <p>Test scenarios...
 * <ol>
 * <li>Typical usage.
 * <li>Adding attributes.
 * <li>Adding elements.
 * <li>Adding CDATA content.
 * <li>Adding stack traces.
 * <li>Adding text value content.
 * </ol>
 * </p>
 * 
 * @author $Author: $ 
 * @version $Revision: $
 */
public class XMLBuilderTest extends TestCase
{
	/**
	 * Typical usage.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testTypicalUsage()
	throws Exception
	{
		final XMLBuilder builder = new XMLBuilder();
		builder.openElement("root", false);
		
		builder.openElement("element_1", true);
		builder.setAttribute("a", "b");
		assertTrue(builder.isElementOpen());

		builder.openElement("element_2", false);
		builder.closeElement();
		assertFalse(builder.isElementOpen());
		
		builder.openElement("element_3", true);
		builder.closeElement("element_3"); // should auto close open declaration
		assertFalse(builder.isElementOpen());

		builder.closeElement("element_1");
		builder.closeElement("root");
		
		final String expected = "<root><element_1 a=\"b\"><element_2> /><element_3></element_3></element_1></root>";
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
		final XMLBuilder builder = new XMLBuilder();
		
		builder.append("hello");		
		assertEquals("hello", builder.toString());
		
		builder.clear();
		
		builder.append("world");
		assertEquals("world", builder.toString());
	}
	
	/**
	 * Adding attributes.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testAttributes()
	throws Exception
	{
		final XMLBuilder builder = new XMLBuilder();
		builder.openElement("root", true);
		builder.setAttribute("string", "'\"<>[]");
		builder.setAttribute("boolean", true);
		builder.setAttribute("long", 99L);
		builder.setAttribute("double", 99.99D);
		builder.setAttribute("ignore-me", null); // should be ignored
		builder.openElementCompleted();		
		builder.closeElement("root");
		
		final String expected = "<root string=\"&#39;&quot;&lt;&gt;[]\" boolean=\"true\" long=\"99\" double=\"99.99\"></root>";
		assertEquals(expected, builder.toString());
	}

	/**
	 * Adding elements.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testElements()
	throws Exception
	{
		final XMLBuilder builder = new XMLBuilder();
		builder.openElement("root", true);
		builder.openElementCompleted();
		builder.openElementCompleted(); // should be ignored
		
		builder.openElement("element_1", false);
		builder.openElementCompleted(); // should be ignored
		builder.appendElement("element_2", "a...b");
		builder.closeElement("element_1");
		
		builder.closeElement("root");
		
		final String expected = "<root><element_1><element_2>a...b</element_2></element_1></root>";
		assertEquals(expected, builder.toString());
	}

	/**
	 * Adding CDATA content.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testCDATAElement()
	throws Exception
	{
		final XMLBuilder builder = new XMLBuilder();
		builder.appendCDATAElement("root", "cdata_1]]>");
		
		final String expected = "<root><![CDATA[cdata_1]]&gt;]]></root>";
		assertEquals(expected, builder.toString());
	}

	/**
	 * Adding CDATA content.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testCDATAValue()
	throws Exception
	{
		final XMLBuilder builder = new XMLBuilder();
		builder.openElement("root", true);
		builder.appendCDATAValue("cdata_1]]>");
		
		builder.openElement("element", false);
		builder.appendCDATAValue("cdata_2]]>");
		builder.closeElement("element");

		builder.closeElement("root");
		
		final String expected = "<root><![CDATA[cdata_1]]&gt;]]><element><![CDATA[cdata_2]]&gt;]]></element></root>";
		assertEquals(expected, builder.toString());
	}

	/**
	 * Adding stack traces.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testStackTraces()
	throws Exception
	{
		Exception err;
		try
		{
			throw new XMLException("Bang!");
		}
		catch (XMLException e)
		{
			err = e;
		}
		
		final XMLBuilder builder = new XMLBuilder();
		builder.appendStackTrace(err);
		
		assertTrue(builder.toString().startsWith("<![CDATA[sw.utils.xml.XMLException: Bang!"));
		assertTrue(builder.toString().endsWith("]]>"));
	}
	
	/**
	 * Adding text value content.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testTextElement()
	throws Exception
	{
		final XMLBuilder builder = new XMLBuilder();
		builder.appendTextElement("root", "'\"<>[]");
		
		final String expected = "<root>&#39;&quot;&lt;&gt;[]</root>";
		assertEquals(expected, builder.toString());
	}

	/**
	 * Adding text value content.
	 * 
	 * @throws Exception when the test should be aborted
	 */
	public void testTextValue()
	throws Exception
	{
		final XMLBuilder builder = new XMLBuilder();
		builder.openElement("root", true);
		builder.appendTextValue("'\"<>[]");
		builder.closeElement("root");
		
		final String expected = "<root>&#39;&quot;&lt;&gt;[]</root>";
		assertEquals(expected, builder.toString());
	}

}
