package com.prontudigital.backend.autenticacao.servicos.impl;

import com.prontudigital.backend.autenticacao.dto.AlterarSenhaRequestDTO;
import com.prontudigital.backend.autenticacao.dto.AtivarAcessoRequestDTO;
import com.prontudigital.backend.autenticacao.dto.AtualizarPerfilRequestDTO;
import com.prontudigital.backend.autenticacao.dto.CadastrarPacienteDTO;
import com.prontudigital.backend.autenticacao.dto.EnderecoDTO;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.entidades.Endereco;
import com.prontudigital.backend.autenticacao.entidades.Perfil;
import com.prontudigital.backend.autenticacao.entidades.Usuario;
import com.prontudigital.backend.autenticacao.excecoes.*;
import com.prontudigital.backend.autenticacao.repositorios.PerfilRepository;
import com.prontudigital.backend.autenticacao.repositorios.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.prontudigital.backend.autenticacao.servicos.impl.fixtures.AutenticacaoTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioServiceImpl")
class UsuarioServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PerfilRepository perfilRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl service;

    // =========================================================
    // listarTodos()
    // =========================================================
    @Nested
    @DisplayName("listarTodos()")
    class ListarTodos {

        @Test
        @DisplayName("retorna todos os usuarios convertidos para DTO")
        void deveListarTodos() {
            when(usuarioRepository.findAll())
                    .thenReturn(List.of(usuarioComPerfil(), usuarioComPerfil()));

            List<UsuarioDTO> resultado = service.listarTodos();

            assertEquals(2, resultado.size());
        }

        @Test
        @DisplayName("retorna lista vazia quando nao ha usuarios")
        void deveRetornarVaziaQuandoSemUsuarios() {
            when(usuarioRepository.findAll()).thenReturn(List.of());

            assertTrue(service.listarTodos().isEmpty());
        }
    }

    // =========================================================
    // buscarPorId()
    // =========================================================
    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("retorna usuario quando encontrado")
        void deveRetornarUsuario() {
            when(usuarioRepository.findById(USUARIO_ID))
                    .thenReturn(Optional.of(usuarioComPerfil()));

            UsuarioDTO resultado = service.buscarPorId(USUARIO_ID);

            assertEquals(USERNAME, resultado.username());
        }

        @Test
        @DisplayName("lanca excecao quando nao encontrado")
        void deveLancarQuandoInexistente() {
            when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.buscarPorId(999L));
        }
    }

    // =========================================================
    // buscarPorUuid()
    // =========================================================
    @Nested
    @DisplayName("buscarPorUuid()")
    class BuscarPorUuid {

        @Test
        @DisplayName("retorna usuario quando encontrado")
        void deveRetornarUsuario() {
            when(usuarioRepository.findByUuid(USUARIO_UUID))
                    .thenReturn(Optional.of(usuarioComPerfil()));

            UsuarioDTO resultado = service.buscarPorUuid(USUARIO_UUID);

            assertEquals(USUARIO_UUID, resultado.uuid());
        }

        @Test
        @DisplayName("lanca excecao quando nao encontrado")
        void deveLancarQuandoInexistente() {
            UUID uuid = UUID.randomUUID();
            when(usuarioRepository.findByUuid(uuid)).thenReturn(Optional.empty());

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.buscarPorUuid(uuid));
        }
    }

    // =========================================================
    // criar()
    // =========================================================
    @Nested
    @DisplayName("criar()")
    class Criar {

        @Test
        @DisplayName("cria usuario com sucesso e codifica senha")
        void deveCriarComSucesso() {
            UsuarioDTO dto = usuarioDTO();

            when(usuarioRepository.existsByUsername(USERNAME)).thenReturn(false);
            when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(SENHA_RAW)).thenReturn(SENHA_HASH);
            when(perfilRepository.findByNome("PACIENTE"))
                    .thenReturn(Optional.of(perfilPaciente()));
            when(usuarioRepository.save(any(Usuario.class)))
                    .thenAnswer(inv -> {
                        Usuario u = inv.getArgument(0);
                        u.setId(USUARIO_ID);
                        u.setUuid(USUARIO_UUID);
                        return u;
                    });

            UsuarioDTO resultado = service.criar(dto, SENHA_RAW);

            assertEquals(USERNAME, resultado.username());
            verify(passwordEncoder).encode(SENHA_RAW);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertEquals(SENHA_HASH, captor.getValue().getSenhaHash());
        }

        @Test
        @DisplayName("usa ativo=true como default quando dto.ativo eh null")
        void deveUsarAtivoTruePorDefault() {
            UsuarioDTO dto = usuarioDTOSemAtivo();

            when(usuarioRepository.existsByUsername(any())).thenReturn(false);
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn(SENHA_HASH);
            when(perfilRepository.findByNome(any()))
                    .thenReturn(Optional.of(perfilPaciente()));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.criar(dto, SENHA_RAW);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertTrue(captor.getValue().getAtivo());
        }

        @Test
        @DisplayName("nao chama atualizarPerfis quando perfis eh null")
        void naoAtualizaPerfisQuandoNull() {
            UsuarioDTO dto = UsuarioDTO.builder()
                    .username(USERNAME)
                    .email(EMAIL)
                    .nomeCompleto(NOME)
                    .ativo(true)
                    .perfis(null)
                    .build();

            when(usuarioRepository.existsByUsername(any())).thenReturn(false);
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn(SENHA_HASH);
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.criar(dto, SENHA_RAW);

            verify(perfilRepository, never()).findByNome(any());
        }

        @Test
        @DisplayName("rejeita criacao quando username ja existe")
        void deveRejeitarUsernameDuplicado() {
            UsuarioDTO dto = usuarioDTO();

            when(usuarioRepository.existsByUsername(USERNAME)).thenReturn(true);

            assertThrows(UserNameExistenteException.class,
                    () -> service.criar(dto, SENHA_RAW));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita criacao quando email ja existe")
        void deveRejeitarEmailDuplicado() {
            UsuarioDTO dto = usuarioDTO();

            when(usuarioRepository.existsByUsername(USERNAME)).thenReturn(false);
            when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThrows(EmailExistenteException.class,
                    () -> service.criar(dto, SENHA_RAW));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita criacao quando perfil nao existe")
        void deveRejeitarPerfilInexistente() {
            UsuarioDTO dto = UsuarioDTO.builder()
                    .username(USERNAME)
                    .email(EMAIL)
                    .nomeCompleto(NOME)
                    .ativo(true)
                    .perfis(Set.of("INEXISTENTE"))
                    .build();

            when(usuarioRepository.existsByUsername(any())).thenReturn(false);
            when(usuarioRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn(SENHA_HASH);
            when(perfilRepository.findByNome("INEXISTENTE")).thenReturn(Optional.empty());

            assertThrows(PerfilNaoEncontradoException.class,
                    () -> service.criar(dto, SENHA_RAW));
        }
    }

    // =========================================================
    // atualizar()
    // =========================================================
    @Nested
    @DisplayName("atualizar()")
    class Atualizar {

        @Test
        @DisplayName("atualiza dados basicos com sucesso")
        void deveAtualizarComSucesso() {
            Usuario existente = usuarioComPerfil();
            UsuarioDTO dto = UsuarioDTO.builder()
                    .email("novo@email.com")
                    .nomeCompleto("Novo Nome")
                    .ativo(false)
                    .perfis(Set.of("PACIENTE"))
                    .build();

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(perfilRepository.findByNome("PACIENTE"))
                    .thenReturn(Optional.of(perfilPaciente()));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UsuarioDTO resultado = service.atualizar(USUARIO_ID, dto);

            assertEquals("novo@email.com", existente.getEmail());
            assertEquals("Novo Nome", existente.getNomeCompleto());
            assertFalse(existente.getAtivo());
            assertNotNull(resultado);
        }

        @Test
        @DisplayName("lanca excecao ao atualizar usuario inexistente")
        void deveLancarQuandoInexistente() {
            when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.atualizar(999L, usuarioDTO()));
        }

        @Test
        @DisplayName("limpa perfis quando dto.perfis vem vazio")
        void deveLimparPerfisQuandoVazio() {
            Usuario existente = usuarioComPerfil();
            int perfisAntes = existente.getUsuarioPerfis().size();
            UsuarioDTO dto = UsuarioDTO.builder()
                    .email(EMAIL)
                    .nomeCompleto(NOME)
                    .ativo(true)
                    .perfis(Set.of())
                    .build();

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.atualizar(USUARIO_ID, dto);

            assertTrue(perfisAntes > 0);
            assertTrue(existente.getUsuarioPerfis().isEmpty());
            verify(perfilRepository, never()).findByNome(any());
        }

        @Test
        @DisplayName("lanca excecao quando novo perfil nao existe")
        void deveLancarQuandoPerfilInexistente() {
            Usuario existente = usuarioComPerfil();
            UsuarioDTO dto = UsuarioDTO.builder()
                    .email(EMAIL)
                    .nomeCompleto(NOME)
                    .ativo(true)
                    .perfis(Set.of("FANTASMA"))
                    .build();

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(perfilRepository.findByNome("FANTASMA")).thenReturn(Optional.empty());

            assertThrows(PerfilNaoEncontradoException.class,
                    () -> service.atualizar(USUARIO_ID, dto));
        }
    }

    // =========================================================
    // deletar()
    // =========================================================
    @Nested
    @DisplayName("deletar()")
    class Deletar {

        @Test
        @DisplayName("deleta usuario existente")
        void deveDeletar() {
            when(usuarioRepository.existsById(USUARIO_ID)).thenReturn(true);

            service.deletar(USUARIO_ID);

            verify(usuarioRepository).deleteById(USUARIO_ID);
        }

        @Test
        @DisplayName("lanca excecao ao deletar usuario inexistente")
        void deveLancarQuandoInexistente() {
            when(usuarioRepository.existsById(999L)).thenReturn(false);

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.deletar(999L));
            verify(usuarioRepository, never()).deleteById(any());
        }
    }

    // =========================================================
    // validarUsuarioExiste()
    // =========================================================
    @Nested
    @DisplayName("validarUsuarioExiste()")
    class ValidarUsuarioExiste {

        @Test
        @DisplayName("nao lanca quando usuario existe")
        void deveAceitarUsuarioExistente() {
            when(usuarioRepository.existsByUuid(USUARIO_UUID)).thenReturn(true);

            assertDoesNotThrow(() -> service.validarUsuarioExiste(USUARIO_UUID));
        }

        @Test
        @DisplayName("lanca excecao quando usuario nao existe")
        void deveLancarQuandoInexistente() {
            when(usuarioRepository.existsByUuid(USUARIO_UUID)).thenReturn(false);

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.validarUsuarioExiste(USUARIO_UUID));
        }
    }

    // =========================================================
    // getInfoUsuario()
    // =========================================================
    @Nested
    @DisplayName("getInfoUsuario()")
    class GetInfoUsuario {

        @Test
        @DisplayName("retorna usuario quando encontrado por username")
        void deveRetornarUsuario() {
            when(usuarioRepository.findByUsername(USERNAME))
                    .thenReturn(Optional.of(usuarioComPerfil()));

            UsuarioDTO resultado = service.getInfoUsuario(USERNAME);

            assertEquals(USERNAME, resultado.username());
        }

        @Test
        @DisplayName("lanca UsernameNotFoundException quando nao encontrado")
        void deveLancarQuandoInexistente() {
            when(usuarioRepository.findByUsername("inexistente"))
                    .thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class,
                    () -> service.getInfoUsuario("inexistente"));
        }
    }

    // =========================================================
    // listarTodos(Pageable)
    // =========================================================
    @Nested
    @DisplayName("listarTodos(Pageable)")
    class ListarTodosPaginado {

        @Test
        @DisplayName("retorna pagina de usuarios convertidos para DTO")
        void deveListarPaginado() {
            when(usuarioRepository.findAll(any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(usuarioComPerfil())));

            var pagina = service.listarTodos(PageRequest.of(0, 10));

            assertEquals(1, pagina.getContent().size());
            assertEquals(USERNAME, pagina.getContent().get(0).username());
        }
    }

    // =========================================================
    // atualizarPerfil()
    // =========================================================
    @Nested
    @DisplayName("atualizarPerfil()")
    class AtualizarPerfil {

        @Test
        @DisplayName("atualiza nome, email e telefone do proprio perfil")
        void deveAtualizarPerfil() {
            Usuario existente = usuarioComPerfil();
            AtualizarPerfilRequestDTO dto =
                    new AtualizarPerfilRequestDTO("Novo Nome", "novo@email.com", "11988887777",
                            null, null, null);

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UsuarioDTO resultado = service.atualizarPerfil(USUARIO_ID, dto);

            assertEquals("Novo Nome", existente.getNomeCompleto());
            assertEquals("novo@email.com", existente.getEmail());
            assertEquals("11988887777", existente.getTelefone());
            assertNotNull(resultado);
            verify(usuarioRepository).save(existente);
        }

        @Test
        @DisplayName("lanca excecao quando usuario nao existe")
        void deveLancarQuandoInexistente() {
            AtualizarPerfilRequestDTO dto =
                    new AtualizarPerfilRequestDTO(NOME, EMAIL, "11988887777", null, null, null);
            when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.atualizarPerfil(999L, dto));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("grava CPF so com digitos e o endereco informado (RF04)")
        void deveGravarDadosPessoais() {
            Usuario existente = usuarioComPerfil();
            EnderecoDTO endereco = EnderecoDTO.builder()
                    .cep("01310-100")
                    .logradouro("  Avenida Paulista  ")
                    .numero("1578")
                    .bairro("Bela Vista")
                    .cidade("Sao Paulo")
                    .uf("sp")
                    .build();
            AtualizarPerfilRequestDTO dto = new AtualizarPerfilRequestDTO(
                    NOME, EMAIL, "11988887777",
                    "529.982.247-25", LocalDate.of(1985, 3, 27), endereco);

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UsuarioDTO resultado = service.atualizarPerfil(USUARIO_ID, dto);

            assertEquals("52998224725", existente.getCpf());
            assertEquals(LocalDate.of(1985, 3, 27), existente.getDataNascimento());
            assertEquals("01310100", existente.getEndereco().getCep());
            assertEquals("Avenida Paulista", existente.getEndereco().getLogradouro());
            assertEquals("SP", existente.getEndereco().getUf());
            assertEquals("52998224725", resultado.cpf());
        }

        @Test
        @DisplayName("rejeita CPF ja usado por outro usuario (RF04)")
        void deveRejeitarCpfDuplicado() {
            Usuario existente = usuarioComPerfil();
            AtualizarPerfilRequestDTO dto = new AtualizarPerfilRequestDTO(
                    NOME, EMAIL, "11988887777", "529.982.247-25", null, null);

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(usuarioRepository.existsByCpfAndIdNot("52998224725", USUARIO_ID))
                    .thenReturn(true);

            assertThrows(CpfExistenteException.class,
                    () -> service.atualizarPerfil(USUARIO_ID, dto));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("endereco ausente na requisicao preserva o que ja estava gravado")
        void devePreservarEnderecoQuandoNaoEnviado() {
            Usuario existente = usuarioComPerfil();
            existente.setEndereco(Endereco.builder().cidade("Recife").uf("PE").build());

            AtualizarPerfilRequestDTO dto = new AtualizarPerfilRequestDTO(
                    NOME, EMAIL, "11988887777", null, null, null);

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.atualizarPerfil(USUARIO_ID, dto);

            assertEquals("Recife", existente.getEndereco().getCidade());
        }

        @Test
        @DisplayName("nao mexe em COREN nem especialidade — sao do ADMIN (RF05)")
        void naoDeveAlterarDadosProfissionais() {
            Usuario existente = usuarioComPerfil();
            existente.setCoren("COREN-SP 123456");
            existente.setEspecialidade("Estomaterapia");

            AtualizarPerfilRequestDTO dto = new AtualizarPerfilRequestDTO(
                    NOME, EMAIL, "11988887777", null, null, null);

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.atualizarPerfil(USUARIO_ID, dto);

            assertEquals("COREN-SP 123456", existente.getCoren());
            assertEquals("Estomaterapia", existente.getEspecialidade());
        }
    }

    // =========================================================
    // atualizar() - dados profissionais (RF05)
    // =========================================================
    @Nested
    @DisplayName("atualizar() com dados profissionais (RF05)")
    class AtualizarDadosProfissionais {

        private UsuarioDTO dtoComCoren(String coren) {
            return UsuarioDTO.builder()
                    .nomeCompleto(NOME)
                    .email(EMAIL)
                    .telefone("11988887777")
                    .ativo(true)
                    .coren(coren)
                    .especialidade("  Estomaterapia  ")
                    .build();
        }

        @Test
        @DisplayName("grava COREN e especialidade normalizados")
        void deveGravarDadosProfissionais() {
            Usuario existente = usuarioComPerfil();

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.atualizar(USUARIO_ID, dtoComCoren("COREN-SP 123456"));

            assertEquals("COREN-SP 123456", existente.getCoren());
            assertEquals("Estomaterapia", existente.getEspecialidade());
        }

        @Test
        @DisplayName("rejeita COREN ja usado por outro profissional")
        void deveRejeitarCorenDuplicado() {
            Usuario existente = usuarioComPerfil();
            UsuarioDTO dto = dtoComCoren("COREN-SP 123456");

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(usuarioRepository.existsByCorenAndIdNot("COREN-SP 123456", USUARIO_ID))
                    .thenReturn(true);

            assertThrows(CorenExistenteException.class,
                    () -> service.atualizar(USUARIO_ID, dto));
            verify(usuarioRepository, never()).save(any());
        }
    }

    // =========================================================
    // alterarSenha()
    // =========================================================
    @Nested
    @DisplayName("alterarSenha()")
    class AlterarSenha {

        @Test
        @DisplayName("altera a senha quando a senha atual confere")
        void deveAlterarComSucesso() {
            Usuario existente = usuarioComPerfil();
            AlterarSenhaRequestDTO dto = new AlterarSenhaRequestDTO(SENHA_RAW, "novaSenha123");

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(passwordEncoder.matches(SENHA_RAW, SENHA_HASH)).thenReturn(true);
            when(passwordEncoder.encode("novaSenha123")).thenReturn("HASH_NOVO");

            service.alterarSenha(USUARIO_ID, dto);

            assertEquals("HASH_NOVO", existente.getSenhaHash());
            verify(usuarioRepository).save(existente);
        }

        @Test
        @DisplayName("rejeita quando a senha atual nao confere")
        void deveRejeitarSenhaAtualInvalida() {
            Usuario existente = usuarioComPerfil();
            AlterarSenhaRequestDTO dto = new AlterarSenhaRequestDTO("errada", "novaSenha123");

            when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(existente));
            when(passwordEncoder.matches("errada", SENHA_HASH)).thenReturn(false);

            assertThrows(SenhaAtualInvalidaException.class,
                    () -> service.alterarSenha(USUARIO_ID, dto));
            verify(passwordEncoder, never()).encode(any());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanca excecao quando usuario nao existe")
        void deveLancarQuandoInexistente() {
            when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.alterarSenha(999L, new AlterarSenhaRequestDTO("x", "y123456")));
        }
    }

    // =========================================================
    // cadastrarPaciente()
    // =========================================================
    @Nested
    @DisplayName("cadastrarPaciente()")
    class CadastrarPaciente {

        @Test
        @DisplayName("cadastra paciente com acesso desativado e username derivado do telefone")
        void deveCadastrarComSucesso() {
            CadastrarPacienteDTO dto = new CadastrarPacienteDTO("  Maria Silva  ", "  11999999999  ");

            when(usuarioRepository.existsByTelefone("11999999999")).thenReturn(false);
            when(usuarioRepository.existsByUsername("pac_11999999999")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn(SENHA_HASH);
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(perfilRepository.findByNome("PACIENTE"))
                    .thenReturn(Optional.of(perfilPaciente()));

            Usuario resultado = service.cadastrarPaciente(dto);

            assertEquals("Maria Silva", resultado.getNomeCompleto());   // trim aplicado
            assertEquals("11999999999", resultado.getTelefone());
            assertEquals("pac_11999999999", resultado.getUsername());
            assertFalse(resultado.getAcessoAtivado());
            assertTrue(resultado.getAtivo());
            verify(usuarioRepository, times(2)).save(any());            // antes e depois do perfil
        }

        @Test
        @DisplayName("gera username com sufixo quando o candidato ja existe")
        void deveGerarUsernameUnicoComColisao() {
            CadastrarPacienteDTO dto = new CadastrarPacienteDTO("Maria", "11999999999");

            when(usuarioRepository.existsByTelefone("11999999999")).thenReturn(false);
            when(usuarioRepository.existsByUsername("pac_11999999999")).thenReturn(true);
            when(usuarioRepository.existsByUsername("pac_119999999991")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn(SENHA_HASH);
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(perfilRepository.findByNome("PACIENTE"))
                    .thenReturn(Optional.of(perfilPaciente()));

            Usuario resultado = service.cadastrarPaciente(dto);

            assertEquals("pac_119999999991", resultado.getUsername());
        }

        @Test
        @DisplayName("rejeita quando ja existe paciente com o telefone")
        void deveRejeitarTelefoneExistente() {
            CadastrarPacienteDTO dto = new CadastrarPacienteDTO("Maria", "11999999999");

            when(usuarioRepository.existsByTelefone("11999999999")).thenReturn(true);

            assertThrows(TelefoneExistenteException.class,
                    () -> service.cadastrarPaciente(dto));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanca excecao quando o perfil PACIENTE nao existe")
        void deveLancarQuandoPerfilPacienteAusente() {
            CadastrarPacienteDTO dto = new CadastrarPacienteDTO("Maria", "11999999999");

            when(usuarioRepository.existsByTelefone("11999999999")).thenReturn(false);
            when(usuarioRepository.existsByUsername("pac_11999999999")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn(SENHA_HASH);
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(perfilRepository.findByNome("PACIENTE")).thenReturn(Optional.empty());

            assertThrows(PerfilNaoEncontradoException.class,
                    () -> service.cadastrarPaciente(dto));
        }
    }

    // =========================================================
    // ativarAcesso()
    // =========================================================
    @Nested
    @DisplayName("ativarAcesso()")
    class AtivarAcesso {

        private Usuario pacienteSemAcesso(String username) {
            return Usuario.builder()
                    .id(USUARIO_ID)
                    .username(username)
                    .telefone("11999999999")
                    .acessoAtivado(false)
                    .ativo(true)
                    .build();
        }

        @Test
        @DisplayName("ativa acesso definindo username, email, senha e acessoAtivado")
        void deveAtivarComSucesso() {
            AtivarAcessoRequestDTO dto =
                    new AtivarAcessoRequestDTO("  11999999999  ", "maria@email.com", "maria.silva", "senha12345");
            Usuario usuario = pacienteSemAcesso("pac_11999999999");

            when(usuarioRepository.findByTelefone("11999999999")).thenReturn(Optional.of(usuario));
            when(usuarioRepository.existsByUsername("maria.silva")).thenReturn(false);
            when(usuarioRepository.existsByEmail("maria@email.com")).thenReturn(false);
            when(passwordEncoder.encode("senha12345")).thenReturn(SENHA_HASH);
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UsuarioDTO resultado = service.ativarAcesso(dto);

            assertEquals("maria.silva", usuario.getUsername());
            assertEquals("maria@email.com", usuario.getEmail());
            assertEquals(SENHA_HASH, usuario.getSenhaHash());
            assertTrue(usuario.getAcessoAtivado());
            assertNotNull(resultado);
        }

        @Test
        @DisplayName("aceita quando o username informado eh o mesmo ja usado pelo usuario")
        void deveAceitarMesmoUsername() {
            AtivarAcessoRequestDTO dto =
                    new AtivarAcessoRequestDTO("11999999999", "maria@email.com", "pac_11999999999", "senha12345");
            Usuario usuario = pacienteSemAcesso("pac_11999999999");

            when(usuarioRepository.findByTelefone("11999999999")).thenReturn(Optional.of(usuario));
            when(usuarioRepository.existsByUsername("pac_11999999999")).thenReturn(true);
            when(usuarioRepository.existsByEmail("maria@email.com")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn(SENHA_HASH);
            when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> service.ativarAcesso(dto));
        }

        @Test
        @DisplayName("lanca excecao quando o telefone nao pertence a nenhum paciente")
        void deveLancarQuandoTelefoneNaoEncontrado() {
            AtivarAcessoRequestDTO dto =
                    new AtivarAcessoRequestDTO("11999999999", "maria@email.com", "maria.silva", "senha12345");

            when(usuarioRepository.findByTelefone("11999999999")).thenReturn(Optional.empty());

            assertThrows(UsuarioNaoEncontradoException.class,
                    () -> service.ativarAcesso(dto));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita quando o username ja pertence a outro usuario")
        void deveRejeitarUsernameDeOutro() {
            AtivarAcessoRequestDTO dto =
                    new AtivarAcessoRequestDTO("11999999999", "maria@email.com", "maria.silva", "senha12345");
            Usuario usuario = pacienteSemAcesso("pac_11999999999");

            when(usuarioRepository.findByTelefone("11999999999")).thenReturn(Optional.of(usuario));
            when(usuarioRepository.existsByUsername("maria.silva")).thenReturn(true);

            assertThrows(UserNameExistenteException.class,
                    () -> service.ativarAcesso(dto));
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejeita quando o email ja esta em uso")
        void deveRejeitarEmailExistente() {
            AtivarAcessoRequestDTO dto =
                    new AtivarAcessoRequestDTO("11999999999", "maria@email.com", "maria.silva", "senha12345");
            Usuario usuario = pacienteSemAcesso("pac_11999999999");

            when(usuarioRepository.findByTelefone("11999999999")).thenReturn(Optional.of(usuario));
            when(usuarioRepository.existsByUsername("maria.silva")).thenReturn(false);
            when(usuarioRepository.existsByEmail("maria@email.com")).thenReturn(true);

            assertThrows(EmailExistenteException.class,
                    () -> service.ativarAcesso(dto));
            verify(usuarioRepository, never()).save(any());
        }
    }
}