package com.prontudigital.schedule_service.client.fallback;


import com.prontudigital.schedule_service.client.UserServiceClient;
import com.prontudigital.schedule_service.dto.UserInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class UserServiceFallback implements UserServiceClient {

    @Override
    public UserInfoDTO getUserByUuid(UUID userUuid) {
        log.warn("Fallback acionado para getUserByUuid: {}", userUuid);
        return createFallbackUser(userUuid);
    }

    private UserInfoDTO createFallbackUser(UUID userUuid) {
        UserInfoDTO fallbackUser = new UserInfoDTO();
        fallbackUser.setUuid(userUuid);
        fallbackUser.setFullName("Usuário Indisponível");
        fallbackUser.setUsername("indisponivel");
        fallbackUser.setEmail("indisponivel@email.com");
        fallbackUser.setIsActive(false);
        return fallbackUser;
    }
}