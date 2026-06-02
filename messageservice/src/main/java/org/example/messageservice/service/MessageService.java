package org.example.messageservice.service;

import org.example.messageservice.entity.OutboxEvent;
import org.example.messageservice.repository.MessageRepository;
import org.example.messageservice.exception.ResourceNotFoundException;
import org.example.messageservice.dto.CreateMessageDTO;
import org.example.messageservice.dto.MessageDTO;
import org.example.messageservice.entity.Message;
import org.example.messageservice.mapper.MessageMapper;
import org.example.messageservice.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final OutboxEventRepository outboxEventRepository;

    public MessageService(MessageRepository messageRepository, MessageMapper messageMapper, OutboxEventRepository outboxEventRepository) {
        this.messageRepository = messageRepository;
        this.messageMapper = messageMapper;
        this.outboxEventRepository = outboxEventRepository;
    }

    public List<MessageDTO> getMessages() {

        log.info("Hämtar alla meddelanden");

        return messageRepository.findAll().stream()
                .sorted(Comparator.comparing(Message::getId))
                .map(messageMapper::toDto)
                .toList();
    }

    public MessageDTO getMessageById(Long id) {

        log.info("Hämtar meddelande med id {}", id);

        return messageRepository.findById(id)
                .map(messageMapper::toDto)
                .orElseThrow( () -> new ResourceNotFoundException("Message not found with id: " + id));

    }

    @Transactional
    public MessageDTO sendMessage(CreateMessageDTO createMessageDTO) {
        log.info("Sending message from {} to {}", createMessageDTO.senderId(), createMessageDTO.receiverId());
        Message message = messageMapper.toEntity(createMessageDTO);
        Message saved   = messageRepository.save(message);

        // Write outbox event atomically with the domain change — no Spring event needed
        OutboxEvent outbox = new OutboxEvent();
        outbox.setMessageId(saved.getId());
        outbox.setSenderId(saved.getSenderId());
        outbox.setReceiverId(saved.getReceiverId());
        outbox.setText(saved.getText());
        outbox.setCreatedAt(saved.getCreatedAt());
        outboxEventRepository.save(outbox);

        return messageMapper.toDto(saved);
    }
}
