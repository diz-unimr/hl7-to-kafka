package de.unimarburg.diz.hl7tokafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.TimeZone;

import org.apache.camel.*;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.mllp.MllpApplicationErrorAcknowledgementException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpointsAndSkip;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    private static final String hl7TestMsg = "MSH|^~\\&|SWISSLAB|INFD|DBSERV||20190417111500|LAB|ORU^R01|test-msg.000042|P|2.2|||AL|NE\rPID|||789012||Müstermann^Erika||19810211000000|W\rPV1|||NEU||||||||||||||||33333333\rORC|RE|20190417_55555555|||IP||||20190417111000\r";

    @Test
    void testReceive() throws Exception {
        // set timezone for Kafka timestamp conversion
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Berlin")));

        mock.reset();
        mock.expectedMessageCount(1);
        mock.expectedHeaderReceived(KafkaConstants.KEY, "test-msg.000042");
        mock.expectedHeaderReceived(KafkaConstants.OVERRIDE_TIMESTAMP,
                "1555492500000"); // 20190417111500
        mock.expectedBodiesReceived(hl7TestMsg);

        template.sendBody(hl7TestMsg);

        mock.assertIsSatisfied();
    }

    @Test
    void testParseError() {
        // must create an exchange to get the result as an exchange
        // where we can get the caused exception
        var exchange = template.getDefaultEndpoint()
                .createExchange(ExchangePattern.InOut);
        exchange.getIn()
                .setBody("MSH|^~\\&|SendingApp|SendingFac|ReceivingApp|ReceivingFac|INVALID_DATE||ORU^R01|59689|P|2.3|\r");

        var out = template.send(exchange);
        assertTrue("Should be failed", out.isFailed());
        assertEquals(MllpApplicationErrorAcknowledgementException.class, out.getException()
                .getClass());
        assertTrue("Will produce an HL7 error ACK", out.getException()
                .getMessage()
                .startsWith("HL7 Application Error Acknowledgment Received"));
    }

    @Test
    void defaultEncodingIsUsed() throws InterruptedException {
        final String msg = "MSH|^~\\&|SWISSLAB|INFD|DBSERV||20190417111500|LAB|ORU^R01|töst|P|2.2|||AL|NE|DEU\r";

        mock.reset();
        // default is iso-8859-1
        mock.expectedPropertyReceived("CamelCharsetName", "ISO-8859-1");
        mock.expectedBodiesReceived(msg);

        var iso = msg.getBytes(StandardCharsets.ISO_8859_1);
        template.sendBody(iso);

        mock.assertIsSatisfied();
    }

    @Test
    void messageEncodingIsUsed() throws InterruptedException {
        final String msg = "MSH|^~\\&|SWISSLAB|INFD|DBSERV||20190417111500|LAB|ORU^R01|töst|P|2.2|||AL|NE|DEU|UNICODE UTF-8\r";

        // default is iso-8859-1
        mock.expectedPropertyReceived("CamelCharsetName", "ISO-8859-1");
        mock.expectedBodiesReceived(msg);

        var utf8 = msg.getBytes(StandardCharsets.UTF_8);
        template.sendBody(utf8);

        mock.assertIsSatisfied();
    }

    @Nested
    @TestPropertySource(properties = {"endpoint.hl7.port=2576",
            "endpoint.kafka.key-expression=ORC-2"})
    @MockEndpointsAndSkip("kafka:hl7-topic")
    class KeySelectingHl7RouterTests {

        @Produce("mllp:0.0.0.0:2576")
        private ProducerTemplate template;

        @EndpointInject("mock:kafka:hl7-topic")
        private MockEndpoint mock;

        @Test
        public void kafkaKeyIsSetViaSelector() throws InterruptedException {

            mock.expectedMessageCount(1);
            mock.expectedHeaderReceived(KafkaConstants.KEY,
                    "20190417_55555555");

            template.sendBody(hl7TestMsg);
            mock.assertIsSatisfied();
        }
    }

    @Nested
    @TestPropertySource(properties = {"endpoint.hl7.port=2577",
            "endpoint.hl7.encoding=utf-8"})
    @MockEndpointsAndSkip("kafka:hl7-topic")
    class EncodingOverrideHl7RouterTests {

        @Produce("mllp:0.0.0.0:2577")
        private ProducerTemplate template;

        @EndpointInject("mock:kafka:hl7-topic")
        private MockEndpoint mock;

        @ParameterizedTest
        @CsvSource({"ISO-8859-1, false", "UTF-8, true"})
        void messageIsUtf8Encoded(String charsetName, boolean shouldPass) throws InterruptedException, UnsupportedEncodingException {
            mock.reset();
            mock.expectedPropertyReceived("CamelCharsetName", "utf-8");

            var msg = "MSH|^~\\&|SWISSLAB|INFD|DBSERV||20190417111500|LAB|ORU^R01|töst|P|2.2|||AL|NE|DEU\r";
            mock.expectedBodiesReceived(msg);

            var encodedMessage = msg.getBytes(charsetName);
            template.sendBody(encodedMessage);

            if (shouldPass) {
                mock.assertIsSatisfied();
            } else {
                mock.assertIsNotSatisfied();
            }
        }
    }
}
