package nl.tno.essim.kpi;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.kpi.data.KPIControlMessage;
import nl.tno.essim.kpi.data.KPIData;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class NATSKPICommunicator extends KPICommunicator {
    private Connection connection;

    public NATSKPICommunicator(KPIManager kpiManager, String url) {
        super(kpiManager);
        createConnection(url);
    }

    private void createConnection(String url) {
        try {
            Options options = Options.builder().server(url).maxMessagesInOutgoingQueue(10000).useDispatcherWithExecutor().build();
            this.connection = Nats.connect(options);

        } catch (IOException | InterruptedException e) {
            log.warn("Could not create nats connection to {}", url, e);
        }
    }

    @Override
    public void sendData(KPIData data) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            objectMapper.writeValue(stream, data);
            connection.publish(OBSERVATION_PREFIX + data.getSimulationId(), stream.toByteArray());

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
