package com.prontudigital.backend.compartilhado.excecoes;

import com.prontudigital.backend.agendamento.excecoes.*;
import com.prontudigital.backend.agendamento.excecoes.TokenConfirmacaoInvalidoException;
import com.prontudigital.backend.autenticacao.excecoes.*;
import com.prontudigital.backend.compartilhado.dto.ErroRespostaDTO;
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
                "Dados inválidos",
                "Verifique os campos enviados",
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
                "Requisição invalida",
                "O corpo da requisição esta ausente ou mal formado",
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
                "Método não permitido",
                "Método " + ex.getMethod() + " não suportado para este endpoint",
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
                "Parâmetro ausente",
                "Parâmetro obrigatório ausente: " + ex.getParameterName(),
                extrairCaminho(request));

        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler({
            NaoAutenticadoException.class,
            AuthenticationException.class
    })
    public ResponseEntity<ErroRespostaDTO> handleNaoAutenticado(
            RuntimeException ex, HttpServletRequest  request) {
        return build(HttpStatus.UNAUTHORIZED, "Não autenticado", ex.getMessage(), request);
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErroRespostaDTO> handleTokenInvalido(
            TokenInvalidoException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Token inválido", ex.getMessage(), request);
    }

    @ExceptionHandler({
            UsuarioSemAutorizacaoException.class,
            AccessDeniedException.class
    })
    public ResponseEntity<ErroRespostaDTO> handleSemPermissao(
            RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Acesso negado", ex.getMessage(), request);
    }

    @ExceptionHandler({
            UsuarioNaoEncontradoException.class,
            AgendamentoNaoEncontradoException.class,
            AvaliacaoNaoEncontradaException.class,
            PerfilNaoEncontradoException.class
    })
    public ResponseEntity<ErroRespostaDTO> handleNaoEncontrado(
            ExcecaoBase ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Não encontrado", ex.getMessage(), request);
    }

    @ExceptionHandler({
            UserNameExistenteException.class,
            EmailExistenteException.class,
            AgendamentoJaCanceladoException.class,
            AgendamentoJaConcluidoException.class,
            HorarioIndisponivelException.class,
            ProfissionalIndisponivelException.class,
            PacienteIndisponivelException.class
    })
    public ResponseEntity<ErroRespostaDTO> handleConflito(
            ExcecaoBase ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflito", ex.getMessage(), request);
    }

    @ExceptionHandler({
            AgendamentoInvalidoException.class,
            AgendamentoDataHoraInvalidaException.class,
            AgendamentoStatusInvalidoException.class,
            TipoVisualizacaoInvalidoException.class
    })
    public ResponseEntity<ErroRespostaDTO> handleRegraDeNegocio(
            ExcecaoBase ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Operação inválida",
                ex.getMessage(), request);
    }

    @ExceptionHandler(TokenConfirmacaoInvalidoException.class)
    public ResponseEntity<ErroRespostaDTO> handleTokenConfirmacaoInvalido(
            TokenConfirmacaoInvalidoException ex, HttpServletRequest request) {
        return build(HttpStatus.GONE, "Link inválido ou expirado", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroRespostaDTO> handleErroGenerico(
            Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente.", request);
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