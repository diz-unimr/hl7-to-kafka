package de.unimarburg.diz.hl7tokafka;

import static org.apache.camel.component.hl7.HL7.ack;

import org.apache.camel.Message;
import org.apache.camel.builder.endpoint.EndpointRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.wildfly.common.annotation.NotNull;

@Component
public class AppRouter extends EndpointRouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(AppRouter.class);
    private final String hl7Url;
    private final String kafkaTopic;


    public AppRouter(@NotNull @Value("${endpoint.hl7.url}") String hl7Url,
        @NotNull @Value("${endpoint.kafka.topic}") String kafkaTopic) {
        this.hl7Url = hl7Url;
        this.kafkaTopic = kafkaTopic;
    }

    @Override
    public void configure() {

        // from("netty:tcp://localhost:8888?sync=true&amp;codec=#hl7codec")
        // .unmarshal().hl7(true)
        from(netty(hl7Url)).routeId("hl7Listener")
            //            .setProperty(Exchange.CHARSET_NAME,
            //                ExpressionBuilder.constantExpression("utf-8"))
            //            .threads()
            .unmarshal().hl7(true).log("The Message body is: ${body}")
            .process(exchange -> {
                final Message message = exchange.getIn().getBody(Message.class);
                System.out.println("Original message: " + message);
            }).to(kafka(kafkaTopic)).onCompletion().transform(ack()).end();

    }

}
