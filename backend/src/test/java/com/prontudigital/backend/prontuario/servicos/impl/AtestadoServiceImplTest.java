package com.prontudigital.backend.prontuario.servicos.impl;

import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.compartilhado.documento.ArquivoGerado;
import com.prontudigital.backend.prontuario.dto.AtestadoRequestDTO;
import com.prontudigital.backend.prontuario.dto.AtestadoResponseDTO;
import com.prontudigital.backend.prontuario.entidades.Atestado;
import com.prontudigital.backend.prontuario.enums.TipoAtestado;
import com.prontudigital.backend.prontuario.excecoes.AtestadoInvalidoException;
import com.prontudigital.backend.prontuario.excecoes.AtestadoNaoEncontradoException;
import com.prontudigital.backend.prontuario.repositorios.AtestadoRepository;
import com.prontudigital.backend.prontuario.seguranca.ProntuarioPermissaoPolicy;
import com.prontudigital.backend.compartilhado.documento.MarcaDocumento;
import com.prontudigital.backend.configuracao.servicos.MarcaDocumentoProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AtestadoServiceImpl (RF17)")
class AtestadoServiceImplTest {

    private static final UUID PACIENTE_UUID = UUID.randomUUID();
    private static final UUID PROFISSIONAL_UUID = UUID.randomUUID();
    private static final UUID ATESTADO_UUID = UUID.randomUUID();

    @Mock private AtestadoRepository atestadoRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private UsuarioContexto usuarioContexto;
    @Mock private ProntuarioPermissaoPolicy permissaoPolicy;
    @Mock private MarcaDocumentoProvider marcaProvider;

    private AtestadoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AtestadoServiceImpl(
                atestadoRepository, usuarioService, usuarioContexto, permissaoPolicy,
                marcaProvider);

        // O cabecalho do PDF passou a vir da configuracao da clinica; aqui basta
        // uma marca minima para o builder montar o documento.
        when(marcaProvider.obter())
                .thenReturn(MarcaDocumento.padrao("Clinica de Teste"));

        when(usuarioContexto.getUsuarioAtual()).thenReturn(profissional());
        when(usuarioService.buscarPorUuid(PACIENTE_UUID))
                .thenReturn(UsuarioDTO.builder().nomeCompleto("Maria da Silva").build());
        when(usuarioService.buscarPorUuid(PROFISSIONAL_UUID))
                .thenReturn(UsuarioDTO.builder()
                        .nomeCompleto("Enf. Ana Souza")
                        .coren("COREN-SP 123456")
                        .build());
        when(permissaoPolicy.podeVisualizar(any(), any())).thenReturn(true);
        when(permissaoPolicy.podeEditar(any(), any())).thenReturn(true);
    }

    private UsuarioDTO profissional() {
        return UsuarioDTO.builder()
                .uuid(PROFISSIONAL_UUID)
                .nomeCompleto("Enf. Ana Souza")
                .perfis(Set.of("PROFISSIONAL"))
                .build();
    }

    private Atestado atestadoSalvo(TipoAtestado tipo, Integer dias) {
        return Atestado.builder()
                .id(1L)
                .uuid(ATESTADO_UUID)
                .pacienteUuid(PACIENTE_UUID)
                .tipo(tipo)
                .diasAfastamento(dias)
                .emitidoPor(PROFISSIONAL_UUID)
                .criadoEm(LocalDateTime.of(2026, 7, 20, 14, 30))
                .build();
    }

    // =========================================================
    // emitir()
    // =========================================================
    @Nested
    @DisplayName("emitir()")
    class Emitir {

        @Test
        @DisplayName("emite atestado de comparecimento sem dias")
        void deveEmitirComparecimento() {
            AtestadoRequestDTO request = new AtestadoRequestDTO(
                    TipoAtestado.COMPARECIMENTO, null, null, null, "Curativo realizado");
            when(atestadoRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            AtestadoResponseDTO resultado = service.emitir(PACIENTE_UUID, request);

            assertEquals(TipoAtestado.COMPARECIMENTO, resultado.tipo());
            assertNull(resultado.diasAfastamento());
            assertEquals("Maria da Silva", resultado.nomePaciente());
            assertEquals(PROFISSIONAL_UUID, resultado.emitidoPor());
        }

        @Test
        @DisplayName("emite atestado de afastamento com dias")
        void deveEmitirAfastamento() {
            AtestadoRequestDTO request = new AtestadoRequestDTO(
                    TipoAtestado.AFASTAMENTO, null, 3, "L97", null);
            when(atestadoRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            AtestadoResponseDTO resultado = service.emitir(PACIENTE_UUID, request);

            assertEquals(3, resultado.diasAfastamento());
            assertEquals("L97", resultado.cid());
        }

        @Test
        @DisplayName("afastamento sem dias e recusado")
        void deveRejeitarAfastamentoSemDias() {
            AtestadoRequestDTO request = new AtestadoRequestDTO(
                    TipoAtestado.AFASTAMENTO, null, null, null, null);

            assertThrows(AtestadoInvalidoException.class,
                    () -> service.emitir(PACIENTE_UUID, request));
            verify(atestadoRepository, never()).save(any());
        }

        @Test
        @DisplayName("comparecimento com dias e recusado")
        void deveRejeitarComparecimentoComDias() {
            AtestadoRequestDTO request = new AtestadoRequestDTO(
                    TipoAtestado.COMPARECIMENTO, null, 5, null, null);

            assertThrows(AtestadoInvalidoException.class,
                    () -> service.emitir(PACIENTE_UUID, request));
            verify(atestadoRepository, never()).save(any());
        }

        @Test
        @DisplayName("paciente nao emite o proprio atestado (RN03)")
        void deveRejeitarSemPermissaoDeEdicao() {
            when(permissaoPolicy.podeEditar(any(), any())).thenReturn(false);
            AtestadoRequestDTO request = new AtestadoRequestDTO(
                    TipoAtestado.COMPARECIMENTO, null, null, null, null);

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.emitir(PACIENTE_UUID, request));
        }

        @Test
        @DisplayName("campos em branco viram nulo")
        void deveNormalizarCamposVazios() {
            AtestadoRequestDTO request = new AtestadoRequestDTO(
                    TipoAtestado.COMPARECIMENTO, null, null, "   ", "  ");
            when(atestadoRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            AtestadoResponseDTO resultado = service.emitir(PACIENTE_UUID, request);

            assertNull(resultado.cid());
            assertNull(resultado.observacoes());
        }
    }

    // =========================================================
    // listarPorPaciente()
    // =========================================================
    @Nested
    @DisplayName("listarPorPaciente()")
    class Listar {

        @Test
        @DisplayName("lista os atestados do paciente")
        void deveListar() {
            when(atestadoRepository.findByPacienteUuidOrderByCriadoEmDesc(PACIENTE_UUID))
                    .thenReturn(List.of(
                            atestadoSalvo(TipoAtestado.AFASTAMENTO, 2),
                            atestadoSalvo(TipoAtestado.COMPARECIMENTO, null)));

            List<AtestadoResponseDTO> resultado = service.listarPorPaciente(PACIENTE_UUID);

            assertEquals(2, resultado.size());
            assertEquals("Enf. Ana Souza", resultado.get(0).nomeProfissional());
        }

        @Test
        @DisplayName("rejeita quem nao pode ver o prontuario (RN03)")
        void deveRejeitarSemPermissao() {
            when(permissaoPolicy.podeVisualizar(any(), any())).thenReturn(false);

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.listarPorPaciente(PACIENTE_UUID));
        }
    }

    // =========================================================
    // gerarPdf()
    // =========================================================
    @Nested
    @DisplayName("gerarPdf()")
    class GerarPdf {

        @Test
        @DisplayName("gera um PDF valido a partir do registro")
        void deveGerarPdf() {
            when(atestadoRepository.findByUuidAndPacienteUuid(ATESTADO_UUID, PACIENTE_UUID))
                    .thenReturn(Optional.of(atestadoSalvo(TipoAtestado.AFASTAMENTO, 3)));

            ArquivoGerado arquivo = service.gerarPdf(PACIENTE_UUID, ATESTADO_UUID);

            assertEquals("application/pdf", arquivo.contentType());
            assertTrue(arquivo.nomeArquivo().endsWith(".pdf"));
            assertTrue(arquivo.conteudo().length > 0);
            // Assinatura do formato PDF
            assertEquals("%PDF", new String(arquivo.conteudo(), 0, 4));
        }

        @Test
        @DisplayName("gera PDF de comparecimento sem dias")
        void deveGerarPdfComparecimento() {
            when(atestadoRepository.findByUuidAndPacienteUuid(ATESTADO_UUID, PACIENTE_UUID))
                    .thenReturn(Optional.of(atestadoSalvo(TipoAtestado.COMPARECIMENTO, null)));

            ArquivoGerado arquivo = service.gerarPdf(PACIENTE_UUID, ATESTADO_UUID);

            assertTrue(arquivo.conteudo().length > 0);
        }

        @Test
        @DisplayName("404 quando o atestado nao e do paciente")
        void deveRejeitarAtestadoDeOutroPaciente() {
            when(atestadoRepository.findByUuidAndPacienteUuid(ATESTADO_UUID, PACIENTE_UUID))
                    .thenReturn(Optional.empty());

            assertThrows(AtestadoNaoEncontradoException.class,
                    () -> service.gerarPdf(PACIENTE_UUID, ATESTADO_UUID));
        }

        @Test
        @DisplayName("rejeita quem nao pode ver o prontuario (RN03)")
        void deveRejeitarSemPermissao() {
            when(permissaoPolicy.podeVisualizar(any(), any())).thenReturn(false);

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.gerarPdf(PACIENTE_UUID, ATESTADO_UUID));
        }
    }

    // =========================================================
    // remover()
    // =========================================================
    @Nested
    @DisplayName("remover()")
    class Remover {

        @Test
        @DisplayName("remove o atestado do paciente")
        void deveRemover() {
            when(atestadoRepository.findByUuidAndPacienteUuid(ATESTADO_UUID, PACIENTE_UUID))
                    .thenReturn(Optional.of(atestadoSalvo(TipoAtestado.COMPARECIMENTO, null)));

            service.remover(PACIENTE_UUID, ATESTADO_UUID);

            verify(atestadoRepository).delete(any(Atestado.class));
        }

        @Test
        @DisplayName("404 quando nao existe")
        void deveRejeitarInexistente() {
            when(atestadoRepository.findByUuidAndPacienteUuid(ATESTADO_UUID, PACIENTE_UUID))
                    .thenReturn(Optional.empty());

            assertThrows(AtestadoNaoEncontradoException.class,
                    () -> service.remover(PACIENTE_UUID, ATESTADO_UUID));
            verify(atestadoRepository, never()).delete(any());
        }

        @Test
        @DisplayName("rejeita quem nao pode editar o prontuario (RN03)")
        void deveRejeitarSemPermissao() {
            when(permissaoPolicy.podeEditar(any(), any())).thenReturn(false);

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.remover(PACIENTE_UUID, ATESTADO_UUID));
        }
    }
}
