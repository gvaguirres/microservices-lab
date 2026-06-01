package org.example.cli;

import org.example.cli.dto.CreateMessageDTO;
import org.example.cli.dto.CreateUserDTO;
import org.example.cli.dto.MessageDTO;
import org.example.cli.dto.UserDTO;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class CliApplication implements CommandLineRunner {

    private final RestClient bffClient = RestClient.create("http://localhost:8080");
    private final DeviceFlowService deviceFlowService;

    public CliApplication(DeviceFlowService deviceFlowService) {
        this.deviceFlowService = deviceFlowService;
    }

    static void main(String[] args) {
        SpringApplication.run(CliApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        Scanner scanner = new Scanner(System.in);
        boolean option = true;

        while(option){

            System.out.print("Enter an option: ");
            System.out.println("""
                        1. Login
                        2. Exit""");
            
            String input = scanner.nextLine();

            if(input.equals("1")) {

                String jwtToken = deviceFlowService.getJwtToken();

                System.out.println("TOKEN:");
                System.out.println(jwtToken);

                while(option) {

                    System.out.print("Enter an option: ");
                    System.out.println("""
                            1. Create a user
                            2. Send messages
                            3. Get messages
                            4. Exit""");

                    String authenticatedInput = scanner.nextLine();
                    option = isOption(authenticatedInput, scanner, jwtToken, option);
                }
            } else if(input.equals("2")) {
                option = false;
            } else {
                System.out.println("Invalid input, please try again.");
            }
        }
    }

    private boolean isOption(String authenticatedInput, Scanner scanner, String jwtToken, boolean option) {
        switch (authenticatedInput) {
            case "1" -> createUser(scanner, jwtToken);
            case "2" -> sendMessage(scanner, jwtToken);
            case "3" -> getMessages(jwtToken);
            case "4" -> option = false;
            default -> System.out.println("Invalid input, please try again.");
        }
        return option;
    }

    private void createUser(Scanner scanner, String jwtToken) {
        System.out.print("Enter first name: ");
        String firstName = scanner.nextLine();
        System.out.print("Enter last name: ");
        String lastName = scanner.nextLine();
        System.out.print("Enter email: ");
        String email = scanner.nextLine();
        System.out.print("Enter phone number: ");
        String phoneNumber = scanner.nextLine();

        CreateUserDTO dto = new CreateUserDTO(firstName, lastName, email, phoneNumber);

        bffClient.post()
                .uri("/bff/users/create")
                .body(dto)
                .headers(h -> h.setBearerAuth(jwtToken))
                .retrieve()
                .body(UserDTO.class);

        System.out.println("User created successfully!\n");
    }

    private void sendMessage(Scanner scanner, String jwtToken) {

        try {

            System.out.print("Sender id: ");
            Long senderId = Long.parseLong(scanner.nextLine());

            System.out.print("Receiver id: ");
            Long receiverId = Long.parseLong(scanner.nextLine());

            System.out.print("Message: ");
            String text = scanner.nextLine();

            CreateMessageDTO dto =
                    new CreateMessageDTO(senderId, receiverId, text);

            bffClient.post()
                    .uri("/bff/messages")
                    .body(dto)
                    .headers(h -> h.setBearerAuth(jwtToken))
                    .retrieve()
                    .body(MessageDTO.class);

            System.out.println("Message sent successfully!");

        } catch (HttpClientErrorException ex) {

            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                System.out.println("A conversation already exists.");
            }
            else if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                System.out.println("User not found.");
            }
            else {
                System.out.println("Request failed.");
            }

        } catch (Exception e) {
            System.out.println("Could not send message: " + e.getMessage());
        }
    }

    private void getMessages(String jwtToken) {
        List<MessageDTO> messages = bffClient.get()
                .uri("/bff/messages")
                .headers(h -> h.setBearerAuth(jwtToken))
                .retrieve()
                .body(new ParameterizedTypeReference<>(){});

        if (messages == null || messages.isEmpty()) {
            System.out.println("No messages found.");
            return;
        }

        System.out.println("\n==========================================================");
        System.out.println("                   MESSAGES                 ");
        System.out.println("==========================================================");

        for (MessageDTO msg : messages) {
            System.out.printf(" [%s] Sender %s ➔ Receiver %s%n", msg.id(), msg.senderId(), msg.receiverId());
            System.out.printf(" Message: %s%n", msg.text());
            System.out.println("----------------------------------------------------------");
        }
        System.out.println();
    }
}
