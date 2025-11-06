package com.prontudigital.schedule_service.client;


import com.prontudigital.schedule_service.client.fallback.UserServiceFallback;
import com.prontudigital.schedule_service.dto.UserInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "user-service",
        url = "${app.services.user-service.url}",
        fallback = UserServiceFallback.class
)
public interface UserServiceClient {

    @GetMapping("/api/users/{userUuid}")
    UserInfoDTO getUserByUuid(@PathVariable("userUuid") UUID userUuid);

    @PostMapping("/api/users/batch")
    List<UserInfoDTO> getUsersBatch(@RequestBody List<UUID> userUuids);

    @GetMapping("/api/users/{userUuid}/exists")
    Boolean userExists(@PathVariable("userUuid") UUID userUuid);
}