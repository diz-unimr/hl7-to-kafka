package de.unimarburg.diz.hl7tokafka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SuppressWarnings("checkstyle:LineLength")
@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS, properties = "endpoint.hl7.port=2576")
public class Hl7ToKafkaApplicationTests {

    @Test
    public void contextLoads() {
    }
}
