package org.example.messageservice.service;

import org.example.messageservice.entity.OutboxEvent;
import org.example.messageservice.event.MessagePublishedEvent;
import org.example.messageservice.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxRepo;
    private final KafkaTemplate<String, MessagePublishedEvent> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxRepo,
                           KafkaTemplate<String, MessagePublishedEvent> kafkaTemplate) {
        this.outboxRepo    = outboxRepo;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5_000)          // every 5 s
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxRepo.findByStatus(OutboxEvent.Status.PENDING);
        for (OutboxEvent evt : pending) {
            try {
                kafkaTemplate.send("message-published",
                                new MessagePublishedEvent(
                                        evt.getMessageId(), evt.getSenderId(),
                                        evt.getReceiverId(), evt.getText(), evt.getCreatedAt()))
                        .get(10, TimeUnit.SECONDS);          // block to confirm broker ack
                evt.setStatus(OutboxEvent.Status.SENT);
                log.debug("Outbox event {} sent", evt.getId());
            } catch (Exception ex) {
                log.error("Failed to publish outbox event {}, will retry", evt.getId(), ex);
                // Leave as PENDING to retry next cycle.
                // Optionally flip to FAILED after N attempts and route to a DLQ.
            }
        }
    }
}
