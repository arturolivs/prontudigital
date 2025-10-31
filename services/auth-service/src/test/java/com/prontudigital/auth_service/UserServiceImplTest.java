package com.prontudigital.auth_service;

import com.prontudigital.auth_service.dto.UserResponseDTO;
import com.prontudigital.auth_service.exception.RoleNotFoundException;
import com.prontudigital.auth_service.exception.UserNameAlreadyExistsException;
import com.prontudigital.auth_service.exception.UserNotFoundException;
import com.prontudigital.auth_service.entity.Role;
import com.prontudigital.auth_service.entity.User;
import com.prontudigital.auth_service.repository.RoleRepository;
import com.prontudigital.auth_service.repository.UserRepository;
import com.prontudigital.auth_service.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;

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
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User sampleUser;
    private Long userId;
    private Role nurseRole;

    @BeforeEach
    void setUp() {
        userId = 1L;
        nurseRole = Role.builder()
                .id(1L)
                .name("Enfermeiro")
                .build();

        sampleUser = User.builder()
                .id(userId)
                .email("enfermeiro@clinica.com")
                .passwordHash("encodedPassword")
                .fullName("Enfermeiro Teste")
                .isActive(true)
                .roles(Set.of(nurseRole))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void findAll_ShouldReturnListOfUserDTOs() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserResponseDTO> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals(sampleUser.getEmail(), result.get(0).getEmail());
        assertEquals(1, result.get(0).getRoles().size());
        assertTrue(result.get(0).getRoles().contains("Enfermeiro"));
        verify(userRepository).findAll();
    }

    @Test
    void findById_WithExistingId_ShouldReturnUserDTO() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        UserResponseDTO result = service.findById(userId);

        assertEquals(userId, result.getId());
        assertEquals(sampleUser.getEmail(), result.getEmail());
        verify(userRepository).findById(userId);
    }

    @Test
    void findById_WithNonExistingId_ShouldThrowException() {
        Long nonExistingId = 1L;
        when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.findById(nonExistingId));
        verify(userRepository).findById(nonExistingId);
    }

    @Test
    void create_WithUniqueEmail_ShouldSaveUser() {
        UserResponseDTO newUserResponseDTO = UserResponseDTO.builder()

                .email("novo@clinica.com")
                .fullName("Novo Usuário")
                .roles(Set.of("Enfermeiro"))
                .build();

        when(userRepository.existsByUsername("novo@clinica.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        Role mockNurseRole = Role.builder()
                .id(1L)
                .name("Enfermeiro")
                .users(new HashSet<>())
                .build();

        when(roleRepository.findByName("Enfermeiro"))
                .thenReturn(Optional.of(mockNurseRole));

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            user.addRole(mockNurseRole);
            return user;
        });

        UserResponseDTO result = service.create(newUserResponseDTO, "password");

        assertNotNull(result.getId());
        assertEquals("novo@clinica.com", result.getEmail());
        assertTrue(result.getRoles().contains("Enfermeiro"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_WithExistingEmail_ShouldThrowException() {
        UserResponseDTO existingUserResponseDTO = UserResponseDTO.builder()
                .email("enfermeiro@clinica.com")
                .build();

        when(userRepository.existsByUsername("enfermeiro@clinica.com")).thenReturn(true);

        assertThrows(UserNameAlreadyExistsException.class,
                () -> service.create(existingUserResponseDTO, "password"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_WithInvalidProfile_ShouldThrowException() {
        UserResponseDTO newUserResponseDTO = UserResponseDTO.builder()
                .email("novo@clinica.com")
                .roles(Set.of("ADMIN"))
                .build();

        when(userRepository.existsByUsername("novo@clinica.com")).thenReturn(false);
        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class,
                () -> service.create(newUserResponseDTO, "password"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_WithExistingId_ShouldUpdateUser() {
        Role adminRole = Role.builder()
                .id(1L)
                .name("ADMIN")
                .users(new HashSet<>()) 
                .build();

        UserResponseDTO updateDTO = UserResponseDTO.builder()
                .email("novoemail@clinica.com")
                .fullName("Nome Atualizado")
                .isActive(false)
                .roles(new HashSet<>(Set.of("ADMIN")))
                .build();

        sampleUser.setRoles(new HashSet<>());

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.of(adminRole));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);

        UserResponseDTO result = service.update(userId, updateDTO);

        assertEquals("novoemail@clinica.com", result.getEmail());
        assertEquals("Nome Atualizado", result.getFullName());
        assertFalse(result.getIsActive());
        assertTrue(result.getRoles().contains("ADMIN"));
        verify(userRepository).save(sampleUser);
    }

    @Test
    void update_WithNonExistingId_ShouldThrowException() {
        Long nonExistingId = 1L;
        when(userRepository.findById(nonExistingId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.update(nonExistingId, new UserResponseDTO()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_WithInvalidProfile_ShouldThrowException() {
        UserResponseDTO updateDTO = UserResponseDTO.builder()
                .roles(Set.of("ADMIN"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class,
                () -> service.update(userId, updateDTO));
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
        Long nonExistingId = 1L;
        when(userRepository.existsById(nonExistingId)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> service.delete(nonExistingId));
        verify(userRepository, never()).deleteById(any());
    }
}