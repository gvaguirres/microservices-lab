package org.example.userservice.service;

import io.grpc.Status;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.entity.User;
import org.example.userservice.exception.ResourceNotFoundException;
import org.springframework.grpc.server.service.GrpcService;
import io.grpc.stub.StreamObserver;
import org.example.grpc.UserProfileRequest;
import org.example.grpc.UserProfileResponse;
import org.example.grpc.UserProfileServiceGrpc;

@GrpcService
public class UserGrpcService extends UserProfileServiceGrpc.UserProfileServiceImplBase {

    private final UserRepository userRepository;

    public UserGrpcService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void getUserProfile(UserProfileRequest request,
                               StreamObserver<UserProfileResponse> responseObserver) {

        try {
            Long userId = request.getUserId();
            User user = userRepository.findById(userId).orElseThrow();

            UserProfileResponse response = UserProfileResponse.newBuilder()
                    .setUserId(user.getId())
                    .setFirstName(user.getFirstName())
                    .setLastName(user.getLastName())
                    .setEmail(user.getEmail())
                    .setPhoneNumber(user.getPhoneNumber())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ResourceNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(e.getMessage())
                            .asRuntimeException()
            );
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .asRuntimeException()
            );
        }
    }
}
