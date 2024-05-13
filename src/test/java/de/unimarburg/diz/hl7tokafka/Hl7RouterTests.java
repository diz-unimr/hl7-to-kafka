package de.unimarburg.diz.hl7tokafka;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpointsAndSkip;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@CamelSpringBootTest
@EnableAutoConfiguration
@SpringBootTest(properties = {"endpoint.hl7.url=localhost:8888",
    "endpoint.kafka.topic=hl7-topic"})
@MockEndpointsAndSkip("kafka:hl7-topic")
//@UseAdviceWith
public class Hl7RouterTests {

    @Autowired
    CamelContext context;

    @Produce("mllp:localhost:8888")
    ProducerTemplate template;

    @EndpointInject("mock:kafka:hl7-topic")
    MockEndpoint mock;

    private static String hl7TestMessage() {
        return """
            MSH|^~\\&|SendingApp|SendingFac|ReceivingApp|ReceivingFac|20120411070545||ORU^R01|59689|P|2.3|\r
            PID|1||ICE999999^^^ICE^ICE||Testpatient^Testy^^^Mr||19740401|M|||123 Barrel Drive^^^^SW18 4RT|||||2||||||||||||||\r
            PV1|1|O|||||71^DUCK^DONALD||||||||||||12376|||||||||||||||||||||||||20120410160227||||||\r
            ORC|RE||12376|||||||100^DUCK^DASIY||71^DUCK^DONALD|||20120411070545|||||\r
            OBR|1||12376|cbc^CBC|R||20120410160227|||22^GOOF^GOOFY|||Fasting: No|201204101625||71^DUCK^DONALD||||||201204101630|||F||^^^^^R|||||||||||||||||85025|\r
            OBX|1|NM|wbc^Wbc^Local^6690-2^Wbc^LN||7.0|/nl|3.8-11.0||||F|||20120410160227|lab|12^XYZ LAB|\r
            OBX|2|NM|neutros^Neutros^Local^770-8^Neutros^LN||68|%|40-82||||F|||20120410160227|lab|12^XYZ LAB|\r
            OBX|3|NM|lymphs^Lymphs^Local^736-9^Lymphs^LN||20|%|11-47||||F|||20120410160227|lab|12^XYZ LAB|\r
            OBX|4|NM|monos^Monos^Local^5905-5^Monos^LN||16|%|4-15|H|||F|||20120410160227|lab|12^XYZ LAB|\r
            OBX|5|NM|eo^Eos^Local^713-8^Eos^LN||3|%|0-8||||F|||20120410160227|lab|12^XYZ LAB|\r
            OBX|6|NM|baso^Baso^Local^706-2^Baso^LN||0|%|0-1||||F|||20120410160227|lab|12^XYZ LAB|\r
            OBX|7|NM|ig^Imm Gran^Local^38518-7^Imm Gran^LN||0|%|0-2||||F|||20120410160227|lab|12^XYZ LAB|\r
            OBX|8|NM|rbc^Rbc^Local^789-8^Rbc^LN||4.02|/pl|4.07-4.92|L|||F|||20120410160227|lab|12^XYZ LAB|\r
            OBX|9|NM|hgb^Hgb^Local^718-7^Hgb^LN||13.7|g/dl|12.0-14.1||||F|||20120410160227|lab|12^XYZ LAB|\r
            OBX|10|NM|hct^Hct^Local^4544-3^Hct^LN||40|%|34-43||||F|||20120410160227|lab|12^XYZ LAB|\r
            OBX|11|NM|mcv^Mcv^Local^787-2^Mcv^LN||80|fl|77-98||||F|||20120410160227|lab|12^XYZ LAB|\r
            OBX|12|NM|mch^Mch||30|pg|27-35||||F|||20120410160227|lab|12^XYZ LAB|\r
            OBX|13|NM|mchc^Mchc||32|g/dl|32-35||||F|||20120410160227|lab|12^XYZ LAB|\r
            OBX|14|NM|plt^Platelets||221|/nl|140-400||||F|||20120410160227|lab|12^XYZ LAB|\r\n
            """;
    }

    @Test
    public void testReceive() throws Exception {

        mock.expectedMessageCount(1);
        mock.expectedHeaderReceived(KafkaConstants.KEY, "59689");
        mock.expectedHeaderReceived(KafkaConstants.OVERRIDE_TIMESTAMP,
            "1334120745000"); // 20120411070545

        template.sendBody(hl7TestMessage());
        mock.assertIsSatisfied();
    }
}
