package com.prontudigital.auth_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prontudigital.auth_service.controller.UserController;
import com.prontudigital.auth_service.dto.UserDTO;
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

    private final UUID userId = UUID.randomUUID();
    private final UserDTO sampleUserDTO = UserDTO.builder()
            .id(userId)
            //.username("test.user")
          //  .profileName(ProfileName.ADMIN)
            .build();

    @Test
    void getAllUsers_ShouldReturnUsers() throws Exception {
        given(userService.findAll()).willReturn(List.of(sampleUserDTO));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("test.user"));
    }

    @Test
    void getUserById_ShouldReturnUser() throws Exception {
        given(userService.findById(userId)).willReturn(sampleUserDTO);

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    void createUser_ShouldReturnCreated() throws Exception {
        given(userService.create(any(UserDTO.class), anyString())).willReturn(sampleUserDTO);

        mockMvc.perform(post("/api/v1/users")
                        .header("X-Raw-Password", "password123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUserDTO)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.username").value("test.user"));
    }

    @Test
    void updateUser_ShouldReturnUpdatedUser() throws Exception {
        given(userService.update(userId, sampleUserDTO)).willReturn(sampleUserDTO);

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleUserDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void deleteUser_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isNoContent());
    }
}