package de.unimarburg.diz.hl7tokafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Hl7ToKafkaApplication {

    /**
     * A main method to start this application.
     */
    public static void main(String[] args) {
        SpringApplication.run(Hl7ToKafkaApplication.class, args);
    }

}
