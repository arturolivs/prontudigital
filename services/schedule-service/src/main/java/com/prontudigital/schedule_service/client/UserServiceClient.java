package com.prontudigital.schedule_service.client;


import com.prontudigital.schedule_service.client.fallback.UserServiceFallbackFactory;
import com.prontudigital.schedule_service.config.FeignConfig;
import com.prontudigital.schedule_service.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(
        name = "user-service",
        url = "${app.services.user-service.url}",
        fallbackFactory = UserServiceFallbackFactory.class,
        configuration = FeignConfig.class
)
public interface UserServiceClient {

    @GetMapping("/api/auth/v1/users/uuid/{userUuid}")
    UserInfoDTO getUserByUuid(@PathVariable("userUuid") UUID userUuid);
}