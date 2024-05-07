package de.unimarburg.diz.hl7tokafka;

import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Hl7ToKafkaApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Berlin")));
        SpringApplication.run(Hl7ToKafkaApplication.class, args);
    }
}
