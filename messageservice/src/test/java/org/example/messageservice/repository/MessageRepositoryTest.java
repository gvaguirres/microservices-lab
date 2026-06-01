package org.example.messageservice.repository;

import org.example.messageservice.entity.Message;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void shouldSaveMessage() {

        Message message = new Message();
        message.setSenderId(1L);
        message.setReceiverId(2L);
        message.setText("Hello World");

        Message savedMessage = messageRepository.save(message);

        assertThat(savedMessage.getId()).isNotNull();
        assertThat(savedMessage.getSenderId()).isEqualTo(1L);
        assertThat(savedMessage.getReceiverId()).isEqualTo(2L);
        assertThat(savedMessage.getText()).isEqualTo("Hello World");
        assertThat(savedMessage.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindAllMessages() {

        Message message1 = new Message();
        message1.setSenderId(1L);
        message1.setReceiverId(2L);
        message1.setText("Message 1");

        Message message2 = new Message();
        message2.setSenderId(2L);
        message2.setReceiverId(1L);
        message2.setText("Message 2");

        messageRepository.save(message1);
        messageRepository.save(message2);

        List<Message> messages = messageRepository.findAll();

        assertThat(messages).hasSize(2);
    }

    @Test
    void shouldFindMessageById() {
        // Given
        Message message = new Message();
        message.setSenderId(1L);
        message.setReceiverId(2L);
        message.setText("Find me");
        Message savedMessage = messageRepository.save(message);

        // When
        Optional<Message> foundMessage = messageRepository.findById(savedMessage.getId());

        // Then
        assertThat(foundMessage).isPresent();
        assertThat(foundMessage.get().getText()).isEqualTo("Find me");
    }

    @Test
    void shouldVerifyMessagesContent() {

        Message m1 = new Message();
        m1.setSenderId(10L);
        m1.setReceiverId(20L);
        m1.setText("First unique message");

        Message m2 = new Message();
        m2.setSenderId(30L);
        m2.setReceiverId(40L);
        m2.setText("Second unique message");

        messageRepository.save(m1);
        messageRepository.save(m2);


        List<Message> messages = messageRepository.findAll();

        assertThat(messages).extracting(Message::getText)
                .contains("First unique message", "Second unique message");
        
        assertThat(messages).filteredOn(m -> m.getSenderId().equals(10L))
                .extracting(Message::getText)
                .containsExactly("First unique message");
    }
}
