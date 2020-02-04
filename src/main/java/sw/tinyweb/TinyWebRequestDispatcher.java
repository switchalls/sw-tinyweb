package sw.tinyweb;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class TinyWebRequestDispatcher implements RequestDispatcher {
    private final String servletPath;

    private final Servlet targetServlet;

    public TinyWebRequestDispatcher(Servlet aServlet, String aServletPath) {
        this.targetServlet = aServlet;
        this.servletPath = aServletPath;
    }

    @Override
    public void forward(ServletRequest aRequest, ServletResponse aResponse)
            throws ServletException, IOException {

        if (aResponse.isCommitted()) {
            throw new IllegalStateException("HTTP response already committed");
        }

        ((TinyWebRequest) aRequest).changeServletPath(this.servletPath);

        aResponse.reset();

        this.targetServlet.service(aRequest, aResponse);
    }

    @Override
    public void include(ServletRequest aRequest, ServletResponse aResponse)
            throws ServletException, IOException {

        this.targetServlet.service(aRequest, aResponse);
    }

}
