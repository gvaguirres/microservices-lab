package org.example.messageservice.service;

import org.example.messageservice.dto.CreateMessageDTO;
import org.example.messageservice.entity.OutboxEvent;
import org.example.messageservice.repository.MessageRepository;
import org.example.messageservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:commit_testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class MessageProducerCommitIntegrationTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        messageRepository.deleteAll();
        outboxEventRepository.deleteAll();
    }

    @Test
    void shouldCommitOutboxEventOnlyAfterTransactionSucceeds() {
        CreateMessageDTO dto = new CreateMessageDTO(10L, 20L, "Commit Test");

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                messageService.sendMessage(dto);
                // The outbox event is saved but not yet committed. 
                // Note: In H2 with default isolation, it might be visible in the same connection 
                // but this test proves the end-to-end commit flow.
            }
        });

        // After commit, outbox event must be present
        assertThat(outboxEventRepository.findAll()).hasSize(1);
        assertThat(messageRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldNotCommitOutboxEventIfTransactionIsRolledBack() {
        CreateMessageDTO dto = new CreateMessageDTO(10L, 20L, "Rollback Test");

        try {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    messageService.sendMessage(dto);
                    throw new RuntimeException("Simulated rollback");
                }
            });
        } catch (RuntimeException e) {
            // Expected
        }

        // After rollback, both message and outbox event should be empty
        assertThat(messageRepository.findAll()).isEmpty();
        assertThat(outboxEventRepository.findAll()).isEmpty();
    }
}
