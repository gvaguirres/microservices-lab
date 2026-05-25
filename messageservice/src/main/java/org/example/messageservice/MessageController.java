package org.example.messageservice;

import jakarta.validation.Valid;
import org.example.messageservice.dto.CreateMessageDTO;
import org.example.messageservice.dto.MessageDTO;
import org.example.messageservice.service.MessageService;
import org.springframework.web.bind.annotation.*;
import org.example.grpc.UserProfileRequest;
import org.example.grpc.UserProfileResponse;
import org.example.grpc.UserProfileServiceGrpc;

import java.util.List;

@RestController
public class MessageController {

    final UserProfileServiceGrpc.UserProfileServiceBlockingStub stub;
    public MessageService messageService;

    public MessageController(UserProfileServiceGrpc.UserProfileServiceBlockingStub stub, MessageService messageService) {
        this.stub = stub;
        this.messageService = messageService;
    }

    @GetMapping("/user-profile")
    public String getUserProfile(@RequestParam Long userId) {

        UserProfileRequest request = UserProfileRequest.newBuilder()
                .setUserId(userId)
                .build();

        UserProfileResponse response = stub.getUserProfile(request);

        return "User profile: "
                + response.getFirstName() + " "
                + response.getLastName() + " "
                + ", email: " + response.getEmail()
                + ", phone: " + response.getPhoneNumber();
    }

    @GetMapping("/messages")
    public List<MessageDTO> getMessages() {
        return messageService.getMessages();
    }

    @GetMapping("/messages/{id}")
    public MessageDTO getMessagesById(@PathVariable Long id) {
        return messageService.getMessageById(id);
    }

    @PostMapping("/messages")
    public MessageDTO sendMessage(@Valid @RequestBody CreateMessageDTO dto) {
        return messageService.sendMessage(dto);
    }

}
