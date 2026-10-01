package com.prontudigital.gateway.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/auth")
    public Mono<String> authFallback() {
        return Mono.just("Auth Service indisponível no momento. Tente novamente mais tarde.");
    }

    @RequestMapping("/fallback/schedule")
    public Mono<String> scheduleFallback() {
        return Mono.just("Schedule Service indisponível no momento. Tente novamente mais tarde.");
    }
}