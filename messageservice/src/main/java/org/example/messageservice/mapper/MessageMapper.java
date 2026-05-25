package org.example.messageservice.mapper;

import org.example.messageservice.dto.CreateMessageDTO;
import org.example.messageservice.dto.MessageDTO;
import org.example.messageservice.entity.Message;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    //Entity to MovieDTO
    MessageDTO toDto(Message message);

    //Create from Dto to Entity
    Message toEntity(CreateMessageDTO createDTO);
}
