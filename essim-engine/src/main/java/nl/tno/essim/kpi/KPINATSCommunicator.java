package nl.tno.essim.kpi;

import io.nats.client.Connection;
import io.nats.client.Nats;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

@Slf4j
public class KPINATSCommunicator {


    private HashMap<String, Connection> connections = new HashMap<>();
    public KPINATSCommunicator() {
    }

    public Connection getNATSConnection(String natsURL) {
        URI natsURI = URI.create(natsURL);
        try {
            Connection connection = connections.get(natsURL);
            if (connection == null ) {
                connection = Nats.connect("nats://" + natsURI.getHost() + ":" + natsURI.getPort());
                connections.put(natsURL, connection);
            }
            return connection;
        } catch (IOException | InterruptedException e) {
            log.warn("Could not create NATS connection", e);
        }
        return null;
    }
}
