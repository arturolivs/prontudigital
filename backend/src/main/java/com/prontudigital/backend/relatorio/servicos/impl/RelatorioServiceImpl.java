package com.prontudigital.backend.relatorio.servicos.impl;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.HorarioTrabalho;
import com.prontudigital.backend.agendamento.enums.StatusAgendamento;
import com.prontudigital.backend.agendamento.enums.TipoAgendamento;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoInvalidoException;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.agendamento.repositorios.HorarioTrabalhoRepository;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import com.prontudigital.backend.relatorio.dto.RelatorioAtendimentoItemDTO;
import com.prontudigital.backend.relatorio.dto.RelatorioAtendimentosDTO;
import com.prontudigital.backend.relatorio.dto.RelatorioOcupacaoDTO;
import com.prontudigital.backend.relatorio.servicos.RelatorioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * RF19 e RF20 — relatorios gerenciais sobre a agenda.
 *
 * <p>Acesso: ADMIN ve a clinica inteira ou qualquer profissional; PROFISSIONAL
 * so a propria agenda (o filtro e forcado para o proprio uuid); PACIENTE nao
 * acessa relatorio nenhum.
 */
@Service
@RequiredArgsConstructor
public class RelatorioServiceImpl implements RelatorioService {

    /** Agendamentos que efetivamente ocuparam a agenda. */
    private static final Set<StatusAgendamento> STATUS_OCUPAM_AGENDA = EnumSet.of(
            StatusAgendamento.AGENDADO,
            StatusAgendamento.CONFIRMADO,
            StatusAgendamento.REALIZADO,
            StatusAgendamento.NAO_COMPARECEU);

    private final AgendamentoRepository agendamentoRepository;
    private final HorarioTrabalhoRepository horarioTrabalhoRepository;
    private final UsuarioService usuarioService;
    private final UsuarioContexto usuarioContexto;
    private final AgendamentoPermissaoPolicy permissaoPolicy;

    @Override
    @Transactional(readOnly = true)
    public RelatorioAtendimentosDTO atendimentos(LocalDate inicio, LocalDate fim,
                                                 UUID profissionalUuid,
                                                 StatusAgendamento status,
                                                 TipoAgendamento tipo) {
        validarPeriodo(inicio, fim);
        UUID filtroProfissional = resolverProfissional(profissionalUuid);

        List<Agendamento> agendamentos = agendamentoRepository.buscarParaRelatorio(
                inicio.atStartOfDay(), fim.atTime(LocalTime.MAX),
                filtroProfissional, status, tipo);

        // Cache de nomes: o relatorio costuma repetir os mesmos pacientes e
        // profissionais, e cada resolucao e uma consulta.
        Map<UUID, String> nomes = new HashMap<>();

        List<RelatorioAtendimentoItemDTO> itens = agendamentos.stream()
                .map(a -> RelatorioAtendimentoItemDTO.builder()
                        .agendamentoId(a.getId())
                        .inicioEm(a.getInicioEm())
                        .fimEm(a.getFimEm())
                        .duracaoMinutos(duracaoMinutos(a))
                        .pacienteUuid(a.getPacienteUuid())
                        .nomePaciente(nome(nomes, a.getPacienteUuid()))
                        .profissionalUuid(a.getProfissionalUuid())
                        .nomeProfissional(nome(nomes, a.getProfissionalUuid()))
                        .tipo(a.getTipo())
                        .procedimentoNome(nomeProcedimento(a))
                        .localAtendimento(a.getLocalAtendimento())
                        .status(a.getStatus())
                        .concluidoEm(a.getConcluidoEm())
                        .build())
                .toList();

        return RelatorioAtendimentosDTO.builder()
                .inicio(inicio)
                .fim(fim)
                .nomeProfissional(nome(nomes, filtroProfissional))
                .total(itens.size())
                .totalPorStatus(contar(agendamentos, a -> a.getStatus().name()))
                .totalPorTipo(contar(agendamentos,
                        a -> a.getTipo() == null ? "NAO_INFORMADO" : a.getTipo().name()))
                .itens(itens)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RelatorioOcupacaoDTO ocupacao(LocalDate inicio, LocalDate fim, UUID profissionalUuid) {
        validarPeriodo(inicio, fim);
        UUID filtroProfissional = resolverProfissional(profissionalUuid);

        List<Agendamento> agendamentos = agendamentoRepository.buscarParaRelatorio(
                inicio.atStartOfDay(), fim.atTime(LocalTime.MAX),
                filtroProfissional, null, null);

        int realizados = contarStatus(agendamentos, StatusAgendamento.REALIZADO);
        int cancelados = contarStatus(agendamentos, StatusAgendamento.CANCELADO);
        int faltas = contarStatus(agendamentos, StatusAgendamento.NAO_COMPARECEU);
        int remarcados = contarStatus(agendamentos, StatusAgendamento.REMARCADO);
        int emAberto = contarStatus(agendamentos, StatusAgendamento.AGENDADO)
                + contarStatus(agendamentos, StatusAgendamento.CONFIRMADO);

        // Comparecimento e absenteismo so fazem sentido sobre o que chegou a
        // acontecer: o que foi cancelado antes nunca virou presenca nem falta.
        int compareceramOuFaltaram = realizados + faltas;

        double horasAgendadas = agendamentos.stream()
                .filter(a -> STATUS_OCUPAM_AGENDA.contains(a.getStatus()))
                .mapToLong(this::duracaoMinutos)
                .sum() / 60.0;

        Double horasDisponiveis = horasDeExpediente(filtroProfissional, inicio, fim);

        return RelatorioOcupacaoDTO.builder()
                .inicio(inicio)
                .fim(fim)
                .nomeProfissional(nomeDe(filtroProfissional))
                .total(agendamentos.size())
                .realizados(realizados)
                .cancelados(cancelados)
                .naoCompareceram(faltas)
                .remarcados(remarcados)
                .emAberto(emAberto)
                .taxaComparecimento(percentual(realizados, compareceramOuFaltaram))
                .taxaCancelamento(percentual(cancelados, agendamentos.size()))
                .taxaAbsenteismo(percentual(faltas, compareceramOuFaltaram))
                .horasAgendadas(arredondar(horasAgendadas))
                .horasDisponiveis(horasDisponiveis)
                .taxaOcupacao(horasDisponiveis == null || horasDisponiveis == 0
                        ? null
                        : arredondar(horasAgendadas / horasDisponiveis * 100))
                .build();
    }

    // ── regras de acesso ─────────────────────────────────────────

    /**
     * Devolve o profissional que deve filtrar o relatorio.
     *
     * <p>Para o PROFISSIONAL o filtro e sempre o proprio uuid — pedir outro e
     * negado em vez de silenciosamente trocado, para nao devolver um relatorio
     * diferente do que foi pedido.
     */
    private UUID resolverProfissional(UUID solicitado) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        String perfil = permissaoPolicy.perfilEfetivo(usuario);

        return switch (perfil) {
            case "ADMIN" -> solicitado;
            case "PROFISSIONAL" -> {
                if (solicitado != null && !solicitado.equals(usuario.uuid())) {
                    throw new UsuarioSemAutorizacaoException(
                            Mensagens.get("relatorio.nao-autorizado.outro-profissional"));
                }
                yield usuario.uuid();
            }
            default -> throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("relatorio.paciente-nao-acessa"));
        };
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new AgendamentoInvalidoException(Mensagens.get("relatorio.periodo-obrigatorio"));
        }
        if (fim.isBefore(inicio)) {
            throw new AgendamentoInvalidoException(Mensagens.get("relatorio.periodo-invertido"));
        }
    }

    // ── apuracao ─────────────────────────────────────────────────

    /**
     * Horas de expediente do profissional no periodo, a partir dos horarios de
     * trabalho do RF05. Devolve {@code null} quando o relatorio cobre a clinica
     * inteira ou quando o profissional nao tem expediente cadastrado — nesses
     * casos nao ha capacidade conhecida para comparar.
     */
    private Double horasDeExpediente(UUID profissionalUuid, LocalDate inicio, LocalDate fim) {
        if (profissionalUuid == null) {
            return null;
        }
        List<HorarioTrabalho> janelas =
                horarioTrabalhoRepository.findByProfissionalUuidAndAtivoTrue(profissionalUuid);
        if (janelas.isEmpty()) {
            return null;
        }

        double minutos = 0;
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            int diaSemana = dia.getDayOfWeek().getValue();
            for (HorarioTrabalho janela : janelas) {
                if (janela.getDiaSemana() == diaSemana) {
                    minutos += Duration.between(
                            janela.getHoraInicio(), janela.getHoraFim()).toMinutes();
                }
            }
        }
        return arredondar(minutos / 60.0);
    }

    private long duracaoMinutos(Agendamento a) {
        if (a.getInicioEm() == null || a.getFimEm() == null) {
            return 0;
        }
        return Duration.between(a.getInicioEm(), a.getFimEm()).toMinutes();
    }

    private int contarStatus(List<Agendamento> agendamentos, StatusAgendamento status) {
        return (int) agendamentos.stream().filter(a -> a.getStatus() == status).count();
    }

    private Map<String, Long> contar(List<Agendamento> agendamentos,
                                     java.util.function.Function<Agendamento, String> chave) {
        Map<String, Long> contagem = new LinkedHashMap<>();
        agendamentos.forEach(a -> contagem.merge(chave.apply(a), 1L, Long::sum));
        return contagem;
    }

    /** Percentual com uma casa; {@code null} quando nao ha base de comparacao. */
    private Double percentual(int parte, int total) {
        return total == 0 ? null : arredondar((double) parte / total * 100);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private String nomeProcedimento(Agendamento a) {
        if (a.getProcedimento() != null) {
            return a.getProcedimento().getNome();
        }
        return a.getTipoProcedimento() == null ? null : a.getTipoProcedimento().name();
    }

    private String nomeDe(UUID uuid) {
        return nome(new HashMap<>(), uuid);
    }

    private String nome(Map<UUID, String> cache, UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return cache.computeIfAbsent(uuid, u -> {
            try {
                return usuarioService.buscarPorUuid(u).nomeCompleto();
            } catch (RuntimeException e) {
                return Mensagens.get("relatorio.usuario-indisponivel");
            }
        });
    }
}
