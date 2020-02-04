package sw.tinyweb;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import sw.tinyweb.utils.IteratorEnumeration;

/**
 * HTTP servlet configuration.
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
 */
public class TinyWebServletConfig implements ServletConfig {

    private ExecutionOptions executionOption = ExecutionOptions.NO_THREAD;

    private final Map<String, String> initParams = new HashMap<String, String>();

    private ServletContext servletContext;

    private String servletClass;

    private String servletName;

    public TinyWebServletConfig(Class<? extends Servlet> aClazz) {
        this(aClazz, ExecutionOptions.NO_THREAD);
    }

    public TinyWebServletConfig(Class<? extends Servlet> aClazz, ExecutionOptions aOptions) {
        this(aClazz, aOptions, null);
    }

    public TinyWebServletConfig(Class<? extends Servlet> aClazz, ExecutionOptions aOptions, String aName) {
        this.setServletClass(aClazz.getName());
        this.setExecutionOption(aOptions);

        if (aName != null) {
            this.setServletName(aName);
        } else {
            final int ipos = aClazz.getName().lastIndexOf(".");
            this.setServletName(aClazz.getName().substring(ipos + 1));
        }
    }

    @Override
    public String getInitParameter(String aName) {
        return this.initParams.get(aName);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return new IteratorEnumeration<String>(this.initParams.keySet());
    }

    @Override
    public ServletContext getServletContext() {
        return this.servletContext;
    }

    @Override
    public String getServletName() {
        return this.servletName;
    }

    /** @return the options */
    public ExecutionOptions getExecutionOption() {
        return this.executionOption;
    }

    public void setExecutionOption(ExecutionOptions aOption) {
        this.executionOption = aOption;
    }

    /** @return the class name */
    public String getServletClass() {
        return this.servletClass;
    }

    public void setServletClass(String aClazz) {
        this.servletClass = aClazz;
    }

    public void setServletContext(ServletContext aContext) {
        this.servletContext = aContext;
    }

    public void setServletName(String aName) {
        this.servletName = aName;
    }

    /** @return the parameters */
    public Map<String, String> getInitParams() {
        return this.initParams;
    }

    /**
     * Add a new parameter.
     *
     * @param aName
     *            The parameter name
     * @param aValue
     *            The parameter value
     */
    public void addInitParam(String aName, String aValue) {
        this.initParams.put(aName, aValue);
    }

}
