package com.prontudigital.backend.prontuario.servicos.impl;

import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import com.prontudigital.backend.prontuario.dto.AnamneseRequestDTO;
import com.prontudigital.backend.prontuario.dto.AnamneseResponseDTO;
import com.prontudigital.backend.prontuario.entidades.Anamnese;
import com.prontudigital.backend.prontuario.excecoes.AnamneseJaExisteException;
import com.prontudigital.backend.prontuario.excecoes.AnamneseNaoEncontradaException;
import com.prontudigital.backend.prontuario.repositorios.AnamneseRepository;
import com.prontudigital.backend.prontuario.seguranca.ProntuarioPermissaoPolicy;
import com.prontudigital.backend.prontuario.servicos.AnamneseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnamneseServiceImpl implements AnamneseService {

    private final AnamneseRepository anamneseRepository;
    private final UsuarioService usuarioService;
    private final UsuarioContexto usuarioContexto;
    private final ProntuarioPermissaoPolicy permissaoPolicy;

    @Override
    @Transactional(readOnly = true)
    public AnamneseResponseDTO buscarPorPaciente(UUID pacienteUuid) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        if (!permissaoPolicy.podeVisualizar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("anamnese.nao-autorizado.visualizar"));
        }

        Anamnese anamnese = anamneseRepository.findByPacienteUuid(pacienteUuid)
                .orElseThrow(() -> new AnamneseNaoEncontradaException(
                        Mensagens.get("anamnese.nao-encontrada")));

        return toResponse(anamnese);
    }

    @Override
    @Transactional
    public AnamneseResponseDTO registrar(UUID pacienteUuid, AnamneseRequestDTO request) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();
        usuarioService.validarUsuarioExiste(pacienteUuid);

        if (!permissaoPolicy.podeEditar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("anamnese.nao-autorizado.registrar"));
        }

        if (anamneseRepository.existsByPacienteUuid(pacienteUuid)) {
            throw new AnamneseJaExisteException(
                    Mensagens.get("anamnese.ja-existe"));
        }

        Anamnese anamnese = Anamnese.builder()
                .pacienteUuid(pacienteUuid)
                .registradoPor(usuario.uuid())
                .build();
        aplicar(anamnese, request);

        Anamnese salva = anamneseRepository.save(anamnese);
        log.info("Anamnese registrada para paciente {} por {}", pacienteUuid, usuario.uuid());
        return toResponse(salva);
    }

    @Override
    @Transactional
    public AnamneseResponseDTO atualizar(UUID pacienteUuid, AnamneseRequestDTO request) {
        UsuarioDTO usuario = usuarioContexto.getUsuarioAtual();

        if (!permissaoPolicy.podeEditar(usuario, pacienteUuid)) {
            throw new UsuarioSemAutorizacaoException(
                    Mensagens.get("anamnese.nao-autorizado.alterar"));
        }

        Anamnese anamnese = anamneseRepository.findByPacienteUuid(pacienteUuid)
                .orElseThrow(() -> new AnamneseNaoEncontradaException(
                        Mensagens.get("anamnese.nao-encontrada")));

        aplicar(anamnese, request);
        anamnese.setRegistradoPor(usuario.uuid());

        Anamnese salva = anamneseRepository.save(anamnese);
        log.info("Anamnese atualizada para paciente {} por {}", pacienteUuid, usuario.uuid());
        return toResponse(salva);
    }

    private void aplicar(Anamnese anamnese, AnamneseRequestDTO request) {
        anamnese.setProfissao(request.profissao());
        anamnese.setResponsavelCuidador(request.responsavelCuidador());

        anamnese.setMotivoConsulta(request.motivoConsulta());
        anamnese.setTempoExistenciaFerida(request.tempoExistenciaFerida());
        anamnese.setComoFeridaSurgiu(request.comoFeridaSurgiu());
        anamnese.setDataInicioAproximada(request.dataInicioAproximada());
        anamnese.setTratamentosAnteriores(request.tratamentosAnteriores());
        anamnese.setCurativosPrevios(request.curativosPrevios());

        anamnese.setDiabetesMellitus(request.diabetesMellitus());
        anamnese.setDiabetesMellitusDetalhe(request.diabetesMellitusDetalhe());
        anamnese.setHipertensaoArterial(request.hipertensaoArterial());
        anamnese.setHipertensaoArterialDetalhe(request.hipertensaoArterialDetalhe());
        anamnese.setDoencaVenosaCronica(request.doencaVenosaCronica());
        anamnese.setDoencaVenosaCronicaDetalhe(request.doencaVenosaCronicaDetalhe());
        anamnese.setDoencaArterialPeriferica(request.doencaArterialPeriferica());
        anamnese.setDoencaArterialPerifericaDetalhe(request.doencaArterialPerifericaDetalhe());
        anamnese.setInsuficienciaRenal(request.insuficienciaRenal());
        anamnese.setInsuficienciaRenalDetalhe(request.insuficienciaRenalDetalhe());
        anamnese.setCancer(request.cancer());
        anamnese.setCancerDetalhe(request.cancerDetalhe());
        anamnese.setProblemasNeurologicos(request.problemasNeurologicos());
        anamnese.setProblemasNeurologicosDetalhe(request.problemasNeurologicosDetalhe());
        anamnese.setHistoricoCirurgias(request.historicoCirurgias());
        anamnese.setHistoricoCirurgiasDetalhe(request.historicoCirurgiasDetalhe());

        anamnese.setMedAntibioticos(request.medAntibioticos());
        anamnese.setMedAnticoagulantes(request.medAnticoagulantes());
        anamnese.setMedCorticoides(request.medCorticoides());
        anamnese.setMedInsulinaHipoglicemiantes(request.medInsulinaHipoglicemiantes());
        anamnese.setMedOutrosContinuos(request.medOutrosContinuos());

        anamnese.setAlergiaMedicamentos(request.alergiaMedicamentos());
        anamnese.setAlergiaProdutosTopicos(request.alergiaProdutosTopicos());
        anamnese.setAlergiaCurativosAdesivos(request.alergiaCurativosAdesivos());

        anamnese.setTabagismo(request.tabagismo());
        anamnese.setConsumoAlcool(request.consumoAlcool());
        anamnese.setAlimentacaoEstadoNutricional(request.alimentacaoEstadoNutricional());
        anamnese.setIngestaoHidrica(request.ingestaoHidrica());
        anamnese.setIngestaoHidricaDetalhe(request.ingestaoHidricaDetalhe());

        anamnese.setDeambulaSozinho(request.deambulaSozinho());
        anamnese.setDeambulaSozinhoDetalhe(request.deambulaSozinhoDetalhe());
        anamnese.setAcamadoOuCadeirante(request.acamadoOuCadeirante());
        anamnese.setAcamadoOuCadeiranteDetalhe(request.acamadoOuCadeiranteDetalhe());
        anamnese.setUsoDispositivos(request.usoDispositivos());
        anamnese.setUsoDispositivosDetalhe(request.usoDispositivosDetalhe());
        anamnese.setMudancaPosicaoLeito(request.mudancaPosicaoLeito());
        anamnese.setMudancaPosicaoLeitoDetalhe(request.mudancaPosicaoLeitoDetalhe());

        anamnese.setExamesRecentes(request.examesRecentes());
        anamnese.setRedeApoio(request.redeApoio());
        anamnese.setAcompanhamentoMedico(request.acompanhamentoMedico());
        anamnese.setAcompanhamentoMedicoDetalhe(request.acompanhamentoMedicoDetalhe());
    }

    private AnamneseResponseDTO toResponse(Anamnese a) {
        String pacienteNome = usuarioService.buscarPorUuid(a.getPacienteUuid()).nomeCompleto();
        return AnamneseResponseDTO.builder()
                .uuid(a.getUuid())
                .pacienteUuid(a.getPacienteUuid())
                .pacienteNome(pacienteNome)
                .profissao(a.getProfissao())
                .responsavelCuidador(a.getResponsavelCuidador())
                .motivoConsulta(a.getMotivoConsulta())
                .tempoExistenciaFerida(a.getTempoExistenciaFerida())
                .comoFeridaSurgiu(a.getComoFeridaSurgiu())
                .dataInicioAproximada(a.getDataInicioAproximada())
                .tratamentosAnteriores(a.getTratamentosAnteriores())
                .curativosPrevios(a.getCurativosPrevios())
                .diabetesMellitus(a.getDiabetesMellitus())
                .diabetesMellitusDetalhe(a.getDiabetesMellitusDetalhe())
                .hipertensaoArterial(a.getHipertensaoArterial())
                .hipertensaoArterialDetalhe(a.getHipertensaoArterialDetalhe())
                .doencaVenosaCronica(a.getDoencaVenosaCronica())
                .doencaVenosaCronicaDetalhe(a.getDoencaVenosaCronicaDetalhe())
                .doencaArterialPeriferica(a.getDoencaArterialPeriferica())
                .doencaArterialPerifericaDetalhe(a.getDoencaArterialPerifericaDetalhe())
                .insuficienciaRenal(a.getInsuficienciaRenal())
                .insuficienciaRenalDetalhe(a.getInsuficienciaRenalDetalhe())
                .cancer(a.getCancer())
                .cancerDetalhe(a.getCancerDetalhe())
                .problemasNeurologicos(a.getProblemasNeurologicos())
                .problemasNeurologicosDetalhe(a.getProblemasNeurologicosDetalhe())
                .historicoCirurgias(a.getHistoricoCirurgias())
                .historicoCirurgiasDetalhe(a.getHistoricoCirurgiasDetalhe())
                .medAntibioticos(a.getMedAntibioticos())
                .medAnticoagulantes(a.getMedAnticoagulantes())
                .medCorticoides(a.getMedCorticoides())
                .medInsulinaHipoglicemiantes(a.getMedInsulinaHipoglicemiantes())
                .medOutrosContinuos(a.getMedOutrosContinuos())
                .alergiaMedicamentos(a.getAlergiaMedicamentos())
                .alergiaProdutosTopicos(a.getAlergiaProdutosTopicos())
                .alergiaCurativosAdesivos(a.getAlergiaCurativosAdesivos())
                .tabagismo(a.getTabagismo())
                .consumoAlcool(a.getConsumoAlcool())
                .alimentacaoEstadoNutricional(a.getAlimentacaoEstadoNutricional())
                .ingestaoHidrica(a.getIngestaoHidrica())
                .ingestaoHidricaDetalhe(a.getIngestaoHidricaDetalhe())
                .deambulaSozinho(a.getDeambulaSozinho())
                .deambulaSozinhoDetalhe(a.getDeambulaSozinhoDetalhe())
                .acamadoOuCadeirante(a.getAcamadoOuCadeirante())
                .acamadoOuCadeiranteDetalhe(a.getAcamadoOuCadeiranteDetalhe())
                .usoDispositivos(a.getUsoDispositivos())
                .usoDispositivosDetalhe(a.getUsoDispositivosDetalhe())
                .mudancaPosicaoLeito(a.getMudancaPosicaoLeito())
                .mudancaPosicaoLeitoDetalhe(a.getMudancaPosicaoLeitoDetalhe())
                .examesRecentes(a.getExamesRecentes())
                .redeApoio(a.getRedeApoio())
                .acompanhamentoMedico(a.getAcompanhamentoMedico())
                .acompanhamentoMedicoDetalhe(a.getAcompanhamentoMedicoDetalhe())
                .registradoPor(a.getRegistradoPor())
                .criadoEm(a.getCriadoEm())
                .atualizadoEm(a.getAtualizadoEm())
                .build();
    }
}
