package sw.tinyweb;

import javax.annotation.Nullable;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;

import org.apache.log4j.Logger;

/**
 * {@link RequestDispatcher} factory.
 */
public class TinyWebRequestDispatcherFactory {

    private static final Logger LOGGER = Logger.getLogger(TinyWebRequestDispatcherFactory.class);

    private static TinyWebRequestDispatcherFactory globalInstance;

    public static TinyWebRequestDispatcherFactory getInstance() {
        if (globalInstance == null) {
            globalInstance = new TinyWebRequestDispatcherFactory();
        }
        return globalInstance;
    }

    private TinyWebServer server;

    @Nullable
    public RequestDispatcher createRequestDispatcherByName(String aName) {
        try {
            Servlet targetServlet = null;
            if (this.server != null) {
                targetServlet = this.server.createServletByName(this.server.getRootContext(), aName);
            }

            if (targetServlet != null) {
                return new TinyWebRequestDispatcher(targetServlet, "/");
            }

        } catch (final Exception e) {
            LOGGER.error("Cannot create request dispatcher", e);
        }

        return null;
    }

    /**
     * Create a new dispatcher.
     *
     * @param aPath
     *            The servlet URL
     * @return the dispatcher
     */
    @Nullable
    public RequestDispatcher createRequestDispatcherByPath(String aPath) {
        try {
            Servlet targetServlet = null;
            if (this.server != null) {
                targetServlet = this.server.createServletByPath(this.server.getRootContext(), aPath);
            }

            if (targetServlet != null) {
                return new TinyWebRequestDispatcher(targetServlet, aPath);
            }

        } catch (final Exception e) {
            LOGGER.error("Cannot create request dispatcher", e);
        }

        return null;
    }

    public void setServer(TinyWebServer aServer) {
        this.server = aServer;
    }

}
