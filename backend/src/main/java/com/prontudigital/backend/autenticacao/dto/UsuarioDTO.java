package com.prontudigital.backend.autenticacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.hibernate.validator.constraints.br.CPF;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Schema(description = "Dados do usuário")
@Builder
public record UsuarioDTO(

        @Schema(description = "ID interno")
        Long id,

        @Schema(description = "UUID publico do usuário")
        UUID uuid,

        @Schema(description = "Nome completo", example = "Joao Silva")
        String nomeCompleto,

        @Schema(description = "Nome de usuÁrio", example = "joaosilva")
        String username,

        @Schema(description = "E-mail", example = "joao@email.com")
        String email,

        @Schema(description = "Telefone", example = "(11) 99999-9999")
        String telefone,

        @Schema(description = "CPF, com ou sem mascara (RF04)", example = "529.982.247-25")
        @CPF(message = "CPF inválido")
        String cpf,

        @Schema(description = "Data de nascimento (RF04)", example = "1985-03-27")
        @Past(message = "Data de nascimento deve ser no passado")
        LocalDate dataNascimento,

        @Schema(description = "Endereço (RF04)")
        @Valid
        EnderecoDTO endereco,

        @Schema(description = "Registro no COREN do profissional (RF05)", example = "COREN-SP 123456")
        @Size(max = 20, message = "COREN deve ter no máximo 20 caracteres")
        String coren,

        @Schema(description = "Especialidade do profissional (RF05)", example = "Estomaterapia")
        @Size(max = 100, message = "Especialidade deve ter no máximo 100 caracteres")
        String especialidade,

        @Schema(description = "Indica se o usuário possui avatar cadastrado. "
                + "O binario e obtido em GET /api/usuarios/me/avatar")
        Boolean temAvatar,

        @Schema(description = "Indica se o usuário esta ativo")
        Boolean ativo,

        @Schema(description = "Indica se o usuário possui acesso ao sistema configurado")
        Boolean acessoAtivado,

        @Schema(description = "Perfis atribuídos ao usuário", example = "[\"USUARIO\"]")
        Set<String> perfis,

        @Schema(description = "Data de criação do registro")
        Instant criadoEm,

        @Schema(description = "Data da ultima atualização")
        Instant atualizadoEm

) {}