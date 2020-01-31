package sw.utils.xml;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import junit.framework.TestCase;

import org.w3c.dom.Element;

import sw.utils.xml.DOMDocument;
import sw.utils.xml.XMLException;

/**
 * Self constructing <code>DOMDocument</code>.
 * 
 * <p>
 * XML document =
 * <pre>
 *     &lt;Junit&gt;
 *         &lt;r1 r1-attr="attr-value" &gt;
 *             &lt;e1.1&gt;
 *                 &lt;e1.1.1 /&gt;
 *                 &lt;e1.1.2 /&gt;
 *             &lt;/e1.1&gt;
 *         &lt;/r1&gt;
 *         &lt;r2&gt;
 *             &lt;find-this&gt;find-me&lt;/find-this&gt;
 *         &lt;r3 r3-attr="r3-attr"&gt;r3-text
 *             &lt;!-- r3-comment --&gt;
 *         &lt;/r3&gt;
 *         &lt;r4&gt;
 *             &lt;find-this&gt;find-me&lt;/find-this&gt;
 *             &lt;e4.1&gt;
 *                 &lt;e4.1.1&gt;e4.1.1-text
 *                     &lt;find-this&gt;find-me&lt;/find-this&gt;
 *                 &lt;/e4.1.1&gt;
 *             &lt;e4.2&gt;r4-text
 *                 &lt;![CDATA[r4-CDATA]]&gt;
 *                 &lt;!-- r4-comment --&gt;
 *             &lt;/e4.2&gt;
 *         &lt;r5&gt;
 *             &lt;find-this&gt;ignore&lt;/r5&gt;
 *         &lt;/r5&gt;
 *         &lt;r6&gt;r6-text-1-2-3,r6-text-2,r6-text-3&lt;/r6&gt;
 *     &lt;/Junit&gt;
 * </pre>
 * </p>
 * 
 * @author $Author: $ 
 * @version $Revision: $
 */
public class MockDOMDocument extends DOMDocument
{
    /**
     * Default constructor.
     * 
     * @throws ParserConfigurationException when the DOM cannot be created
     * @throws TransformerException when something goes wrong
     * @throws XPathExpressionException when the DOM cannot be searched
     */
	public MockDOMDocument()
	throws ParserConfigurationException, TransformerException, XPathExpressionException
	{
		super( "Junit" );
		this.init();
	}

    /**
     * Constructor.
     * 
     * @param aOther The XML document
     */
	public MockDOMDocument(DOMDocument aOther)
	{
		super( aOther.getOwnerDocument() );
	}

	/**
	 * Validate this document's structure and contents.
     * 
	 * @throws TransformerException when the document cannot be read
	 * @throws XMLException when data is missing or is invalid
	 * @throws XPathExpressionException when the document cannot be searched
	 */
	public void assertValidContent()
	throws TransformerException, XMLException, XPathExpressionException
	{
		TestCase.assertTrue( "/Junit", this.contains("/Junit") );

		TestCase.assertTrue( "r2", this.contains("r2") );
		TestCase.assertTrue( "r1/e1.1/e1.1.1", this.contains("r1/e1.1/e1.1.1") );
		TestCase.assertTrue( "r1/e1.1/e1.1.2", this.contains("r1/e1.1/e1.1.2") );

		TestCase.assertEquals( "r3-text", "r3-text", this.getNodeText(this.getRootNode(), "r3") );
		TestCase.assertEquals( "r3-comment", "r3-comment", this.getComment(this.getRootNode(), "r3") );
		TestCase.assertEquals( "r3-attr", "r3-attr", this.getAttribute(this.getRootNode(), "r3", "r3-attr") );

		TestCase.assertEquals( "e4.1-CDATA", "e4.1-CDATA", this.getCDATAContent(this.getRootNode(), "r4/e4.1") );
		TestCase.assertEquals( "e4.2-text", "e4.2-text", this.getNodeText(this.getRootNode(), "r4/e4.2") );
	}

	/**
	 * Creates a DOM containing child elements, attributes, comments,
     * CDATA and text.
	 * 
	 * @throws TransformerException when the document cannot be created
	 * @throws XPathExpressionException when the document cannot be searched
	 */
	protected void init()
	throws TransformerException, XPathExpressionException
	{
        final Element r1 = this.appendElement( this.getRootNode(), "r1" );
        r1.setAttribute("r1-attr", "attr-value");
        
        this.appendElement( this.getRootNode(), "r2" );
        this.appendElement( this.getRootNode(), "r5" );
        
        this.appendElement( this.getRootNode(), "r1", "e1.1" );
        this.appendElement( this.getRootNode(), "r1/e1.1", "e1.1.1" );
        this.appendElement( this.getRootNode(), "r1/e1.1", "e1.1.2" );
        
        this.appendElement( this.getRootNode(), "r3" );
        this.appendText( this.getRootNode(), "r3", "r3-text" );
        this.appendComment( this.getRootNode(), "r3", "r3-comment" );
        this.setAttribute( this.getRootNode(), "r3", "r3-attr", "r3-attr" );

        this.appendElement( this.getRootNode(), "r4" );
        this.appendElement( this.getRootNode(), "r4", "e4.1" );
        this.appendElement( this.getRootNode(), "r4/e4.1", "e4.1.1" );
        this.appendElement( this.getRootNode(), "r4", "e4.2" );
        this.appendText( this.getRootNode(), "r4/e4.2", "e4.2-text" );
        this.appendComment( this.getRootNode(), "r4", "r4-comment" );
		this.appendCDATASection( this.getRootNode(), "r4/e4.1", "e4.1-CDATA" );
		this.appendText( this.getRootNode(), "r4/e4.1/e4.1.1", "e4.1.1-text" );

        this.appendElement( this.getRootNode(), "r2", "find-this" );
        this.appendText( this.getRootNode(), "r2/find-this", "find-me" );
        
		this.appendElement( this.getRootNode(), "r4", "find-this" );
        this.appendText( this.getRootNode(), "r4/find-this", "find-me" );
        
		this.appendElement( this.getRootNode(), "r4/e4.1/e4.1.1", "find-this" );
        this.appendText( this.getRootNode(), "r4/e4.1/e4.1.1/find-this", "find-me" );
        
		this.appendElement( this.getRootNode(), "r5", "find-this" );
        this.appendText( this.getRootNode(), "r5/find-this", "ignore" );

		org.w3c.dom.Element e = this.appendElement( this.getRootNode(), "r6" );
		this.appendText( e, "r6-text-1" );
		this.appendText( e, "-2" );
		this.appendText( e, "-3" );

		e = this.appendElement( this.getRootNode(), "r6" );
		this.appendText( e, ",r6-text-2" );

		e = this.appendElement( this.getRootNode(), "r6" );
		this.appendText( e, ",r6-text-3" );
	}
}