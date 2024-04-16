package nl.tno.essim.kpi.kpi.imbalance;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.kpi.KPIModule;
import nl.tno.essim.kpi.KPIProcessor;
import nl.tno.essim.kpi.data.KPIData;
import nl.tno.essim.kpi.data.ModuleInfo;
import nl.tno.essim.kpi.kpi.KPIProcessorFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Slf4j
public class ImbalanceCalculator implements KPIProcessor {

    private final String simulationID;
    private final ObjectMapper objectMapper;
    private Map<String, String> configuration;
    private int totalMessageCount;
    private int messageCounter;

    public static void main(String[] args) throws IOException {
        ModuleInfo moduleInfo = ModuleInfo.builder()
                .title("Total Energy Imbalance")
                .typeId("TotalEnergyImbalanceID")
                .description("")
                .upSince(new Date())
                .sampleOutput(new String(ImbalanceCalculator.class.getResourceAsStream("/imbalance_sample.json").readAllBytes(), StandardCharsets.UTF_8))
                .build();
        KPIProcessorFactory factory = new KPIProcessorFactory() {
            @Override
            public KPIProcessor createKPIProcessor(String simulationID) {
                return new ImbalanceCalculator(simulationID);
            }

            @Override
            public ModuleInfo getModuleInfo() {
                return moduleInfo;
            }
        };
        KPIModule kpiModule = new KPIModule(factory);
        kpiModule.start();
    }

    public ImbalanceCalculator(String simulationID) {
        this.simulationID = simulationID;
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    @Override
    public void configure(Map<String, String> configuration, int messageCount) {
        this.configuration = configuration;
        this.totalMessageCount = messageCount;
    }

    @Override
    public void messageReceived(KPIData measurement) {
        messageCounter++;
        if (messageCounter == totalMessageCount) {
            log.debug("{} messages received for simulation {}", messageCounter, simulationID);
        }
    }

    @Override
    public String getJSONResult() {
        return "{}";
    }

    @Override
    public void reset() {
        messageCounter = 0;
    }
}
