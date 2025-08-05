package com.prontudigital.user_service.dto;

import lombok.*;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private UUID id;
    private String fullName;
    private String userName;
    private String email;
    private Boolean isActive;
    private Set<String> profiles;
    private Instant createdAt;
    private Instant updatedAt;
}