package com.prontudigital.auth_service;

import com.prontudigital.auth_service.controller.UserController;
import com.prontudigital.auth_service.dto.UserResponseDTO;
import com.prontudigital.auth_service.exception.UserNotFoundException;
import com.prontudigital.auth_service.exception.EmailAlreadyExistsException;
import com.prontudigital.auth_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private final UUID userId = UUID.randomUUID();
    private final UserResponseDTO sampleUserResponseDTO = UserResponseDTO.builder()
            .id(userId)
            .build();

    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        when(userService.findAll()).thenReturn(List.of(sampleUserResponseDTO));

        ResponseEntity<List<UserResponseDTO>> response = userController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(userService).findAll();
    }

    @Test
    void getUserById_WithValidId_ShouldReturnUser() {
        when(userService.findById(userId)).thenReturn(sampleUserResponseDTO);

        ResponseEntity<UserResponseDTO> response = userController.getUserById(userId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody().getId());
        verify(userService).findById(userId);
    }

    @Test
    void getUserById_WithInvalidId_ShouldThrowException() {
        when(userService.findById(userId)).thenThrow(new UserNotFoundException(userId));

        assertThrows(UserNotFoundException.class, () -> userController.getUserById(userId));
        verify(userService).findById(userId);
    }

    @Test
    void createUser_WithDuplicateUsername_ShouldThrowException() {
        when(userService.create(any(UserResponseDTO.class), anyString()))
                .thenThrow(new EmailAlreadyExistsException("test.user"));

        assertThrows(EmailAlreadyExistsException.class,
                () -> userController.createUser(sampleUserResponseDTO, "rawPassword123"));
        verify(userService).create(any(UserResponseDTO.class), anyString());
    }

    @Test
    void updateUser_WithValidData_ShouldReturnUpdatedUser() {
        when(userService.update(userId, sampleUserResponseDTO)).thenReturn(sampleUserResponseDTO);

        ResponseEntity<UserResponseDTO> response = userController.updateUser(userId, sampleUserResponseDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody().getId());
        verify(userService).update(userId, sampleUserResponseDTO);
    }

    @Test
    void updateUser_WithInvalidId_ShouldThrowException() {
        when(userService.update(userId, sampleUserResponseDTO))
                .thenThrow(new UserNotFoundException(userId));

        assertThrows(UserNotFoundException.class,
                () -> userController.updateUser(userId, sampleUserResponseDTO));
        verify(userService).update(userId, sampleUserResponseDTO);
    }

    @Test
    void deleteUser_WithValidId_ShouldReturnNoContent() {
        doNothing().when(userService).delete(userId);

        ResponseEntity<Void> response = userController.deleteUser(userId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService).delete(userId);
    }

    @Test
    void deleteUser_WithInvalidId_ShouldThrowException() {
        doThrow(new UserNotFoundException(userId)).when(userService).delete(userId);

        assertThrows(UserNotFoundException.class,
                () -> userController.deleteUser(userId));
        verify(userService).delete(userId);
    }
}