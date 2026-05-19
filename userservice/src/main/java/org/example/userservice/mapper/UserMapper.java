package org.example.userservice.mapper;

import org.example.userservice.dto.CreateUserDTO;
import org.example.userservice.entity.User;
import org.example.userservice.dto.UpdateUserDTO;
import org.example.userservice.dto.UserDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    //Entity to MovieDTO
    UserDTO toDto(User user);

    //Create from Dto to Entity
    User toEntity(CreateUserDTO createDTO);

    //Update an existent Entity
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(UpdateUserDTO updateDto, @MappingTarget User user);
}
