package com.prontudigital.schedule_service.client.decoder;

import com.prontudigital.schedule_service.exception.UserNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Slf4j
public class UserServiceErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {

        // Extrai o corpo da resposta para log e mensagem
        String responseBody = extractResponseBody(response);

        log.warn("Erro na chamada ao User Service - Status: {}, Método: {}",
                response.status(), methodKey);

        if (response.status() == HttpStatus.NOT_FOUND.value()) {
            log.warn("Usuário não encontrado no User Service. Response: {}", responseBody);
            return new UserNotFoundException(extractUserUuidFromResponse(responseBody));
        }

        return switch (response.status()) {
            case 400 -> new IllegalArgumentException("Requisição inválida para User Service: " + responseBody);
            case 401, 403 -> new SecurityException("Acesso não autorizado ao User Service");
            case 500 -> new RuntimeException("Erro interno no User Service: " + responseBody);
            default -> defaultErrorDecoder.decode(methodKey, response);
        };
    }

    private String extractResponseBody(Response response) {
        try {
            if (response.body() != null) {
                return new BufferedReader(new InputStreamReader(response.body().asInputStream()))
                        .lines()
                        .collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            log.warn("Não foi possível extrair corpo da resposta de erro", e);
        }
        return "Corpo não disponível";
    }

    private String extractUserUuidFromResponse(String responseBody) {
        try {
            // Extrai UUID do corpo da resposta JSON
            // Exemplo: "Usuário não encontrado com UUID: 6fbe4cc3-ccaf-4e1b-ae2c-a98fe83ac1e0"
            if (responseBody.contains("UUID:")) {
                String[] parts = responseBody.split("UUID:");
                if (parts.length > 1) {
                    return parts[1].trim().replaceAll("\"", "").split("}")[0].trim();
                }
            }
        } catch (Exception e) {
            log.debug("Não foi possível extrair UUID do corpo da resposta");
        }
        return "UUID não identificado";
    }
}
