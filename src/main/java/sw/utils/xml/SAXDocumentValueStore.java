package sw.utils.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * <code>SAXDocument</code> plugin to store element values.
 * 
 * <p>XML attributes are not stored.
 * </p>
 * 
 * <p>The user defines which elements should be loaded by calling
 * <code>loadValue</code>. XPath(s) begining with '/' define fully
 * qualified paths. Otherwise, any element whose path ends with the
 * XPath will be loaded.
 * </p>
 * 
 * <p>Use <code>getNodeValues</code> to extract values. The XPath pattern
 * must obey the same rules as <code>loadValue</code>. For example,
 * <code>b/c</code> will match <code>/a/b/c</code>,
 * where <code>/a/b</code> will not match <code>/a/b/c</code>.
 * </p>
 *
 * <p>Code example:
 * <pre>
 *    SAXDocumentValueStore valueStore = new SAXDocumentValueStore();
 *    valueStore.loadValue( "/Junit/r6" );
 *
 *    SAXDocument sax = new SAXDocument();
 *    sax.setValueStore( valueStore );
 *    sax.loadDocument( ... );
 *
 *    String[] values = valueStore.getNodeValues( "r6" );
 * </pre>
 * <p>
 * 
 * @author Stewart Witchalls
 * @version 4.0
 */
public class SAXDocumentValueStore implements SAXDocument.ContentHandler
{
    public static final String MISSING_NODE_VALUE = "Missing node value";

    /** List&lt;XPath&gt; - Which nodes should have their values cached? */
    private List<String> loadValueList = new ArrayList<String>();

    /** List&lt;XPath,List&lt;String&gt;&gt; - Cache of node values  */
    private Map<String, List<String>> elementValueMap = new HashMap<String, List<String>>();
    
    /** Reset this store */
    public void reset()
    {
        this.loadValueList.clear();        
        this.elementValueMap.clear();        
    }
    
    /**
     * Any element matching the XPath will have its value stored.
     *
     * <p>This method must be called before any document is loaded.
     * </p>
     *
     * @param aXPath the path
     */
    public void loadValue(String aXPath)
    {
        loadValueList.add(aXPath);
    }

    /** Remove all loaded values */
    public void clearValues()
    {
        loadValueList.clear();
    }

    /**
     * Was a value stored for the element or one of its children?
     *
     * <p>This method assumes that the XPath only contains the path to be
     * matched. XPath expressions (eg. match-where) are not supported.
     * </p>
     *
     * @param aPattern the path to match
     * @return true when the value for an element or child has been loaded
     * @throws XPathExpressionException when the document cannot be searched
     */
    public boolean contains(String aPattern)
    throws XPathExpressionException
    {
    	for ( String epath : elementValueMap.keySet() )
        {            
            if ( epath.equals(aPattern) )
                return true;

            if ( epath.startsWith(aPattern+"/") )
                return true;

            if ( epath.endsWith("/"+aPattern) )
                return true;

            if ( epath.indexOf("/"+aPattern+"/") > 0 )
                return true;  
        }

        return false;
    }
    
    /**
     * Return all stored values.
     *
     * <p>This method assumes that the XPath only contains the path to be
     * matched. XPath expressions (eg. match-where) are not supported.
     * </p>
     *
     * <p>XPath(s) beginning with '/' define fully qualified paths
     * (child elements will not be matched). Otherwise, any XPath
     * ending with the stated pattern will be matched.
     * </p>
     * 
     * @param aPattern the path to match
     * @return the value of all nodes matching the pattern
     * @throws XPathExpressionException when the document cannot be searched
     * @throws XMLException when no nodes are matched
     */
    public String[] getNodeValues(String aPattern)
    throws XPathExpressionException, XMLException
    {
        Collection<String> values = new ArrayList<String>();
        
        for ( String epath : elementValueMap.keySet() )
        {
            boolean isMatch = false;
            if (aPattern.startsWith("/"))
            {
                isMatch = epath.equals(aPattern);
            }
            else
            {
                isMatch = epath.endsWith("/"+aPattern);
            }

            if ( isMatch )
            {
                final List<String> c = elementValueMap.get(epath);
                values.addAll( c );
            }
        }

        if (values.size() < 1)
        {
            String msg = MISSING_NODE_VALUE + ", xpath=[" + aPattern + "]";
            throw new XMLException(msg);
        }

        String[] results = new String[values.size()];
        values.toArray(results);
        return results;
    }

    /**
     * Element started event.
     * 
     * @param aXPath The path from the document root
     * @param aElementName The XML element
     * @param aAttrs The XML attributes
     * @throws SAXException when an error should abort parsing
     */
    public void elementStart(String aXPath, String aElementName, Attributes aAttrs)
    throws SAXException
    {
        // do nothing
    }

    /**
     * Element closed event.
     * 
     * @param aXPath The path from the document root
     * @param aElementName The XML element
     * @param aDefaultContent The XML element default content
     * @param aAttrs The XML attributes
     * @throws SAXException when an error should abort parsing
     */
    public void elementEnd(
            String aXPath,
            String aElementName,
            String aDefaultContent,
            Attributes aAttrs)
    throws  SAXException
    {
        if (storeElementPath(aXPath))
        {
            List<String> valueList = elementValueMap.get(aXPath);
            if (valueList == null)
            {
                valueList = new ArrayList<String>();
                elementValueMap.put(aXPath, valueList);
            }

            valueList.add(aDefaultContent);
        }        
    }

    /**
     * Should this element's value be stored?
     *
     * <p>XPath(s) beginning with '/' define fully qualified paths
     * (child elements will not be matched). Otherwise, any XPath
     * ending with the stated pattern will be matched.
     * </p>
     *
     * @param aXPath the element's fully qualified XPath
     * @return true if the value should be stored
     * @see #loadValue(String)
     */
    protected boolean storeElementPath(String aXPath)
    {
    	for ( String pattern : loadValueList )
        {
            boolean isMatch = false;
            if (pattern.startsWith("/"))
            {
                isMatch = aXPath.equals(pattern);
            }
            else
            {
                isMatch = aXPath.endsWith("/"+pattern);
            }

            if ( isMatch )
            {
                return true;
            }
        }

        return false;
    }

}
