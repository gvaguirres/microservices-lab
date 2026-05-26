package org.example.bff;

import org.example.bff.dto.CreateMessageDTO;
import org.example.bff.dto.CreateUserDTO;
import org.example.bff.dto.MessageDTO;
import org.example.bff.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;

@RestController
@RequestMapping("/bff")
public class BffRestController {

    private static final Logger log = LoggerFactory.getLogger(BffRestController.class);
    private final Oauth2JwtTokenService tokenService;
    private final RestClient authClient = RestClient.create("http://localhost:9000");
    private final RestClient userClient = RestClient.create("http://localhost:8081");
    private final RestClient messageClient = RestClient.create("http://localhost:8082");

    public BffRestController(Oauth2JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {

        log.info("BFF REST: Loggar in användare {}", request.username());

        return authClient.post()
                .uri("/login")
                .body(request)
                .retrieve()
                .body(TokenResponse.class);
    }

    @PostMapping("/users/create")
    public UserDTO createUser(@RequestBody CreateUserDTO createUserDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String jwtToken = tokenService.getAccessToken(auth);

        log.info("BFF REST: Skapar användare med email {}", createUserDTO.email());

        return userClient.post()
                .uri("/users/create")
                .headers(h -> h.setBearerAuth(jwtToken))
                .body(createUserDTO)
                .retrieve()
                .body(UserDTO.class);
    }

    @GetMapping("/messages")
    public List<MessageDTO> getMessages() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String jwtToken = tokenService.getAccessToken(auth);

        log.info("BFF REST: Hämtar meddelanden från Message Service");

        return messageClient.get()
                .uri("/messages")
                .headers(h -> h.setBearerAuth(jwtToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    @PostMapping("/messages")
    public MessageDTO sendMessage(@RequestBody CreateMessageDTO createMessageDTO) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String jwtToken = tokenService.getAccessToken(auth);

        log.info("BFF REST: Skickar meddelande från {} till {}",
                createMessageDTO.senderId(),
                createMessageDTO.receiverId());

        return messageClient.post()
                .uri("/messages")
                .headers(h -> h.setBearerAuth(jwtToken))
                .body(createMessageDTO)
                .retrieve()
                .body(MessageDTO.class);
    }
}

record LoginRequest(
        String username,
        String password){}

record TokenResponse(
        String accessToken) {}
