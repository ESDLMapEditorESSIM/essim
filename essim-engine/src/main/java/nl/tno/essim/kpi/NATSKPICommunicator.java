package nl.tno.essim.kpi;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.kpi.data.KPIControlMessage;
import nl.tno.essim.kpi.data.KPIData;
import nl.tno.essim.kpi.data.KPIModuleMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class NATSKPICommunicator extends KPICommunicator {
    private Connection connection;
    private Dispatcher dispatcher;

    public NATSKPICommunicator(KPIManager kpiManager, String url) {
        super(kpiManager);
        createConnection(url);
    }

    private void createConnection(String url) {
        try {
            log.info("Connecting to NATS server at {}", url);
            Options options = Options.builder().server(url).maxMessagesInOutgoingQueue(10000).useDispatcherWithExecutor().build();
            this.connection = Nats.connect(options);
            dispatcher = connection.createDispatcher((msg) -> {
                try {
                    KPIModuleMessage message = objectMapper.readValue(msg.getData(), KPIModuleMessage.class);
                    responseReceived(message);
                } catch (IOException e) {
                    log.warn("Could not decode message", e);
                }
            });
            dispatcher.subscribe(KPI_RESPONSE_TOPIC);

        } catch (IOException | InterruptedException e) {
            log.warn("Could not create nats connection to {}", url, e);
        }
    }

    @Override
    public void sendData(KPIData data) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            objectMapper.writeValue(stream, data);
            connection.publish(OBSERVATION_PREFIX + "all", stream.toByteArray());

        } catch (IOException e) {
            log.warn("Could not send message", e);
        }

    }

    @Override
    protected void sendControlMessage(KPIControlMessage message) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            objectMapper.writeValue(stream, message);
            connection.publish(KPI_CONTROL_TOPIC, stream.toByteArray());

        } catch (IOException e) {
            log.warn("Could not send message", e);
        }
    }

    @Override
    public Type getType() {
        return Type.NATS;
    }
}
