package com.prontudigital.backend.prontuario.servicos.impl;

import com.prontudigital.backend.agendamento.entidades.Agendamento;
import com.prontudigital.backend.agendamento.entidades.EvolucaoClinica;
import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import com.prontudigital.backend.prontuario.dto.HistoricoItemDTO;
import com.prontudigital.backend.prontuario.entidades.Anexo;
import com.prontudigital.backend.prontuario.entidades.Prescricao;
import com.prontudigital.backend.prontuario.enums.TipoHistorico;
import com.prontudigital.backend.prontuario.repositorios.AnexoRepository;
import com.prontudigital.backend.prontuario.repositorios.PrescricaoRepository;
import com.prontudigital.backend.prontuario.seguranca.ProntuarioPermissaoPolicy;
import com.prontudigital.backend.prontuario.servicos.HistoricoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class HistoricoServiceImpl implements HistoricoService {

    private final AgendamentoRepository agendamentoRepository;
    private final PrescricaoRepository prescricaoRepository;
    private final AnexoRepository anexoRepository;
    private final UsuarioContexto usuarioContexto;
    private final ProntuarioPermissaoPolicy permissaoPolicy;

    private static final Comparator<HistoricoItemDTO> POR_DATA_DESC =
            Comparator.comparing(HistoricoItemDTO::data,
                    Comparator.nullsLast(Comparator.reverseOrder()));

    @Override
    @Transactional(readOnly = true)
    public List<HistoricoItemDTO> montarHistorico(UUID pacienteUuid) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        if (!permissaoPolicy.podeVisualizar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("historico.nao-autorizado.visualizar"));
        }

        List<Agendamento> agendamentos =
                agendamentoRepository.findByPacienteUuidOrderByInicioEmDesc(pacienteUuid);

        Stream<HistoricoItemDTO> deAgendamentos = agendamentos.stream().map(this::itemAgendamento);
        Stream<HistoricoItemDTO> deEvolucoes = agendamentos.stream()
                .filter(a -> a.getEvolucaoClinica() != null)
                .map(this::itemEvolucao);
        Stream<HistoricoItemDTO> dePrescricoes =
                prescricaoRepository.findByPacienteUuidOrderByCriadoEmDesc(pacienteUuid)
                        .stream().map(this::itemPrescricao);
        Stream<HistoricoItemDTO> deAnexos =
                anexoRepository.findByPacienteUuidOrderByCriadoEmDesc(pacienteUuid)
                        .stream().map(this::itemAnexo);

        return Stream.of(deAgendamentos, deEvolucoes, dePrescricoes, deAnexos)
                .flatMap(s -> s)
                .sorted(POR_DATA_DESC)
                .collect(Collectors.toList());
    }

    private HistoricoItemDTO itemAgendamento(Agendamento a) {
        String titulo = a.getTipoProcedimento() != null
                ? a.getTipo() + " — " + a.getTipoProcedimento()
                : String.valueOf(a.getTipo());
        return new HistoricoItemDTO(
                TipoHistorico.AGENDAMENTO,
                a.getInicioEm(),
                titulo,
                String.valueOf(a.getStatus()),
                a.getUuid(),
                a.getUuid());
    }

    private HistoricoItemDTO itemEvolucao(Agendamento a) {
        EvolucaoClinica ev = a.getEvolucaoClinica();
        return new HistoricoItemDTO(
                TipoHistorico.EVOLUCAO,
                a.getInicioEm(),
                Mensagens.get("historico.evolucao.titulo"),
                juntar(ev.getLocalizacaoAnatomica(), ev.getEtiologia()),
                null,
                a.getUuid());
    }

    private HistoricoItemDTO itemPrescricao(Prescricao p) {
        return new HistoricoItemDTO(
                TipoHistorico.PRESCRICAO,
                p.getCriadoEm(),
                p.getDescricao(),
                juntar(p.getPosologia(), p.getFrequencia()),
                p.getUuid(),
                p.getAgendamentoUuid());
    }

    private HistoricoItemDTO itemAnexo(Anexo a) {
        return new HistoricoItemDTO(
                TipoHistorico.ANEXO,
                a.getCriadoEm(),
                a.getNomeOriginal(),
                a.getTipoConteudo(),
                a.getUuid(),
                a.getAgendamentoUuid());
    }

    /** Concatena os trechos nao vazios com " · ", ignorando nulos/brancos. */
    private String juntar(String... partes) {
        String texto = Arrays.stream(partes)
                .filter(p -> p != null && !p.isBlank())
                .collect(Collectors.joining(" · "));
        return texto.isBlank() ? null : texto;
    }
}
