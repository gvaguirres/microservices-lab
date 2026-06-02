package org.example.messageservice.service;

import org.example.messageservice.event.MessagePublishedEvent;
import org.example.messageservice.repository.MessageRepository;
import org.example.messageservice.exception.ResourceNotFoundException;
import org.example.messageservice.dto.CreateMessageDTO;
import org.example.messageservice.dto.MessageDTO;
import org.example.messageservice.entity.Message;
import org.example.messageservice.mapper.MessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    public MessageService(MessageRepository messageRepository,
                          MessageMapper messageMapper,
                          ApplicationEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.messageMapper     = messageMapper;
        this.eventPublisher    = eventPublisher;
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

    public MessageDTO sendMessage(CreateMessageDTO createMessageDTO) {

        log.info("Skickar meddelande från {} till {}",
                createMessageDTO.senderId(),
                createMessageDTO.receiverId());

        Message message = messageMapper.toEntity(createMessageDTO);
        Message newMessage = messageRepository.save(message);

        //Publicerar händelsen "message-published" till Message Queue
        MessagePublishedEvent event = new MessagePublishedEvent(
                newMessage.getId(),
                newMessage.getSenderId(),
                newMessage.getReceiverId(),
                newMessage.getText(),
                newMessage.getCreatedAt()
        );

        eventPublisher.publishEvent(event);

        return messageMapper.toDto(newMessage);
    }
}
