package com.prontudigital.backend.autenticacao.config;

import com.prontudigital.backend.autenticacao.filtros.JWTFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTFilter jwtFilter;

    private static final String[] PATHS_PUBLICOS_INFRA = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-resources/**",
            "/webjars/**",
            "/api-docs/**",
            "/actuator/health",
            "/favicon.ico",
            "/error"
    };

    private static final String[] AUTH_POST_PUBLICOS = {
            "/api/auth/registrar",
            "/api/auth/login",
            "/api/auth/renovar-token",
            "/api/auth/cadastrar-paciente",
            "/api/auth/ativar-acesso",
            "/api/auth/recuperar-senha/solicitar",
            "/api/auth/recuperar-senha/confirmar"
    };

    private static final String[] GET_PUBLICOS = {
            "/api/bloqueios-horario/public",
            // RF05: expediente do profissional (a tela publica so oferece horarios validos)
            "/api/horarios-trabalho/public",
            "/api/public/**",
            "/api/confirmacao/**",
            // RF06: catalogo de procedimentos (necessario no agendamento publico)
            "/api/procedimentos",
            "/api/procedimentos/*",
            // Marca da clinica: nome e logo compoem a tela de login e o
            // agendamento publico, ambos anteriores a autenticacao. Sao dados
            // institucionais — os mesmos que a clinica publica no proprio site
            // — e nao expoem nada de paciente. A ESCRITA continua restrita ao
            // ADMIN, via @PreAuthorize no controller.
            "/api/configuracao",
            "/api/configuracao/logo"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PATHS_PUBLICOS_INFRA).permitAll()
                        .requestMatchers(HttpMethod.POST, AUTH_POST_PUBLICOS).permitAll()
                        .requestMatchers(HttpMethod.GET, GET_PUBLICOS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}