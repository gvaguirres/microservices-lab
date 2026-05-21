package org.example.bff;

import org.example.bff.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CompletableFuture;

@Controller
public class GraphQLController {

    private static final Logger log = LoggerFactory.getLogger(GraphQLController.class);

    private final Oauth2JwtTokenService tokenService;
    private final RestClient client1 = RestClient.create("http://localhost:8082");
    private final RestClient client2 = RestClient.create("http://localhost:8081");

    public GraphQLController(Oauth2JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @QueryMapping
    public Result merged() {
        log.info("---> Anropade huvud-query: merged");
        return new Result(null, null);
    }

    @SchemaMapping(typeName = "Result", field = "messageservice")
    public CompletableFuture<String> getMessageService(Result result) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String jwtToken = tokenService.getAccessToken(auth);

        return CompletableFuture.supplyAsync(() -> {
            log.info("Exekverar: Hämtar data från Message Service");

            return client1.get()
                    .uri("/user-profile?userId=1")
                    .headers(h -> h.setBearerAuth(jwtToken))
                    .retrieve()
                    .body(String.class);
        });
    }

    @SchemaMapping(typeName = "Result", field = "userservice")
    public CompletableFuture<UserDTO> getUserService(Result result) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String jwtToken = tokenService.getAccessToken(auth);
        return CompletableFuture.supplyAsync(() -> {
            log.info("Exekverar: Hämtar data från User Service");

            return client2.get()
                    .uri("/users/1")
                    .headers(h -> h.setBearerAuth(jwtToken))
                    .retrieve()
                    .body(UserDTO.class);
        });
    }
}

record Result(String messageservice, String userservice) {}
