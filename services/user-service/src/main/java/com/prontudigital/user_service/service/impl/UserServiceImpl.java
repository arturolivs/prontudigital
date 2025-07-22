package com.prontudigital.user_service.service.impl;

import com.prontudigital.user_service.dto.UserDTO;
import com.prontudigital.user_service.exception.UserNotFoundException;
import com.prontudigital.user_service.exception.UsernameAlreadyExistsException;
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

    private final UserRepository userRepository;
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
        validateUsernameUniqueness(dto.getUsername());

        User user = buildUserFromDTO(dto, rawPassword);
        user = userRepository.save(user);

        return convertToDTO(user);
    }

    @Override
    @Transactional
    public UserDTO update(UUID id, UserDTO dto) {
        User user = getUserById(id);
        user.setRole(dto.getRole());

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
                .username(user.getUsername())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private User buildUserFromDTO(UserDTO dto, String rawPassword) {
        return User.builder()
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(dto.getRole())
                .build();
    }

    private User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    private void validateUsernameUniqueness(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException(username);
        }
    }
}