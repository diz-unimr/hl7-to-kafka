package de.unimarburg.diz.hl7tokafka;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
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


    public Hl7Router(@NotNull @Value("${endpoint.hl7.port}") String hl7Port,
        @NotNull @Value("${endpoint.hl7.encoding}") String encoding,
        @NotNull @Value("${endpoint.kafka.topic}") String kafkaTopic) {
        this.hl7Port = hl7Port;
        this.encoding = encoding;
        this.kafkaTopic = kafkaTopic;
    }

    @SuppressWarnings("checkstyle:LineLength")
    @Override
    public void configure() {

        from(mllp("0.0.0.0:" + hl7Port).charsetName(encoding))
            .routeId("hl7Listener").unmarshal().hl7().log(LoggingLevel.DEBUG,
                "Message received: ${header" + ".CamelHL7MessageControl}").process(
                ex -> ex.getIn().setHeader(KafkaConstants.OVERRIDE_TIMESTAMP,
                    convertTimestamp(
                        ex.getIn().getHeader("CamelHL7Timestamp", String.class))))

            .log(LoggingLevel.DEBUG,
                "Timestamp converted with tz: " + ZoneId.systemDefault())
            .setHeader(KafkaConstants.KEY, header("CamelHL7MessageControl"))
            .to(kafka(kafkaTopic)).log(LoggingLevel.DEBUG,
                "Message send to Kafka topic: " + kafkaTopic).end();
    }

    private long convertTimestamp(String dateString) {
        return LocalDateTime.parse(dateString, DATE_FORMATTER)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
