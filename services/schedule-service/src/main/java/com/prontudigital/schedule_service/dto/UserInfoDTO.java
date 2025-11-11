package com.prontudigital.schedule_service.dto;


import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserInfoDTO {
    private UUID uuid;
    private String username;
    private String email;
    private String fullName;
    private Boolean isActive;
    private List<String> roles;
}