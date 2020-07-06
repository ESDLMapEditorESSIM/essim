package nl.tno.essim.rest;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.bridge.SLF4JBridgeHandler;

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

		ResourceConfig rc = ResourceConfig.forApplication(new EssimApplication());
		rc.register(LoggingFeature.class);
		this.server = GrizzlyHttpServerFactory.createHttpServer(publishURI, rc);
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
