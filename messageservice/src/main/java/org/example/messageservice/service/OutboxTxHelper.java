package org.example.messageservice.service;

import org.example.messageservice.entity.OutboxEvent;
import org.example.messageservice.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OutboxTxHelper {

    private final OutboxEventRepository outboxRepo;

    public OutboxTxHelper(OutboxEventRepository outboxRepo) {
        this.outboxRepo = outboxRepo;
    }

    /**
     * Atomically claims all PENDING rows by flipping them to PROCESSING
     * inside its own short transaction. Returns detached snapshots so the
     * caller can read field values after the transaction closes.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<OutboxEvent> claimPendingEvents() {
        List<OutboxEvent> pending =
                outboxRepo.findByStatusForUpdate(OutboxEvent.Status.PENDING);
        pending.forEach(e -> e.setStatus(OutboxEvent.Status.PROCESSING));
        outboxRepo.flush(); // commit the claim before returning
        return List.copyOf(pending);
    }

    /**
     * Marks a single event SENT or FAILED in its own short transaction,
     * committed independently of the Kafka call.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStatus(Long id, OutboxEvent.Status status) {
        outboxRepo.findById(id)
                .ifPresent(e -> e.setStatus(status));
    }
}
