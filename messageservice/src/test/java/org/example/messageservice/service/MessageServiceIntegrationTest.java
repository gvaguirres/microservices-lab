package org.example.messageservice.service;

import org.example.messageservice.dto.CreateMessageDTO;
import org.example.messageservice.dto.MessageDTO;
import org.example.messageservice.entity.Message;
import org.example.messageservice.entity.OutboxEvent;
import org.example.messageservice.exception.ResourceNotFoundException;
import org.example.messageservice.repository.MessageRepository;
import org.example.messageservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Transactional
class MessageServiceIntegrationTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void shouldCreateMessageAndPersistIt() {

        CreateMessageDTO dto = new CreateMessageDTO(1L, 2L, "Hello World");

        MessageDTO result = messageService.sendMessage(dto);

        assertThat(result.id()).isNotNull();
        assertThat(result.senderId()).isEqualTo(1L);
        assertThat(result.receiverId()).isEqualTo(2L);
        assertThat(result.text()).isEqualTo("Hello World");
        assertThat(result.createdAt()).isNotNull();

        assertThat(messageRepository.findById(result.id())).isPresent();
    }

    @Test
    void shouldReturnAllMessages() {

        Message m1 = new Message();
        m1.setSenderId(1L);
        m1.setReceiverId(2L);
        m1.setText("Msg 1");
        messageRepository.save(m1);

        Message m2 = new Message();
        m2.setSenderId(3L);
        m2.setReceiverId(4L);
        m2.setText("Msg 2");
        messageRepository.save(m2);

        List<MessageDTO> results = messageService.getMessages();

        assertThat(results).hasSize(2);
        assertThat(results).extracting(MessageDTO::text).containsExactlyInAnyOrder("Msg 1", "Msg 2");
    }

    @Test
    void shouldCreateOutboxEventWhenMessageIsCreated() {

        CreateMessageDTO dto = new CreateMessageDTO(10L, 20L, "Outbox Test");

        MessageDTO result = messageService.sendMessage(dto);

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(1);
        
        OutboxEvent event = outboxEvents.get(0);
        assertThat(event.getMessageId()).isEqualTo(result.id());
        assertThat(event.getSenderId()).isEqualTo(10L);
        assertThat(event.getReceiverId()).isEqualTo(20L);
        assertThat(event.getText()).isEqualTo("Outbox Test");
        assertThat(event.getStatus()).isEqualTo(OutboxEvent.Status.PENDING);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenMessageNotFoundById() {

        assertThatThrownBy(() -> messageService.getMessageById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Message not found with id: 999");
    }
}
