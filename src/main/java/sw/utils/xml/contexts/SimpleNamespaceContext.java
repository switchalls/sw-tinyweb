package sw.utils.xml.contexts;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * Helper class for creating a XPath <code>NamespaceContext</code>.
 * 
 * <p>XML documents can contain a single <i>default</i> namespace
 * (where no prefix is declared) and multiple named namepaces
 * (where prefixes are declared), eg.
 * <pre>
 *  &lt;svg <b>xmlns</b>="http://www.w3.org/2000/svg" <b>xmlns:xlink</b>="http://www.w3.org/1999/xlink" <b>xmlns:math</b>="http://exslt.org/math" ... &gt;
 *    &lt;title&gt; ... &lt;/title&gt;
 *    &lt;desc&gt; ... &lt;/desc&gt;
 *    &lt;g style="fill:none"&gt;
 *      &lt;g pid="1" x="35" y="55" stroke-linecap="round"&gt;                          
 *        &lt;text font-size="10" fill="black" id="1" &gt; ... &lt;/text&gt;
 *        ...
 *      &lt;/g&gt;
 *    &lt;/g&gt;
 *  &lt;/svg&gt;
 * </pre>
 * </p>
 * 
 * <p>XPath queries will only find elements in specific namespaces.
 * Consequently, they will not (by default) find elements declared
 * in a <i>default</i> namespace. For example, the expression
 * "//text" will not return any elements from the above XML.
 * </p>
 * 
 * <p>Finding elements in a <i>default</i> namespace requires the
 * association of a prefix with that namespace. This is achieved by
 * injecting a prefix into the XPath expression (eg. <code>//svg:text</code>)
 * and providing the expression interpreter with a means of <i>discovering</i>
 * the URI associated with that prefix, eg.
 * <pre>
 *   XPath xpath = ...
 *   xpath.<b>setNamespaceContext</b>( new <b>DefaultNamespaceContext</b>("svg", "http://www.w3.org/2000/svg") );
 * </pre>
 * </p>
 * 
 * @author Stewart Witchalls
 * @version 1.0
 */
public class SimpleNamespaceContext implements NamespaceContext
{
	/** Collection of namespaceURIs indexed by prefix. */
	private Map<String,String> namespaces = new HashMap<String,String>();

	/** Default constructor. */
	public SimpleNamespaceContext()
	{
		// do nothing
	}

	/**
	 * Constructor.
	 * 
	 * @param aPrefix The prefix
	 * @param aURI The namespace URI
	 */
	public SimpleNamespaceContext(String aPrefix, String aURI)
	{
		this.namespaces.put( aPrefix, aURI );
	}

	/** @return collection of namespace URI(s) indexed by prefix */
	public Map<String,String> getNamespaces()
	{
		return this.namespaces;
	}

	/** @param aMap A Collection of namespace URI(s) indexed by prefix */
	public void setNamespaces(Map<String,String> aMap)
	{
		this.namespaces = aMap;
	}

	/**
	 * Get the URI associated with the prefix.
	 * 
	 * @param aPrefix The prefix
	 * @return the URI or <code>XMLConstants.NULL_NS_URI</code> (not found)
	 */
    @Override
	public String getNamespaceURI(String aPrefix)
	{
    	final String uri = this.namespaces.get(aPrefix);
    	return (uri != null) ? uri : XMLConstants.NULL_NS_URI;
	}
    
	/**
	 * Get the prefix associated with the URI.
	 * 
	 * @param aNamespaceURI The URI
	 * @return the prefix or null (not found)
	 */
    @Override
	public String getPrefix( String aNamespaceURI)
    {
    	for ( Map.Entry<String,String> me : namespaces.entrySet() )
    	{
    		if ( me.getValue().equals(aNamespaceURI) )
    		{
    			return me.getKey();
    		}
    	}
    	return null;
     }
    
	/**
	 * List all prefixes associated with the URI.
	 * 
	 * @param aNamespaceURI The URI
	 * @return the prefixes
	 */
    @Override
	public Iterator<String> getPrefixes( String aNamespaceURI)
    {
    	final ArrayList<String> prefixes = new ArrayList<String>();
    	for ( Map.Entry<String,String> me : namespaces.entrySet() )
    	{
    		if ( me.getValue().equals(aNamespaceURI) )
    		{
    			prefixes.add( me.getKey() );
    		}
    	}
    	return prefixes.iterator();
    }

}
