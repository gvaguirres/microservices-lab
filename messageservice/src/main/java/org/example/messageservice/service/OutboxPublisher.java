package org.example.messageservice.service;

import org.example.messageservice.entity.OutboxEvent;
import org.example.messageservice.event.MessagePublishedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final String TOPIC = "message-published";

    private final OutboxTxHelper txHelper;
    private final KafkaTemplate<String, MessagePublishedEvent> kafkaTemplate;

    public OutboxPublisher(OutboxTxHelper txHelper,
                           KafkaTemplate<String, MessagePublishedEvent> kafkaTemplate) {
        this.txHelper     = txHelper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5_000)
    // NOTE: intentionally NOT `@Transactional` — DB work happens in dedicated REQUIRES_NEW methods
    public void publishPendingEvents() {

        // Step 1 — claim rows in a short, isolated transaction (FOR UPDATE + flush)
        List<OutboxEvent> claimed = txHelper.claimPendingEvents();

        for (OutboxEvent evt : claimed) {
            OutboxEvent.Status next = OutboxEvent.Status.FAILED;
            try {
                // Step 2 — send to Kafka OUTSIDE any DB transaction
                kafkaTemplate.send(TOPIC,
                                new MessagePublishedEvent(
                                        evt.getMessageId(), evt.getSenderId(),
                                        evt.getReceiverId(), evt.getText(), evt.getCreatedAt()))
                        .get(10, TimeUnit.SECONDS);

                next = OutboxEvent.Status.SENT;
                log.debug("Outbox event {} published", evt.getId());

            } catch (InterruptedException ex) {
                // Restore the interrupt flag and stop the current scheduling cycle
                Thread.currentThread().interrupt();
                log.warn("Interrupted while publishing outbox event {}; stopping cycle", evt.getId());
                // Leave event as PROCESSING — the next fixedDelay cycle will not re-claim it
                // (add a PROCESSING→PENDING reset on startup or after a configurable TTL if needed)
                return;

            } catch (Exception ex) {
                log.error("Failed to publish outbox event {}, will mark FAILED", evt.getId(), ex);
            }

            // Step 3 — persist the final status in its own short, isolated transaction
            txHelper.updateStatus(evt.getId(), next);
        }
    }
}
