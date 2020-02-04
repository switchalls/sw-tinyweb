package sw.tinyweb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import sw.tinyweb.utils.HttpHeaderUtils;

/**
 * A single HTTP response.
 */
public class TinyWebResponse implements HttpServletResponse {

    private static final Logger LOGGER = Logger.getLogger(TinyWebResponse.class);

    public static final int DEFAULT_BUFFER_SIZE = (4 * 1024); // 4K buffer

    /**
     * Google Chrome is very particular about CRLF usage in chunked responses.
     *
     * <p>
     * According the the spec, chunked format is ...
     * <pre>
     *     &lt;chunk size as HEX>CRLF
     *     &lt;chunk content as byte stream>CRLF
     *     ...
     *     0CRLF
     *     CRLF
     * </pre>
     * </p>
     */
    private static final byte[] CRLF = "\r\n".getBytes();

    /** Buffer that auto writes content to the HTTP output stream when running out of capacity. */
    protected class FixedSizeBuffer extends ByteArrayOutputStream {
        @Override
        public synchronized void close() throws IOException {
            // stream will be closed by TinyWeb main loop
        }

        @Override
        public synchronized void flush() throws IOException {
            flushBuffer();
        }

        @Override
        public synchronized void write(int aValue) {
            super.write(aValue);

            if (this.size() > getBufferSize()) {
                this.flush_ignoreError();
            }
        }

        @Override
        public synchronized void write(byte aBuf[], int aOffset, int aLen) {
            super.write(aBuf, aOffset, aLen);

            if (this.size() > getBufferSize()) {
                this.flush_ignoreError();
            }
        }

        private void flush_ignoreError() {
            try {
                this.flush();
            } catch (final IOException e) {
                LOGGER.error("Cannot flush buffer to HTTP output stream", e);
            }
        }
    }

    private final FixedSizeBuffer buffer = new FixedSizeBuffer();

    private int bufferSize = DEFAULT_BUFFER_SIZE;

    private boolean chunkedOutput;

    private boolean committed;

    private final List<Cookie> cookies = new ArrayList<Cookie>();

    private final Map<String, String> headers = new HashMap<String, String>();

    private Locale locale;

    private final OutputStream outputStream;

    private final HttpServletRequest servletRequest;

    private int statusCode;

    private String statusMessage;

    public TinyWebResponse(HttpServletRequest aRequest, OutputStream aOut) {
        this.outputStream = aOut;
        this.servletRequest = aRequest;

        this.reset();

        this.setCharacterEncoding("UTF-8");
        this.setContentType("text/html");
        this.setLocale(Locale.getDefault());
    }

    @Override
    public String getCharacterEncoding() {
        return this.headers.get("Character-Encoding");
    }

    @Override
    public String getContentType() {
        return this.headers.get("Content-Type");
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {

        return new ServletOutputStream() {
            @Override
            public void close() throws IOException {
                buffer.close();
            }

            @Override
            public void flush() throws IOException {
                buffer.flush();
            }

            @Override
            public void write(int aValue) throws IOException {
                buffer.write(aValue);
            }
        };
    }

    @Override
    public void setCharacterEncoding(String aEncoding) {
        this.setHeader("Character-Encoding", aEncoding);
    }

    @Override
    public void setContentType(String aType) {
        this.setHeader("Content-Type", aType);
    }

    @Override
    public void setHeader(String aName, String aValue) {
        this.headers.put(aName, aValue);
    }

    @Override
    public void setStatus(int aCode) {
        this.statusCode = aCode;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated As of version 2.1, replaced by {@link #sendError(int, String)}
     */
    @Deprecated
    @Override
    public void setStatus(int aCode, String aMsg) {
        throw new UnsupportedOperationException("HttpServletResponse.setStatus(int, String) not supported");
    }

    @Override
    public void flushBuffer() throws IOException {
        if (!this.committed) {
            this.writeResponseHeader();
            this.committed = true;
        }

        try {
            final byte[] data = this.buffer.toByteArray();
            if (!this.chunkedOutput) {
                this.outputStream.write(data);

            } else if (data.length > 0) {
                // EOF is marked with a empty chunk. Therefore, must NEVER
                // create a zero sized chunk whilst stream is open.

                final String chunkSize = Integer.toHexString(this.buffer.size());
                this.outputStream.write(chunkSize.getBytes("UTF-8"));
                this.outputStream.write(CRLF);
                this.outputStream.write(data);
                this.outputStream.write(CRLF);
            }

        } finally {
            this.resetBuffer();
        }
    }

    @Override
    public int getBufferSize() {
        return this.bufferSize;
    }

    @Override
    public Locale getLocale() {
        return this.locale;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(this.buffer, true);
    }

    @Override
    public boolean isCommitted() {
        return this.committed;
    }

    @Override
    public void reset() {
        if (this.committed) {
            throw new IllegalStateException("Cannot reset output stream after it has been committed");
        }

        this.buffer.reset();
        this.cookies.clear();
        this.headers.clear();
        this.locale = Locale.getDefault();
        this.statusCode = SC_OK;
        this.statusMessage = "OK";
    }

    @Override
    public void resetBuffer() {
        this.buffer.reset();
    }

    @Override
    public void setBufferSize(int aSize) {
        this.bufferSize = aSize;
    }

    @Override
    public void setContentLength(int aLength) {
        this.setIntHeader("Content-Length", aLength);
    }

    @Override
    public void setDateHeader(String aName, long aValue) {
        final String dateStr = HttpHeaderUtils.getInstance().formatDate(new Date(aValue), this.getLocale());
        this.setHeader(aName, dateStr);
    }

    @Override
    public void setIntHeader(String aName, int aValue) {
        this.setHeader(aName, Integer.toString(aValue));
    }

    @Override
    public void setLocale(Locale aLocale) {
        this.locale = aLocale;
    }

    @Override
    public void addCookie(Cookie aCookie) {
        this.cookies.add(aCookie);
    }

    @Override
    public void addDateHeader(String aName, long aValue) {
        this.setDateHeader(aName, aValue);
    }

    @Override
    public void addHeader(String aName, String aValue) {
        this.setHeader(aName, aValue);
    }

    @Override
    public void addIntHeader(String aName, int aValue) {
        this.setIntHeader(aName, aValue);
    }

    @Override
    public boolean containsHeader(String aName) {
        return this.headers.containsKey(aName);
    }

    @Override
    public String encodeRedirectURL(String aURL) {
        try {
            return URLEncoder.encode(aURL, "UTF-8");

        } catch (final UnsupportedEncodingException e) {
            // ignore
        }

        return aURL;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated As of version 2.1, replaced by {@link #encodeRedirectURL(String)}
     */
    @Deprecated
    @Override
    public String encodeRedirectUrl(String aURL) {
        throw new UnsupportedOperationException("HttpServletResponse.encodeRedirectUrl() not supported");
    }

    @Override
    public String encodeURL(String aURL) {
        try {
            return URLEncoder.encode(aURL, "UTF-8");

        } catch (final UnsupportedEncodingException e) {
            // ignore
        }
        return aURL;
    }

    /**
     * {@inheritDoc}
     *
     * @deprecated As of version 2.1, replaced by {@link #encodeURL(String)}
     */
    @Deprecated
    @Override
    public String encodeUrl(String aURL) {
        throw new UnsupportedOperationException("HttpServletResponse.encodeUrl() not supported");
    }

    @Override
    public void sendError(int aCode) throws IOException {
        this.setStatus(aCode);
        this.closeStream();
    }

    @Override
    public void sendError(int aCode, String aMsg) throws IOException {
        this.statusCode = aCode;
        this.statusMessage = aMsg;
        this.closeStream();
    }

    @Override
    public void sendRedirect(String aURL) throws IOException {
        throw new UnsupportedOperationException("HttpServletResponse.sendRedirect() not supported");
    }

    /** @return the buffered content */
    public byte[] getBufferContent() {
        return this.buffer.toByteArray();
    }

    /**
     * Find the stated cookie.
     *
     * @param aName
     *            The cookie identifier
     * @return the cookie or null (not found)
     */
    public Cookie findCookie(String aName) {
        for (final Cookie c : this.cookies) {
            if (c.getName().equals(aName)) {
                return c;
            }
        }
        return null;
    }

    /** Close the output stream. */
    public void closeStream() {
        try {
            this.flushBuffer();

            if (this.chunkedOutput) {
                this.outputStream.write('0'); // zero bytes remaining
                this.outputStream.write(CRLF);
                this.outputStream.write(CRLF);
            }

        } catch (final IOException e) {
            LOGGER.error("Cannot commit HttpResponseServlet changes", e);
        }

        try {
            this.outputStream.close();

        } catch (final IOException e) {
            LOGGER.error("Cannot close HTTP response stream", e);
        }
    }

    /**
     * Write the HTTP response header.
     *
     * @throws IOException
     *             when the headers cannot be sent
     */
    private void writeResponseHeader() throws IOException {
        final ByteArrayOutputStream header = new ByteArrayOutputStream();
        final PrintWriter writer = new PrintWriter(header);
        writer.print("HTTP/1.1 ");
        writer.print(this.statusCode);
        writer.print(" ");
        writer.println(this.statusMessage);

        if (this.statusCode < 400) // start of error codes
        {
            if (this.findCookie(TinyWebSession.SESSION_ID) == null) {
                // avoid creating unnecessary HTTP sessions

                final HttpSession s = this.servletRequest.getSession(false);
                if (s != null) {
                    final TinyWebCookie c = new TinyWebCookie(TinyWebSession.SESSION_ID, s.getId());

                    // allow cookie to be used in cross-domain javascript
                    c.setHttpOnly(false);

                    this.addCookie(c);
                }
            }

            if (!this.containsHeader("Content-Length")) {
                // response size unknown, so chunk data to client
                this.addHeader("Transfer-Encoding", "chunked");
                this.chunkedOutput = true;
            }

            for (final String name : this.headers.keySet()) {
                writer.print(name);
                writer.print(": ");
                writer.println(this.headers.get(name));
            }

            final HttpHeaderUtils utils = HttpHeaderUtils.getInstance();
            for (final Cookie c : this.cookies) {
                utils.writeSetCookie(writer, c, this.locale);
            }
        }

        writer.close();

        if (LOGGER.isDebugEnabled()) {
            final String s = new String(header.toByteArray(), "UTF-8");
            LOGGER.debug(s);
        }

        this.outputStream.write(header.toByteArray());
        this.outputStream.write(CRLF);
    }

}
