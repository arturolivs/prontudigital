package com.prontudigital.user_service.service.impl;


import com.prontudigital.user_service.dto.UserDTO;
import com.prontudigital.user_service.model.User;
import com.prontudigital.user_service.repository.UserRepository;
import com.prontudigital.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    @Override
    public List<UserDTO> findAll() {
        return repo.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO findById(UUID id) {
        return repo.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    @Override
    @Transactional
    public UserDTO create(UserDTO dto, String rawPassword) {
        if (repo.existsByUsername(dto.getUsername()))
            throw new RuntimeException("Username já existe");

        User user = User.builder()
                .username(dto.getUsername())
                .passwordHash(encoder.encode(rawPassword))
                .role(dto.getRole())
                .build();
        user = repo.save(user);

        return toDTO(user);
    }

    @Override
    @Transactional
    public UserDTO update(UUID id, UserDTO dto) {
        User user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        user.setRole(dto.getRole());
        user = repo.save(user);

        return toDTO(user);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!repo.existsById(id))
            throw new RuntimeException("Usuário não encontrado");
        repo.deleteById(id);
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
