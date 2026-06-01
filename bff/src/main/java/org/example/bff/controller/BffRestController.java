package org.example.bff.controller;

import org.example.bff.dto.CreateMessageDTO;
import org.example.bff.dto.CreateUserDTO;
import org.example.bff.dto.MessageDTO;
import org.example.bff.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;

@RestController
@RequestMapping("/bff")
public class BffRestController {

    private static final Logger log = LoggerFactory.getLogger(BffRestController.class);
    private final RestClient userClient = RestClient.create("http://localhost:8081");
    private final RestClient messageClient = RestClient.create("http://localhost:8082");

    @PostMapping("/users/create")
    public UserDTO createUser(@RequestBody CreateUserDTO createUserDTO,
                              @RequestHeader("Authorization") String authorization) {

        log.info("BFF REST: Skapar användare med email {}", createUserDTO.email());

        return userClient.post()
                .uri("/users/create")
                .headers(h -> h.set("Authorization", authorization))
                .body(createUserDTO)
                .retrieve()
                .body(UserDTO.class);
    }

    @GetMapping("/messages")
    public List<MessageDTO> getMessages(@RequestHeader("Authorization") String authorization) {

        log.info("BFF REST: Hämtar meddelanden från Message Service");

        return messageClient.get()
                .uri("/messages")
                .headers(h -> h.set("Authorization", authorization))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @PostMapping("/messages")
    public MessageDTO sendMessage(@RequestBody CreateMessageDTO createMessageDTO,
                                  @RequestHeader("Authorization") String authorization) {

        log.info("BFF REST: Skickar meddelande från {} till {}",
                createMessageDTO.senderId(),
                createMessageDTO.receiverId());

        return messageClient.post()
                .uri("/messages")
                .headers(h -> h.set("Authorization", authorization))
                .body(createMessageDTO)
                .retrieve()
                .body(MessageDTO.class);
    }
}
