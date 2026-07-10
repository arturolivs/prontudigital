package com.prontudigital.backend.compartilhado.excecoes;

import com.prontudigital.backend.agendamento.excecoes.*;
import com.prontudigital.backend.prontuario.excecoes.AnamneseJaExisteException;
import com.prontudigital.backend.prontuario.excecoes.AnamneseNaoEncontradaException;
import com.prontudigital.backend.notificacao.excecoes.TokenConfirmacaoInvalidoException;
import com.prontudigital.backend.autenticacao.excecoes.*;
import com.prontudigital.backend.autenticacao.excecoes.AcessoNaoAtivadoException;
import com.prontudigital.backend.autenticacao.excecoes.TelefoneExistenteException;
import com.prontudigital.backend.compartilhado.dto.ErroRespostaDTO;
import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .sorted()
                .toList();

        ErroRespostaDTO erro = new ErroRespostaDTO(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                Mensagens.get("erro.validacao.titulo"),
                Mensagens.get("erro.validacao.mensagem"),
                extrairCaminho(request),
                detalhes);

        return ResponseEntity.unprocessableEntity().body(erro);
    }

    // JSON malformado no body
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ErroRespostaDTO erro = new ErroRespostaDTO(
                HttpStatus.BAD_REQUEST.value(),
                Mensagens.get("erro.requisicao-invalida.titulo"),
                Mensagens.get("erro.requisicao-invalida.mensagem"),
                extrairCaminho(request));

        return ResponseEntity.badRequest().body(erro);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException  ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ErroRespostaDTO erro = new ErroRespostaDTO(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                Mensagens.get("erro.metodo-nao-permitido.titulo"),
                Mensagens.get("erro.metodo-nao-permitido.mensagem", ex.getMethod()),
                extrairCaminho(request));

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(erro);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        ErroRespostaDTO erro = new ErroRespostaDTO(
                HttpStatus.BAD_REQUEST.value(),
                Mensagens.get("erro.parametro-ausente.titulo"),
                Mensagens.get("erro.parametro-ausente.mensagem", ex.getParameterName()),
                extrairCaminho(request));

        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler({
            NaoAutenticadoException.class,
            AuthenticationException.class,
            AcessoNaoAtivadoException.class
    })
    public ResponseEntity<ErroRespostaDTO> handleNaoAutenticado(
            RuntimeException ex, HttpServletRequest  request) {
        return build(HttpStatus.UNAUTHORIZED, Mensagens.get("erro.nao-autenticado.titulo"), ex.getMessage(), request);
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErroRespostaDTO> handleTokenInvalido(
            TokenInvalidoException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, Mensagens.get("erro.token-invalido.titulo"), ex.getMessage(), request);
    }

    @ExceptionHandler({
            UsuarioSemAutorizacaoException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<ErroRespostaDTO> handleSemPermissao(
            RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, Mensagens.get("erro.acesso-negado.titulo"), ex.getMessage(), request);
    }

    @ExceptionHandler({
            UsuarioNaoEncontradoException.class,
            AgendamentoNaoEncontradoException.class,
            AvaliacaoNaoEncontradaException.class,
            AnamneseNaoEncontradaException.class,
            com.prontudigital.backend.prontuario.excecoes.PrescricaoNaoEncontradaException.class,
            com.prontudigital.backend.prontuario.excecoes.AnexoNaoEncontradoException.class,
            PerfilNaoEncontradoException.class
    })
    public ResponseEntity<ErroRespostaDTO> handleNaoEncontrado(
            ExcecaoBase ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, Mensagens.get("erro.nao-encontrado.titulo"), ex.getMessage(), request);
    }

    @ExceptionHandler({
            UserNameExistenteException.class,
            EmailExistenteException.class,
            TelefoneExistenteException.class,
            AgendamentoJaCanceladoException.class,
            AgendamentoJaConcluidoException.class,
            AnamneseJaExisteException.class,
            HorarioIndisponivelException.class,
            ProfissionalIndisponivelException.class,
            PacienteIndisponivelException.class
    })
    public ResponseEntity<ErroRespostaDTO> handleConflito(
            ExcecaoBase ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, Mensagens.get("erro.conflito.titulo"), ex.getMessage(), request);
    }

    @ExceptionHandler({
            AgendamentoInvalidoException.class,
            AgendamentoDataHoraInvalidaException.class,
            AgendamentoStatusInvalidoException.class,
            CancelamentoForaDoPrazoException.class,
            TipoVisualizacaoInvalidoException.class,
            com.prontudigital.backend.autenticacao.excecoes.SenhaAtualInvalidaException.class,
            com.prontudigital.backend.autenticacao.excecoes.CodigoRecuperacaoInvalidoException.class,
            com.prontudigital.backend.prontuario.excecoes.AnexoInvalidoException.class
    })
    public ResponseEntity<ErroRespostaDTO> handleRegraDeNegocio(
            ExcecaoBase ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, Mensagens.get("erro.operacao-invalida.titulo"),
                ex.getMessage(), request);
    }

    // Upload acima do limite do servlet (multipart) — antes de chegar ao service
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroRespostaDTO> handleUploadGrande(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, Mensagens.get("erro.operacao-invalida.titulo"),
                Mensagens.get("anexo.tamanho-excedido", 10), request);
    }

    @ExceptionHandler(TokenConfirmacaoInvalidoException.class)
    public ResponseEntity<ErroRespostaDTO> handleTokenConfirmacaoInvalido(
            TokenConfirmacaoInvalidoException ex, HttpServletRequest request) {
        return build(HttpStatus.GONE, Mensagens.get("erro.link-invalido.titulo"), ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroRespostaDTO> handleErroGenerico(
            Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, Mensagens.get("erro.interno.titulo"),
                Mensagens.get("erro.interno.mensagem"), request);
    }

    private ResponseEntity<ErroRespostaDTO> build(HttpStatus status, String erro,
                                                String mensagem,
                                                HttpServletRequest request) {
        return ResponseEntity.status(status).body(
                new ErroRespostaDTO(status.value(), erro, mensagem,
                        request.getRequestURI()));
    }

    private String extrairCaminho(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}