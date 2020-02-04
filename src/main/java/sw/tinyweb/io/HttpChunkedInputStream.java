package sw.tinyweb.io;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Input stream for reading chunked data from HTTP payload.
 *
 * <p>
 * For example,
 * <pre>
 *     POST /login.jsp HTTP/1.1
 *     Content-Type: chunked
 *
 *     182
 *     &lt;0x182 characters&gt;
 *     0
 * </pre>
 * </p>
 *
 * @author $Author: $
 * @version $Revision: $
 */
public class HttpChunkedInputStream extends InputStream {

    private static final byte[] NO_DATA = new byte[0];

    private int currentPos;

    private byte[] data = NO_DATA;

    private boolean eof;

    private final BufferedReader reader;

    public HttpChunkedInputStream(InputStream aIn) {
        reader = new BufferedReader(new InputStreamReader(aIn));
    }

    public HttpChunkedInputStream(HttpHeaderReader aReader) {
        reader = new BufferedReader(aReader.getReader());
    }

    @Override
    public void close() throws IOException {
        this.reader.close();
    }

    @Override
    public int read() throws IOException {
        if (this.eof) {
            return -1;
        }

        if (this.currentPos >= this.data.length) {
            final int len = this.readChunk();
            if (len < 1) {
                this.eof = true;
                return -1;
            }
        }

        return this.data[this.currentPos++];
    }

    /**
     * Buffer the next chunk.
     *
     * @return the chunk size or -1 (EOF)
     * @throws IOException
     *             when the chunk cannot be read
     */
    public int readChunk() throws IOException {
        this.currentPos = 0;
        this.data = NO_DATA;

        // read chunk header

        final String header = this.reader.readLine();
        if (header == null) {
            return -1; // EOF
        }

        int chunkSize = 0;
        try {
            chunkSize = Integer.parseInt(header, 16);

        } catch (final NumberFormatException e) {
            throw new IOException("Invalid chunk header: " + header);
        }

        // buffer chunk content

        if (chunkSize > 0) {
            final ByteArrayOutputStream buf = new ByteArrayOutputStream();
            for (int i = 0; i < chunkSize; i++) {
                final int c = this.reader.read();
                buf.write(c);
            }

            this.data = buf.toByteArray();

            // skip end-of-line

            final String eol = this.reader.readLine();
            if (eol.length() > 0) {
                throw new IOException("Invalid chunk size: expected " + chunkSize + " bytes but read " + (chunkSize + eol.length()));
            }
        }

        return data.length;
    }

}
