package com.paystatement.services.service;

import com.paystatement.services.dto.GenerateStatementRequest;
import com.paystatement.services.model.PayStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "paystatement.kafka.enabled", havingValue = "true")
public class PayStatementKafkaListener {
    private static final Logger log = LoggerFactory.getLogger(PayStatementKafkaListener.class);
    private final PayStatementService service;
    private final PayStatementGeneratedPublisher publisher;

    public PayStatementKafkaListener(PayStatementService service, PayStatementGeneratedPublisher publisher) {
        this.service = service;
        this.publisher = publisher;
    }

    @KafkaListener(topics = "${paystatement.kafka.generate-topic:paystatement.generate}",
            groupId = "${spring.kafka.consumer.group-id:paystatement-service}")
    public void generate(GenerateStatementRequest request) {
        PayStatement generated = service.generate(request);
        publisher.publish(generated);
        log.info("Generated statement {} from Kafka request", generated.getId());
    }
}
