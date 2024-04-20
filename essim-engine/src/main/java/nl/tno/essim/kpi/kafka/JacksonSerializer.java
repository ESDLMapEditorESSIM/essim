package nl.tno.essim.kpi.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class JacksonSerializer<T> implements Serializer<T> {
    public static final String JACKSON_SERIALIZATION_CONFIG = "jackson.serialization.config";
    private ObjectMapper objectMapper;

    public JacksonSerializer() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void configure(Map<String, ?> settings, boolean isKey) {
        SerializationConfig config = (SerializationConfig) settings.get(JACKSON_SERIALIZATION_CONFIG);
        if (config != null) {
            objectMapper.setConfig(config);
        }
    }

    @Override
    public byte[] serialize(String topic, T message) {
        if (null == message) {
            return null;
        }
        try {
            return this.objectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void close() {

    }
}
