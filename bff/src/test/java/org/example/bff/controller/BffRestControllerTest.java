package org.example.bff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.bff.dto.CreateMessageDTO;
import org.example.bff.dto.CreateUserDTO;
import org.example.bff.dto.MessageDTO;
import org.example.bff.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BffRestControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private BffRestController bffRestController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockRestServiceServer userServer;
    private MockRestServiceServer messageServer;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // Configure JWT decoder to return a mock JWT
        Jwt mockJwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject("test-user")
                .claim("scope", "user.read")
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(mockJwt);

        // Use reflection to replace the hardcoded RestClients with ones we can mock
        RestClient.Builder userBuilder = RestClient.builder().baseUrl("http://localhost:8081");
        userServer = MockRestServiceServer.bindTo(userBuilder).build();
        ReflectionTestUtils.setField(bffRestController, "userClient", userBuilder.build());

        RestClient.Builder messageBuilder = RestClient.builder().baseUrl("http://localhost:8082");
        messageServer = MockRestServiceServer.bindTo(messageBuilder).build();
        ReflectionTestUtils.setField(bffRestController, "messageClient", messageBuilder.build());
    }

    @Test
    void shouldRejectRequestWithoutJwt() throws Exception {
        mockMvc.perform(get("/bff/messages"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAcceptRequestWithValidJwt() throws Exception {
        // We need to mock the downstream call because getMessages is called
        messageServer.expect(requestTo("http://localhost:8082/messages"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/bff/messages")
                        .with(jwt())
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldForwardCreateUserRequestWithAuthorizationHeader() throws Exception {
        CreateUserDTO requestDto = new CreateUserDTO("First", "Last", "test@example.com", "12345");
        UserDTO responseDto = new UserDTO(1L, "First", "Last", "test@example.com", "12345");

        userServer.expect(requestTo("http://localhost:8081/users/create"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer mock-token"))
                .andExpect(content().json(objectMapper.writeValueAsString(requestDto)))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseDto), MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/bff/users/create")
                        .with(jwt())
                        .header("Authorization", "Bearer mock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("First"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        userServer.verify();
    }

    @Test
    void shouldForwardCreateMessageRequestWithAuthorizationHeader() throws Exception {
        CreateMessageDTO requestDto = new CreateMessageDTO(1L, 2L, "Hello from BFF");
        MessageDTO responseDto = new MessageDTO(100L, 1L, 2L, "Hello from BFF");

        messageServer.expect(requestTo("http://localhost:8082/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer mock-token"))
                .andExpect(content().json(objectMapper.writeValueAsString(requestDto)))
                .andRespond(withSuccess(objectMapper.writeValueAsString(responseDto), MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/bff/messages")
                        .with(jwt())
                        .header("Authorization", "Bearer mock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Hello from BFF"))
                .andExpect(jsonPath("$.id").value(100));

        messageServer.verify();
    }

    @Test
    void shouldReturnMessagesFromMessageService() throws Exception {
        MessageDTO m1 = new MessageDTO(1L, 1L, 2L, "Msg 1");
        MessageDTO m2 = new MessageDTO(2L, 2L, 1L, "Msg 2");
        List<MessageDTO> messages = List.of(m1, m2);

        messageServer.expect(requestTo("http://localhost:8082/messages"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer mock-token"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(messages), MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/bff/messages")
                        .with(jwt())
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].text").value("Msg 1"))
                .andExpect(jsonPath("$[1].text").value("Msg 2"));

        messageServer.verify();
    }
}
