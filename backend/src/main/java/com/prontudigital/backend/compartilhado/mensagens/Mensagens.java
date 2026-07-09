package com.prontudigital.backend.compartilhado.mensagens;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

/**
 * Ponto unico de acesso as mensagens da aplicacao (i18n).
 *
 * As mensagens ficam em {@code src/main/resources/messages.properties} e sao
 * resolvidas pelo {@link MessageSource} do Spring. O acesso e estatico para
 * poder ser usado de qualquer lugar (services, policies, excecoes, filtros),
 * inclusive fora do contexto Spring (ex.: testes unitarios), onde recorre a um
 * bundle proprio carregado do classpath.
 *
 * Uso: {@code Mensagens.get("anamnese.nao-encontrada")} ou
 * {@code Mensagens.get("agendamento.duracao-minima", 15)} com parametros.
 */
@Component
public class Mensagens {

    private static MessageSource messageSource = bundlePadrao();

    /** Injeta o MessageSource do Spring quando o contexto sobe. */
    public Mensagens(MessageSource messageSource) {
        Mensagens.messageSource = messageSource;
    }

    public static String get(String chave, Object... args) {
        return messageSource.getMessage(chave, args, LocaleContextHolder.getLocale());
    }

    /**
     * Bundle usado fora do contexto Spring (testes unitarios). Le o mesmo
     * messages.properties do classpath e devolve a propria chave quando a
     * mensagem nao existe, evitando excecoes.
     */
    private static MessageSource bundlePadrao() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setUseCodeAsDefaultMessage(true);
        return ms;
    }
}
