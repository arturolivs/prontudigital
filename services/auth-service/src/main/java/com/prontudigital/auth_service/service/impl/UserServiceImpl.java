package com.prontudigital.auth_service.service.impl;

import com.prontudigital.auth_service.dto.UserResponseDTO;
import com.prontudigital.auth_service.exception.EmailAlreadyExistsException;
import com.prontudigital.auth_service.exception.RoleNotFoundException;
import com.prontudigital.auth_service.exception.UserNameAlreadyExistsException;
import com.prontudigital.auth_service.exception.UserNotFoundException;
import com.prontudigital.auth_service.model.Role;
import com.prontudigital.auth_service.model.User;
import com.prontudigital.auth_service.repository.RoleRepository;
import com.prontudigital.auth_service.repository.UserRepository;
import com.prontudigital.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {
        return userRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional
    public UserResponseDTO create(UserResponseDTO dto, String rawPassword) {
        validate(dto);

        User user = buildUserFromDTO(dto, rawPassword);
        user = userRepository.save(user);

        return convertToDTO(user);
    }

    @Override
    @Transactional
    public UserResponseDTO update(UUID id, UserResponseDTO dto) {
        User user = getUserById(id);

        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setIsActive(dto.getIsActive());

        updateUserProfiles(user, dto.getRoles());

        return convertToDTO(userRepository.save(user));
    }
    @Override
    @Transactional
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }

    private UserResponseDTO convertToDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .isActive(user.getIsActive())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private User buildUserFromDTO(UserResponseDTO dto, String rawPassword) {
        User user = User.builder()
                .email(dto.getEmail())
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .fullName(dto.getFullName())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        if (dto.getRoles() != null) {
            updateUserProfiles(user, dto.getRoles());
        }

        return user;
    }

    private User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private void validate(UserResponseDTO dto) {
        validateUserNameUniqueness(dto.getUsername());
        validateEmailUniqueness(dto.getEmail());
    }

    private void validateUserNameUniqueness(String userName) {
        if (userRepository.existsByUsername(userName)) {
            throw new UserNameAlreadyExistsException(userName);
        }
    }

    private void validateEmailUniqueness(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    private void updateUserProfiles(User user, Set<String> roles) {
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        } else {
            if (!(user.getRoles() instanceof HashSet)) {
                user.setRoles(new HashSet<>(user.getRoles()));
            }
        }
        user.getRoles().clear();

        if (roles != null && !roles.isEmpty()) {
            Set<String> mutableRoleNames = new HashSet<>(roles);
            mutableRoleNames.forEach(roleName -> {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new RoleNotFoundException(roleName));
                user.addRole(role);
            });
        }
    }
}