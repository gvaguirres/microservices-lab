package org.example.userservice.service;

import org.example.userservice.repository.UserRepository;
import org.example.userservice.dto.CreateUserDTO;
import org.example.userservice.dto.UpdateUserDTO;
import org.example.userservice.dto.UserDTO;
import org.example.userservice.entity.User;
import org.example.userservice.exception.ResourceNotFoundException;
import org.example.userservice.exception.UserAlreadyExistsException;
import org.example.userservice.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public UserDTO createUser(CreateUserDTO createDto) {

        boolean exists = userRepository.existsByEmail(createDto.email());

        if (exists) {
            log.warn("Användare med email {} finns redan", createDto.email());
            throw new UserAlreadyExistsException("User with email " + createDto.email() + " already exists.");
        }

        log.info("Skapar användare med email {}", createDto.email());
        User user = userMapper.toEntity(createDto);
        User newUser = userRepository.save(user);
        return userMapper.toDto(newUser);
    }

    public UserDTO updateUser(Long id, UpdateUserDTO updateDto) {

        User user = userRepository.findById(id).orElseThrow( () -> {
            log.warn("Användare med id {} hittades inte", id);
            return new ResourceNotFoundException("User not found with id: " + id);
        });

        if (updateDto.email() != null &&
                userRepository.existsByEmailAndIdNot(updateDto.email(), id)) {
            log.warn("Email {} används redan av en annan användare", updateDto.email());
            throw new UserAlreadyExistsException(
                    "User with email " + updateDto.email() + " already exists."
            );
        }
        log.info("Uppdaterar användare med id {}", id);
        userMapper.updateEntityFromDto(updateDto, user);
        User updatedUser = userRepository.save(user);
        return userMapper.toDto(updatedUser);
    }

    public void deleteUser(Long id){

        if (!userRepository.existsById(id))
            throw new ResourceNotFoundException("User not found with id: " + id);

        log.info("Tar bort användare med id {}", id);
        userRepository.deleteById(id);
    }

    public List<UserDTO> getAllUsers() {

        log.info("Hämtar alla användare");

        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(userMapper::toDto)
                .toList();
    }

    public UserDTO getUserById(Long id) {

        log.info("Hämtar användare med id {}", id);

        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow( () -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public UserDTO getUserByEmail(String email) {

        log.info("Hämtar användare med email {}", email);

        return userRepository.findByEmail(email)
                .map(userMapper::toDto)
                .orElseThrow( () -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public UserDTO getUserByPhoneNumber(String phoneNumber) {

        log.info("Hämtar användare med telefonnummer {}", phoneNumber);

        return userRepository.findByPhoneNumber(phoneNumber)
                .map(userMapper::toDto)
                .orElseThrow( () -> new ResourceNotFoundException("User not found with phone number: " + phoneNumber));
    }
}
