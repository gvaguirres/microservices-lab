package org.example.bff.controller;

import org.example.bff.dto.CreateMessageDTO;
import org.example.bff.dto.CreateUserDTO;
import org.example.bff.dto.MessageDTO;
import org.example.bff.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;

@RestController
@RequestMapping("/bff")
public class BffRestController {

    private static final Logger log = LoggerFactory.getLogger(BffRestController.class);
    private final RestClient userClient;
    private final RestClient messageClient;

    public BffRestController(
            @Value("${app.services.user-url}") String userUrl,
            @Value("${app.services.message-url}") String messageUrl) {

        this.messageClient = RestClient.create(messageUrl);
        this.userClient    = RestClient.create(userUrl);
    }


        @PostMapping("/users/create")
    public UserDTO createUser(@RequestBody CreateUserDTO createUserDTO,
                              @AuthenticationPrincipal Jwt jwt) {

        log.info("BFF REST: Skapar användare med email");

        return userClient.post()
                .uri("/users/create")
                .headers(h -> h.setBearerAuth(jwt.getTokenValue()))
                .body(createUserDTO)
                .retrieve()
                .body(UserDTO.class);
    }

    @GetMapping("/messages")
    public List<MessageDTO> getMessages(@AuthenticationPrincipal Jwt jwt) {

        log.info("BFF REST: Hämtar meddelanden från Message Service");

        return messageClient.get()
                .uri("/messages")
                .headers(h -> h.setBearerAuth(jwt.getTokenValue()))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @PostMapping("/messages")
    public MessageDTO sendMessage(@RequestBody CreateMessageDTO createMessageDTO,
                                  @AuthenticationPrincipal Jwt jwt) {

        log.info("BFF REST: Skickar meddelande från {} till {}",
                createMessageDTO.senderId(),
                createMessageDTO.receiverId());

        return messageClient.post()
                .uri("/messages")
                .headers(h -> h.setBearerAuth(jwt.getTokenValue()))
                .body(createMessageDTO)
                .retrieve()
                .body(MessageDTO.class);
    }
}
