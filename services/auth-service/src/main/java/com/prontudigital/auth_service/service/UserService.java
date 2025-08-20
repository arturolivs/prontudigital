package com.prontudigital.auth_service.service;

import com.prontudigital.auth_service.dto.UserResponseDTO;

import java.util.List;
import java.util.UUID;

public interface UserService {
    List<UserResponseDTO> findAll();
    UserResponseDTO findById(UUID id);
    UserResponseDTO create(UserResponseDTO dto, String rawPassword);
    UserResponseDTO update(UUID id, UserResponseDTO dto);
    void delete(UUID id);
}
