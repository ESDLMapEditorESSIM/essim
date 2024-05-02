package nl.tno.essim.observation.consumers.csv;

import lombok.Getter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

class Table {

    @Getter
    private String name;
    private TreeSet<String> headers;
    private FileWriter fw;
    private boolean firstRow;
    private String delimiter;
    private String eolChar;
    private DateTimeFormatter timeFormat;

    public Table(String folderName, String name) {
        delimiter = System.getenv("CSV_DELIMITER");
        if (delimiter == null) {
            delimiter = ",";
        }
        eolChar = System.getenv("CSV_EOL_CHAR");
        if (eolChar == null) {
            eolChar = "\n";
        }
        String timeFormatString = System.getenv("CSV_TIME_FORMAT");
        if (timeFormatString == null) {
            timeFormatString = "yyyy-MM-dd HH:mm:ss";
        }
        timeFormat = DateTimeFormatter.ofPattern(timeFormatString);

        File file = new File(folderName);
        if (!file.exists()) {
            file.mkdirs();
        }
        this.name = name;
        headers = new TreeSet<String>();
        headers.add("Time");
        firstRow = true;
        try {
            fw = new FileWriter(Paths.get(folderName, name + ".csv").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addRow(String simulationRunName, LocalDateTime time, Map<String, Object> valueMap,
                       Map<String, String> tagMap) throws IOException {

        if (firstRow) {
            synchronized (fw) {
                headers.addAll(tagMap.keySet());
                headers.addAll(valueMap.keySet());
                fw.write(String.join(delimiter, headers.stream().collect(Collectors.toList())));
                fw.write(eolChar);
                firstRow = false;
            }
        }

        HashMap<String, Object> allMap = new HashMap<String, Object>();
        allMap.putAll(tagMap);
        allMap.putAll(valueMap);
        allMap.put("simulationRun", simulationRunName);
        synchronized (fw) {
            fw.write(time.atZone(ZoneOffset.UTC).format(timeFormat));
            fw.write(String.join(delimiter,
                    headers.stream()
                            .map(header -> allMap.get(header) == null ? ""
                                    : allMap.get(header).toString().contains(delimiter)
                                    ? "\"" + allMap.get(header).toString() + "\""
                                    : allMap.get(header).toString())
                            .collect(Collectors.toList())));
            fw.write(eolChar);
        }
    }

    public void close() throws IOException {
        fw.close();
    }
}
