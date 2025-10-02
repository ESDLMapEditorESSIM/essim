/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 * <p>
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * <p>
 *      http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */
package nl.tno.essim.rest;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

@Slf4j
public class EssimHttpServer  {

    public EssimHttpServer(String scheme, String host, int port) throws Exception {
		// direct java.util.logging to slf4j (Jersey/Grizzly)
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
        Logger.getGlobal().setLevel(Level.FINE);

        ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContext.setContextPath("/");


        ServletHolder jerseyServlet = servletContext.addServlet(ServletContainer.class, "/essim/*");
        jerseyServlet.setInitOrder(1);
        jerseyServlet.setInitParameter(
                "jersey.config.server.provider.packages",
                String.join(
                        ",",
                        new String[]{
                                "io.swagger.v3.jaxrs2.integration.resources",
                                "nl.tno.essim.rest",
                                "nl.tno.essim.rest.util"
	}
                )
        );

        jerseyServlet.setInitParameter("jakarta.ws.rs.Application", EssimJerseyApplication.class.getCanonicalName());
        jerseyServlet.setInitParameter("jersey.config.property.WebPageContentRegex", "/.*");
        jerseyServlet.setInitParameter("jersey.config.server.provider.classnames", "org.glassfish.jersey.media.multipart.MultiPartFeature,org.glassfish.jersey.logging.LoggingFeature");
        jerseyServlet.setInitParameter("jersey.config.servlet.filter.forwardOn404", "true");
        jerseyServlet.setInitParameter("jersey.config.server.trace", "ALL");
        jerseyServlet.setInitParameter("openApi.configuration.prettyPrint", "true");


        Server server = new Server(port);
        server.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", "100000000");
        server.setHandler(
                new HandlerList(
                        createContextHandler("/openapi", createStaticFileHandler("/web/swagger-ui")),
                        servletContext
                )
        );
        server.setRequestLog(new JettyRequestLog());

        server.start();
        log.info("ESSIM OpenAPI endpoint available at {}://{}:{}/openapi/", scheme, host, port);
        URI publishURI = URI.create(String.format("%s://%s:%d/essim", scheme, host, port));
        log.info("ESSIM @ {}", publishURI);
        server.join();
	}


    private static ContextHandler createContextHandler(String contextPath, ResourceHandler handler) {
        ContextHandler context = new ContextHandler();
        context.setContextPath(contextPath);
        context.setHandler(handler);
        return context;
	}

    private static ResourceHandler createStaticFileHandler(String resourceBase) {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setBaseResource(Resource.newClassPathResource(resourceBase));
        return resourceHandler;
		}

}
