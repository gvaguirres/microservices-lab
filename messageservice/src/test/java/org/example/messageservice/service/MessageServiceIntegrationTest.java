package org.example.messageservice.service;

import org.example.messageservice.dto.CreateMessageDTO;
import org.example.messageservice.dto.MessageDTO;
import org.example.messageservice.entity.Message;
import org.example.messageservice.event.MessagePublishedEvent;
import org.example.messageservice.exception.ResourceNotFoundException;
import org.example.messageservice.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

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

    @MockitoBean
    private MessageProducer messageProducer;

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
    void shouldPublishMessagePublishedEventWhenMessageIsCreated() {

        CreateMessageDTO dto = new CreateMessageDTO(10L, 20L, "Kafka Test");

        MessageDTO result = messageService.sendMessage(dto);

        verify(messageProducer, times(1)).publishMessage(any(MessagePublishedEvent.class));

        verify(messageProducer).publishMessage(org.mockito.ArgumentMatchers.argThat(event -> 
            event.messageId().equals(result.id()) &&
            event.senderId().equals(10L) &&
            event.receiverId().equals(20L) &&
            event.message().equals("Kafka Test")
        ));
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenMessageNotFoundById() {

        assertThatThrownBy(() -> messageService.getMessageById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Message not found with id: 999");
    }
}
