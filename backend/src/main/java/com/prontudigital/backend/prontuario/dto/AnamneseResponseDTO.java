package com.prontudigital.backend.prontuario.dto;

import com.prontudigital.backend.prontuario.enums.RedeApoio;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Anamnese do paciente (RF13)")
@Builder
public record AnamneseResponseDTO(

        UUID uuid,
        UUID pacienteUuid,
        String pacienteNome,

        // Identificacao complementar
        String profissao,
        String responsavelCuidador,

        // Historia da ferida
        String motivoConsulta,
        String tempoExistenciaFerida,
        String comoFeridaSurgiu,
        LocalDate dataInicioAproximada,
        String tratamentosAnteriores,
        String curativosPrevios,

        // Historico de saude (Sim/Nao + detalhe)
        Boolean diabetesMellitus,
        String diabetesMellitusDetalhe,
        Boolean hipertensaoArterial,
        String hipertensaoArterialDetalhe,
        Boolean doencaVenosaCronica,
        String doencaVenosaCronicaDetalhe,
        Boolean doencaArterialPeriferica,
        String doencaArterialPerifericaDetalhe,
        Boolean insuficienciaRenal,
        String insuficienciaRenalDetalhe,
        Boolean cancer,
        String cancerDetalhe,
        Boolean problemasNeurologicos,
        String problemasNeurologicosDetalhe,
        Boolean historicoCirurgias,
        String historicoCirurgiasDetalhe,

        // Medicamentos em uso
        Boolean medAntibioticos,
        Boolean medAnticoagulantes,
        Boolean medCorticoides,
        Boolean medInsulinaHipoglicemiantes,
        Boolean medOutrosContinuos,

        // Alergias
        Boolean alergiaMedicamentos,
        Boolean alergiaProdutosTopicos,
        Boolean alergiaCurativosAdesivos,

        // Habitos de vida
        Boolean tabagismo,
        Boolean consumoAlcool,
        String alimentacaoEstadoNutricional,
        Boolean ingestaoHidrica,
        String ingestaoHidricaDetalhe,

        // Mobilidade
        Boolean deambulaSozinho,
        String deambulaSozinhoDetalhe,
        Boolean acamadoOuCadeirante,
        String acamadoOuCadeiranteDetalhe,
        Boolean usoDispositivos,
        String usoDispositivosDetalhe,
        Boolean mudancaPosicaoLeito,
        String mudancaPosicaoLeitoDetalhe,

        // Outros
        Boolean examesRecentes,
        RedeApoio redeApoio,
        Boolean acompanhamentoMedico,
        String acompanhamentoMedicoDetalhe,

        UUID registradoPor,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm

) {}
