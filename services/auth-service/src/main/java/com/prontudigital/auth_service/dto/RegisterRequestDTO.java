package com.prontudigital.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequestDTO {
    private String fullName;
    private String email;
    private String username;
    private String password;
    private Set<String> profiles;
}