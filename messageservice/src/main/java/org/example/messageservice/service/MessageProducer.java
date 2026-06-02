package org.example.messageservice.service;

import org.example.messageservice.event.MessagePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class MessageProducer {

    private static final Logger log = LoggerFactory.getLogger(MessageProducer.class);
    private final KafkaTemplate<String, MessagePublishedEvent> kafkaTemplate;

    public MessageProducer(KafkaTemplate<String, MessagePublishedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishMessage(MessagePublishedEvent event) {

        log.info("Publicerar event till Kafka topic message-published för message id {}",
                event.messageId());
        kafkaTemplate.send("message-published", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish message event for message id {}", event.messageId(), ex);
                    } else {
                        log.debug("Successfully published message event for message id {}", event.messageId());
                    }
                });
    }
}
