package com.prontudigital.backend.compartilhado.mensagens;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guarda o encoding do messages.properties.
 *
 * <p>O arquivo ja teve ~73 acentos corrompidos em U+FFFD ("Usu&#xFFFD;rio"), que
 * chegavam assim na tela do usuario. Estes testes falham se a corrupcao voltar.
 */
@DisplayName("Mensagens (encoding do bundle)")
class MensagensTest {

    private static final char SUBSTITUTO = '�';

    @Test
    @DisplayName("nenhuma mensagem contem o caractere de substituicao U+FFFD")
    void bundleNaoTemCaractereDeSubstituicao() throws IOException {
        try (InputStream entrada = getClass().getClassLoader()
                .getResourceAsStream("messages.properties")) {

            assertNotNull(entrada, "messages.properties nao encontrado no classpath");
            String conteudo = new String(entrada.readAllBytes(), StandardCharsets.UTF_8);

            int posicao = conteudo.indexOf(SUBSTITUTO);
            if (posicao >= 0) {
                int inicioLinha = conteudo.lastIndexOf('\n', posicao) + 1;
                int fimLinha = conteudo.indexOf('\n', posicao);
                String linha = conteudo.substring(
                        inicioLinha, fimLinha < 0 ? conteudo.length() : fimLinha);
                fail("Acento corrompido em messages.properties: " + linha);
            }
        }
    }

    @ParameterizedTest(name = "{0} resolve com acento correto")
    @ValueSource(strings = {
            "erro.nao-encontrado.titulo",
            "erro.operacao-invalida.titulo",
            "auth.sem-perfil-atribuido",
            "usuario.nao-encontrado.username"
    })
    @DisplayName("mensagens acentuadas chegam legiveis pelo MessageSource")
    void mensagensAcentuadasResolvemCorretamente(String chave) {
        String mensagem = Mensagens.get(chave, "teste");

        assertFalse(mensagem.indexOf(SUBSTITUTO) >= 0,
                "mensagem veio corrompida: " + mensagem);
        // Se a chave nao existisse, o bundle padrao devolveria a propria chave.
        assertNotEquals(chave, mensagem);
    }

    @Test
    @DisplayName("acentuacao especifica sobrevive a leitura do bundle")
    void acentuacaoEspecifica() {
        assertEquals("Não encontrado", Mensagens.get("erro.nao-encontrado.titulo"));
        assertEquals("Operação inválida", Mensagens.get("erro.operacao-invalida.titulo"));
        assertEquals("Usuário sem perfil atribuído",
                Mensagens.get("auth.sem-perfil-atribuido"));
    }
}
