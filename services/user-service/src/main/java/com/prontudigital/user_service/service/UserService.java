package com.prontudigital.user_service.service;

import com.prontudigital.user_service.dto.UserDTO;

import java.util.List;
import java.util.UUID;

public interface UserService {
    List<UserDTO> findAll();
    UserDTO findById(UUID id);
    UserDTO create(UserDTO dto, String rawPassword);
    UserDTO update(UUID id, UserDTO dto);
    void delete(UUID id);
}
