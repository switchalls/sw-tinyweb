package sw.tinyweb.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import sw.utils.xml.XMLHelper;

/**
 * Helper for creating XML documents.
 * 
 * @author $Author: $
 * @version $Revision: $
 */
public class XMLBuilder
{
    private final StringBuilder builder = new StringBuilder();
    private boolean elementOpen = false;
    
    /** Reset the builder. */
    public void clear()
    {
    	this.builder.setLength(0);
    }
    
    /** @return true when the current element is NOT closed */
    public boolean isElementOpen()
    {
    	return this.elementOpen;
    }
    
    /**
     * Append the stated text verbatim.
     * 
     * @param aText The text to be added
     * @return the builder
     */
    public XMLBuilder append(String aText)
    {
        this.builder.append(aText);
        return this;
    }

    /**
     * Create a new element.
     * 
     * @param aName The element name
     * @param aFormattedContent The element content
     * @return the builder
     * 
     * @see #openElement(String, boolean)
     * @see #append(String)
     */
    public XMLBuilder appendElement(String aName, String aFormattedContent)
    {
    	this.openElement(aName, false);
    	this.append(aFormattedContent);
    	this.closeElement(aName);
    	return this;
    }

    /**
     * Open a new XML element.
     * 
     * <p>If attributes are being added to a previous element, close the
     * previous "open" element.
     * </p>
     * 
     * @param aName The element name
     * @param aHasAttrs True when elements will be added
     * @return the builder
     * 
     * @see #setAttribute(String, String)
     * @see #closeElement(String)
     */
    public XMLBuilder openElement(String aName, boolean aHasAttrs)
    {
        if (this.elementOpen)
        {
            this.builder.append(">");
            this.elementOpen = false;
        }

        this.builder.append("<");
        this.builder.append(aName);

        this.elementOpen = aHasAttrs;

        if (!this.elementOpen)
        {
            this.builder.append(">");
        }

        return this;
    }

    /**
     * Close off the "open" element declaration.
     * 
     * @return the builder
     */
    public XMLBuilder openElementCompleted()
    {
    	if (this.elementOpen)
    	{
    		this.builder.append(">");
    		this.elementOpen = false;
    	}
    	return this;
    }
    
    /**
     * Set the attribute.
     * 
     * <p>Encodes content using XML encoding
     * {@link XMLHelper#encodeString(String) rules}.
     * </p>
     * 
     * @param aName The attribute name
     * @param aValue The attribute value (can be null)
     * @return the builder
     */
    public XMLBuilder setAttribute(String aName, String aValue)
    {
        if (aValue != null)
        {
            this.builder.append(" ");
            this.builder.append(aName);
            this.builder.append("=\"");
            this.builder.append(XMLHelper.getInstance().encodeString(aValue));
            this.builder.append("\"");
        }
        return this;
    }

    /**
     * Set the attribute.
     * 
     * @param aName The attribute name
     * @param aValue The attribute value
     * @return the builder
     */
    public XMLBuilder setAttribute(String aName, boolean aValue)
    {
        this.builder.append(" ");
        this.builder.append(aName);
        this.builder.append("=\"");
        this.builder.append(Boolean.toString(aValue));
        this.builder.append("\"");
        return this;
   }

    /**
     * Set the attribute.
     * 
     * @param aName The attribute name
     * @param aValue The attribute value
     * @return the builder
     */
    public XMLBuilder setAttribute(String aName, long aValue)
    {
        this.builder.append(" ");
        this.builder.append(aName);
        this.builder.append("=\"");
        this.builder.append(Long.toString(aValue));
        this.builder.append("\"");
        return this;
    }

    /**
     * Set the attribute.
     * 
     * @param aName The attribute name
     * @param aValue The attribute value
     * @return the builder
     */
    public XMLBuilder setAttribute(String aName, double aValue)
    {
        this.builder.append(" ");
        this.builder.append(aName);
        this.builder.append("=\"");
        this.builder.append(Double.toString(aValue));
        this.builder.append("\"");
        return this;
    }

    /**
     * Create a new element.
     * 
     * @param aName The element name
     * @param aText The text
     * @return the builder
     * 
     * @see #openElement(String, boolean)
     * @see #appendCDATAValue(String)
     */
    public XMLBuilder appendCDATAElement(String aName, String aText)
    {
    	this.openElement(aName, false);
    	this.appendCDATAValue(aText);
    	this.closeElement(aName);
    	return this;
    }

    /**
     * Append a BDATA block to the current element.
     * 
     * <p>Encodes content using XML encoding
     * {@link XMLHelper#encodeCDATA(String) rules}.
     * </p>
     * 
     * @param aText The CDATA content
     * @return the builder
     */
    public XMLBuilder appendCDATAValue(String aText)
    {
        if (this.elementOpen)
        {
            this.builder.append(">");
            this.elementOpen = false;
        }

        this.builder.append("<![CDATA[");
        this.builder.append(XMLHelper.getInstance().encodeCDATA(aText));
        this.builder.append("]]>");
        return this;
    }

    /**
     * Append a stack trace as CDATA.
     * 
     * @param aError The error
     * @return the builder
     * 
     * @see #appendCDATAValue(String)
     */
    public XMLBuilder appendStackTrace(Exception aError)
    {
	    final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final PrintWriter pwriter = new PrintWriter(out);
		aError.printStackTrace(pwriter);
	    pwriter.close();

	    return this.appendCDATAValue(new String(out.toByteArray()));
    }
    
    /**
     * Create a new element.
     * 
     * @param aName The element name
     * @param aText The text
     * @return the builder
     * 
     * @see #openElement(String, boolean)
     * @see #appendTextValue(String)
     */
    public XMLBuilder appendTextElement(String aName, String aText)
    {
    	this.openElement(aName, false);
    	this.appendTextValue(aText);
    	this.closeElement(aName);
    	return this;
    }

    /**
     * Append a text value to the current element.
     * 
     * <p>Encodes content using XML encoding
     * {@link XMLHelper#encodeString(String) rules}.
     * </p>
     * 
     * @param aText The text
     * @return the builder
     */
    public XMLBuilder appendTextValue(String aText)
    {
        if (this.elementOpen)
        {
            this.builder.append(">");
            this.elementOpen = false;
        }

        this.builder.append( XMLHelper.getInstance().encodeString(aText) );
    	return this;
    }

    /**
     * Close the current "open" element being created.
     * 
     * @return the builder
     */
    public XMLBuilder closeElement()
    {
        this.builder.append(" />");
        this.elementOpen = false;
        return this;
    }

    /**
     * Close the XML element.
     * 
     * @param aName The element name
     * @return the builder
     */
    public XMLBuilder closeElement(String aName)
    {
        if (this.elementOpen)
        {
            this.builder.append(">");
            this.elementOpen = false;
        }

        this.builder.append("</");
        this.builder.append(aName);
        this.builder.append(">");
        return this;
    }

    @Override
    public String toString()
    {
        return this.builder.toString();
    }

}