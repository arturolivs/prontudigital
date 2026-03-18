package com.prontudigital.gateway.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.function.Predicate;

public class RecordFailurePredicate implements Predicate<Throwable> {

    @Override
    public boolean test(Throwable throwable) {
        // Não registrar como falha erros 4xx (cliente)
        if (throwable instanceof ResponseStatusException) {
            HttpStatus status = (HttpStatus) ((ResponseStatusException) throwable).getStatusCode();
            return status.is5xxServerError();
        }
        return true;
    }
}