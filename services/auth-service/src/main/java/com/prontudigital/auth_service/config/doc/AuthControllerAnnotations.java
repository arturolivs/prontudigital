package com.prontudigital.auth_service.config.doc;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classe centralizada para todas as anotações do AuthController
 */
public final class AuthControllerAnnotations {

    private AuthControllerAnnotations() {
        // Classe utilitária - não deve ser instanciada
    }

    /**
     * Meta-anotação base para endpoints de POST
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Operation(
            summary = "Registrar novo usuário",
            description = "Cria uma nova conta de usuário no sistema"
    )
    @ApiResponses
    public @interface AuthPostEndpoint {

        @AliasFor(annotation = PostMapping.class, attribute = "path")
        String[] path() default {};

        @AliasFor(annotation = PostMapping.class, attribute = "value")
        String[] value() default {};

        @AliasFor(annotation = Operation.class, attribute = "summary")
        String summary() default "";

        @AliasFor(annotation = Operation.class, attribute = "description")
        String description() default "";
    }

    /**
     * Endpoint de registro
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "Usuário ou e-mail já existe")
    })
    public @interface Register {
    }

    /**
     * Endpoint de login
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação bem-sucedida"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    public @interface SignIn {
    }

    /**
     * Endpoint de refresh token
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token renovado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado")
    })
    @SecurityRequirement(name = "bearerAuth")
    public @interface RefreshToken {
    }

    /**
     * Endpoint de logout
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout realizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Token inválido")
    })
    @SecurityRequirement(name = "bearerAuth")
    public @interface Logout {
    }

    /**
     * Endpoint específico para operações de usuário
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SecurityRequirement(name = "bearerAuth")
    public @interface UserOperation {

        @AliasFor(annotation = AuthPostEndpoint.class, attribute = "path")
        String[] path() default {};

        @AliasFor(annotation = AuthPostEndpoint.class, attribute = "summary")
        String summary() default "";

        @AliasFor(annotation = AuthPostEndpoint.class, attribute = "description")
        String description() default "";
    }

    /**
     * Endpoint público (sem autenticação)
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface PublicEndpoint {

        @AliasFor(annotation = AuthPostEndpoint.class, attribute = "path")
        String[] path() default {};

        @AliasFor(annotation = AuthPostEndpoint.class, attribute = "summary")
        String summary() default "";

        @AliasFor(annotation = AuthPostEndpoint.class, attribute = "description")
        String description() default "";
    }
}