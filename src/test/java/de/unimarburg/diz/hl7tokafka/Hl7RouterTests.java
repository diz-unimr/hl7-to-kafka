package de.unimarburg.diz.hl7tokafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import java.time.ZoneId;
import java.util.TimeZone;
import org.apache.camel.EndpointInject;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.mllp.MllpApplicationErrorAcknowledgementException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpointsAndSkip;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(properties = {"endpoint.hl7.port=2575",
    "endpoint.kafka.topic=hl7-topic"})
@MockEndpointsAndSkip("kafka:hl7-topic")
public class Hl7RouterTests {

    @Produce("mllp:0.0.0.0:2575")
    private ProducerTemplate template;

    @EndpointInject("mock:kafka:hl7-topic")
    private MockEndpoint mock;

    @SuppressWarnings("checkstyle:LineLength")
    private static String hl7TestMessage() {
        return """
            MSH|^~\\&|SWISSLAB|INFD|DBSERV||20190417111500|LAB|ORU^R01|test-msg.000042|P|2.2|||AL|NE\r
            PID|||789012||Mustermann^Erika||19810211000000|W\r
            PV1|||NEU||||||||||||||||33333333\r
            ORC|RE|20190417_55555555|||IP||||20190417111000\r
            """;
    }

    @SuppressWarnings("checkstyle:LineLength")
    private static String hl7FaultyMessage() {
        return "MSH|^~\\&|SendingApp|SendingFac|ReceivingApp|ReceivingFac"
            + "|INVALID_DATE||ORU^R01|59689|P|2.3|\r\n";
    }

    @Test
    public void testReceive() throws Exception {
        // set timezone for Kafka timestamp conversion
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Berlin")));

        mock.expectedMessageCount(1);
        mock.expectedHeaderReceived(KafkaConstants.KEY, "test-msg.000042");
        mock.expectedHeaderReceived(KafkaConstants.OVERRIDE_TIMESTAMP,
            "1555492500000"); // 20190417111500

        template.sendBody(hl7TestMessage());
        mock.assertIsSatisfied();
    }

    @Test
    public void testParseError() throws Exception {
        // must create an exchange to get the result as an exchange
        // where we can get the caused exception
        var exchange = template.getDefaultEndpoint()
            .createExchange(ExchangePattern.InOut);
        exchange.getIn()
            .setBody(hl7FaultyMessage());

        var out = template.send(exchange);
        assertTrue("Should be failed", out.isFailed());
        assertEquals(out.getException()
            .getClass(), MllpApplicationErrorAcknowledgementException.class);
        assertTrue("Will produce an HL7 error ACK", out.getException()
            .getMessage()
            .startsWith("HL7 Application Error Acknowledgment Received"));
    }

    @Nested
    @TestPropertySource(properties = {"endpoint.hl7.port=2577",
        "endpoint.kafka.key-expression=ORC-2"})
    @MockEndpointsAndSkip("kafka:hl7-topic")
    class KeySelectingHl7RouterTests {

        @Produce("mllp:0.0.0.0:2577")
        private ProducerTemplate template;

        @EndpointInject("mock:kafka:hl7-topic")
        private MockEndpoint mock;

        @Test
        public void kafkaKeyIsSetViaSelector() throws InterruptedException {

            mock.expectedMessageCount(1);
            mock.expectedHeaderReceived(KafkaConstants.KEY,
                "20190417_55555555");

            template.sendBody(hl7TestMessage());
            mock.assertIsSatisfied();
        }
    }
}
