package com.prontudigital.backend.prontuario.dto;

import com.prontudigital.backend.prontuario.enums.RedeApoio;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

@Schema(description = "Dados para registro/atualizacao da anamnese do paciente (RF13)")
@Builder
public record AnamneseRequestDTO(

        // Identificacao complementar
        @Size(max = 255)
        @Schema(description = "Profissao do paciente", example = "Aposentado")
        String profissao,

        @Size(max = 255)
        @Schema(description = "Responsavel ou cuidador do paciente")
        String responsavelCuidador,

        // Historia da ferida
        @Size(max = 5000)
        @Schema(description = "Motivo da consulta",
                example = "Dor e secrecao em ferida no membro inferior direito")
        String motivoConsulta,

        @Size(max = 255)
        @Schema(description = "Tempo de existencia da ferida", example = "3 meses")
        String tempoExistenciaFerida,

        @Size(max = 5000)
        @Schema(description = "Como a ferida surgiu")
        String comoFeridaSurgiu,

        @Schema(description = "Data de inicio aproximada da ferida", example = "2026-04-01")
        LocalDate dataInicioAproximada,

        @Size(max = 5000)
        @Schema(description = "Tratamentos anteriores realizados")
        String tratamentosAnteriores,

        @Size(max = 5000)
        @Schema(description = "Curativos previos utilizados")
        String curativosPrevios,

        // Historico de saude (Sim/Nao + detalhe)
        @Schema(description = "Diabetes mellitus")
        Boolean diabetesMellitus,
        @Size(max = 1000)
        @Schema(description = "Detalhe da diabetes mellitus (tipo, tempo, controle)")
        String diabetesMellitusDetalhe,

        @Schema(description = "Hipertensao arterial")
        Boolean hipertensaoArterial,
        @Size(max = 1000)
        @Schema(description = "Detalhe da hipertensao arterial")
        String hipertensaoArterialDetalhe,

        @Schema(description = "Doenca venosa cronica")
        Boolean doencaVenosaCronica,
        @Size(max = 1000)
        @Schema(description = "Detalhe da doenca venosa cronica")
        String doencaVenosaCronicaDetalhe,

        @Schema(description = "Doenca arterial periferica")
        Boolean doencaArterialPeriferica,
        @Size(max = 1000)
        @Schema(description = "Detalhe da doenca arterial periferica")
        String doencaArterialPerifericaDetalhe,

        @Schema(description = "Insuficiencia renal")
        Boolean insuficienciaRenal,
        @Size(max = 1000)
        @Schema(description = "Detalhe da insuficiencia renal")
        String insuficienciaRenalDetalhe,

        @Schema(description = "Cancer")
        Boolean cancer,
        @Size(max = 1000)
        @Schema(description = "Detalhe do cancer (tipo, tratamento)")
        String cancerDetalhe,

        @Schema(description = "Problemas neurologicos")
        Boolean problemasNeurologicos,
        @Size(max = 1000)
        @Schema(description = "Detalhe dos problemas neurologicos")
        String problemasNeurologicosDetalhe,

        @Schema(description = "Historico de cirurgias")
        Boolean historicoCirurgias,
        @Size(max = 1000)
        @Schema(description = "Detalhe do historico de cirurgias")
        String historicoCirurgiasDetalhe,

        // Medicamentos em uso
        @Schema(description = "Uso de antibioticos")
        Boolean medAntibioticos,
        @Schema(description = "Uso de anticoagulantes")
        Boolean medAnticoagulantes,
        @Schema(description = "Uso de corticoides")
        Boolean medCorticoides,
        @Schema(description = "Uso de insulina/hipoglicemiantes")
        Boolean medInsulinaHipoglicemiantes,
        @Schema(description = "Uso de outros medicamentos continuos")
        Boolean medOutrosContinuos,

        // Alergias
        @Schema(description = "Alergia a medicamentos")
        Boolean alergiaMedicamentos,
        @Schema(description = "Alergia a produtos topicos")
        Boolean alergiaProdutosTopicos,
        @Schema(description = "Alergia a curativos/adesivos")
        Boolean alergiaCurativosAdesivos,

        // Habitos de vida
        @Schema(description = "Tabagismo")
        Boolean tabagismo,
        @Schema(description = "Consumo de alcool")
        Boolean consumoAlcool,
        @Size(max = 5000)
        @Schema(description = "Alimentacao / estado nutricional")
        String alimentacaoEstadoNutricional,
        @Schema(description = "Ingestao hidrica adequada")
        Boolean ingestaoHidrica,
        @Size(max = 1000)
        @Schema(description = "Detalhe da ingestao hidrica")
        String ingestaoHidricaDetalhe,

        // Mobilidade
        @Schema(description = "Deambula sozinho")
        Boolean deambulaSozinho,
        @Size(max = 1000)
        @Schema(description = "Detalhe sobre deambulacao")
        String deambulaSozinhoDetalhe,

        @Schema(description = "Acamado ou cadeirante")
        Boolean acamadoOuCadeirante,
        @Size(max = 1000)
        @Schema(description = "Detalhe sobre acamado/cadeirante")
        String acamadoOuCadeiranteDetalhe,

        @Schema(description = "Uso de dispositivos (bengala, andador, cadeira)")
        Boolean usoDispositivos,
        @Size(max = 1000)
        @Schema(description = "Detalhe dos dispositivos utilizados")
        String usoDispositivosDetalhe,

        @Schema(description = "Realiza mudanca de posicao no leito")
        Boolean mudancaPosicaoLeito,
        @Size(max = 1000)
        @Schema(description = "Detalhe da mudanca de posicao no leito")
        String mudancaPosicaoLeitoDetalhe,

        // Outros
        @Schema(description = "Possui exames recentes")
        Boolean examesRecentes,

        @Schema(description = "Rede de apoio do paciente")
        RedeApoio redeApoio,

        @Schema(description = "Possui acompanhamento medico")
        Boolean acompanhamentoMedico,
        @Size(max = 1000)
        @Schema(description = "Detalhe do acompanhamento medico (nome/especialidade/contato)")
        String acompanhamentoMedicoDetalhe

) {}
