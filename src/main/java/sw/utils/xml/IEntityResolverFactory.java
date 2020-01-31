package sw.utils.xml;

import org.xml.sax.EntityResolver;

/**
 * <code>EntityResolver</code> factory.
 * 
 * <p>Factory must support a default constructor.
 * </p>
 * 
 * @author Stewart Witchalls
 * @version 1.0
 */
public interface IEntityResolverFactory
{
	/**
	 * Get a external resource resolver.
	 * 
	 * @return the resolver or null (use default resolvers).
	 */
	EntityResolver getEntityResolver();
}
