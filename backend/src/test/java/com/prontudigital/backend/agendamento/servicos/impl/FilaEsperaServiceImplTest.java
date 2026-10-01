package com.prontudigital.backend.agendamento.servicos.impl;

import com.prontudigital.backend.agendamento.dto.FilaEsperaDTO;
import com.prontudigital.backend.agendamento.dto.FilaEsperaRequestDTO;
import com.prontudigital.backend.agendamento.entidades.FilaEspera;
import com.prontudigital.backend.agendamento.enums.StatusFilaEspera;
import com.prontudigital.backend.agendamento.eventos.AgendamentoCanceladoEvento;
import com.prontudigital.backend.agendamento.excecoes.AgendamentoNaoEncontradoException;
import com.prontudigital.backend.agendamento.repositorios.FilaEsperaRepository;
import com.prontudigital.backend.agendamento.seguranca.AgendamentoPermissaoPolicy;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.AgendamentoTestFixtures.*;
import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.FilaEsperaTestFixtures.*;
import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.FilaEsperaTestFixtures.OUTRO_UUID;
import static com.prontudigital.backend.agendamento.servicos.impl.fixtures.FilaEsperaTestFixtures.PROFISSIONAL_UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FilaEsperaServiceImpl")
class FilaEsperaServiceImplTest {

    @Mock private FilaEsperaRepository repository;
    @Mock private UsuarioContexto usuarioContexto;

    @Spy
    private AgendamentoPermissaoPolicy permissaoPolicy = new AgendamentoPermissaoPolicy();

    @InjectMocks
    private FilaEsperaServiceImpl service;

    // =========================================================
    // entrar()
    // =========================================================
    @Nested
    @DisplayName("entrar()")
    class Entrar {

        @Test
        @DisplayName("paciente entra na fila para si mesmo com sucesso")
        void pacienteEntraNaPropriaFila() {
            FilaEsperaRequestDTO request = requestValido();

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());
            when(repository.save(any())).thenReturn(filaAtiva());

            FilaEsperaDTO resultado = service.entrar(request);

            assertNotNull(resultado);
            assertEquals(StatusFilaEspera.ATIVO, resultado.status());
            verify(repository).save(any(FilaEspera.class));
        }

        @Test
        @DisplayName("paciente nao pode entrar na fila por outro paciente")
        void pacienteNaoPodeEntrarPorOutro() {
            // Fixture usa PACIENTE_UUID nas fixtures de Agendamento.
            // Aqui o request usa OUTRO_UUID como paciente — diferente do logado.
            FilaEsperaRequestDTO request = requestComPaciente(OUTRO_UUID);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class, () -> service.entrar(request));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("admin pode adicionar qualquer paciente na fila")
        void adminPodeAdicionarQualquer() {
            FilaEsperaRequestDTO request = requestComPaciente(OUTRO_UUID);

            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioAdmin());
            when(repository.save(any())).thenReturn(filaAtiva());

            assertDoesNotThrow(() -> service.entrar(request));
            verify(repository).save(any());
        }
    }

    // =========================================================
    // sair()
    // =========================================================
    @Nested
    @DisplayName("sair()")
    class Sair {

        @Test
        @DisplayName("paciente sai da propria fila com sucesso")
        void pacienteSaiDaPropriaFila() {
            FilaEspera fila = filaAtiva();

            when(repository.findById(1L)).thenReturn(Optional.of(fila));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            service.sair(1L);

            assertEquals(StatusFilaEspera.CANCELADO, fila.getStatus());
            verify(repository).save(fila);
        }

        @Test
        @DisplayName("paciente nao pode remover outro paciente da fila")
        void pacienteNaoPodeRemoverOutro() {
            FilaEspera fila = filaAtiva();
            fila.setPacienteUuid(OUTRO_UUID);

            when(repository.findById(1L)).thenReturn(Optional.of(fila));
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuarioPaciente());

            assertThrows(UsuarioSemAutorizacaoException.class, () -> service.sair(1L));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("entrada inexistente lanca excecao")
        void deveLancarSeNaoEncontrada() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(AgendamentoNaoEncontradoException.class, () -> service.sair(999L));
        }
    }

    // =========================================================
    // marcarComoNotificado()
    // =========================================================
    @Nested
    @DisplayName("marcarComoNotificado()")
    class MarcarComoNotificado {

        @Test
        @DisplayName("marca entrada como notificada com sucesso")
        void deveMarcarComoNotificado() {
            FilaEspera fila = filaAtiva();

            when(repository.findById(1L)).thenReturn(Optional.of(fila));

            service.marcarComoNotificado(1L);

            assertEquals(StatusFilaEspera.NOTIFICADO, fila.getStatus());
            verify(repository).save(fila);
        }

        @Test
        @DisplayName("entrada inexistente lanca excecao")
        void deveLancarSeNaoEncontrada() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(AgendamentoNaoEncontradoException.class,
                    () -> service.marcarComoNotificado(999L));
        }
    }

    // =========================================================
    // listarFilaDoProfissional()
    // =========================================================
    @Nested
    @DisplayName("listarFilaDoProfissional()")
    class ListarFila {

        @Test
        @DisplayName("retorna apenas entradas ativas ordenadas por prioridade")
        void deveListarApenasAtivas() {
            when(repository.findByProfissionalUuidAndStatusOrderByPrioridadeAscCriadoEmAsc(
                    eq(PROFISSIONAL_UUID), eq(StatusFilaEspera.ATIVO)))
                    .thenReturn(List.of(filaAtiva(), filaAtiva()));

            List<FilaEsperaDTO> resultado = service.listarFilaDoProfissional(PROFISSIONAL_UUID);

            assertEquals(2, resultado.size());
            verify(repository).findByProfissionalUuidAndStatusOrderByPrioridadeAscCriadoEmAsc(
                    PROFISSIONAL_UUID, StatusFilaEspera.ATIVO);
        }
    }

    // =========================================================
    // onAgendamentoCancelado() — listener do RF12
    // =========================================================
    @Nested
    @DisplayName("onAgendamentoCancelado()")
    class OnAgendamentoCancelado {

        @Test
        @DisplayName("notifica o primeiro da fila quando ha pacientes esperando")
        void deveNotificarProximo() {
            AgendamentoCanceladoEvento evento = eventoCancelamento();
            FilaEspera proximo = filaAtiva();

            when(repository.findByProfissionalUuidAndStatusOrderByPrioridadeAscCriadoEmAsc(
                    eq(PROFISSIONAL_UUID), eq(StatusFilaEspera.ATIVO)))
                    .thenReturn(List.of(proximo));

            service.onAgendamentoCancelado(evento);

            assertEquals(StatusFilaEspera.NOTIFICADO, proximo.getStatus());
            verify(repository).save(proximo);
        }

        @Test
        @DisplayName("nao faz nada quando fila esta vazia")
        void naoFazNadaSeFilaVazia() {
            AgendamentoCanceladoEvento evento = eventoCancelamento();

            when(repository.findByProfissionalUuidAndStatusOrderByPrioridadeAscCriadoEmAsc(
                    any(), any()))
                    .thenReturn(List.of());

            service.onAgendamentoCancelado(evento);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("notifica apenas o primeiro da fila quando ha varios")
        void deveNotificarApenasOPrimeiro() {
            AgendamentoCanceladoEvento evento = eventoCancelamento();
            FilaEspera primeiro = filaAtiva();
            FilaEspera segundo = filaAtiva();
            segundo.setId(2L);

            when(repository.findByProfissionalUuidAndStatusOrderByPrioridadeAscCriadoEmAsc(
                    any(), any()))
                    .thenReturn(List.of(primeiro, segundo));

            service.onAgendamentoCancelado(evento);

            assertEquals(StatusFilaEspera.NOTIFICADO, primeiro.getStatus());
            assertEquals(StatusFilaEspera.ATIVO, segundo.getStatus());   // intocado
            verify(repository, times(1)).save(any());                    // só 1 save
            verify(repository).save(primeiro);
        }
    }
}