package com.prontudigital.backend.prontuario.servicos.impl;

import com.prontudigital.backend.agendamento.repositorios.AgendamentoRepository;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.UsuarioSemAutorizacaoException;
import com.prontudigital.backend.autenticacao.seguranca.UsuarioContexto;
import com.prontudigital.backend.autenticacao.servicos.UsuarioService;
import com.prontudigital.backend.compartilhado.armazenamento.ArmazenamentoException;
import com.prontudigital.backend.compartilhado.armazenamento.ArmazenamentoProperties;
import com.prontudigital.backend.compartilhado.armazenamento.ArmazenamentoService;
import com.prontudigital.backend.prontuario.dto.AnexoDownloadDTO;
import com.prontudigital.backend.prontuario.dto.AnexoResponseDTO;
import com.prontudigital.backend.prontuario.entidades.Anexo;
import com.prontudigital.backend.prontuario.excecoes.AnexoInvalidoException;
import com.prontudigital.backend.prontuario.excecoes.AnexoNaoEncontradoException;
import com.prontudigital.backend.prontuario.repositorios.AnexoRepository;
import com.prontudigital.backend.prontuario.seguranca.ProntuarioPermissaoPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnexoServiceImpl")
class AnexoServiceImplTest {

    @Mock private AnexoRepository anexoRepository;
    @Mock private UsuarioService usuarioService;
    @Mock private UsuarioContexto usuarioContexto;
    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private ArmazenamentoService armazenamentoService;

    private ProntuarioPermissaoPolicy permissaoPolicy;
    private ArmazenamentoProperties propriedades;
    private AnexoServiceImpl service;

    private static final UUID PACIENTE_UUID     = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROFISSIONAL_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID OUTRO_UUID        = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID ANEXO_UUID        = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        permissaoPolicy = new ProntuarioPermissaoPolicy(agendamentoRepository);
        propriedades = new ArmazenamentoProperties();
        service = new AnexoServiceImpl(
                anexoRepository, usuarioService, usuarioContexto,
                permissaoPolicy, armazenamentoService, propriedades);
    }

    private UsuarioDTO usuario(UUID uuid, String perfil) {
        return UsuarioDTO.builder().uuid(uuid).perfis(Set.of(perfil)).build();
    }

    private void mockPacienteNome() {
        when(usuarioService.buscarPorUuid(PACIENTE_UUID))
                .thenReturn(UsuarioDTO.builder().uuid(PACIENTE_UUID).nomeCompleto("Carlos").build());
    }

    private MultipartFile arquivo(String nome, String tipo, long tamanho) throws IOException {
        MultipartFile mf = mock(MultipartFile.class);
        when(mf.isEmpty()).thenReturn(false);
        when(mf.getSize()).thenReturn(tamanho);
        when(mf.getContentType()).thenReturn(tipo);
        when(mf.getOriginalFilename()).thenReturn(nome);
        when(mf.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));
        return mf;
    }

    private Anexo anexoExistente() {
        return Anexo.builder()
                .id(1L)
                .pacienteUuid(PACIENTE_UUID)
                .nomeOriginal("exame.pdf")
                .tipoConteudo("application/pdf")
                .tamanhoBytes(1234L)
                .chaveArmazenamento(PACIENTE_UUID + "/abc.pdf")
                .build();
    }

    private void autorizarProfissional() {
        when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
        when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                .thenReturn(true);
    }

    private void mockPersistencia() {
        when(anexoRepository.save(any(Anexo.class))).thenAnswer(inv -> inv.getArgument(0));
        mockPacienteNome();
    }

    private Anexo capturarAnexoSalvo() {
        ArgumentCaptor<Anexo> captor = ArgumentCaptor.forClass(Anexo.class);
        verify(anexoRepository).save(captor.capture());
        return captor.getValue();
    }

    // =========================================================
    // enviar()
    // =========================================================
    @Nested
    @DisplayName("enviar()")
    class Enviar {

        @Test
        @DisplayName("profissional vinculado envia anexo com sucesso")
        void deveEnviar() throws IOException {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
            when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                    .thenReturn(true);
            when(anexoRepository.save(any(Anexo.class))).thenAnswer(inv -> inv.getArgument(0));
            mockPacienteNome();

            AnexoResponseDTO resp = service.enviar(
                    PACIENTE_UUID, arquivo("foto-ferida.png", "image/png", 2048L), null);

            assertEquals("foto-ferida.png", resp.nomeOriginal());
            assertEquals("image/png", resp.tipoConteudo());
            assertEquals(2048L, resp.tamanhoBytes());
            assertEquals("Carlos", resp.pacienteNome());

            verify(armazenamentoService).salvar(anyString(), any(), anyLong());

            ArgumentCaptor<Anexo> captor = ArgumentCaptor.forClass(Anexo.class);
            verify(anexoRepository).save(captor.capture());
            assertEquals(PROFISSIONAL_UUID, captor.getValue().getRegistradoPor());
            assertTrue(captor.getValue().getChaveArmazenamento().startsWith(PACIENTE_UUID + "/"));
            assertTrue(captor.getValue().getChaveArmazenamento().endsWith(".png"));
        }

        @Test
        @DisplayName("rejeita tipo de arquivo nao permitido")
        void deveRejeitarTipoInvalido() throws IOException {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
            when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                    .thenReturn(true);

            MultipartFile mf = mock(MultipartFile.class);
            when(mf.isEmpty()).thenReturn(false);
            when(mf.getSize()).thenReturn(1024L);
            when(mf.getContentType()).thenReturn("text/plain");

            assertThrows(AnexoInvalidoException.class,
                    () -> service.enviar(PACIENTE_UUID, mf, null));
            verify(armazenamentoService, never()).salvar(anyString(), any(), anyLong());
            verify(anexoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita arquivo acima do tamanho maximo")
        void deveRejeitarTamanho() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
            when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                    .thenReturn(true);

            MultipartFile mf = mock(MultipartFile.class);
            when(mf.isEmpty()).thenReturn(false);
            when(mf.getSize()).thenReturn(propriedades.getTamanhoMaximoBytes() + 1);

            assertThrows(AnexoInvalidoException.class,
                    () -> service.enviar(PACIENTE_UUID, mf, null));
            verify(anexoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita profissional sem vinculo com o paciente (RN03)")
        void deveRejeitarSemVinculo() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
            when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                    .thenReturn(false);

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.enviar(PACIENTE_UUID, mock(MultipartFile.class), null));
            verify(anexoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita arquivo nulo")
        void deveRejeitarArquivoNulo() {
            autorizarProfissional();

            assertThrows(AnexoInvalidoException.class,
                    () -> service.enviar(PACIENTE_UUID, null, null));
            verify(anexoRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita arquivo vazio")
        void deveRejeitarArquivoVazio() {
            autorizarProfissional();
            MultipartFile mf = mock(MultipartFile.class);
            when(mf.isEmpty()).thenReturn(true);

            assertThrows(AnexoInvalidoException.class,
                    () -> service.enviar(PACIENTE_UUID, mf, null));
            verify(anexoRepository, never()).save(any());
        }

        @Test
        @DisplayName("converte falha de I/O na gravacao em ArmazenamentoException")
        void deveTratarIOException() throws IOException {
            autorizarProfissional();
            MultipartFile mf = mock(MultipartFile.class);
            when(mf.isEmpty()).thenReturn(false);
            when(mf.getSize()).thenReturn(2048L);
            when(mf.getContentType()).thenReturn("image/png");
            when(mf.getOriginalFilename()).thenReturn("x.png");
            when(mf.getInputStream()).thenThrow(new IOException("disco cheio"));

            assertThrows(ArmazenamentoException.class,
                    () -> service.enviar(PACIENTE_UUID, mf, null));
            verify(anexoRepository, never()).save(any());
        }

        @Test
        @DisplayName("deriva a extensao a partir do MIME (jpeg/webp/pdf)")
        void deveDerivarExtensaoPorMime() throws IOException {
            autorizarProfissional();
            mockPersistencia();

            service.enviar(PACIENTE_UUID, arquivo("f", "image/jpeg", 2048L), null);
            assertTrue(capturarAnexoSalvo().getChaveArmazenamento().endsWith(".jpg"));
        }

        @Test
        @DisplayName("usa extensao .webp para image/webp")
        void deveDerivarWebp() throws IOException {
            autorizarProfissional();
            mockPersistencia();

            service.enviar(PACIENTE_UUID, arquivo("f", "image/webp", 2048L), null);
            assertTrue(capturarAnexoSalvo().getChaveArmazenamento().endsWith(".webp"));
        }

        @Test
        @DisplayName("usa extensao .pdf para application/pdf")
        void deveDerivarPdf() throws IOException {
            autorizarProfissional();
            mockPersistencia();

            service.enviar(PACIENTE_UUID, arquivo("f", "application/pdf", 2048L), null);
            assertTrue(capturarAnexoSalvo().getChaveArmazenamento().endsWith(".pdf"));
        }

        @Test
        @DisplayName("grava sem extensao quando o MIME permitido nao mapeia extensao")
        void deveGravarSemExtensaoParaMimeSemMapa() throws IOException {
            propriedades.setTiposPermitidos(java.util.List.of("image/gif"));
            autorizarProfissional();
            mockPersistencia();

            service.enviar(PACIENTE_UUID, arquivo("f", "image/gif", 2048L), null);

            // extensaoPara -> default "" : a chave nao possui ponto de extensao
            assertFalse(capturarAnexoSalvo().getChaveArmazenamento().contains("."));
        }

        @Test
        @DisplayName("usa nome padrao quando o nome original eh nulo")
        void deveUsarNomePadraoQuandoNulo() throws IOException {
            autorizarProfissional();
            mockPersistencia();

            AnexoResponseDTO resp = service.enviar(
                    PACIENTE_UUID, arquivo(null, "image/png", 2048L), null);

            assertEquals("arquivo", resp.nomeOriginal());
        }

        @Test
        @DisplayName("usa nome padrao quando o nome original eh em branco")
        void deveUsarNomePadraoQuandoEmBranco() throws IOException {
            autorizarProfissional();
            mockPersistencia();

            AnexoResponseDTO resp = service.enviar(
                    PACIENTE_UUID, arquivo("   ", "image/png", 2048L), null);

            assertEquals("arquivo", resp.nomeOriginal());
        }

        @Test
        @DisplayName("remove o caminho do nome (protege contra path traversal)")
        void deveRemoverCaminhoDoNome() throws IOException {
            autorizarProfissional();
            mockPersistencia();

            AnexoResponseDTO resp = service.enviar(
                    PACIENTE_UUID, arquivo("../../etc/senha.pdf", "application/pdf", 2048L), null);

            assertEquals("senha.pdf", resp.nomeOriginal());
        }

        @Test
        @DisplayName("usa nome padrao quando sobra apenas pontos apos limpeza")
        void deveUsarNomePadraoQuandoSoPontos() throws IOException {
            autorizarProfissional();
            mockPersistencia();

            AnexoResponseDTO resp = service.enviar(
                    PACIENTE_UUID, arquivo("..", "application/pdf", 2048L), null);

            assertEquals("arquivo", resp.nomeOriginal());
        }

        @Test
        @DisplayName("trunca nome muito longo para 255 caracteres")
        void deveTruncarNomeLongo() throws IOException {
            autorizarProfissional();
            mockPersistencia();

            AnexoResponseDTO resp = service.enviar(
                    PACIENTE_UUID, arquivo("a".repeat(300), "image/png", 2048L), null);

            assertEquals(255, resp.nomeOriginal().length());
        }
    }

    // =========================================================
    // listarPorPaciente()
    // =========================================================
    @Nested
    @DisplayName("listarPorPaciente()")
    class Listar {

        @Test
        @DisplayName("paciente lista os proprios anexos")
        void pacienteVeProprios() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PACIENTE_UUID, "PACIENTE"));
            when(anexoRepository.findByPacienteUuidOrderByCriadoEmDesc(PACIENTE_UUID))
                    .thenReturn(java.util.List.of(anexoExistente()));
            mockPacienteNome();

            var resp = service.listarPorPaciente(PACIENTE_UUID);

            assertEquals(1, resp.size());
            assertEquals("exame.pdf", resp.get(0).nomeOriginal());
        }

        @Test
        @DisplayName("paciente nao lista anexos de outro paciente")
        void pacienteNaoVeDeOutro() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "PACIENTE"));

            assertThrows(UsuarioSemAutorizacaoException.class,
                    () -> service.listarPorPaciente(PACIENTE_UUID));
            verify(anexoRepository, never()).findByPacienteUuidOrderByCriadoEmDesc(any());
        }
    }

    // =========================================================
    // baixar()
    // =========================================================
    @Nested
    @DisplayName("baixar()")
    class Baixar {

        @Test
        @DisplayName("profissional vinculado baixa o conteudo do anexo")
        void deveBaixar() {
            Resource recurso = new ByteArrayResource(new byte[] {9, 8, 7});
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
            when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                    .thenReturn(true);
            when(anexoRepository.findByUuidAndPacienteUuid(ANEXO_UUID, PACIENTE_UUID))
                    .thenReturn(Optional.of(anexoExistente()));
            when(armazenamentoService.carregar("11111111-1111-1111-1111-111111111111/abc.pdf"))
                    .thenReturn(recurso);

            AnexoDownloadDTO download = service.baixar(PACIENTE_UUID, ANEXO_UUID);

            assertEquals("exame.pdf", download.nomeOriginal());
            assertEquals("application/pdf", download.tipoConteudo());
            assertSame(recurso, download.recurso());
        }

        @Test
        @DisplayName("rejeita download de anexo inexistente")
        void deveRejeitarInexistente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "ADMIN"));
            when(anexoRepository.findByUuidAndPacienteUuid(ANEXO_UUID, PACIENTE_UUID))
                    .thenReturn(Optional.empty());

            assertThrows(AnexoNaoEncontradoException.class,
                    () -> service.baixar(PACIENTE_UUID, ANEXO_UUID));
        }
    }

    // =========================================================
    // excluir()
    // =========================================================
    @Nested
    @DisplayName("excluir()")
    class Excluir {

        @Test
        @DisplayName("profissional vinculado exclui anexo e remove o arquivo")
        void deveExcluir() {
            Anexo existente = anexoExistente();
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(PROFISSIONAL_UUID, "PROFISSIONAL"));
            when(agendamentoRepository.existsByProfissionalUuidAndPacienteUuid(PROFISSIONAL_UUID, PACIENTE_UUID))
                    .thenReturn(true);
            when(anexoRepository.findByUuidAndPacienteUuid(ANEXO_UUID, PACIENTE_UUID))
                    .thenReturn(Optional.of(existente));

            service.excluir(PACIENTE_UUID, ANEXO_UUID);

            verify(armazenamentoService).remover(existente.getChaveArmazenamento());
            verify(anexoRepository).delete(existente);
        }

        @Test
        @DisplayName("rejeita exclusao de anexo inexistente")
        void deveRejeitarInexistente() {
            when(usuarioContexto.getUsuarioAtual()).thenReturn(usuario(OUTRO_UUID, "ADMIN"));
            when(anexoRepository.findByUuidAndPacienteUuid(ANEXO_UUID, PACIENTE_UUID))
                    .thenReturn(Optional.empty());

            assertThrows(AnexoNaoEncontradoException.class,
                    () -> service.excluir(PACIENTE_UUID, ANEXO_UUID));
            verify(anexoRepository, never()).delete(any());
        }
    }
}
