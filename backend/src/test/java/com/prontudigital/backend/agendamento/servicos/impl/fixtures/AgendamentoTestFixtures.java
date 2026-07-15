package com.prontudigital.backend.agendamento.servicos.impl.fixtures;

import com.prontudigital.backend.agendamento.dto.AgendamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.EvolucaoTratamentoRequestDTO;
import com.prontudigital.backend.agendamento.dto.ReagendarRequestDTO;
import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.BloqueioHorario;
import com.prontudigital.backend.agendamento.entidades.EvolucaoClinica;
import com.prontudigital.backend.agendamento.entidades.Procedimento;
import com.prontudigital.backend.agendamento.enums.AvaliacaoPulsos;
import com.prontudigital.backend.agendamento.enums.CaracteristicaBorda;
import com.prontudigital.backend.agendamento.enums.CaracteristicaPerilesional;
import com.prontudigital.backend.agendamento.enums.ClassificacaoDor;
import com.prontudigital.backend.agendamento.enums.EvolucaoFerida;
import com.prontudigital.backend.agendamento.enums.ExsudatoCaracteristica;
import com.prontudigital.backend.agendamento.enums.ExsudatoVolume;
import com.prontudigital.backend.agendamento.enums.GrauEdema;
import com.prontudigital.backend.agendamento.enums.LocalAtendimento;
import com.prontudigital.backend.agendamento.enums.OdorIntensidade;
import com.prontudigital.backend.agendamento.enums.SinalEvolucao;
import com.prontudigital.backend.agendamento.enums.SinalInfeccao;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoBloqueio;
import com.prontudigital.backend.agendamento.enums.TipoProcedimento;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public final class AgendamentoTestFixtures {

    public static final UUID PACIENTE_UUID     = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID PROFISSIONAL_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID ADMIN_UUID        = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID OUTRO_UUID        = UUID.fromString("99999999-9999-9999-9999-999999999999");

    public static final LocalDateTime INICIO = LocalDateTime.of(2026, 6, 1, 14, 0);
    public static final LocalDateTime FIM    = LocalDateTime.of(2026, 6, 1, 15, 0);

    private AgendamentoTestFixtures() {}

    public static UsuarioDTO usuarioPaciente() {
        return UsuarioDTO.builder()
                .uuid(PACIENTE_UUID)
                .username("paciente1")
                .perfis(Set.of("PACIENTE"))
                .build();
    }

    public static UsuarioDTO usuarioProfissional() {
        return UsuarioDTO.builder()
                .uuid(PROFISSIONAL_UUID)
                .username("profissional1")
                .perfis(Set.of("PROFISSIONAL"))
                .build();
    }

    public static UsuarioDTO usuarioAdmin() {
        return UsuarioDTO.builder()
                .uuid(ADMIN_UUID)
                .username("admin1")
                .perfis(Set.of("ADMIN"))
                .build();
    }

    public static Procedimento procedimentoPodiatria() {
        return Procedimento.builder()
                .id(1L)
                .codigo(TipoProcedimento.PODIATRIA.name())
                .nome("Podiatria")
                .ativo(true)
                .build();
    }

    public static Procedimento procedimentoNovo() {
        return Procedimento.builder()
                .id(5L)
                .nome("Laserterapia")
                .ativo(true)
                .build();
    }

    public static AgendamentoRequestDTO requestAvaliacao() {
        return new AgendamentoRequestDTO(
                PACIENTE_UUID, PROFISSIONAL_UUID,
                INICIO, FIM,
                TipoAgendamento.AVALIACAO, TipoProcedimento.PODIATRIA, null,
                LocalAtendimento.CLINICA, false, null);
    }

    public static AgendamentoRequestDTO requestAvaliacaoComProcedimentoId(Long procedimentoId) {
        return new AgendamentoRequestDTO(
                PACIENTE_UUID, PROFISSIONAL_UUID,
                INICIO, FIM,
                TipoAgendamento.AVALIACAO, null, procedimentoId,
                LocalAtendimento.CLINICA, false, null);
    }

    public static AgendamentoRequestDTO requestTratamento(Long avaliacaoId) {
        return new AgendamentoRequestDTO(
                PACIENTE_UUID, PROFISSIONAL_UUID,
                INICIO, FIM,
                TipoAgendamento.TRATAMENTO, TipoProcedimento.PODIATRIA, null,
                LocalAtendimento.CLINICA, false, avaliacaoId);
    }

    public static Agendamento agendamentoAgendado() {
        return Agendamento.builder()
                .id(1L)
                .pacienteUuid(PACIENTE_UUID)
                .profissionalUuid(PROFISSIONAL_UUID)
                .inicioEm(INICIO)
                .fimEm(FIM)
                .status(StatusAgendamento.AGENDADO)
                .tipo(TipoAgendamento.AVALIACAO)
                .build();
    }

    public static Agendamento agendamentoComStatus(StatusAgendamento status) {
        Agendamento a = agendamentoAgendado();
        a.setStatus(status);
        return a;
    }

    public static Agendamento avaliacaoConcluida() {
        Agendamento a = agendamentoAgendado();
        a.setId(99L);
        a.setStatus(StatusAgendamento.REALIZADO);
        a.setTipo(TipoAgendamento.AVALIACAO);
        return a;
    }

    public static Agendamento agendamentoTratamento() {
        return Agendamento.builder()
                .id(2L)
                .pacienteUuid(PACIENTE_UUID)
                .profissionalUuid(PROFISSIONAL_UUID)
                .inicioEm(INICIO)
                .fimEm(FIM)
                .status(StatusAgendamento.CONFIRMADO)
                .tipo(TipoAgendamento.TRATAMENTO)
                .avaliacao(avaliacaoConcluida())
                .build();
    }

    public static EvolucaoClinica evolucaoClinicaExistente(Agendamento agendamento) {
        return EvolucaoClinica.builder()
                .id(10L)
                .agendamento(agendamento)
                .etiologia("Úlcera venosa")
                .classificacaoDor(ClassificacaoDor.LEVE)
                .build();
    }

    public static EvolucaoTratamentoRequestDTO evolucaoRequest() {
        return new EvolucaoTratamentoRequestDTO(
                // Dados da ferida
                "Membro inferior direito",
                "Úlcera venosa",
                "3 meses",
                // Mensuração
                new BigDecimal("3.5"),
                new BigDecimal("2.0"),
                new BigDecimal("0.5"),
                true,
                false,
                // Leito da ferida
                20,
                60,
                15,
                5,
                false,
                false,
                false,
                // Exsudato
                ExsudatoVolume.MODERADO,
                ExsudatoCaracteristica.SEROSO,
                OdorIntensidade.LEVE,
                // Bordas
                Set.of(CaracteristicaBorda.INTEGRAS),
                // Pele perilesional
                Set.of(CaracteristicaPerilesional.HIPEREMIADA),
                // Sinais de infecção
                Set.of(SinalInfeccao.AUSENTES),
                // Dor
                ClassificacaoDor.MODERADA,
                // Avaliação vascular
                GrauEdema.MAIS_1,
                AvaliacaoPulsos.PALPAVEIS,
                // Evolução da ferida
                EvolucaoFerida.MELHORANDO,
                Set.of(SinalEvolucao.AUMENTO_GRANULACAO),
                // Conduta
                true,
                false,
                true,
                "Alginato de cálcio",
                false,
                null,
                true,
                // Observações
                "Paciente colaborativo");
    }

    public static BloqueioHorario bloqueio() {
        return BloqueioHorario.builder()
                .id(1L)
                .profissionalUuid(PROFISSIONAL_UUID)
                .inicioEm(INICIO)
                .fimEm(FIM)
                .motivo("Ferias")
                .tipo(TipoBloqueio.INDISPONIVEL)
                .build();
    }

    public static ReagendarRequestDTO reagendarRequest() {
        return new ReagendarRequestDTO(
                INICIO.plusDays(1), FIM.plusDays(1), "Solicitacao");
    }
}
