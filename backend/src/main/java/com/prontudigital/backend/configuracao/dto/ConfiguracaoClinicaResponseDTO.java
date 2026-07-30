package com.prontudigital.backend.configuracao.dto;

import com.prontudigital.backend.autenticacao.dto.EnderecoDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Configuracao da clinica desta instalacao")
public record ConfiguracaoClinicaResponseDTO(

        String nome,
        String cnpj,
        String telefone,
        String email,
        String site,
        EnderecoDTO endereco,
        String rodapeDocumentos,

        @Schema(description = "Ha logo cadastrada. O binario vem por "
                + "GET /api/configuracao/logo, nunca embutido nesta resposta.")
        boolean temLogo,

        LocalDateTime atualizadoEm

) {}
