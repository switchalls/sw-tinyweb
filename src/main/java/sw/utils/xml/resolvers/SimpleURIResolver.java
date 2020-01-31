package sw.utils.xml.resolvers;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * A <code>URIResolver</code> that delegates to an <code>EntityResolver</code>.
 * 
 * @author Stewart Witchalls
 * @version 1.0
 */
public class SimpleURIResolver implements URIResolver
{
	private static final Logger LOGGER = Logger.getLogger(SimpleURIResolver.class);

	private final EntityResolver resolver;

	/**
	 * Constructor.
	 * 
	 * @param aResolver The entity resolver
	 */
	public SimpleURIResolver(EntityResolver aResolver)
	{
		this.resolver = aResolver;
	}
	
	/** @return the entity resolver */
	public EntityResolver getResolver()
	{
		return this.resolver;
	}

    @Override
	public Source resolve(String aHref, String aBase)
    throws TransformerException
    {
    	try
    	{
    		final String targetURI = this.getAbsoluteURI( aBase, aHref );
    		final InputSource isource  = this.getResolver().resolveEntity(
    			null, targetURI
    		);

    		if ( isource != null )
    		{
    			final StreamSource s = new StreamSource();
    			s.setInputStream( isource.getByteStream() );
    			s.setReader( isource.getCharacterStream() );
    			s.setPublicId( null );
    			s.setSystemId( targetURI );
    			return s;
    		}
    	}
    	catch (Exception e)
    	{
    		LOGGER.error("", e ); // with stack trace
    	}
		return null; // inform parser to resolve URI itself
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

