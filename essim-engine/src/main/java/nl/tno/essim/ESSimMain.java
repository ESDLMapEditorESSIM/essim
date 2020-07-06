package nl.tno.essim;

import org.mongojack.JacksonMongoCollection;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.model.EssimSimulation;
import nl.tno.essim.mongo.MongoBackend;
import nl.tno.essim.rest.EssimHttpServer;

@Slf4j
public class ESSimMain {

	private static final String HTTP_SERVER_SCHEME = "HTTP_SERVER_SCHEME";
	private static final String HTTP_SERVER_HOSTNAME = "HTTP_SERVER_HOSTNAME";
	private static final String HTTP_SERVER_PORT = "HTTP_SERVER_PORT";
	private static final String HTTP_SERVER_PATH = "HTTP_SERVER_PATH";
	private static final String MONGODB_HOST = "MONGODB_HOST";
	private static final String MONGODB_PORT = "MONGODB_PORT";

	@Getter
	private MongoClient mongoClient;
	@Getter
	private MongoDatabase database;
	@Getter
	private JacksonMongoCollection<EssimSimulation> simCollection;
	@Getter
	private EssimHttpServer ramlServer;
	private MongoBackend mongoBackend;
	
	public static void main(String[] args) {
		new ESSimMain();
	}

	public ESSimMain() {
		
		// Setup MongoDB server
		log.debug("Connecting to Mongo Backend");
		mongoBackend = new MongoBackend(System.getenv(MONGODB_HOST), System.getenv(MONGODB_PORT));

		// Setup RAML server
		log.debug("Starting ESSIM REST Server");
		ramlServer = new EssimHttpServer(System.getenv(HTTP_SERVER_SCHEME), System.getenv(HTTP_SERVER_HOSTNAME),
				Integer.parseInt(System.getenv(HTTP_SERVER_PORT)), System.getenv(HTTP_SERVER_PATH));
	}

	public void cleanup() {
		ramlServer.stop();
		mongoBackend.stop();
	}
}
