package de.unimarburg.diz.hl7tokafka;

import static org.apache.camel.component.hl7.HL7.hl7terser;

import io.micrometer.common.util.StringUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.camel.Expression;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.component.hl7.HL7Constants;
import org.apache.camel.component.kafka.KafkaConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.wildfly.common.annotation.NotNull;

@Component
public class Hl7Router extends EndpointRouteBuilder {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final String hl7Port;
    private final String encoding;
    private final String kafkaTopic;
    private final Expression kafkaKeyExpression;


    public Hl7Router(@NotNull @Value("${endpoint.hl7.port}") String hl7Port,
        @NotNull @Value("${endpoint.hl7.encoding}") String encoding,
        @NotNull @Value("${endpoint.kafka.topic}") String kafkaTopic,
        @Value("${endpoint.kafka.key-expression:#{null}}")
        String kafkaKeyExpression) {
        this.hl7Port = hl7Port;
        this.encoding = encoding;
        this.kafkaTopic = kafkaTopic;
        this.kafkaKeyExpression =
            StringUtils.isBlank(kafkaKeyExpression) ? header(
                HL7Constants.HL7_MESSAGE_CONTROL)
                : hl7terser(kafkaKeyExpression);
    }

    @SuppressWarnings("checkstyle:LineLength")
    @Override
    public void configure() {

        from(mllp("0.0.0.0:" + hl7Port).charsetName(encoding)).routeId(
                "hl7Listener")
            .unmarshal()
            .hl7(false)
            .log(LoggingLevel.DEBUG,
                "Message received: ${header.CamelHL7MessageControl}")
            .process(ex -> ex.getIn()
                .setHeader(KafkaConstants.OVERRIDE_TIMESTAMP, convertTimestamp(
                    ex.getIn()
                        .getHeader(HL7Constants.HL7_TIMESTAMP, String.class))))

            .log(LoggingLevel.DEBUG,
                "Timestamp converted with tz: " + ZoneId.systemDefault())
            .setHeader(KafkaConstants.KEY, kafkaKeyExpression)
            .to(kafka(kafkaTopic))
            .log(LoggingLevel.DEBUG,
                "Message send to Kafka topic: " + kafkaTopic)
            .end();
    }

    private long convertTimestamp(String dateString) {
        return LocalDateTime.parse(dateString, DATE_FORMATTER)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }
}
