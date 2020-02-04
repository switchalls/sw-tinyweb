package sw.tinyweb.servlets;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import sw.tinyweb.TinyWebServletContext;
import sw.tinyweb.utils.ServletUtils;

/**
 * Download the contents of a single resource.
 *
 * <p>
 * Supports <code>Last-Modified</code> and <code>Cache-Control</code>
 * based caching.
 * <p>
 *
 * @author $Author: $
 * @version $Revision: $
 */
public class DownloadFileServlet extends HttpServlet {
    private static final long serialVersionUID = -4596308910266964873L;

    private static final String CACHE_CONTROL = "Cache-Control";

    private static final String MAX_AGE = "max-age=180"; // 30 minutes

    private static final String IF_MODIFIED_SINCE = "If-Modified-Since";

    private static final String LAST_MODIFIED = "Last-Modified";

    private static final Logger LOGGER = Logger.getLogger(DownloadFileServlet.class);

    @Override
    public void doGet(HttpServletRequest aRequest, HttpServletResponse aResponse)
            throws ServletException, IOException {

        final TinyWebServletContext context = (TinyWebServletContext) this.getServletContext();

        final String resourcePath = aRequest.getServletPath();
        if (resourcePath.indexOf("WEB-INF") > -1) {
            LOGGER.error("Cannot access resource: " + resourcePath);
            aResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Cannot access resource: " + resourcePath);
            return;
        }

        final long lastModified = context.getResourceLastModified(resourcePath);
        if (lastModified == -1) {
            LOGGER.error("Cannot find resource: " + resourcePath);
            aResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Cannot find resource: " + resourcePath);
            return;
        }

        final long ifModifiedSince = aRequest.getDateHeader(IF_MODIFIED_SINCE);
        if (ifModifiedSince > -1) {
            if ((lastModified / 1000) <= (ifModifiedSince / 1000)) // ignore milli-seconds
            {
                LOGGER.info("Resource not modified: " + resourcePath);
                aResponse.sendError(HttpServletResponse.SC_NOT_MODIFIED, "Not Modified");
                return;
            }
        }

        final InputStream in = context.getResourceAsStream(resourcePath);
        assert (in != null);

        aResponse.setCharacterEncoding("UTF-8 ");
        aResponse.setContentType(context.getMimeType(resourcePath));
        aResponse.addDateHeader(LAST_MODIFIED, lastModified);
        aResponse.addHeader(CACHE_CONTROL, MAX_AGE);

        try {
            ServletUtils.copyContent(in, aResponse.getOutputStream());

        } finally {
            in.close();
        }
    }

    @Override
    public void doPost(HttpServletRequest aRequest, HttpServletResponse aResponse)
            throws ServletException, IOException {

        this.doGet(aRequest, aResponse);
    }

}
