package com.prontudigital.auth_service.dto;

import lombok.*;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private UUID id;
    private String fullName;
    private String username;
    private String email;
    private Boolean isActive;
    private Set<String> roles;
    private Instant createdAt;
    private Instant updatedAt;
}