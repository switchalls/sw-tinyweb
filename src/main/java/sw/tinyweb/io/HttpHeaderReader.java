package sw.tinyweb.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * HTTP header reader.
 * 
 * <p>Stops the user reading into the data portion.
 * </p>
 * 
 * @author $Author: $
 * @version $Revision: $
 */
public class HttpHeaderReader
{
	private final StringBuffer data = new StringBuffer( 80 );
	private final Reader reader;
	
	/**
	 * Constructor.
	 * 
	 * @param aIn The HTTP stream
	 */
	public HttpHeaderReader(InputStream aIn)
	{
		this.reader = new InputStreamReader( aIn );
	}

	/** @return the reader */
	public Reader getReader()
	{
		return this.reader;
	}
	
	/**
	 * Read the stated number of characters.
	 * 
	 * @param aLen The required number of characters
	 * @return the data
	 * @throws IOException when the stream cannot be read
	 */
	public String read(int aLen)
	throws IOException
	{
		if ( aLen < 0 )
		{
			throw new IllegalArgumentException("Invalid read length: "+aLen);
		}
		
		data.setLength( 0 );
		
		for ( int i=0;  i < aLen;  i++)
		{
			final int c = this.reader.read();
			data.append( (char)c );
		}
		
		return data.toString();
	}
	
	/**
	 * Read single line of text from the header.
	 * 
	 * @return the line of text or null (EOF)
	 * @throws IOException when the stream cannot be read
	 */
	public String readLine()
	throws IOException
	{
		data.setLength( 0 );
		
		int c;
		while ( (c = this.reader.read()) > -1 )
		{
			if ( c == '\n' )
			{
				break;
			}

			if ( c != '\r' )
			{
				data.append( (char)c );
			}
		}
		
		if ( data.length() > 0 )
		{
			return data.toString();
		}

		return null;
	}

}
