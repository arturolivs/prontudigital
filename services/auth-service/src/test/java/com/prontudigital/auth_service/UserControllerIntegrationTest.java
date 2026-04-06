package com.prontudigital.auth_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prontudigital.auth_service.controller.UserController;
import com.prontudigital.auth_service.dto.UserResponseDTO;
import com.prontudigital.auth_service.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private final Long userId = 1L;
    private final UserResponseDTO sampleUserResponseDTO = UserResponseDTO.builder()
            .id(userId)
            //.username("test.user")
          //  .profileName(ProfileName.ADMIN)
            .build();

    @Test
    void getAllUsers_ShouldReturnUsers() throws Exception {
        given(userService.findAll()).willReturn(List.of(sampleUserResponseDTO));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("test.user"));
    }

    @Test
    void getUserById_ShouldReturnUser() throws Exception {
        given(userService.findById(userId)).willReturn(sampleUserResponseDTO);

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    void create_ShouldReturnCreated() throws Exception {
        given(userService.create(any(UserResponseDTO.class), anyString())).willReturn(sampleUserResponseDTO);

        mockMvc.perform(post("/api/v1/users")
                        .header("X-Raw-Password", "password123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUserResponseDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.username").value("test.user"));
    }

    @Test
    void updateUser_ShouldReturnUpdated() throws Exception {
        given(userService.update(userId, sampleUserResponseDTO)).willReturn(sampleUserResponseDTO);

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUserResponseDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void delete_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isNoContent());
    }
}