/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */
package nl.tno.essim.rest;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import nl.tno.essim.rest.application.EssimJerseyApplication;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EssimHttpServer {

	private HttpServer server;
	private URI publishURI;

	public EssimHttpServer(String scheme, String host, int port, String path) {
		// direct java.util.logging to slf4j (Jersey/Grizzly)
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
		Logger logger = Logger.getLogger("org.mongodb.driver");
		logger.setLevel(Level.ALL);

		Logger.getGlobal()
				.setLevel(Level.ALL);

		publishURI = URI.create(String.format("%s://%s:%d/%s", scheme, host, port, path));

		EssimJerseyApplication application = new EssimJerseyApplication();
		this.server = GrizzlyHttpServerFactory.createHttpServer(publishURI, application);
		this.server.getServerConfiguration().addHttpHandler(new CLStaticHttpHandler(EssimHttpServer.class.getClassLoader(), "/web/swagger-ui/"), "/openapi");
		log.info(String.format("ESSIM OpenAPI endpoint available at %s://%s:%d/openapi/", scheme, host, port));
	}

	public URI getBaseURI() {
		return publishURI;
	}

	public HttpServer getServer() {
		return server;
	}

	public void stop() {
		if (server != null) {
			server.shutdown();
		}
	}
}
