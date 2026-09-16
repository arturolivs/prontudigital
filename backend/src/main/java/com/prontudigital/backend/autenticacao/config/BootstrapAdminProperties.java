package com.prontudigital.backend.autenticacao.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Credenciais do primeiro ADMIN da instalacao (prefixo
 * {@code app.bootstrap-admin}).
 *
 * <p>Existe porque o banco nasce sem nenhum usuario: {@code POST
 * /api/auth/registrar} exige ADMIN e {@code /api/auth/cadastrar-paciente} so
 * cria PACIENTE — sem este atalho nao haveria caminho para o primeiro acesso.
 * Ver {@link BootstrapAdminRunner}.
 *
 * <p>Os valores vem do ambiente ({@code APP_BOOTSTRAP_ADMIN_SENHA} e
 * companhia), nunca do repositorio: uma senha versionada em SQL e publica para
 * sempre — foi exatamente o problema da antiga migracao de dados de exemplo.
 */
@Component
@ConfigurationProperties(prefix = "app.bootstrap-admin")
@Getter
@Setter
public class BootstrapAdminProperties {

    /** Desliga a criacao automatica mesmo com username e senha preenchidos. */
    private boolean habilitado = true;

    /** Login do ADMIN inicial. Vazio = nada e criado. */
    private String username;

    /** Senha em claro do ADMIN inicial; o hash e gerado no boot. Vazio = nada e criado. */
    private String senha;

    /** Nome exibido do ADMIN inicial. */
    private String nomeCompleto = "Administrador";

    /** E-mail do ADMIN inicial. Opcional — a coluna aceita nulo. */
    private String email;

    /** Telefone do ADMIN inicial, no formato usado no cadastro. Opcional. */
    private String telefone;
}
