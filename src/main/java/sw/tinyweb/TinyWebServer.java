package sw.tinyweb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * A small footprint HTTP web server.
 *
 * <p>
 * Can process basic HTTP 1.1 requests only.
 * </p>
 *
 * <p>
 * Designed for use on low powered embedded platforms.
 * </p>
 *
 * @see Servlet
 */
public class TinyWebServer implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(TinyWebServer.class);

    private boolean cancelled;

    private final List<ServletContextListener> contextListeners = new ArrayList<ServletContextListener>();

    private final List<ServletContextAttributeListener> contextAttributeListeners = new ArrayList<ServletContextAttributeListener>();

    private final int listenPort;

    private TinyWebServletContext rootContext;

    private final Map<String, Servlet> servletCache = new HashMap<String, Servlet>();

    private final Map<String, TinyWebServletConfig> servletConfigs = new HashMap<String, TinyWebServletConfig>();

    private final Map<String, String> servletMappings = new HashMap<String, String>();

    private final File webContentHome;

    /**
     * Constructor.
     *
     * @param aPort
     *            The port to listen on
     * @param aWebContentHome
     *            Location of the web content folder
     */
    public TinyWebServer(int aPort, File aWebContentHome) {
        this.listenPort = aPort;
        this.webContentHome = aWebContentHome;
    }

    /** @return the root servlet context */
    public ServletContext getRootContext() {
        return this.rootContext;
    }

    /**
     * Add a new listener.
     *
     * <p>
     * From <code>web.xml</code> ...
     * <pre>
     *     &lt;listener>
     *         &lt;listener-class>org.apache.tiles.web.startup.TilesListener&lt;/listener-class>
     *     &lt;/listener>
     * </pre>
     * </p>
     *
     * @param aListener
     *            The listener
     */
    public void addServletContextListener(ServletContextListener aListener) {
        this.contextListeners.add(aListener);
    }

    /**
     * Remove the stated listener.
     *
     * @param aListener
     *            The listener
     */
    public void removeServletContextListener(ServletContextListener aListener) {
        this.contextListeners.remove(aListener);
    }

    /**
     * Add a new listener.
     *
     * <p>
     * From <code>web.xml</code> ...
     * <pre>
     *     &lt;listener>
     *         &lt;listener-class>...MyAttributerListener&lt;/listener-class>
     *     &lt;/listener>
     * </pre>
     * </p>
     *
     * @param aListener
     *            The listener
     */
    public void addServletContextAttributeListener(ServletContextAttributeListener aListener) {
        this.contextAttributeListeners.add(aListener);
    }

    /**
     * Remove the stated listener.
     *
     * @param aListener
     *            The listener
     */
    public void removeServletContextAttributeListener(ServletContextAttributeListener aListener) {
        this.contextAttributeListeners.remove(aListener);
    }

    /**
     * Add servlet configuration.
     *
     * <p>
     * From <code>web-config.xml</code> ...
     * <pre>
     *     &lt;servlet>
     *         &lt;servlet-name>MessageBrokerServlet&lt;/servlet-name>
     *         &lt;servlet-class>flex.messaging.MessageBrokerServlet&lt;/servlet-class>
     *         &lt;init-param>
     *             &lt;param-name>services.configuration.file&lt;/param-name>
     *             &lt;param-value>/WEB-INF/flex/services-config.xml&lt;/param-value>
     *         &lt;/init-param>
     *     &lt;/servlet>
     * </pre>
     *
     * @param aConfig
     *            The servlet configuration
     */
    public void addServletConfiguration(TinyWebServletConfig aConfig) {
        this.servletConfigs.put(aConfig.getServletName(), aConfig);
    }

    /**
     * Add mapping.
     *
     * <p>
     * From <code>web.xml</code> ...
     * <pre>
     *    &lt;servlet-mapping>
     *        &lt;servlet-name>MessageBrokerServlet&lt;/servlet-name>
     *        &lt;url-pattern>/messagebroker/*&lt;/url-pattern>
     *    &lt;/servlet-mapping>
     * </pre>
     * </p>
     *
     * @param aPattern
     *            The URL pattern or "*" (match any)
     * @param aServletName
     *            The servlet identifier
     */
    public void addServletMapping(String aPattern, String aServletName) {
        this.servletMappings.put(aPattern, aServletName);
    }

    /**
     * Find the configuration for the stated servlet.
     *
     * @param aServletName
     *            The servlet identifier
     * @return the configuration or null (not found)
     */
    public TinyWebServletConfig findServletConfigByName(String aServletName) {
        return this.servletConfigs.get(aServletName);
    }

    /**
     * Find the configuration for the stated servlet.
     *
     * @param aPath
     *            The servlet URL
     * @return the configuration or null (not found)
     */
    public TinyWebServletConfig findServletConfigByPath(String aPath) {
        String name = this.servletMappings.get(aPath);
        if (name == null) {
            name = this.servletMappings.get("*");
        }

        return this.servletConfigs.get(name);
    }

    /**
     * Create the servlet associated with the stated url path.
     *
     * @param aContext
     *            The application context
     * @param aURL
     *            The request path, eg. "/xyz"
     * @return the servlet
     * @throws Exception
     *             when the servlet cannot be created
     *
     * @see #findServletConfigByPath(String)
     * @see #createServletByName(ServletContext, String)
     */
    public Servlet createServletByPath(ServletContext aContext, String aURL)
            throws Exception {

        final TinyWebServletConfig config = this.findServletConfigByPath(aURL);
        if (config != null) {
            return this.createServletByName(aContext, config.getServletName());
        }

        throw new TinyWebException(HttpStatusCodes.NOT_IMPLEMENTED, "Cannot find servlet by path: " + aURL);
    }

    /**
     * Create the servlet associated with the stated url path.
     *
     * @param aContext
     *            The application context
     * @param aServletName
     *            The servlet identifier
     * @return the servlet
     * @throws Exception
     *             when the servlet cannot be created
     *
     * @see #findServletConfigByName(String)
     */
    public Servlet createServletByName(ServletContext aContext, String aServletName)
            throws Exception {

        final TinyWebServletConfig config = this.findServletConfigByName(aServletName);
        if (config == null) {
            throw new TinyWebException(HttpStatusCodes.NOT_IMPLEMENTED, "Cannot find servlet: " + aServletName);
        }

        Servlet s = this.servletCache.get(config.getServletClass());
        if (s == null) {
            final Class<?> c = Class.forName(config.getServletClass());
            s = (Servlet) c.newInstance();

            config.setServletContext(aContext);
            s.init(config);

            this.servletCache.put(c.getName(), s);
        }

        return s;
    }

    /**
     * Execute the server.
     *
     * @see #processRequest(ServletContext, Socket)
     * @see #stop()
     */
    @Override
    public void run() {
        final TinyWebSessionManager smgr = TinyWebSessionManager.getInstance();

        ServerSocket listeningSocket = null;
        try {
            TinyWebRequestDispatcherFactory.getInstance().setServer(this);

            // create root servlet context

            this.rootContext = new TinyWebServletContext("/", "TinyWeb 1.0", this.webContentHome);
            this.fireContextInitialized(this.rootContext);

            smgr.setServletContext(rootContext);

            rootContext.addServletContextAttributeListener(this.contextAttributeListeners);

            // wait for and process HTTP requests

            LOGGER.info("Starting web server on port " + this.listenPort);

            listeningSocket = new ServerSocket(this.listenPort);
            while (!this.cancelled) {
                final Socket clientSocket = listeningSocket.accept();

                smgr.removeStaleSessions();

                this.processRequest(rootContext, clientSocket);
            }

            // shutdown server

            for (final String clazz : this.servletCache.keySet()) {
                final Servlet s = this.servletCache.get(clazz);
                s.destroy();
            }

            this.fireContextDestroyed(this.rootContext);

        } catch (final Throwable e) {
            LOGGER.error("Web server failed", e);
        } finally {
            closeSocket(listeningSocket);
        }
    }

    /** Stop server execution. */
    public void stop() {
        LOGGER.info("Web server stopped by user");
        this.cancelled = true;
    }

    /**
     * Process the HTTP request.
     *
     * @param aContext
     *            The application context
     * @param aSocket
     *            The socket on which the request was received
     * @throws Exception
     *             when the request cannot be processed
     *
     * @see #createServletByPath(ServletContext, String)
     * @see #executeServlet(Socket, Servlet, TinyWebRequest, TinyWebResponse)
     */
    private void processRequest(ServletContext aContext, Socket aSocket) throws Exception {
        final InputStream in = aSocket.getInputStream();
        final TinyWebRequest hreq = new TinyWebRequest();

        final OutputStream out = aSocket.getOutputStream();
        final TinyWebResponse hresp = new TinyWebResponse(hreq, out);

        try {
            hreq.setLocalAddress((InetSocketAddress) aSocket.getLocalSocketAddress());
            hreq.setRemoteAddress((InetSocketAddress) aSocket.getRemoteSocketAddress());
            hreq.setServletResponse(hresp);
            hreq.initRequest(in);

            final Servlet servlet = this.createServletByPath(aContext, hreq.getRequestURI());
            hreq.setContextPath(""); // root context; individual application contexts not supported
            hreq.setServletPath(hreq.getRequestURI());

            this.executeServlet(aSocket, servlet, hreq, hresp);

        } catch (final IOException e) {
            this.sendError(hresp, HttpStatusCodes.BAD_REQUEST, e.getMessage());
            closeSocket(aSocket, hreq);
        } catch (final TinyWebException e) {
            this.sendError(hresp, e.getErrorCode(), e.getMessage());
            closeSocket(aSocket, hreq);
        }
    }

    /**
     * Execute the stated servlet.
     *
     * @param aClientSocket
     *            The socket associated with the request
     * @param aServlet
     *            The servlet
     * @param aRequest
     *            The HTTP request
     * @param aResponse
     *            The HTTP response
     *
     * @see #findServletConfigByPath(String)
     */
    private void executeServlet(
            final Socket aClientSocket,
            final Servlet aServlet,
            final TinyWebRequest aRequest,
            final TinyWebResponse aResponse) {

        final Runnable servletRunner = new Runnable() {
            @Override
            public void run() {
                try {
                    aServlet.service(aRequest, aResponse);
                    aResponse.closeStream();
                } catch (final Exception e) {
                    LOGGER.error("TinyWeb servlet execution failed", e);
                    sendError(aResponse, HttpStatusCodes.INTERNAL_SERVER_ERROR, e.getMessage());
                } finally {
                    closeSocket(aClientSocket, aRequest);
                }
            }
        };

        final TinyWebServletConfig config = this.findServletConfigByPath(aRequest.getRequestURI());
        if (config.getExecutionOption() == ExecutionOptions.REQUIRES_THREAD) {
            final Thread t = new Thread(servletRunner);
            t.start();

        } else {
            servletRunner.run();
        }
    }

    /**
     * Inform listeners that a context has been created.
     *
     * @param aContext
     *            The new context
     */
    private void fireContextInitialized(ServletContext aContext) {
        final ServletContextEvent evt = new ServletContextEvent(aContext);
        for (final ServletContextListener l : this.contextListeners) {
            l.contextInitialized(evt);
        }
    }

    /**
     * Inform listeners that a context has been destroyed.
     *
     * @param aContext
     *            The new context
     */
    private void fireContextDestroyed(ServletContext aContext) {
        final ServletContextEvent evt = new ServletContextEvent(aContext);
        for (final ServletContextListener l : this.contextListeners) {
            l.contextDestroyed(evt);
        }
    }

    private void sendError(HttpServletResponse aResponse, int aCode, String aMsg) {
        try {
            aResponse.sendError(aCode, aMsg);

        } catch (final IOException e) {
            // ignore
        }
    }

    private void closeSocket(ServerSocket aSocket) {
        try {
            if (aSocket != null) {
                aSocket.close();
            }

        } catch (final IOException e) {
            // ignore
        }
    }

    private void closeSocket(Socket aSocket, TinyWebRequest aReq) {
        aReq.closeStream();

        try {
            aSocket.close();

        } catch (final IOException e) {
            // ignore
        }
    }

}
