package com.prontudigital.auth_service.service.impl;

import com.prontudigital.auth_service.dto.UserDTO;
import com.prontudigital.auth_service.exception.RoleNotFoundException;
import com.prontudigital.auth_service.exception.UserNameAlreadyExistsException;
import com.prontudigital.auth_service.exception.UserNotFoundException;
import com.prontudigital.auth_service.model.Role;
import com.prontudigital.auth_service.model.User;
import com.prontudigital.auth_service.repository.ProfileRepository;
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
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO findById(UUID id) {
        return userRepository.findById(id)
                .map(this::convertToDTO)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional
    public UserDTO create(UserDTO dto, String rawPassword) {
        validateUserNameUniqueness(dto.getUsername());

        User user = buildUserFromDTO(dto, rawPassword);
        user = userRepository.save(user);

        return convertToDTO(user);
    }

    @Override
    @Transactional
    public UserDTO update(UUID id, UserDTO dto) {
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

    private UserDTO convertToDTO(User user) {
        return UserDTO.builder()
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

    private User buildUserFromDTO(UserDTO dto, String rawPassword) {
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

    private void validateUserNameUniqueness(String userName) {
        if (userRepository.existsByUsername(userName)) {
            throw new UserNameAlreadyExistsException(userName);
        }
    }

    private void updateUserProfiles(User user, Set<String> profileNames) {
        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
        } else {
            if (!(user.getRoles() instanceof HashSet)) {
                user.setRoles(new HashSet<>(user.getRoles()));
            }
        }
        user.getRoles().clear();

        if (profileNames != null && !profileNames.isEmpty()) {
            Set<String> mutableProfileNames = new HashSet<>(profileNames);
            mutableProfileNames.forEach(profileName -> {
                Role role = profileRepository.findByName(profileName)
                        .orElseThrow(() -> new RoleNotFoundException(profileName));
                user.addRole(role);
            });
        }
    }
}