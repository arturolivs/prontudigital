package com.prontudigital.backend.autenticacao.controladores;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prontudigital.backend.autenticacao.dto.RegistrarRequestDTO;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.servicos.AutenticacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regra de seguranca de POST /api/auth/registrar.
 *
 * O endpoint aceita a lista de perfis vinda do corpo da requisicao. Enquanto
 * esteve em AUTH_POST_PUBLICOS, qualquer anonimo podia criar um ADMIN com
 * acesso a todos os prontuarios — e nenhum teste apontava isso. Estes testes
 * existem para que reabrir a rota quebre a suite.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AutenticacaoController - seguranca de /registrar")
class AutenticacaoControllerSegurancaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AutenticacaoService autenticacaoService;

    private static final RegistrarRequestDTO PEDIDO_ADMIN = new RegistrarRequestDTO(
            "Invasor",
            "invasor@email.com",
            "invasor",
            "senha12345",
            "11999999999",
            Set.of("ADMIN"));

    @Nested
    @DisplayName("POST /api/auth/registrar")
    class Registrar {

        @Test
        @WithAnonymousUser
        @DisplayName("nega anonimo e nao chega ao servico")
        void negaAnonimo() throws Exception {
            // 403, nao 401: a configuracao nao declara authenticationEntryPoint,
            // entao vale o padrao Http403ForbiddenEntryPoint — mesmo comportamento
            // do resto da API para requisicao sem credencial.
            mockMvc.perform(post("/api/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(PEDIDO_ADMIN)))
                    .andExpect(status().isForbidden());

            verify(autenticacaoService, never()).registrar(any());
        }

        @Test
        @WithMockUser(roles = "PROFISSIONAL")
        @DisplayName("nega autenticado sem perfil ADMIN")
        void negaNaoAdmin() throws Exception {
            mockMvc.perform(post("/api/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(PEDIDO_ADMIN)))
                    .andExpect(status().isForbidden());

            verify(autenticacaoService, never()).registrar(any());
        }

        @Test
        @WithMockUser(roles = "PACIENTE")
        @DisplayName("nega paciente autenticado — escalada de privilegio")
        void negaPaciente() throws Exception {
            mockMvc.perform(post("/api/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(PEDIDO_ADMIN)))
                    .andExpect(status().isForbidden());

            verify(autenticacaoService, never()).registrar(any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("permite ADMIN")
        void permiteAdmin() throws Exception {
            when(autenticacaoService.registrar(any()))
                    .thenReturn(UsuarioDTO.builder()
                            .id(1L)
                            .username("novo.usuario")
                            .nomeCompleto("Novo Usuario")
                            .build());

            mockMvc.perform(post("/api/auth/registrar")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(PEDIDO_ADMIN)))
                    .andExpect(status().isCreated());

            verify(autenticacaoService).registrar(any());
        }
    }

    @Nested
    @DisplayName("Rotas que continuam publicas")
    class RotasPublicas {

        @Test
        @WithAnonymousUser
        @DisplayName("/cadastrar-paciente segue aberto: o perfil PACIENTE e fixado no servico")
        void cadastrarPacienteSegueAberto() throws Exception {
            // Corpo vazio de proposito: o que importa e nao ser barrado por
            // autenticacao. Se estivesse fechado, a resposta seria 403, nunca 4xx de
            // validacao.
            mockMvc.perform(post("/api/auth/cadastrar-paciente")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("/login segue aberto")
        void loginSegueAberto() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
