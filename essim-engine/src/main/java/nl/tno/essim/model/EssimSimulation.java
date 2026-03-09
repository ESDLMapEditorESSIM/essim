package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "The ESDL energy system description, either as raw XML or Base64-encoded.")
    private String esdlContents;

    @JsonProperty("user")
    @Schema(description = "Name or identifier of the user starting the simulation.")
    private String user;

    @JsonProperty("scenarioID")
    @Schema(description = "Identifier for the scenario; used as the schema name in Postgres and the measurement name in InfluxDB.")
    private String scenarioID;

    @JsonProperty("simulationDescription")
    @Schema(description = "Human-readable description of this simulation run.")
    private String simulationDescription;

    @JsonProperty("startDate")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )
    @JsonDeserialize(
            using = TimestampDeserializer.class
    )
    @Schema(description = "Start of the simulation period.", example = "2019-01-01T00:00:00.000+01:00")
    private Date startDate;

    @JsonProperty("endDate")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )
    @JsonDeserialize(
            using = TimestampDeserializer.class
    )
    @Schema(description = "End of the simulation period.", example = "2020-01-01T00:00:00.000+01:00")
    private Date endDate;

    @JsonProperty("timeStep")
    @Schema(description = "Simulation time step as a number followed by a unit (s, m, h, d, M, y). Defaults to 1h.", example = "1h")
    private String timeStep;

    @JsonProperty("status")
    @Schema(description = "Current status of the simulation. Set by ESSIM, not required on input.")
    private SimulationStatus status;

    @JsonProperty("influxURL")
    @Setter(AccessLevel.NONE)
    @Schema(description = "If set, ESSIM writes results to this InfluxDB instance. Omit to skip.", example = "influxdb:8086")
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
    @Schema(description = "If set, ESSIM writes results to Postgres. Omit to skip.", example = "postgresql://localhost:5432/essim", nullable = true)
    private String postgresURL;

    @JsonProperty("csvFilesLocation")
    @Schema(description = "If set, ESSIM writes results as CSV files to this directory path. Omit to skip.", example = "/opt/essim/output")
    private String csvFilesLocation;

    @JsonProperty("mqttURL")
    @Schema(description = "MQTT broker URL, used when an MSO configuration is provided.")
    private String mqttURL;

    @JsonProperty("amqpURL")
    @Schema(description = "AMQP broker URL. Not currently used.")
    private String amqpURL;

    @JsonProperty("mso")
    @Schema(description = "Configuration for the Model Service Orchestrator, used to deploy external models during simulation.")
    private MSOConfiguration mso;

    @JsonProperty("kafkaURL")
    @Schema(description = "Kafka broker URL for KPI output. Takes effect only when a kpiModule is also configured.")
    private String kafkaURL;

    @JsonProperty("externalSolvers")
    @Schema(description = "List of external solver configurations to use alongside the built-in transport solver.")
    private List<ExternalSolver> externalSolvers;

    @JsonProperty("kpiModule")
    @Schema(description = "Configuration for a remote KPI module that receives simulation results over Kafka or RabbitMQ.")
    private RemoteKPIModule kpiModule;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )
    @JsonDeserialize(
            using = TimestampDeserializer.class
    )
    @JsonProperty("simRunDate")
    @Schema(description = "Timestamp of when the simulation was started. Set by ESSIM, not required on input.")
    private Date simRunDate;

    @JsonProperty("transport")
    @Schema(description = "Transport network definitions. If omitted, ESSIM derives transport networks from the ESDL.")
    private List<TransportNetwork> transport;

    @JsonProperty("dashboardURL")
    @Schema(description = "URL of the generated Grafana dashboard. Set by ESSIM after the simulation completes.")
    private String dashboardURL;

    @JsonProperty("loadAnimationJSON")
    @Schema(description = "JSON payload for the load animation view. Set by ESSIM after the simulation completes.")
    private String loadAnimationJSON;

    @JsonProperty("nodeConfig")
    @Schema(description = "Per-node configuration overrides, matched to ESDL asset IDs.")
    private List<NodeConfiguration> nodeConfig;

    @JsonProperty("market")
    @Schema(description = "Market price boundaries used during the simulation. Defaults to the ESSIM built-ins if omitted.")
    private Market market;

    @JsonProperty("powsyblLoadFlow")
    @Schema(description = "Set to true to enable load flow calculations via PowSyBl for electricity networks.")
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
