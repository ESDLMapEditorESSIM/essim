package nl.tno.essim.observation.consumers;

import java.util.Map.Entry;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;

import nl.tno.essim.observation.IObservation;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.observation.IObservationProvider;
import nl.tno.essim.time.EssimTime;

public class KafkaObservationConsumer implements IObservationConsumer {

	private final static int LINGER_MS = 30 * 1000; // Wait 30 seconds or until 500 records are gathered,
	private final static int BATCH_SIZE = 500; // whichever is first, before publishing
	private KafkaProducer<String, String> producer;
	private static final String OBSERVATION_TOPIC = "observation";
	private String topicPrefix;
	private long timestamp;
	private String observationTopic;
	private String url;

	public KafkaObservationConsumer(String url) {
		this.url = url;
	}

	@Override
	public void init(String simulationId) {
		this.topicPrefix = simulationId;
		observationTopic = topicPrefix + "." + OBSERVATION_TOPIC;

		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, url);
		props.put(ProducerConfig.LINGER_MS_CONFIG, LINGER_MS);
		props.put(ProducerConfig.BATCH_SIZE_CONFIG, BATCH_SIZE);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringSerializer");
		props.put(ProducerConfig.ACKS_CONFIG, "0");
		props.put(ProducerConfig.RETRIES_CONFIG, 10);

		producer = new KafkaProducer<String, String>(props);
	}

	@Override
	public void consume(String simulationRunName, IObservationProvider source, IObservation observation) {
		JSONObject point = new JSONObject();

		// Measurement & Time
		point.put("measurement", source.getProviderName());
		timestamp = observation.getObservedAt()
				.atZone(EssimTime.defaultTimeZone)
				.toEpochSecond();
		point.put("time", timestamp);

		// Fields
		JSONObject fieldSet = new JSONObject();
		for (Entry<String, Object> field : observation.getValues()
				.entrySet()) {
			fieldSet.put(field.getKey(), field.getValue());
		}
		point.put("fields", fieldSet);

		// Tags
		JSONObject tagSet = new JSONObject();
		for (Entry<String, String> tag : observation.getTags()
				.entrySet()) {
			tagSet.put(tag.getKey(), tag.getValue());
		}
		tagSet.put("simulationRun", simulationRunName);
		point.put("tags", tagSet);

		if (producer != null) {
			producer.send(new ProducerRecord<String, String>(observationTopic, observationTopic, point.toString()));
		}
	}

	@Override
	public void consumeBatch(String simulationRunName, IObservationProvider source, IObservation observation) {
		// TODO Auto-generated method stub
	}

	@Override
	public void cleanup() {
		JSONObject endOfSimulation = new JSONObject();
		endOfSimulation.put("event", "endOfSimulation");
		endOfSimulation.put("lastTimestamp", timestamp);
		producer.send(
				new ProducerRecord<String, String>(observationTopic, observationTopic, endOfSimulation.toString()));
		producer.flush();
		producer.close();
	}

}
