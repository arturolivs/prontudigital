package com.prontudigital.schedule_service.client.fallback;


import com.prontudigital.schedule_service.client.UserServiceClient;
import com.prontudigital.schedule_service.dto.UserInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class UserServiceFallback implements UserServiceClient {

    @Override
    public UserInfoDTO getUserByUuid(UUID userUuid) {
        log.warn("Fallback acionado para getUserByUuid: {}", userUuid);
        return createFallbackUser(userUuid);
    }

    @Override
    public List<UserInfoDTO> getUsersBatch(List<UUID> userUuids) {
        log.warn("Fallback acionado para getUsersBatch: {} usuários", userUuids.size());
        return userUuids.stream()
                .map(this::createFallbackUser)
                .toList();
    }

    @Override
    public Boolean userExists(UUID userUuid) {
        log.warn("Fallback acionado para userExists: {}", userUuid);
        return false; // Assume que usuário não existe em caso de fallback
    }

    private UserInfoDTO createFallbackUser(UUID userUuid) {
        UserInfoDTO fallbackUser = new UserInfoDTO();
        fallbackUser.setUuid(userUuid);
        fallbackUser.setFullName("Usuário Indisponível");
        fallbackUser.setUserName("indisponivel");
        fallbackUser.setEmail("indisponivel@email.com");
        fallbackUser.setActive(false);
        return fallbackUser;
    }
}