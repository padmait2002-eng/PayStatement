package com.paystatement.services.service;

import com.paystatement.services.dto.PayStatementGeneratedEvent;
import com.paystatement.services.model.PayStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class PayStatementGeneratedPublisher {
    private static final Logger log = LoggerFactory.getLogger(PayStatementGeneratedPublisher.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean enabled;
    private final String topic;

    public PayStatementGeneratedPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                          @Value("${paystatement.kafka.enabled:false}") boolean enabled,
                                          @Value("${paystatement.kafka.generated-topic:paystatement.generated}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.enabled = enabled;
        this.topic = topic;
    }

    public void publish(PayStatement statement) {
        if (!enabled) {
            log.warn("Kafka publishing is disabled; generated statement {} was not sent", statement.getId());
            return;
        }
        var event = new PayStatementGeneratedEvent(statement.getId(), statement.getEmployeeId(),
                statement.getWeekEnding(), statement.getStatus(), statement.getGeneratedAt());
        kafkaTemplate.send(topic, statement.getEmployeeId(), event).whenComplete((result, error) -> {
            if (error != null) {
                log.error("Failed to publish generated statement {} to {}", statement.getId(), topic, error);
            }
        });
    }

    public void publishAfterCommit(PayStatement statement) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(statement);
                }
            });
        } else {
            publish(statement);
        }
    }
}
