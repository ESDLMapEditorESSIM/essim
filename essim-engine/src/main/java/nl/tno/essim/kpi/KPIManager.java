package nl.tno.essim.kpi;

import lombok.Getter;
import nl.tno.essim.kpi.data.KPIResponseMessage;
import nl.tno.essim.kpi.data.KPIResultMessage;
import nl.tno.essim.kpi.data.ModuleInfo;
import nl.tno.essim.model.KPIModuleInfo;
import nl.tno.essim.model.KPIModuleInfoImpl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class KPIManager {
    private final static int STATUS_INTERVAL = 20; // seconds

    @Getter
    private final static KPIManager instance = new KPIManager();
    private final ScheduledExecutorService executor;
    private final HashMap<String, KPICommunicator> communicatorMap = new HashMap<>();
    private HashMap<String, KPIModuleState> moduleStates;

    public void resultReceived(KPIResultMessage result) {
    }
    private class StateRemover implements Runnable {
        private final String uuid;


        public StateRemover(String uuid) {
            this.uuid = uuid;
        }
        @Override
        public void run() {
            moduleStates.remove(uuid);
        }

    }

    public KPIManager() {
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::queryStatus, STATUS_INTERVAL, STATUS_INTERVAL, TimeUnit.SECONDS);
    }

    private void queryStatus() {
        for (KPICommunicator c : communicatorMap.values()) {
            c.sendKPIGetStatus();
        }
    }

    public void processKPIMessage(KPIResponseMessage message) {
        KPIModuleState moduleState = moduleStates.get(message.getUuid());
        if (moduleState == null) {
            moduleState = new KPIModuleState(message.getModuleInfo());
            moduleStates.put(message.getUuid(), moduleState);
        }
        ScheduledFuture<?> future = moduleState.getFuture();
        if (future != null) {
            future.cancel(false);
        }
        future = executor.schedule(new StateRemover(message.getUuid()), STATUS_INTERVAL * 4, TimeUnit.SECONDS);
        moduleState.setFuture(future);
        for (KPIResponseMessage.SimulationProgress p : message.getSimulationProgresses()) {
            moduleState.setSimulationProgresses(p.getSimulationID(), p);
        }
    }

    public KPICommunicator getKPICommunicator(String url) {
        KPICommunicator communicator = communicatorMap.get(url);
        if (communicator == null) {
            communicator = new NATSKPICommunicator(this, url);
            communicatorMap.put(url, communicator);
            communicator.sendKPIGetStatus();
        }
        return communicator;
    }

    public List<KPIModuleInfo> getAllKPIModules() {
        ArrayList<KPIModuleInfo> infos = new ArrayList<>();
        for (var entry : moduleStates.entrySet()) {
            KPIModuleInfoImpl info = new KPIModuleInfoImpl();
            infos.add(info);
            ModuleInfo moduleInfo = entry.getValue().getModuleInfo();
            info.setStatus("registered");
            info.setDescription(moduleInfo.getDescription());
            info.setSampleOutput(moduleInfo.getSampleOutput());
            info.setCalculatorId(moduleInfo.getTypeId());
            info.setConfigConfig(moduleInfo.getConfig());
            info.setTitle(moduleInfo.getTitle());
            info.setUpSince(moduleInfo.getUpSince());
        }
        return infos;
    }

}
