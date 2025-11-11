package com.prontudigital.schedule_service.config;

import com.prontudigital.schedule_service.client.decoder.UserServiceErrorDecoder;
import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public UserServiceErrorDecoder userServiceErrorDecoder() {
        return new UserServiceErrorDecoder();
    }
}