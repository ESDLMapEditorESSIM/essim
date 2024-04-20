package nl.tno.essim.kpi.kafka;


import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class JacksonDeserializer<T> implements Deserializer<T> {
    public static final String JACKSON_DESERIALIZATION_CONFIG = "jackson.deserialization.config";
    private final ObjectMapper objectMapper;
    private Class<T> cls;


    public JacksonDeserializer() {
        this.objectMapper = new ObjectMapper();
    }

    public JacksonDeserializer(ObjectMapper mapper, Class<T> cls) {
        this.objectMapper = mapper;
        this.cls = cls;
    }

    @Override
    public void configure(Map<String, ?> settings, boolean isKey) {
        DeserializationConfig config = (DeserializationConfig) settings.get(JACKSON_DESERIALIZATION_CONFIG);
        if (config != null) {
            objectMapper.setConfig(config);
        }
    }

    @Override
    public T deserialize(String topic, byte[] bytes) {
        if (null == bytes) {
            return null;
        }

        try {
            return this.objectMapper.readValue(bytes, this.cls);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void close() {

    }
}
