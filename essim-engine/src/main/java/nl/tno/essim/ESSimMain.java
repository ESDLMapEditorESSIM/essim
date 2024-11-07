/**
 * This work is based on original code developed and copyrighted by TNO 2020.
 * Subsequent contributions are licensed to you by the developers of such code and are
 * made available to the Project under one or several contributor license agreements.
 * <p>
 * This work is licensed to you under the Apache License, Version 2.0.
 * You may obtain a copy of the license at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Contributors:
 * TNO         - Initial implementation
 * Manager:
 * TNO
 */
package nl.tno.essim;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.Version.GitRepositoryState;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.kpi.KPIManager;
import nl.tno.essim.mongo.IMongoBackend;
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
    private EssimHttpServer httpServer;
    private IMongoBackend mongoBackend;

    public static void main(String[] args) {
        new ESSimMain();
    }

    public ESSimMain() {
        GitRepositoryState gitVersion = Version.getGitRepositoryState();
        String version = gitVersion.getCommitIdAbbrev();

        log.debug("ESSIM version: {}", gitVersion.getBuildVersion());
        log.debug("ESSIM git commit id: {} / {}", version, gitVersion.getDescribeShort());
        log.debug("ESSIM Last git commit by {} at {}", gitVersion.getCommitUserName(), gitVersion.getCommitTime());
        log.debug("ESSIM build by {} at {}", gitVersion.getBuildUserName(), gitVersion.getBuildTime());
        System.setProperty(Commons.ESSIM_REVISION, version);

        // Setup MongoDB server
        log.debug("Connecting to Mongo Backend");
        mongoBackend = new MongoBackend(System.getenv(MONGODB_HOST), System.getenv(MONGODB_PORT));

        // Setup RAML server
        log.debug("Starting ESSIM REST Server");
        try {
            KPIManager.getInstance();
            httpServer = new EssimHttpServer(System.getenv(HTTP_SERVER_SCHEME), System.getenv(HTTP_SERVER_HOSTNAME),
                    Integer.parseInt(System.getenv(HTTP_SERVER_PORT)), System.getenv(HTTP_SERVER_PATH));
        } catch (Exception e) {
            log.error("Could not start HTTP server", e);
            System.exit(1);
        }
    }

    public void cleanup() throws Exception {
        httpServer.stop();
        mongoBackend.stop();
    }
}
