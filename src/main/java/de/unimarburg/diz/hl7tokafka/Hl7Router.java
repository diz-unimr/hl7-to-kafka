package de.unimarburg.diz.hl7tokafka;

import static org.apache.camel.component.hl7.HL7.ack;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.wildfly.common.annotation.NotNull;

@Component
public class Hl7Router extends EndpointRouteBuilder {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final String hl7Url;
    private final String encoding;
    private final String kafkaTopic;


    public Hl7Router(@NotNull @Value("${endpoint.hl7.url}") String hl7Url,
        @NotNull @Value("${endpoint.hl7.encoding}") String encoding,
        @NotNull @Value("${endpoint.kafka.topic}") String kafkaTopic) {
        this.hl7Url = hl7Url;
        this.encoding = encoding;
        this.kafkaTopic = kafkaTopic;
    }

    @SuppressWarnings("checkstyle:LineLength")
    @Override
    public void configure() {

        from(mllp(hl7Url).charsetName(encoding)).routeId("hl7Listener")
            .onException(Exception.class).handled(true).transform(
                ack()) // auto-generates negative ack because of exception
            .end().unmarshal().hl7()
            .log("Message received: ${header.CamelHL7MessageControl}").process(
                ex -> ex.getIn().setHeader(KafkaConstants.OVERRIDE_TIMESTAMP,
                    convertTimestamp(
                        ex.getIn().getHeader("CamelHL7Timestamp", String.class))))

            .setHeader(KafkaConstants.KEY, header("CamelHL7MessageControl"))
            .to(kafka(kafkaTopic)).onCompletion()
            .log("Message send to Kafka topic: " + kafkaTopic).transform(ack())
            .end();
    }

    private long convertTimestamp(String dateString) {
        return LocalDateTime.parse(dateString, DATE_FORMATTER)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
