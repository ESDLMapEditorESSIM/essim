package nl.tno.essim.mso;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import esdl.EnergyAsset;
import esdl.EnergySystem;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.externalmodel.protos.DeployModels;
import nl.tno.essim.externalmodel.protos.DeployModels.Builder;
import nl.tno.essim.externalmodel.protos.EnvironmentVariable;
import nl.tno.essim.externalmodel.protos.HealthStatus;
import nl.tno.essim.externalmodel.protos.ModelConfiguration;
import nl.tno.essim.externalmodel.protos.PingHealthESSIMToMSO;
import nl.tno.essim.externalmodel.protos.SimulationDone;
import nl.tno.essim.managers.SimulationManager;
import nl.tno.essim.model.MSOConfiguration;
import nl.tno.essim.model.NodeConfiguration;

@Slf4j
public class MSOClient {

	private static final String DEFAULT_MSO_HEALTH_TIMEOUT = "30";
	private static final String DEFAULT_MODEL_DEPLOY_TIMEOUT = "600";
	private static final long MODEL_TERMINATE_TIMEOUT_SEC = 600l;
	private static final long MSO_HEALTHCHECK_COUNTER_INTERVAL = 1000l;
	private static final int AT_LEAST_ONCE = 1;
	private static final String PUBLISH_TOPIC_PREFIX = "/lifecycle/essim/mso/%s/";
	private static final String SUBSCRIBE_TOPIC_PREFIX = "/lifecycle/mso/essim/%s/";
	private static final String SUBSCRIBE_TOPIC = SUBSCRIBE_TOPIC_PREFIX + "#";
	private static final String ESSIM_MSO_MQTT_CLIENT_ID = "essim-mso-client";
	private static final String DEPLOY_MODELS_TOPIC_TEMPLATE = PUBLISH_TOPIC_PREFIX + "DeployModels";
	private static final String SIMULATION_DONE_TOPIC_TEMPLATE = PUBLISH_TOPIC_PREFIX + "SimulationDone";
	private static final String PING_TOPIC_TEMPLATE = PUBLISH_TOPIC_PREFIX + "PingHealthESSIMToMSO";
	private static final String MODEL_DOCKER_IMAGE = "ci.tno.nl/heatlosscalculation/essim-building-model:latest";
	private String simulationId;
	private List<NodeConfiguration> nodeConfigurations;
	private EnergySystem energySystem;
	private MqttAsyncClient client;
	private EnvironmentVariable mqttHostEnvVar;
	private EnvironmentVariable mqttPortEnvVar;
	private EnvironmentVariable simulationIdEnvVar;
	private String essimId;
	private String deployModelsTopic;
	private String simulationDoneTopic;
	private String pingHealthTopic;
	private String subscribeTopic;
	private int msoTimeout;
	private AtomicInteger msoTimeoutCounter;
	private Timer timer;
	private String error;
	private CountDownLatch msoBarrier;
	private CountDownLatch modelBarrier;

	private IMqttMessageListener messageProcessor = new IMqttMessageListener() {

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			byte[] payload = message.getPayload();
			log.debug("Received message: {} on topic {}".formatted(new String(payload)), topic);
			if (topic.contains("ModelsReady")) {
				msoBarrier.countDown();
			} else if (topic.contains("UnhealthyModel")) {
				log.debug("Unhealthy model : {}", topic);
			} else if (topic.contains("ModelHasTerminated")) {
				log.debug("Model has terminated : {}", topic);
			} else if (topic.contains("AllModelsHaveTerminated")) {
				log.debug("All models have terminated! MSOClient shutting down.");
				shutdown();
				modelBarrier.countDown();
			} else if (topic.contains("PongHealthMSOToEssim")) {
				msoTimeoutCounter.set(msoTimeout);
				pingMSO(error);
				if (error != null) {
					shutdown();
				}
			}
		}
	};

	public MSOClient(String essimId, String simulationId, MSOConfiguration msoConfig,
			List<NodeConfiguration> nodeConfig, EnergySystem energySystem, SimulationManager simulationManager) {
		this.essimId = essimId;
		this.simulationId = simulationId;
		this.nodeConfigurations = nodeConfig;
		this.energySystem = energySystem;
		deployModelsTopic = String.format(DEPLOY_MODELS_TOPIC_TEMPLATE, simulationId);
		subscribeTopic = String.format(SUBSCRIBE_TOPIC, simulationId);
		simulationDoneTopic = String.format(SIMULATION_DONE_TOPIC_TEMPLATE, simulationId);

		pingHealthTopic = String.format(PING_TOPIC_TEMPLATE, essimId);

		String modelDeployTimeoutStr = System.getenv("MODEL_DEPLOY_TIMEOUT_SEC");
		if (modelDeployTimeoutStr == null) {
			modelDeployTimeoutStr = DEFAULT_MODEL_DEPLOY_TIMEOUT;
		}
		String msoTimeoutStr = System.getenv("MSO_HEALTH_TIMEOUT_SEC");
		if (msoTimeoutStr == null) {
			msoTimeoutStr = DEFAULT_MSO_HEALTH_TIMEOUT;
		}
		msoTimeout = Integer.parseInt(msoTimeoutStr);
		msoTimeoutCounter = new AtomicInteger(msoTimeout);
		timer = new Timer(true);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (msoTimeoutCounter.getAndDecrement() == 1) {
					try {
						shutdown();
					} catch (MqttException e) {
						e.printStackTrace();
					}
					simulationManager.interrupt("Unresponsive MSO");
				}
			}
		}, 0l, MSO_HEALTHCHECK_COUNTER_INTERVAL);

		String url = String.format("%s:%s", msoConfig.getMqttHost(), msoConfig.getMqttPort());
		mqttHostEnvVar = EnvironmentVariable.newBuilder().setName("MQTT_HOST").setValue(msoConfig.getMqttHost())
				.build();
		mqttPortEnvVar = EnvironmentVariable.newBuilder().setName("MQTT_PORT")
				.setValue(String.valueOf(msoConfig.getMqttPort())).build();
		simulationIdEnvVar = EnvironmentVariable.newBuilder().setName("SIMULATION_ID")
				.setValue(String.valueOf(simulationId)).build();

		try {
			client = new MqttAsyncClient(url, ESSIM_MSO_MQTT_CLIENT_ID);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setMaxInflight(50000);
			connOpts.setCleanSession(true);
			client.connect(connOpts);
			client.subscribe(subscribeTopic, AT_LEAST_ONCE, messageProcessor);
		} catch (MqttException e) {
			e.printStackTrace();
		}
		msoBarrier = new CountDownLatch(1);
		modelBarrier = new CountDownLatch(1);
		simulationManager.setMSOClient(this, msoBarrier, Long.parseLong(modelDeployTimeoutStr));
	}

	public void setError(String error) {
		this.error = error;
	}

	public void pingMSO(String error) throws MqttPersistenceException, MqttException {
		HealthStatus health = HealthStatus.HEALTHY;
		String reasons = "";
		if (error != null) {
			health = HealthStatus.UNHEALTHY;
			reasons = error;
		}

		PingHealthESSIMToMSO ping = PingHealthESSIMToMSO.newBuilder().addActiveSimulations(simulationId)
				.setHealthy(health).addReasons(reasons).build();
		MqttMessage pingMessage = new MqttMessage(ping.toByteArray());
		client.publish(pingHealthTopic, pingMessage);
	}

	public void deployModels() throws MqttPersistenceException, MqttException {
		String[] regexStrings = new String[nodeConfigurations.size()];
		String regex = String.join("|", nodeConfigurations.stream().map(nc -> nc.getEsdlNodeId())
				.collect(Collectors.toList()).toArray(regexStrings));
		TreeIterator<EObject> iterator = energySystem.eAllContents();
		Builder deployModelsBuilder = DeployModels.newBuilder().setEssimID(essimId);
		while (iterator.hasNext()) {
			EObject eObject = iterator.next();
			if (!(eObject instanceof EnergyAsset)) {
				continue;
			} else {
				EnergyAsset energyAsset = (EnergyAsset) eObject;
				if (energyAsset.getId().matches(regex)) {
					EnvironmentVariable modelIdEnvVar = EnvironmentVariable.newBuilder().setName("MODEL_ID")
							.setValue(String.valueOf(energyAsset.getId())).build();
					ModelConfiguration modelConfiguration = ModelConfiguration.newBuilder()
							.setModelID(energyAsset.getId()).setContainerURL(MODEL_DOCKER_IMAGE)
							.addEnvironmentVariables(mqttHostEnvVar).addEnvironmentVariables(mqttPortEnvVar)
							.addEnvironmentVariables(simulationIdEnvVar).addEnvironmentVariables(modelIdEnvVar).build();
					deployModelsBuilder.addModelConfigurations(modelConfiguration);
				} else {
					System.out.println("Ignored asset : " + energyAsset.getId());
				}
			}
		}
		byte[] deployModelsPayload = deployModelsBuilder.build().toByteArray();
		MqttMessage deployModelsMessage = new MqttMessage(deployModelsPayload);
		client.publish(deployModelsTopic, deployModelsMessage);
	}

	public void simulationDone() throws MqttPersistenceException, MqttException, InterruptedException {
		byte[] simulationDonePayload = SimulationDone.newBuilder().build().toByteArray();
		MqttMessage simulationDoneMessage = new MqttMessage(simulationDonePayload);
		client.publish(simulationDoneTopic, simulationDoneMessage);
		
		boolean await = modelBarrier.await(MODEL_TERMINATE_TIMEOUT_SEC, TimeUnit.SECONDS);
		if (!await) {
			log.error("Timed out waiting for external models to terminate. Stopping simulation.");
		}
	}

	private void shutdown() throws MqttException {
		timer.cancel();
		client.disconnect();
		client.close();
	}
}
