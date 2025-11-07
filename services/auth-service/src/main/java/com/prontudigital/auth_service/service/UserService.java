package com.prontudigital.auth_service.service;

import com.prontudigital.auth_service.dto.UserResponseDTO;

import java.util.List;
import java.util.UUID;

public interface UserService {
    List<UserResponseDTO> findAll();
    UserResponseDTO findById(Long id);
    UserResponseDTO findByUuId(UUID id);
    UserResponseDTO create(UserResponseDTO dto, String rawPassword);
    UserResponseDTO update(Long id, UserResponseDTO dto);
    void delete(Long id);
}
