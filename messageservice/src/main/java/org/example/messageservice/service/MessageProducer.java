package org.example.messageservice.service;

import org.example.messageservice.event.MessagePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageProducer {

    private static final Logger log = LoggerFactory.getLogger(MessageProducer.class);
    private final KafkaTemplate<String, MessagePublishedEvent> kafkaTemplate;

    public MessageProducer(KafkaTemplate<String, MessagePublishedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishMessage(MessagePublishedEvent event) {

        log.info("Publicerar event till Kafka topic message-published för message id {}",
                event.messageId());
        kafkaTemplate.send("message-published", event);
    }
}
