package com.prontudigital.backend.autenticacao.config;

import com.prontudigital.backend.autenticacao.entidades.Perfil;
import com.prontudigital.backend.autenticacao.entidades.Usuario;
import com.prontudigital.backend.autenticacao.excecoes.PerfilNaoEncontradoException;
import com.prontudigital.backend.autenticacao.repositorios.PerfilRepository;
import com.prontudigital.backend.autenticacao.repositorios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o primeiro ADMIN da instalacao, uma unica vez, a partir do ambiente.
 *
 * <p>As migracoes nao semeiam usuario nenhum — a senha ficaria versionada em
 * SQL e visivel no historico do Git. Mas o banco recem-criado precisa de
 * alguem que consiga entrar: {@code POST /api/auth/registrar} exige ADMIN
 * (@PreAuthorize no controller) e o cadastro publico so produz PACIENTE. Este
 * runner e a ponte entre os dois fatos.
 *
 * <p>E deliberadamente conservador — em qualquer duvida ele registra o motivo
 * e nao escreve nada:
 *
 * <ul>
 *   <li>ja existe algum usuario com perfil ADMIN: nao faz nada. E o caso de
 *       todo boot a partir do segundo, e tambem a garantia de que trocar a
 *       senha pela tela nao e desfeita no proximo restart;</li>
 *   <li>username ou senha em branco: nao faz nada. Vale para o ambiente de
 *       teste e para quem prefere criar o ADMIN na mao;</li>
 *   <li>username ja em uso: nao faz nada, para nao colidir com um cadastro
 *       existente nem elevar o perfil de quem ja esta la.</li>
 * </ul>
 *
 * <p>Nao derruba o boot quando nao esta configurado: um backend no ar sem
 * ADMIN ainda serve o agendamento publico, e o WARN abaixo diz o que fazer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapAdminRunner implements ApplicationRunner {

    /** Mesmo minimo exigido no registro pela API (RegistrarRequestDTO). */
    private static final int TAMANHO_MINIMO_SENHA = 8;

    private static final String PERFIL_ADMIN = "ADMIN";

    private final BootstrapAdminProperties propriedades;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!propriedades.isHabilitado()) {
            log.debug("Bootstrap do ADMIN desabilitado por configuracao.");
            return;
        }

        if (usuarioRepository.existsByUsuarioPerfis_Perfil_Nome(PERFIL_ADMIN)) {
            log.debug("Ja existe usuario com perfil ADMIN; bootstrap ignorado.");
            return;
        }

        String username = normalizar(propriedades.getUsername());
        String senha = propriedades.getSenha();

        if (username == null || senha == null || senha.isBlank()) {
            log.warn("Nenhum ADMIN no banco e username/senha do ADMIN inicial nao"
                    + " informados: ninguem consegue entrar no sistema. Em producao,"
                    + " preencha ADMIN_USERNAME no .env e secrets/admin.senha, depois"
                    + " recrie o backend (up -d backend).");
            return;
        }

        if (senha.length() < TAMANHO_MINIMO_SENHA) {
            log.error("A senha do ADMIN inicial tem menos de {} caracteres;"
                    + " ADMIN inicial NAO criado. Em producao ela esta em"
                    + " secrets/admin.senha.", TAMANHO_MINIMO_SENHA);
            return;
        }

        if (usuarioRepository.existsByUsername(username)) {
            log.warn("Username '{}' ja existe mas nao e ADMIN; bootstrap ignorado."
                    + " Escolha outro ADMIN_USERNAME ou promova o usuario pelo"
                    + " banco.", username);
            return;
        }

        String email = normalizar(propriedades.getEmail());
        if (email != null && usuarioRepository.existsByEmail(email)) {
            log.warn("E-mail '{}' ja esta em uso; bootstrap ignorado.", email);
            return;
        }

        Perfil perfilAdmin = perfilRepository.findByNome(PERFIL_ADMIN)
                .orElseThrow(() -> new PerfilNaoEncontradoException(PERFIL_ADMIN));

        Usuario admin = Usuario.builder()
                .nomeCompleto(propriedades.getNomeCompleto())
                .username(username)
                .email(email)
                .telefone(normalizar(propriedades.getTelefone()))
                .senhaHash(passwordEncoder.encode(senha))
                // Diferente do paciente cadastrado pela recepcao, este usuario
                // nasce com credencial propria — nao passa por ativar-acesso.
                .acessoAtivado(true)
                .ativo(true)
                .build();

        admin = usuarioRepository.save(admin);
        admin.adicionarPerfil(perfilAdmin);
        usuarioRepository.save(admin);

        log.info("ADMIN inicial '{}' criado. Troque a senha no primeiro acesso e"
                + " esvazie secrets/admin.senha (esvazie, nao apague).", username);
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
