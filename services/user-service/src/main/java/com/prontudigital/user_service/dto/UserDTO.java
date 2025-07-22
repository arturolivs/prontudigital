package com.prontudigital.user_service.dto;

import com.prontudigital.user_service.model.Role;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private UUID id;
    private String username;
    private Role role;
    private Instant createdAt;
    private Instant updatedAt;
}
