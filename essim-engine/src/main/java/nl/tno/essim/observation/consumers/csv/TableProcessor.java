package nl.tno.essim.observation.consumers.csv;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

class TableProcessor implements Runnable {

    private Table table;
    private String simulationRunName;
    private LocalDateTime time;
    private Map<String, Object> valueMap;
    private Map<String, String> tagMap;

    public TableProcessor(Table table, String simulationRunName, LocalDateTime time, Map<String, Object> valueMap,
                          Map<String, String> tagMap) {
        this.table = table;
        this.simulationRunName = simulationRunName;
        this.time = time;
        this.valueMap = valueMap;
        this.tagMap = tagMap;
    }

    @Override
    public void run() {
        try {
            synchronized (table) {
                table.addRow(simulationRunName, time, valueMap, tagMap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
