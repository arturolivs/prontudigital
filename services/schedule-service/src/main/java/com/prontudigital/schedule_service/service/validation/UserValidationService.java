package com.prontudigital.schedule_service.service.validation;


import com.prontudigital.schedule_service.client.UserServiceClient;
import com.prontudigital.schedule_service.dto.UserInfoDTO;
import com.prontudigital.schedule_service.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserValidationService {

    private final UserServiceClient userServiceClient;

    public void validateUserExists(UUID userUuid) {
        try {
            UserInfoDTO user = userServiceClient.getUserByUuid(userUuid);

            if (user == null) {
                log.warn("Usuário não encontrado - UUID: {}", userUuid);
                throw new UserNotFoundException(userUuid);
            }

            if (!user.getIsActive()) {
                log.warn("Usuário inativo - UUID: {}", userUuid);
                throw new IllegalArgumentException("Usuário está inativo: " + userUuid);
            }

            log.debug("Usuário validado com sucesso - UUID: {}, Nome: {}",
                    userUuid, user.getFullName());

        } catch (UserNotFoundException e) {
            // ✅ Agora esta exceção será lançada pelo ErrorDecoder
            log.warn("Usuário não encontrado via Feign Client - UUID: {}", userUuid);
            throw e;
        } catch (Exception e) {
            log.error("Erro ao validar usuário - UUID: {}, Erro: {}", userUuid, e.getMessage());
            throw new RuntimeException("Erro ao validar usuário: " + e.getMessage(), e);
        }
    }

    // Método alternativo que retorna o usuário se existir
    public UserInfoDTO getUserIfExists(UUID userUuid) {
        try {
            UserInfoDTO user = userServiceClient.getUserByUuid(userUuid);

            if (user == null) {
                throw new UserNotFoundException(userUuid);
            }

            if (!user.getIsActive()) {
                throw new IllegalArgumentException("Usuário está inativo: " + userUuid);
            }

            return user;

        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao buscar usuário - UUID: {}", userUuid, e);
            throw new RuntimeException("Erro ao buscar usuário: " + e.getMessage(), e);
        }
    }
}