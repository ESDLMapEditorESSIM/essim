package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
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
  private String influxURL;

    @Getter
    @Setter
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

  @Getter
  @Setter
  @JsonProperty("powsyblLoadFlow")
  private boolean powsyblLoadFlow;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new ExcludingMap();

  @JsonProperty("esdlContents")
  public String getEsdlContents() {
    return this.esdlContents;
  }

  @JsonProperty("esdlContents")
  public void setEsdlContents(String esdlContents) {
    this.esdlContents = esdlContents;
  }

  @JsonProperty("user")
  public String getUser() {
    return this.user;
  }

  @JsonProperty("user")
  public void setUser(String user) {
    this.user = user;
  }

  @JsonProperty("scenarioID")
  public String getScenarioID() {
    return this.scenarioID;
  }

  @JsonProperty("scenarioID")
  public void setScenarioID(String scenarioID) {
    this.scenarioID = scenarioID;
  }

  @JsonProperty("simulationDescription")
  public String getSimulationDescription() {
    return this.simulationDescription;
  }

  @JsonProperty("simulationDescription")
  public void setSimulationDescription(String simulationDescription) {
    this.simulationDescription = simulationDescription;
  }

  @JsonProperty("startDate")
  public Date getStartDate() {
    return this.startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  @JsonProperty("endDate")
  public Date getEndDate() {
    return this.endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  @JsonProperty("timeStep")
  public String getTimeStep() {
    return this.timeStep;
  }

  @JsonProperty("timeStep")
  public void setTimeStep(String timeStep) {
    this.timeStep = timeStep;
  }

  @JsonProperty("status")
  public SimulationStatus getStatus() {
    return this.status;
  }

  @JsonProperty("status")
  public void setStatus(SimulationStatus status) {
    this.status = status;
  }

  @JsonProperty("influxURL")
  public String getInfluxURL() {
    return this.influxURL;
  }

  @JsonProperty("influxURL")
  public void setInfluxURL(String influxURL) {
    this.influxURL = influxURL;
  }

  @JsonProperty("csvFilesLocation")
  public String getCsvFilesLocation() {
    return this.csvFilesLocation;
  }

  @JsonProperty("csvFilesLocation")
  public void setCsvFilesLocation(String csvFilesLocation) {
    this.csvFilesLocation = csvFilesLocation;
  }

  @JsonProperty("natsURL")
  public String getNatsURL() {
    return this.natsURL;
  }

  @JsonProperty("natsURL")
  public void setNatsURL(String natsURL) {
    this.natsURL = natsURL;
  }

  @JsonProperty("mqttURL")
  public String getMqttURL() {
    return this.mqttURL;
  }

  @JsonProperty("mqttURL")
  public void setMqttURL(String mqttURL) {
    this.mqttURL = mqttURL;
  }

  @JsonProperty("amqpURL")
  public String getAmqpURL() {
    return this.amqpURL;
  }

  @JsonProperty("amqpURL")
  public void setAmqpURL(String amqpURL) {
    this.amqpURL = amqpURL;
  }

  @JsonProperty("mso")
  public MSOConfiguration getMso() {
    return this.mso;
  }

  @JsonProperty("mso")
  public void setMso(MSOConfiguration mso) {
    this.mso = mso;
  }

  @JsonProperty("kafkaURL")
  public String getKafkaURL() {
    return this.kafkaURL;
  }

  @JsonProperty("kafkaURL")
  public void setKafkaURL(String kafkaURL) {
    this.kafkaURL = kafkaURL;
  }

  @JsonProperty("externalSolvers")
  public List<ExternalSolver> getExternalSolvers() {
    return this.externalSolvers;
  }

  @JsonProperty("externalSolvers")
  public void setExternalSolvers(List<ExternalSolver> externalSolvers) {
    this.externalSolvers = externalSolvers;
  }

  @JsonProperty("kpiModule")
  public RemoteKPIModule getKpiModule() {
    return this.kpiModule;
  }

  @JsonProperty("kpiModule")
  public void setKpiModule(RemoteKPIModule kpiModule) {
    this.kpiModule = kpiModule;
  }

  @JsonProperty("simRunDate")
  public Date getSimRunDate() {
    return this.simRunDate;
  }

  @JsonProperty("simRunDate")
  public void setSimRunDate(Date simRunDate) {
    this.simRunDate = simRunDate;
  }

  @JsonProperty("transport")
  public List<TransportNetwork> getTransport() {
    return this.transport;
  }

  @JsonProperty("transport")
  public void setTransport(List<TransportNetwork> transport) {
    this.transport = transport;
  }

  @JsonProperty("dashboardURL")
  public String getDashboardURL() {
    return this.dashboardURL;
  }

  @JsonProperty("dashboardURL")
  public void setDashboardURL(String dashboardURL) {
    this.dashboardURL = dashboardURL;
  }

  @JsonProperty("loadAnimationJSON")
  public String getLoadAnimationJSON() {
    return this.loadAnimationJSON;
  }

  @JsonProperty("loadAnimationJSON")
  public void setLoadAnimationJSON(String loadAnimationJSON) {
    this.loadAnimationJSON = loadAnimationJSON;
  }

  @JsonProperty("nodeConfig")
  public List<NodeConfiguration> getNodeConfig() {
    return this.nodeConfig;
  }

  @JsonProperty("nodeConfig")
  public void setNodeConfig(List<NodeConfiguration> nodeConfig) {
    this.nodeConfig = nodeConfig;
  }

  @JsonProperty("market")
  public Market getMarket() {
    return this.market;
  }

  @JsonProperty("market")
  public void setMarket(Market market) {
    this.market = market;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperties(String key, Object value) {
    this.additionalProperties.put(key, value);
  }

}
