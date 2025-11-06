package com.prontudigital.schedule_service.dto;


import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserInfoDTO {
    private UUID uuid;
    private String userName;
    private String email;
    private String fullName;
    private Boolean active;
    private List<String> roles;
}