package com.prontudigital.backend.configuracao.dto;

import com.prontudigital.backend.autenticacao.dto.EnderecoDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CNPJ;

@Schema(description = "Dados da clinica exibidos na aplicacao e nos documentos")
public record ConfiguracaoClinicaRequestDTO(

        @Schema(description = "Nome exibido no topo dos documentos",
                example = "Clinica Vida & Saude")
        @NotBlank(message = "Nome da clinica e obrigatorio")
        @Size(max = 150)
        String nome,

        @Schema(description = "CNPJ, com ou sem mascara", example = "12.345.678/0001-95")
        @CNPJ(message = "CNPJ invalido")
        String cnpj,

        @Schema(description = "Telefone de contato", example = "(81) 3333-3333")
        @Size(max = 20)
        String telefone,

        @Schema(description = "E-mail de contato", example = "contato@clinica.com.br")
        @Email(message = "E-mail invalido")
        @Size(max = 150)
        String email,

        @Schema(description = "Site", example = "www.clinica.com.br")
        @Size(max = 150)
        String site,

        @Schema(description = "Endereco da clinica")
        @Valid
        EnderecoDTO endereco,

        @Schema(description = "Texto livre no rodape dos documentos",
                example = "Responsavel tecnico: Maria da Silva - COREN-PE 123456")
        @Size(max = 2000)
        String rodapeDocumentos

) {}
