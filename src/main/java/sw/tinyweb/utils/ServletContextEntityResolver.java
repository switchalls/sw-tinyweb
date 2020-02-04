package sw.tinyweb.utils;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletContext;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * SAX entity resolver.
 */
public class ServletContextEntityResolver implements EntityResolver {

    private final ServletContext context;

    public ServletContextEntityResolver(ServletContext aContext) {
        this.context = aContext;
    }

    /**
     * {@inheritDoc}
     *
     * @see #getPath(String)
     * @see ServletContext#getResourceAsStream(String)
     */
    @Override
    public InputSource resolveEntity(String aPublicId, String aSystemId)
            throws SAXException, IOException {

        final InputStream bstream = this.context.getResourceAsStream(
                this.getPath(aSystemId));

        if (bstream == null) {
            return null;
        }

        final InputSource i = new InputSource();
        i.setByteStream(bstream);
        i.setPublicId(aPublicId);
        i.setSystemId(aSystemId);

        return i;
    }

    /**
     * Extract the "wanted file" path from the system identifier.
     *
     * <p>
     * When executing XML / XSLT includes, some engines may prepend
     * the location of the document requesting the file to the
     * "wanted file" path, eg.
     * <code>file:///<b>C:/WEB-INF/gradex/xsl/GradexPresenter-jobrunner.xsl/</b>/WEB-INF/gradex/xsl/GradexPresenterItems.xsl</code>.
     * </p>
     *
     * @param aSystemId
     *            The system identifier
     * @return the "wanted file" path
     */
    protected String getPath(String aSystemId) {
        final String path = aSystemId.toLowerCase();

        int ipos = path.indexOf(".xsl/");
        if (ipos > 0) {
            return aSystemId.substring(ipos + 5);
        }

        ipos = path.indexOf(".xslt/");
        if (ipos > 0) {
            return aSystemId.substring(ipos + 6);
        }

        ipos = path.indexOf(".xml/");
        if (ipos > 0) {
            return aSystemId.substring(ipos + 5);
        }

        return aSystemId;
    }

}
