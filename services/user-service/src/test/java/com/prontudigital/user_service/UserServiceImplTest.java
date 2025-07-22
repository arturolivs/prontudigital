package com.prontudigital.user_service;


import com.prontudigital.user_service.dto.UserDTO;
import com.prontudigital.user_service.model.Role;
import com.prontudigital.user_service.model.User;
import com.prontudigital.user_service.repository.UserRepository;
import com.prontudigital.user_service.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl service;

    @Mock
    private UserRepository repo;

    @Mock
    private PasswordEncoder encoder;

    private User sample;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sample = User.builder()
                .id(UUID.randomUUID())
                .username("john")
                .passwordHash("hashed")
                .role(Role.NURSE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void findAll_returnsMappedDTOs() {
        when(repo.findAll()).thenReturn(List.of(sample));

        var dtos = service.findAll();

        assertEquals(1, dtos.size());
        assertEquals("john", dtos.get(0).getUsername());
        verify(repo).findAll();
    }

    @Test
    void findById_existing_returnsDTO() {
        when(repo.findById(sample.getId())).thenReturn(Optional.of(sample));

        var dto = service.findById(sample.getId());

        assertEquals(sample.getId(), dto.getId());
        verify(repo).findById(sample.getId());
    }

    @Test
    void findById_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.findById(id));
        verify(repo).findById(id);
    }

    @Test
    void create_uniqueUsername_savesAndPublishesEvent() {
        UserDTO dto = UserDTO.builder()
                .username("john")
                .role(Role.NURSE)
                .build();
        when(repo.existsByUsername("john")).thenReturn(false);
        when(encoder.encode("pwd")).thenReturn("hashed");
        when(repo.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(sample.getId());
            u.setCreatedAt(Instant.now());
            return u;
        });

        var result = service.create(dto, "pwd");

        assertEquals("john", result.getUsername());
        assertEquals(Role.NURSE, result.getRole());
        verify(repo).save(any(User.class));
    }

    @Test
    void create_duplicateUsername_throwsException() {
        UserDTO dto = UserDTO.builder().username("john").role(Role.NURSE).build();
        when(repo.existsByUsername("john")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> service.create(dto, "pwd"));
        verify(repo, never()).save(any());
    }

    @Test
    void update_existing_updatesAndPublishes() {
        UserDTO dto = UserDTO.builder().role(Role.ADMIN).build();
        when(repo.findById(sample.getId())).thenReturn(Optional.of(sample));
        when(repo.save(any())).thenReturn(sample);

        var updated = service.update(sample.getId(), dto);

        assertEquals(Role.ADMIN, updated.getRole());
        verify(repo).save(sample);
    }

    @Test
    void update_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.update(id, new UserDTO()));
    }

    @Test
    void delete_existing_deletesAndPublishes() {
        when(repo.existsById(sample.getId())).thenReturn(true);

        service.delete(sample.getId());

        verify(repo).deleteById(sample.getId());
    }

    @Test
    void delete_notFound_throwsException() {
        UUID id = UUID.randomUUID();
        when(repo.existsById(id)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> service.delete(id));
        verify(repo, never()).deleteById(id);
    }
}