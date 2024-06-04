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
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

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
            MSH|^~\\&|SendingApp|SendingFac|ReceivingApp|ReceivingFac|20120411070545||ORU^R01|59689|P|2.3|\r
            PID|1||ICE999999^^^ICE^ICE||Testpatient^Testy^^^Mr||19740401|M|||123 Barrel Drive^^^^SW18 4RT|||||2||||||||||||||
            PV1|1|O|||||71^DUCK^DONALD||||||||||||12376|||||||||||||||||||||||||20120410160227||||||
            ORC|RE||12376|||||||100^DUCK^DASIY||71^DUCK^DONALD|||20120411070545|||||
            OBR|1||12376|cbc^CBC|R||20120410160227|||22^GOOF^GOOFY|||Fasting: No|201204101625||71^DUCK^DONALD||||||201204101630|||F||^^^^^R|||||||||||||||||85025|
            OBX|1|NM|wbc^Wbc^Local^6690-2^Wbc^LN||7.0|/nl|3.8-11.0||||F|||20120410160227|lab|12^XYZ LAB|
            OBX|2|NM|neutros^Neutros^Local^770-8^Neutros^LN||68|%|40-82||||F|||20120410160227|lab|12^XYZ LAB|
            OBX|3|NM|lymphs^Lymphs^Local^736-9^Lymphs^LN||20|%|11-47||||F|||20120410160227|lab|12^XYZ LAB|
            OBX|4|NM|monos^Monos^Local^5905-5^Monos^LN||16|%|4-15|H|||F|||20120410160227|lab|12^XYZ LAB|
            OBX|5|NM|eo^Eos^Local^713-8^Eos^LN||3|%|0-8||||F|||20120410160227|lab|12^XYZ LAB|
            OBX|6|NM|baso^Baso^Local^706-2^Baso^LN||0|%|0-1||||F|||20120410160227|lab|12^XYZ LAB|
            OBX|7|NM|ig^Imm Gran^Local^38518-7^Imm Gran^LN||0|%|0-2||||F|||20120410160227|lab|12^XYZ LAB|
            OBX|8|NM|rbc^Rbc^Local^789-8^Rbc^LN||4.02|/pl|4.07-4.92|L|||F|||20120410160227|lab|12^XYZ LAB|
            OBX|9|NM|hgb^Hgb^Local^718-7^Hgb^LN||13.7|g/dl|12.0-14.1||||F|||20120410160227|lab|12^XYZ LAB|
            OBX|10|NM|hct^Hct^Local^4544-3^Hct^LN||40|%|34-43||||F|||20120410160227|lab|12^XYZ LAB|
            OBX|11|NM|mcv^Mcv^Local^787-2^Mcv^LN||80|fl|77-98||||F|||20120410160227|lab|12^XYZ LAB|
            OBX|12|NM|mch^Mch||30|pg|27-35||||F|||20120410160227|lab|12^XYZ LAB|
            OBX|13|NM|mchc^Mchc||32|g/dl|32-35||||F|||20120410160227|lab|12^XYZ LAB|
            OBX|14|NM|plt^Platelets||221|/nl|140-400||||F|||20120410160227|lab|12^XYZ LAB|
            """;
    }

    @SuppressWarnings("checkstyle:LineLength")
    private static String hl7FaultyMessage() {
        return "MSH|^~\\&|SendingApp|SendingFac|ReceivingApp|ReceivingFac|INVALID_DATE||ORU^R01|59689|P|2.3|\r\n";
    }

    @Test
    public void testReceive() throws Exception {
        // set timezone for Kafka timestamp conversion
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Berlin")));

        mock.expectedMessageCount(1);
        mock.expectedHeaderReceived(KafkaConstants.KEY, "59689");
        mock.expectedHeaderReceived(KafkaConstants.OVERRIDE_TIMESTAMP,
            "1334120745000"); // 20120411070545

        template.sendBody(hl7TestMessage());
        mock.assertIsSatisfied();
    }

    @Test
    public void testParseError() throws Exception {
        // must create an exchange to get the result as an exchange
        // where we can get the caused exception
        var exchange =
            template.getDefaultEndpoint().createExchange(ExchangePattern.InOut);
        exchange.getIn().setBody(hl7FaultyMessage());

        var out = template.send(exchange);
        assertTrue("Should be failed", out.isFailed());
        assertEquals(out.getException().getClass(),
            MllpApplicationErrorAcknowledgementException.class);
        assertTrue("Will produce an HL7 error ACK",
            out.getException().getMessage()
                .startsWith("HL7 Application Error Acknowledgment Received"));
    }
}
