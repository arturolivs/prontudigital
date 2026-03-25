package com.prontudigital.schedule_service.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoDTO {
    private UUID uuid;
    private String username;
    private String email;
    private String fullName;
    private Boolean isActive;
    private List<String> roles;
}