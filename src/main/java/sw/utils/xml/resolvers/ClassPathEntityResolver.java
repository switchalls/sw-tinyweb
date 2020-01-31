package sw.utils.xml.resolvers;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <code>EntityResolver</code> that locates resources by searching the
 * classpath.
 * 
 * <p>Uses <code>Class.getResourceAsStream(String)</code>.
 * </p>
 * 
 * @author Stewart Witchalls
 * @version 1.0
 */
public class ClassPathEntityResolver implements EntityResolver
{
	private static final Logger LOGGER = Logger.getLogger(ClassPathEntityResolver.class);

	private Class<?> classPath[] = {ClassPathEntityResolver.class};

	/** @param aClasses The classes to search from */
	public void setClassPath(Class<?>[] aClasses)
	{
		this.classPath = aClasses;
	}

    @Override
	public InputSource resolveEntity(String aPublicId, String aSystemId)
    throws SAXException, IOException
    {
    	final String fname = this.getFilename( aSystemId );
    	
    	InputStream bstream = null;
    	for ( int i=0;  (bstream == null) && (i < this.classPath.length);  i++)
    	{
    		bstream = this.classPath[i].getResourceAsStream(fname);
    	}

    	if ( bstream == null )
    	{
    		LOGGER.warn( "Cannot find "+fname+" on classpath" );
    		return null;
    	}

    	final InputSource i = new InputSource();
    	i.setByteStream( bstream );
    	i.setPublicId( aPublicId );
    	i.setSystemId( aSystemId );
    	return i;
    }
    
    /**
     * Get the name of the file to be found.
     * 
     * @param aURI The file's full location
     * @return the file name
     */
    protected String getFilename(String aURI)
    {
    	final int ipos = aURI.lastIndexOf('/');
    	return (ipos < 0) ? aURI : aURI.substring(ipos+1);
    }

}
