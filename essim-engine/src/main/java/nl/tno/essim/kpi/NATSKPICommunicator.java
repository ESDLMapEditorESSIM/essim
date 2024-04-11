package nl.tno.essim.kpi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.kpi.data.KPIControlMessage;
import nl.tno.essim.kpi.data.KPIData;
import nl.tno.essim.kpi.data.KPIInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public class NATSKPICommunicator extends KPICommunicator {
    private final static String KPI_REQUEST_TOPIC = "KPIControl.request";
    private final static String OBSERVATION_PREFIX = "observation.";
    private final ObjectMapper objectMapper;
    private Connection connection;

    public NATSKPICommunicator(KPIManager kpiManager, String url) {
        super(kpiManager);
        createConnection(url);
        objectMapper = new ObjectMapper();

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
    public void sendKPIStart(KPIInfo info) {
        info.getKpiCalculators().stream().forEach(kc -> {
            KPIControlMessage start = KPIControlMessage.builder().start(KPIControlMessage.Start.builder().simulationId(info.getSimulationId()).messageCount((int) info.getNoOfMessages()).config(kc.getConfig()).build()).build();
            sendControlMessage(start);
        });
    }

    @Override
    public void sendKPIGetStatus() {
        KPIControlMessage message = KPIControlMessage.builder().status(KPIControlMessage.Status.builder().build()).build();
        sendControlMessage(message);
    }

    @Override
    public void sendKPIStop(String simulationID) {
        KPIControlMessage message = KPIControlMessage.builder().stop(KPIControlMessage.Stop.builder().simulationId(simulationID).build()).build();
        sendControlMessage(message);
    }

    @Override
    public void sendKPIData(KPIData data) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            objectMapper.writeValue(stream, data);
            connection.publish(OBSERVATION_PREFIX + data.getSimulationId(), stream.toByteArray());

        } catch (IOException e) {
            log.warn("Could not send message", e);
        }

    }


    private void sendControlMessage(KPIControlMessage message) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            objectMapper.writeValue(stream, message);
            connection.publish(KPI_REQUEST_TOPIC, stream.toByteArray());

        } catch (IOException e) {
            log.warn("Could not send message", e);
        }
    }
}
