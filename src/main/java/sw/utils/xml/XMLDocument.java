package sw.utils.xml;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

/**
 * Generic interface for all XML documents.
 * 
 * @author Stewart Witchalls
 * @version 1.0
 */
public interface XMLDocument
{
	/** Validation option. */
	public static final String NO_VALIDATION = "none";
	/** Validation option. */
	public static final String DTD_VALIDATION = "dtd";
	/** Validation option. */
	public static final String W3C_SCHEMA_VALIDATION = "w3cSchema";

    /** @return true if the current document was loaded with validation */
    boolean isValidated();

    /** @return the name of the current document's root element */
    String getDocumentName();

	/**
	 * Set the external resource resolver.
	 * 
	 * <p>
	 * Resolvers are used to locate and load any resources referenced by the
	 * XML document, eg. DTDs.
	 * </p>
	 * 
	 * <p>
	 * <code>null</code> = use default resolvers.
	 * </p>
	 * 
	 * @param aResolver The custom resolver (can be null)
	 */
	void setEntityResolver(EntityResolver aResolver);

	/** @return the current document's public id */
    String getPublicId();

    /** @return the current document's system id */
    String getSystemId();

    /**
     * What W3C schema was used?
     * 
     * @return the schema
     */
    String getSchemaURL();

    /** @return the type of validation used, eg. {@link #DTD_VALIDATION} */
    String getValidationType();

	/**
     * Load the XML document.
     *
     * @param aDocumentName The document id
     * @param aXmlContent The XML content
     * @param aValidateXmlContent True when the parser should validate the XML
     * @throws IOException when the stream cannot be read
     * @throws ParserConfigurationException when the XML parser cannot be created
     * @throws SAXException when the contents fail validation
     */
    void loadDocument(
    		String aDocumentName,
    		InputStream aXmlContent,
    		boolean aValidateXmlContent)
    throws	IOException,
    		ParserConfigurationException,
    		SAXException;

    /**
     * Load the XML document.
     *
     * <p>
     * Validate the XML content using the stated W3C schema/
     * </p>
     * 
     * @param aDocumentName The document id
     * @param aXmlContent The XML content
     * @param aSchemaURL The W3C schema
     * @throws IOException when the stream cannot be read
     * @throws ParserConfigurationException when the XML parser cannot be created
     * @throws SAXException when the contents fail validation
     */
    void loadDocumentUsingW3CSchema(
            String aDocumentName,
            InputStream aXmlContent,
            String aSchemaURL)
    throws  IOException,
            ParserConfigurationException,
            SAXException;
}