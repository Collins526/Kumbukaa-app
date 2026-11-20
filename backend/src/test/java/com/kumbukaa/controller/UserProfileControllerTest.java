package com.kumbukaa.controller;

import com.kumbukaa.dto.UpdateUserProfileRequest;
import com.kumbukaa.dto.UserProfileResponse;
import com.kumbukaa.dto.MessageResponse;
import com.kumbukaa.entity.User;
import com.kumbukaa.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    @Mock
    private UserRepository userRepository;

    private UserProfileController controller;

    @BeforeEach
    void setUp() {
        controller = new UserProfileController(userRepository);
    }

    @Test
    void updateProfile_normalizesAndSavesCurrentUserDetails() {
        User user = User.builder()
                .id(1L)
                .fullName("Old Name")
                .email("user@example.com")
                .phoneNumber("254700000000")
                .build();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setName("  New Name ");
        request.setEmail(" NEW@example.com ");
        request.setPhoneNumber("+254 712 345 678");
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findByPhoneNumber("254712345678")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.updateProfile(user, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        UserProfileResponse profile = assertInstanceOf(UserProfileResponse.class, response.getBody());
        assertEquals("New Name", profile.getName());
        assertEquals("new@example.com", profile.getEmail());
        assertEquals("254712345678", profile.getPhoneNumber());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_rejectsEmailOwnedByAnotherUser() {
        User user = User.builder().id(1L).build();
        User otherUser = User.builder().id(2L).build();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setName("New Name");
        request.setEmail("taken@example.com");
        request.setPhoneNumber("254712345678");
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(otherUser));

        ResponseEntity<?> response = controller.updateProfile(user, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        MessageResponse error = assertInstanceOf(MessageResponse.class, response.getBody());
        assertEquals("Email is already registered", error.getMessage());
    }
}
