package com.prontudigital.user_service;

import com.prontudigital.user_service.controller.UserController;
import com.prontudigital.user_service.dto.UserDTO;
import com.prontudigital.user_service.exception.UserNotFoundException;
import com.prontudigital.user_service.exception.UserNameAlreadyExistsException;
import com.prontudigital.user_service.service.UserService;
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
    private final UserDTO sampleUserDTO = UserDTO.builder()
            .id(userId)
          //  .username("test.user")
       //     .profileName(ProfileName.ADMIN)
            .build();

    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        // Arrange
        when(userService.findAll()).thenReturn(List.of(sampleUserDTO));

        // Act
        ResponseEntity<List<UserDTO>> response = userController.getAllUsers();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
       // assertEquals("test.user", response.getBody().get(0).getUsername());
        verify(userService).findAll();
    }

    @Test
    void getUserById_WithValidId_ShouldReturnUser() {
        // Arrange
        when(userService.findById(userId)).thenReturn(sampleUserDTO);

        // Act
        ResponseEntity<UserDTO> response = userController.getUserById(userId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody().getId());
        verify(userService).findById(userId);
    }

    @Test
    void getUserById_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(userService.findById(userId)).thenThrow(new UserNotFoundException(userId));

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userController.getUserById(userId));
        verify(userService).findById(userId);
    }
/*
    @Test
    void createUser_WithValidData_ShouldReturnCreated() {
        // Arrange
        UserDTO newUserDTO = UserDTO.builder()
                .username("new.user")
                .role(Role.ADMIN)
                .build();

        UserDTO createdUserDTO = UserDTO.builder()
                .id(UUID.randomUUID())
                .username("new.user")
                .role(Role.ADMIN)
                .build();

        when(userService.create(any(UserDTO.class), anyString())).thenReturn(createdUserDTO);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.newInstance();

        // Act
        ResponseEntity<UserDTO> response = userController.createUser(
                newUserDTO,
                "password123",
                uriBuilder);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().contains(createdUserDTO.getId().toString()));
        assertEquals("new.user", response.getBody().getUsername());
    }
*/
    @Test
    void createUser_WithDuplicateUsername_ShouldThrowException() {
        // Arrange
        when(userService.create(any(UserDTO.class), anyString()))
                .thenThrow(new UserNameAlreadyExistsException("test.user"));

        // Act & Assert
        assertThrows(UserNameAlreadyExistsException.class,
                () -> userController.createUser(sampleUserDTO, "rawPassword123"));
        verify(userService).create(any(UserDTO.class), anyString());
    }

    @Test
    void updateUser_WithValidData_ShouldReturnUpdatedUser() {
        // Arrange
        when(userService.update(userId, sampleUserDTO)).thenReturn(sampleUserDTO);

        // Act
        ResponseEntity<UserDTO> response = userController.updateUser(userId, sampleUserDTO);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(userId, response.getBody().getId());
        verify(userService).update(userId, sampleUserDTO);
    }

    @Test
    void updateUser_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(userService.update(userId, sampleUserDTO))
                .thenThrow(new UserNotFoundException(userId));

        // Act & Assert
        assertThrows(UserNotFoundException.class,
                () -> userController.updateUser(userId, sampleUserDTO));
        verify(userService).update(userId, sampleUserDTO);
    }

    @Test
    void deleteUser_WithValidId_ShouldReturnNoContent() {
        // Arrange
        doNothing().when(userService).delete(userId);

        // Act
        ResponseEntity<Void> response = userController.deleteUser(userId);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService).delete(userId);
    }

    @Test
    void deleteUser_WithInvalidId_ShouldThrowException() {
        // Arrange
        doThrow(new UserNotFoundException(userId)).when(userService).delete(userId);

        // Act & Assert
        assertThrows(UserNotFoundException.class,
                () -> userController.deleteUser(userId));
        verify(userService).delete(userId);
    }
}