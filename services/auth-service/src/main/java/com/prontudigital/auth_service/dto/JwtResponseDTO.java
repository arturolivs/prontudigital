package com.prontudigital.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtResponseDTO {
    private String token;
    private String type = "Bearer";
    private String username;
    private Collection<String> roles;

    public JwtResponseDTO(String token,String username,Collection<String> roles) {
        this.token = token;
        this.username = username;
        this.roles = roles;
    }
}