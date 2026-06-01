package org.example.userservice.service;

import org.example.userservice.dto.CreateUserDTO;
import org.example.userservice.dto.UpdateUserDTO;
import org.example.userservice.dto.UserDTO;
import org.example.userservice.entity.User;
import org.example.userservice.exception.ResourceNotFoundException;
import org.example.userservice.exception.UserAlreadyExistsException;
import org.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

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
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldCreateUserAndPersistIt() {
        // Given
        CreateUserDTO dto = new CreateUserDTO("John", "Doe", "john.doe@example.com", "123456789");

        // When
        UserDTO result = userService.createUser(dto);

        // Then
        assertThat(result.id()).isNotNull();
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");
        assertThat(result.email()).isEqualTo("john.doe@example.com");
        assertThat(result.phoneNumber()).isEqualTo("123456789");

        Optional<User> persistedUser = userRepository.findById(result.id());
        assertThat(persistedUser).isPresent();
        assertThat(persistedUser.get().getFirstName()).isEqualTo("John");
        assertThat(persistedUser.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    void shouldReturnAllUsers() {
        // Given
        userRepository.save(new User("Alice", "Smith", "alice@example.com", "111222333"));
        userRepository.save(new User("Bob", "Jones", "bob@example.com", "444555666"));

        // When
        List<UserDTO> users = userService.getAllUsers();

        // Then
        assertThat(users).hasSize(2);
        assertThat(users).extracting(UserDTO::email)
                .containsExactlyInAnyOrder("alice@example.com", "bob@example.com");
    }

    @Test
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        userRepository.save(new User("Existing", "User", "existing@example.com", "999888777"));
        CreateUserDTO dto = new CreateUserDTO("New", "User", "existing@example.com", "000000000");

        // When / Then
        assertThatThrownBy(() -> userService.createUser(dto))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("User with email existing@example.com already exists.");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserNotFoundById() {
        // When / Then
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserNotFoundByEmail() {
        // When / Then
        assertThatThrownBy(() -> userService.getUserByEmail("nonexistent@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with email: nonexistent@example.com");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUserNotFoundByPhoneNumber() {
        // When / Then
        assertThatThrownBy(() -> userService.getUserByPhoneNumber("000"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with phone number: 000");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenUpdatingNonExistentUser() {
        // Given
        UpdateUserDTO updateDto = new UpdateUserDTO("John", "Doe", "john@example.com", "123");

        // When / Then
        assertThatThrownBy(() -> userService.updateUser(999L, updateDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
    }

    @Test
    void shouldThrowUserAlreadyExistsExceptionWhenUpdatingToExistingEmail() {
        // Given
        User user1 = userRepository.save(new User("User1", "One", "user1@example.com", "111"));
        userRepository.save(new User("User2", "Two", "user2@example.com", "222"));
        UpdateUserDTO updateDto = new UpdateUserDTO("Updated", "Name", "user2@example.com", "111");

        // When / Then
        assertThatThrownBy(() -> userService.updateUser(user1.getId(), updateDto))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("User with email user2@example.com already exists.");
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenDeletingNonExistentUser() {
        // When / Then
        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
    }
}
