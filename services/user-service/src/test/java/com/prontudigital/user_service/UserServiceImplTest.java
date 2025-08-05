package com.prontudigital.user_service;

import com.prontudigital.user_service.dto.UserDTO;
import com.prontudigital.user_service.exception.ProfileNotFoundException;
import com.prontudigital.user_service.exception.UserNotFoundException;
import com.prontudigital.user_service.exception.UserNameAlreadyExistsException;
import com.prontudigital.user_service.model.Profile;
import com.prontudigital.user_service.model.User;
import com.prontudigital.user_service.repository.ProfileRepository;
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
    private ProfileRepository profileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User sampleUser;
    private UUID userId;
    private Profile nurseProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        nurseProfile = Profile.builder()
                .id(UUID.randomUUID())
                .name("Enfermeiro")
                .build();

        sampleUser = User.builder()
                .id(userId)
                .email("enfermeiro@clinica.com")
                .passwordHash("encodedPassword")
                .fullName("Enfermeiro Teste")
                .isActive(true)
                .profiles(Set.of(nurseProfile))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void findAll_ShouldReturnListOfUserDTOs() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));

        List<UserDTO> result = service.findAll();

        assertEquals(1, result.size());
        assertEquals(sampleUser.getEmail(), result.get(0).getEmail());
        assertEquals(1, result.get(0).getProfiles().size());
        assertTrue(result.get(0).getProfiles().contains("Enfermeiro"));
        verify(userRepository).findAll();
    }

    @Test
    void findById_WithExistingId_ShouldReturnUserDTO() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

        UserDTO result = service.findById(userId);

        assertEquals(userId, result.getId());
        assertEquals(sampleUser.getEmail(), result.getEmail());
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
    void create_WithUniqueEmail_ShouldSaveUser() {
        UserDTO newUserDTO = UserDTO.builder()

                .email("novo@clinica.com")
                .fullName("Novo Usuário")
                .profiles(Set.of("Enfermeiro"))
                .build();

        // Configuração do mock
        when(userRepository.existsByUserName("novo@clinica.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        // Cria um profile mock com users inicializado
        Profile mockNurseProfile = Profile.builder()
                .id(UUID.randomUUID())
                .name("Enfermeiro")
                .users(new HashSet<>()) // Inicializa a coleção
                .build();

        when(profileRepository.findByName("Enfermeiro"))
                .thenReturn(Optional.of(mockNurseProfile));

        // Configura o comportamento do save
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            user.addProfile(mockNurseProfile);
            return user;
        });

        UserDTO result = service.create(newUserDTO, "password");

        // Verificações
        assertNotNull(result.getId());
        assertEquals("novo@clinica.com", result.getEmail());
        assertTrue(result.getProfiles().contains("Enfermeiro"));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_WithExistingEmail_ShouldThrowException() {
        UserDTO existingUserDTO = UserDTO.builder()
                .email("enfermeiro@clinica.com")
                .build();

        when(userRepository.existsByUserName("enfermeiro@clinica.com")).thenReturn(true);

        assertThrows(UserNameAlreadyExistsException.class,
                () -> service.create(existingUserDTO, "password"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void create_WithInvalidProfile_ShouldThrowException() {
        UserDTO newUserDTO = UserDTO.builder()
                .email("novo@clinica.com")
                .profiles(Set.of("ADMIN"))
                .build();

        when(userRepository.existsByUserName("novo@clinica.com")).thenReturn(false);
        when(profileRepository.findByName("ADMIN"))
                .thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class,
                () -> service.create(newUserDTO, "password"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void update_WithExistingId_ShouldUpdateUser() {
        // Cria um profile ADMIN com users inicializado
        Profile adminProfile = Profile.builder()
                .id(UUID.randomUUID())
                .name("ADMIN")
                .users(new HashSet<>()) // Coleção mutável
                .build();

        // Cria um UserDTO com HashSet mutável
        UserDTO updateDTO = UserDTO.builder()
                .email("novoemail@clinica.com")
                .fullName("Nome Atualizado")
                .isActive(false)
                .profiles(new HashSet<>(Set.of("ADMIN"))) // Convertemos para HashSet mutável
                .build();

        // Configura o sampleUser com uma coleção mutável de profiles
        sampleUser.setProfiles(new HashSet<>());

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(profileRepository.findByName("ADMIN"))
                .thenReturn(Optional.of(adminProfile));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);

        UserDTO result = service.update(userId, updateDTO);

        // Verificações
        assertEquals("novoemail@clinica.com", result.getEmail());
        assertEquals("Nome Atualizado", result.getFullName());
        assertFalse(result.getIsActive());
        assertTrue(result.getProfiles().contains("ADMIN"));
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
    void update_WithInvalidProfile_ShouldThrowException() {
        UserDTO updateDTO = UserDTO.builder()
                .profiles(Set.of("ADMIN"))
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(profileRepository.findByName("ADMIN"))
                .thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class,
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
        UUID nonExistingId = UUID.randomUUID();
        when(userRepository.existsById(nonExistingId)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> service.delete(nonExistingId));
        verify(userRepository, never()).deleteById(any());
    }
}