package nl.tno.essim.kpi;

import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.kpi.data.KPIControlMessage;
import nl.tno.essim.kpi.data.KPIData;
import nl.tno.essim.kpi.data.KPIModuleMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Slf4j
public class RabbitMQKPICommunicator extends KPICommunicator {

    private String responseQueue;
    private Channel channel;
    private Connection connection;
    private final static String DATA_EXCHANGE_NAME = OBSERVATION_PREFIX + "all";

    public static String RABBITMQ_USER;
    public static String RABBITMQ_PASSWORD;

    static {
        RABBITMQ_USER = System.getenv("RABBITMQ_USER");
        RABBITMQ_PASSWORD = System.getenv("RABBITMQ_PASSWORD");
    }

    public RabbitMQKPICommunicator(KPIManager kpiManager, String url) {
        super(kpiManager);
        ConnectionFactory factory = new ConnectionFactory();
        if (RABBITMQ_USER != null && RABBITMQ_PASSWORD != null) {
            factory.setUsername(RABBITMQ_USER);
            factory.setPassword(RABBITMQ_PASSWORD);
        }
        String[] split = url.split(":");
        factory.setHost(split[0]);
        if (split.length > 1) {
            factory.setPort(Integer.parseInt(split[1]));
        }
        log.info("Connecting to RabbitMQ server at {}", url);
        try {
            connection = factory.newConnection();

            channel = connection.createChannel();
            channel.exchangeDeclare(KPI_CONTROL_TOPIC, BuiltinExchangeType.FANOUT);
            channel.exchangeDeclare(KPI_RESPONSE_TOPIC, BuiltinExchangeType.FANOUT);
            channel.exchangeDeclare(DATA_EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
            responseQueue = channel.queueDeclare().getQueue();
            channel.queueBind(responseQueue, KPI_RESPONSE_TOPIC, "");
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                try {
                    KPIModuleMessage message = objectMapper.readValue(delivery.getBody(), KPIModuleMessage.class);
                    responseReceived(message);
                } catch (Exception e) {
                    log.warn("Exception occurred handling message:\n {}", new String(delivery.getBody(), StandardCharsets.UTF_8), e);
                }
            };
            channel.basicConsume(responseQueue, true, deliverCallback, consumerTag -> log.debug("Message canceled: {}", consumerTag));
            log.debug("Connection established");
        } catch (IOException | TimeoutException e) {
            log.error("Could not connect to rabbitMQ server at {}", url, e);
            System.exit(1);
        }
    }

    @Override
    public void sendData(KPIData data) {
        try {
            channel.basicPublish(DATA_EXCHANGE_NAME, "", null, objectMapper.writeValueAsBytes(data));

        } catch (IOException e) {
            log.warn("Could not send data message {}", data, e);
        }
    }

    @Override
    protected void sendControlMessage(KPIControlMessage message) {
        try {
            channel.basicPublish(KPI_CONTROL_TOPIC, "", null, objectMapper.writeValueAsBytes(message));

        } catch (IOException e) {
            log.warn("Could not send control message {}", message, e);
        }
    }

    @Override
    public Type getType() {
        return Type.RabbitMQ;
    }
}
