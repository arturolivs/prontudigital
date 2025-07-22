package com.prontudigital.user_service;

import com.prontudigital.user_service.dto.UserDTO;
import com.prontudigital.user_service.exception.UserNotFoundException;
import com.prontudigital.user_service.exception.UsernameAlreadyExistsException;
import com.prontudigital.user_service.model.Role;
import com.prontudigital.user_service.model.User;
import com.prontudigital.user_service.repository.UserRepository;
import com.prontudigital.user_service.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl service;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User sampleUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(userId)
                .username("usuario.teste")
                .passwordHash("encodedPassword")
                .role(Role.NURSE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void findAll_ShouldReturnListOfUserDTOs() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserDTO> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals(sampleUser.getUsername(), result.get(0).getUsername());
        verify(userRepository).findAll();
    }

    @Test
    void findById_WithExistingId_ShouldReturnUserDTO() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        UserDTO result = service.findById(userId);

        assertEquals(userId, result.getId());
        verify(userRepository).findById(userId);
    }

    @Test
    void findById_WithNonExistingId_ShouldThrowException() {
        UUID nonExistingId = UUID.randomUUID();
        when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.findById(nonExistingId));
        verify(userRepository).findById(nonExistingId);
    }

    @Test
    void create_WithUniqueUsername_ShouldSaveUser() {
        UserDTO newUserDTO = UserDTO.builder()
                .username("novo.usuario")
                .role(Role.ADMIN)
                .build();

        when(userRepository.existsByUsername("novo.usuario")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        UserDTO result = service.create(newUserDTO, "password");

        assertNotNull(result.getId());
        assertEquals("novo.usuario", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_WithExistingUsername_ShouldThrowException() {
        UserDTO existingUserDTO = UserDTO.builder()
                .username("usuario.teste")
                .role(Role.NURSE)
                .build();

        when(userRepository.existsByUsername("usuario.teste")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class,
                () -> service.create(existingUserDTO, "password"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_WithExistingId_ShouldUpdateUser() {
        UserDTO updateDTO = UserDTO.builder().role(Role.ADMIN).build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);

        UserDTO result = service.update(userId, updateDTO);

        assertEquals(Role.ADMIN, result.getRole());
        verify(userRepository).save(sampleUser);
    }

    @Test
    void update_WithNonExistingId_ShouldThrowException() {
        UUID nonExistingId = UUID.randomUUID();
        when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.update(nonExistingId, new UserDTO()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void delete_WithExistingId_ShouldDeleteUser() {
        when(userRepository.existsById(userId)).thenReturn(true);

        service.delete(userId);

        verify(userRepository).deleteById(userId);
    }

    @Test
    void delete_WithNonExistingId_ShouldThrowException() {
        UUID nonExistingId = UUID.randomUUID();
        when(userRepository.existsById(nonExistingId)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> service.delete(nonExistingId));
        verify(userRepository, never()).deleteById(any());
    }
}