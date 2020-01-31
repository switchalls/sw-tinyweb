package sw.utils.xml.resolvers;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * A <code>LSResourceResolver</code> that delegates to an
 * <code>EntityResolver</code>.
 * 
 * @author Stewart Witchalls
 * @version 1.0
 */
public class SimpleLSResourceResolver implements LSResourceResolver
{
	private static final Logger LOGGER = Logger.getLogger(SimpleLSResourceResolver.class);

	private final DOMImplementationLS domImplementation;
	private final EntityResolver resolver;

	/**
	 * Constructor.
	 * 
	 * @param aImpl The DOM implementation
	 * @param aResolver The entity resolver
	 */
	public SimpleLSResourceResolver(DOMImplementation aImpl, EntityResolver aResolver)
	{
		this.domImplementation = (DOMImplementationLS) aImpl;
		this.resolver = aResolver;
	}

	/**
	 * Constructor.
	 * 
	 * @param aDoc The DOM document
	 * @param aResolver The entity resolver
	 */
	public SimpleLSResourceResolver(Document aDoc, EntityResolver aResolver)
	{
		this( aDoc.getImplementation(), aResolver );
	}

	/** @return the DOM implementation */
	public DOMImplementationLS getDOMImplementation()
	{
		return this.domImplementation;
	}
	
	/** @return the entity resolver */
	public EntityResolver getResolver()
	{
		return this.resolver;
	}

    @Override
	public LSInput resolveResource(
    		String aType, 
            String aNamespaceURI, 
            String aPublicId, 
            String aSystemId, 
            String aBaseURI)
    {
    	try
    	{
    		final InputSource isource  = this.getResolver().resolveEntity(
    			aPublicId, this.getAbsoluteURI(aBaseURI, aSystemId)
    		);

    		if ( isource != null )
    		{
	    		final LSInput i = this.getDOMImplementation().createLSInput();
	    		i.setByteStream( isource.getByteStream() );
	    		i.setCharacterStream( isource.getCharacterStream() );
	    		i.setPublicId( aPublicId );
	    		i.setSystemId( aSystemId );
	    		i.setBaseURI( aBaseURI );
	    		return i;
    		}
    	}
    	catch (Exception e)
    	{
    		LOGGER.error( "", e ); // with stack trace
    	}
		return null; // open regular URI connection
    }

    /**
     * Get the target URI.
     * 
     * <p>If <code>aURI</code> is relative, prefix it with <code>aBaseURI</code>.
     * </p>
     * 
     * @param aBaseURI The base URI
     * @param aURI The URI to be resolved
     * @return the absolute URI
     */
    public String getAbsoluteURI(String aBaseURI, String aURI)
    {
    	if ( aURI.indexOf(":/") > -1 )
    	{
    		return aURI;
    	}
		return aBaseURI + "/" + aURI;
    }

}

