package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "esdlContents",
        "user",
        "scenarioID",
        "simulationDescription",
        "startDate",
        "endDate",
        "timeStep",
        "status",
        "influxURL",
        "postgresURL",
        "csvFilesLocation",
        "natsURL",
        "mqttURL",
        "amqpURL",
        "mso",
        "kafkaURL",
        "externalSolvers",
        "kpiModule",
        "simRunDate",
        "transport",
        "dashboardURL",
        "loadAnimationJSON",
        "nodeConfig",
        "market"
})
@Getter
@Setter
public class EssimSimulation {
    @JsonProperty("esdlContents")
    private String esdlContents;

    @JsonProperty("user")
    private String user;

    @JsonProperty("scenarioID")
    private String scenarioID;

    @JsonProperty("simulationDescription")
    private String simulationDescription;

    @JsonProperty("startDate")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )
    @JsonDeserialize(
            using = TimestampDeserializer.class
    )
    private Date startDate;

    @JsonProperty("endDate")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )
    @JsonDeserialize(
            using = TimestampDeserializer.class
    )
    private Date endDate;

    @JsonProperty("timeStep")
    private String timeStep;

    @JsonProperty("status")
    private SimulationStatus status;

    @JsonProperty("influxURL")
    @Setter(AccessLevel.NONE)
    private String influxURL;

    @JsonSetter("influxURL")
    public void setInfluxURL(String influxURL) {
        this.influxURL = addSchemeIfMissing(influxURL);
    }

    // Single-label hostnames (no dots) get http://, FQDNs get https://
    public static String addSchemeIfMissing(String url) {
        if (url == null || url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String host;
        if (url.contains(":")) {
            host = url.substring(0, url.indexOf(':'));
        } else {
            host = url;
        }
        if (host.contains(".")) {
            return "https://" + url;
        } else {
            return "http://" + url;
        }
    }

    @JsonProperty("postgresURL")
    private String postgresURL;

    @JsonProperty("csvFilesLocation")
    private String csvFilesLocation;

    @JsonProperty("natsURL")
    private String natsURL;

    @JsonProperty("mqttURL")
    private String mqttURL;

    @JsonProperty("amqpURL")
    private String amqpURL;

    @JsonProperty("mso")
    private MSOConfiguration mso;

    @JsonProperty("kafkaURL")
    private String kafkaURL;

    @JsonProperty("externalSolvers")
    private List<ExternalSolver> externalSolvers;

    @JsonProperty("kpiModule")
    private RemoteKPIModule kpiModule;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )
    @JsonDeserialize(
            using = TimestampDeserializer.class
    )
    @JsonProperty("simRunDate")
    private Date simRunDate;

    @JsonProperty("transport")
    private List<TransportNetwork> transport;

    @JsonProperty("dashboardURL")
    private String dashboardURL;

    @JsonProperty("loadAnimationJSON")
    private String loadAnimationJSON;

    @JsonProperty("nodeConfig")
    private List<NodeConfiguration> nodeConfig;

    @JsonProperty("market")
    private Market market;

    @JsonProperty("powsyblLoadFlow")
    private boolean powsyblLoadFlow;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new ExcludingMap();


    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(String key, Object value) {
        this.additionalProperties.put(key, value);
    }

}
