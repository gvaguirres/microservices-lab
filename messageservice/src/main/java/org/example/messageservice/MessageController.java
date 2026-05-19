package org.example.messageservice;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.example.grpc.UserProfileRequest;
import org.example.grpc.UserProfileResponse;
import org.example.grpc.UserProfileServiceGrpc;

@RestController
public class MessageController {

    final UserProfileServiceGrpc.UserProfileServiceBlockingStub stub;

    public MessageController(UserProfileServiceGrpc.UserProfileServiceBlockingStub stub) {
        this.stub = stub;
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
}
