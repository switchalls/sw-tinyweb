package sw.tinyweb;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Logger;

import sw.tinyweb.servlets.AboutTinyWebServlet;
import sw.tinyweb.servlets.DownloadFileServlet;

public class Main {

	private static final Logger LOGGER = Logger.getLogger( Main.class );

    private static final TinyWebServletConfig[] SERVLET_CONFIGURATIONS = new TinyWebServletConfig[] {		
    	new TinyWebServletConfig( AboutTinyWebServlet.class ),
    	new TinyWebServletConfig( DownloadFileServlet.class, ExecutionOptions.REQUIRES_THREAD ),
    };

    private static final String[][] SERVLET_MAPPINGS = new String[][] {
        { "/about", "AboutTinyWebServlet" },
        { "*",      "DownloadFileServlet"},
    };
    
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("tinyweb <port> <web-content>");
			System.exit(1);
		}

		final int port = Integer.parseInt(args[0]);
		final File webContent = new File(args[1]);
		
		if (!webContent.isDirectory()) {
			System.out.println("Invalid <web-content> folder: " + args[1]);
			System.exit(1);			
		}

		final TinyWebServer tserver = new TinyWebServer(port, webContent);

		tserver.addServletContextListener( new ServletContextListener() {

			@Override
			public void contextInitialized(ServletContextEvent sce) {
				LOGGER.info("contextInitialized(" + sce + ")");
				
			}

			@Override
			public void contextDestroyed(ServletContextEvent sce) {
				LOGGER.info("contextDestroyed(" + sce + ")");				
			}
		});
    	
        for ( TinyWebServletConfig c : SERVLET_CONFIGURATIONS ) {
        	tserver.addServletConfiguration( c );
        }
    	
    	for ( String[] m : SERVLET_MAPPINGS ) {
    		tserver.addServletMapping( m[0], m[1] );
        }

    	tserver.run();
	}

}
