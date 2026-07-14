package com.prontudigital.backend.prontuario.servicos.impl;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.EvolucaoClinica;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoProcedimento;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.prontuario.dto.HistoricoItemDTO;
import com.prontudigital.backend.prontuario.entidades.Anexo;
import com.prontudigital.backend.prontuario.entidades.Prescricao;
import com.prontudigital.backend.prontuario.enums.TipoHistorico;
import com.prontudigital.backend.prontuario.enums.TipoPrescricao;
import com.prontudigital.backend.prontuario.repositorios.AnexoRepository;
import com.prontudigital.backend.prontuario.repositorios.PrescricaoRepository;
import com.prontudigital.backend.prontuario.seguranca.ProntuarioPermissaoPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HistoricoServiceImpl")
class HistoricoServiceImplTest {

    private static final UUID PACIENTE_UUID     = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROFISSIONAL_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OUTRO_UUID        = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID AG1_UUID          = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID AG2_UUID          = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private PrescricaoRepository prescricaoRepository;
    @Mock private AnexoRepository anexoRepository;
    @Mock private UsuarioContexto usuarioContexto;

    private ProntuarioPermissaoPolicy permissaoPolicy;
    private HistoricoServiceImpl service;

    @BeforeEach
    void setUp() {
        permissaoPolicy = new ProntuarioPermissaoPolicy(agendamentoRepository);
        service = new HistoricoServiceImpl(
                agendamentoRepository, prescricaoRepository, anexoRepository,
                usuarioContexto, permissaoPolicy);
    }

    private UsuarioDTO usuario(UUID uuid, String perfil) {
        return UsuarioDTO.builder().uuid(uuid).perfis(Set.of(perfil)).build();
    }

    private Agendamento agendamento(UUID uuid, LocalDateTime inicio, EvolucaoClinica evolucao) {
        return Agendamento.builder()
                .uuid(uuid)
                .pacienteUuid(PACIENTE_UUID)
                .profissionalUuid(PROFISSIONAL_UUID)
                .inicioEm(inicio)
                .fimEm(inicio.plusHours(1))
                .status(StatusAgendamento.REALIZADO)
                .tipo(TipoAgendamento.AVALIACAO)
                .tipoProcedimento(TipoProcedimento.PODIATRIA)
                .evolucaoClinica(evolucao)
                .build();
    }

    private Prescricao prescricao(LocalDateTime criadoEm) {
        return Prescricao.builder()
                .uuid(UUID.randomUUID())
                .pacienteUuid(PACIENTE_UUID)
                .agendamentoUuid(AG1_UUID)
                .tipo(TipoPrescricao.MEDICAMENTO)
                .descricao("Dipirona 500mg")
                .posologia("1 comprimido")
                .frequencia("a cada 6h")
                .criadoEm(criadoEm)
                .build();
    }

    private Anexo anexo(LocalDateTime criadoEm) {
        return Anexo.builder()
                .uuid(UUID.randomUUID())
                .pacienteUuid(PACIENTE_UUID)
                .agendamentoUuid(AG1_UUID)
                .nomeOriginal("exame.pdf")
                .tipoConteudo("application/pdf")
                .criadoEm(criadoEm)
                .build();
    }

    @Test
    @DisplayName("rejeita paciente consultando historico de outro paciente")
    void deveRejeitarAcessoDeOutroPaciente() {
        when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "PACIENTE"));

        assertThrows(UsuarioSemAutorizacaoException.class,
                () -> service.montarHistorico(PACIENTE_UUID));

        verify(agendamentoRepository, never()).findByPacienteUuidOrderByInicioEmDesc(any());
        verifyNoInteractions(prescricaoRepository, anexoRepository);
    }

    @Test
    @DisplayName("consolida as 4 fontes e ordena da mais recente para a mais antiga")
    void deveConsolidarEOrdenar() {
        EvolucaoClinica evolucao = EvolucaoClinica.builder()
                .localizacaoAnatomica("MID")
                .etiologia("Úlcera venosa")
                .build();

        Agendamento comEvolucao = agendamento(AG1_UUID, LocalDateTime.of(2026, 6, 10, 10, 0), evolucao);
        Agendamento semEvolucao = agendamento(AG2_UUID, LocalDateTime.of(2026, 6, 1, 10, 0), null);

        when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "ADMIN"));
        when(agendamentoRepository.findByPacienteUuidOrderByInicioEmDesc(PACIENTE_UUID))
                .thenReturn(List.of(comEvolucao, semEvolucao));
        when(prescricaoRepository.findByPacienteUuidOrderByCriadoEmDesc(PACIENTE_UUID))
                .thenReturn(List.of(prescricao(LocalDateTime.of(2026, 6, 15, 9, 0))));
        when(anexoRepository.findByPacienteUuidOrderByCriadoEmDesc(PACIENTE_UUID))
                .thenReturn(List.of(anexo(LocalDateTime.of(2026, 6, 5, 9, 0))));

        List<HistoricoItemDTO> historico = service.montarHistorico(PACIENTE_UUID);

        // 2 agendamentos + 1 evolucao (do primeiro) + 1 prescricao + 1 anexo = 5
        assertEquals(5, historico.size());

        // Ordenacao decrescente por data
        assertEquals(TipoHistorico.PRESCRICAO, historico.get(0).tipo());   // 06-15
        assertEquals(TipoHistorico.AGENDAMENTO, historico.get(4).tipo());  // 06-01 (mais antigo)
        assertEquals(LocalDateTime.of(2026, 6, 1, 10, 0), historico.get(4).data());

        // A evolucao aparece ancorada no agendamento de origem
        HistoricoItemDTO evolucaoItem = historico.stream()
                .filter(i -> i.tipo() == TipoHistorico.EVOLUCAO)
                .findFirst().orElseThrow();
        assertEquals(AG1_UUID, evolucaoItem.agendamentoUuid());
        assertEquals(LocalDateTime.of(2026, 6, 10, 10, 0), evolucaoItem.data());
        assertNull(evolucaoItem.referenciaUuid());
    }

    @Test
    @DisplayName("lida com campos opcionais nulos/em branco (procedimento e detalhes da evolucao)")
    void deveLidarComCamposOpcionaisVazios() {
        EvolucaoClinica evolucaoVazia = EvolucaoClinica.builder()
                .localizacaoAnatomica("")   // em branco
                .etiologia(null)            // nulo
                .build();
        Agendamento semProcedimento = Agendamento.builder()
                .uuid(AG1_UUID)
                .pacienteUuid(PACIENTE_UUID)
                .inicioEm(LocalDateTime.of(2026, 6, 2, 8, 0))
                .status(StatusAgendamento.REALIZADO)
                .tipo(TipoAgendamento.AVALIACAO)
                .tipoProcedimento(null)     // sem procedimento
                .evolucaoClinica(evolucaoVazia)
                .build();

        when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "ADMIN"));
        when(agendamentoRepository.findByPacienteUuidOrderByInicioEmDesc(PACIENTE_UUID))
                .thenReturn(List.of(semProcedimento));
        when(prescricaoRepository.findByPacienteUuidOrderByCriadoEmDesc(PACIENTE_UUID))
                .thenReturn(List.of());
        when(anexoRepository.findByPacienteUuidOrderByCriadoEmDesc(PACIENTE_UUID))
                .thenReturn(List.of());

        List<HistoricoItemDTO> historico = service.montarHistorico(PACIENTE_UUID);

        HistoricoItemDTO agendamento = historico.stream()
                .filter(i -> i.tipo() == TipoHistorico.AGENDAMENTO).findFirst().orElseThrow();
        assertEquals("AVALIACAO", agendamento.titulo());   // sem " — procedimento"

        HistoricoItemDTO evolucao = historico.stream()
                .filter(i -> i.tipo() == TipoHistorico.EVOLUCAO).findFirst().orElseThrow();
        assertNull(evolucao.descricao());                  // juntar de partes vazias -> null
    }

    @Test
    @DisplayName("retorna lista vazia quando o paciente nao tem registros")
    void deveRetornarVazioSemRegistros() {
        when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "ADMIN"));
        when(agendamentoRepository.findByPacienteUuidOrderByInicioEmDesc(PACIENTE_UUID))
                .thenReturn(List.of());
        when(prescricaoRepository.findByPacienteUuidOrderByCriadoEmDesc(PACIENTE_UUID))
                .thenReturn(List.of());
        when(anexoRepository.findByPacienteUuidOrderByCriadoEmDesc(PACIENTE_UUID))
                .thenReturn(List.of());

        assertTrue(service.montarHistorico(PACIENTE_UUID).isEmpty());
    }

    @Test
    @DisplayName("profissional vinculado ao paciente acessa o historico (RN03)")
    void profissionalVinculadoAcessa() {
        when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
        when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                .thenReturn(true);
        when(agendamentoRepository.findByPacienteUuidOrderByInicioEmDesc(PACIENTE_UUID))
                .thenReturn(List.of());
        when(prescricaoRepository.findByPacienteUuidOrderByCriadoEmDesc(PACIENTE_UUID))
                .thenReturn(List.of());
        when(anexoRepository.findByPacienteUuidOrderByCriadoEmDesc(PACIENTE_UUID))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> service.montarHistorico(PACIENTE_UUID));
    }
}
