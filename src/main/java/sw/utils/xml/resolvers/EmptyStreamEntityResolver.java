package sw.utils.xml.resolvers;

import java.io.ByteArrayInputStream;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * An <code>EntityResolver</code> that will return an empty stream for
 * all requested entities.
 * 
 * <p>Used to stop <i>cannot find DTD</i> errors.
 * </p>
 * 
 * @author Stewart Witchalls
 * @version 1.0
 */
public class EmptyStreamEntityResolver implements EntityResolver
{
	/** Empty byte array. */
	public static final byte[] NO_BYTES = new byte[0];

	@Override
	public InputSource resolveEntity(String aPublicId, String aSystemId)
	{
		return new InputSource( new ByteArrayInputStream(NO_BYTES) );
	}
}
