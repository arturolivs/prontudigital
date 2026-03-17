package com.prontudigital.schedule_service.client.fallback;


import com.prontudigital.schedule_service.client.UserServiceClient;
import com.prontudigital.schedule_service.dto.UserInfoDTO;
import com.prontudigital.schedule_service.exception.UserNotFoundException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class UserServiceFallbackFactory implements FallbackFactory<UserServiceClient> {

    @Override
    public UserServiceClient create(Throwable cause) {
        log.error("Erro no Feign client para user-service: {}", cause.getMessage());

        return new UserServiceClient() {
            @Override
            public UserInfoDTO getUserByUuid(UUID userUuid) {
                UserInfoDTO fallbackUser = new UserInfoDTO();
                if (cause instanceof FeignException.NotFound) {
                    log.warn("Usuário {} não encontrado no auth-service", userUuid);
                    throw new UserNotFoundException("Usuário não encontrado: " + userUuid);
                } else if (cause instanceof FeignException.ServiceUnavailable) {
                    log.error("Serviço de usuário indisponível");
                    fallbackUser.setUuid(userUuid);
                    fallbackUser.setFullName("Usuário Indisponível");
                    fallbackUser.setUsername("indisponivel");
                    fallbackUser.setEmail("indisponivel@email.com");
                    fallbackUser.setIsActive(false);
                    return fallbackUser;
                }
                fallbackUser.setUuid(userUuid);
                fallbackUser.setFullName("Usuário Indisponível");
                fallbackUser.setUsername("indisponivel");
                fallbackUser.setEmail("indisponivel@email.com");
                fallbackUser.setIsActive(false);
                return fallbackUser;
            }

            @Override
            public UserInfoDTO getCurrentUser() {
                UserInfoDTO fallbackUser = new UserInfoDTO();
                if (cause instanceof FeignException.NotFound) {
                    log.warn("Usuário não encontrado no auth-service");
                    throw new UserNotFoundException("Usuário não encontrado: ");
                } else if (cause instanceof FeignException.ServiceUnavailable) {
                    log.error("Serviço de usuário indisponível");
                    fallbackUser.setUuid(UUID.randomUUID());
                    fallbackUser.setFullName("Usuário Indisponível");
                    fallbackUser.setUsername("indisponivel");
                    fallbackUser.setEmail("indisponivel@email.com");
                    fallbackUser.setIsActive(false);
                    return fallbackUser;
                }
                fallbackUser.setUuid(UUID.randomUUID());
                fallbackUser.setFullName("Usuário Indisponível");
                fallbackUser.setUsername("indisponivel");
                fallbackUser.setEmail("indisponivel@email.com");
                fallbackUser.setIsActive(false);
                return fallbackUser;
            }
        };
    }
}