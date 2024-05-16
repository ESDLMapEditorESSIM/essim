package nl.tno.essim.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.Date;
import java.util.List;
import java.util.Map;

@JsonDeserialize(
    as = EssimSimulationImpl.class
)
public interface EssimSimulation {
  @JsonProperty("esdlContents")
  String getEsdlContents();

  @JsonProperty("esdlContents")
  void setEsdlContents(String esdlContents);

  @JsonProperty("user")
  String getUser();

  @JsonProperty("user")
  void setUser(String user);

  @JsonProperty("scenarioID")
  String getScenarioID();

  @JsonProperty("scenarioID")
  void setScenarioID(String scenarioID);

  @JsonProperty("simulationDescription")
  String getSimulationDescription();

  @JsonProperty("simulationDescription")
  void setSimulationDescription(String simulationDescription);

  @JsonProperty("startDate")
  Date getStartDate();

  @JsonProperty("startDate")
  void setStartDate(Date startDate);

  @JsonProperty("endDate")
  Date getEndDate();

  @JsonProperty("endDate")
  void setEndDate(Date endDate);

  @JsonProperty("timeStep")
  String getTimeStep();

  @JsonProperty("timeStep")
  void setTimeStep(String timeStep);

  @JsonProperty("status")
  SimulationStatus getStatus();

  @JsonProperty("status")
  void setStatus(SimulationStatus status);

  @JsonProperty("influxURL")
  String getInfluxURL();

  @JsonProperty("influxURL")
  void setInfluxURL(String influxURL);

  @JsonProperty("csvFilesLocation")
  String getCsvFilesLocation();

  @JsonProperty("csvFilesLocation")
  void setCsvFilesLocation(String csvFilesLocation);

  @JsonProperty("natsURL")
  String getNatsURL();

  @JsonProperty("natsURL")
  void setNatsURL(String natsURL);

  @JsonProperty("mqttURL")
  String getMqttURL();

  @JsonProperty("mqttURL")
  void setMqttURL(String mqttURL);

  @JsonProperty("amqpURL")
  String getAmqpURL();

  @JsonProperty("amqpURL")
  void setAmqpURL(String amqpURL);

  @JsonProperty("mso")
  MSOConfiguration getMso();

  @JsonProperty("mso")
  void setMso(MSOConfiguration mso);

  @JsonProperty("kafkaURL")
  String getKafkaURL();

  @JsonProperty("kafkaURL")
  void setKafkaURL(String kafkaURL);

  @JsonProperty("externalSolvers")
  List<ExternalSolver> getExternalSolvers();

  @JsonProperty("externalSolvers")
  void setExternalSolvers(List<ExternalSolver> externalSolvers);

  @JsonProperty("kpiModule")
  RemoteKPIModule getKpiModule();

  @JsonProperty("kpiModule")
  void setKpiModule(RemoteKPIModule kpiModule);

  @JsonProperty("simRunDate")
  Date getSimRunDate();

  @JsonProperty("simRunDate")
  void setSimRunDate(Date simRunDate);

  @JsonProperty("transport")
  List<TransportNetwork> getTransport();

  @JsonProperty("transport")
  void setTransport(List<TransportNetwork> transport);

  @JsonProperty("dashboardURL")
  String getDashboardURL();

  @JsonProperty("dashboardURL")
  void setDashboardURL(String dashboardURL);

  @JsonProperty("loadAnimationJSON")
  String getLoadAnimationJSON();

  @JsonProperty("loadAnimationJSON")
  void setLoadAnimationJSON(String loadAnimationJSON);

  @JsonProperty("nodeConfig")
  List<NodeConfiguration> getNodeConfig();

  @JsonProperty("nodeConfig")
  void setNodeConfig(List<NodeConfiguration> nodeConfig);

  @JsonProperty("market")
  Market getMarket();

  @JsonProperty("market")
  void setMarket(Market market);

  @JsonAnyGetter
  Map<String, Object> getAdditionalProperties();

  @JsonAnySetter
  void setAdditionalProperties(String key, Object value);
}
