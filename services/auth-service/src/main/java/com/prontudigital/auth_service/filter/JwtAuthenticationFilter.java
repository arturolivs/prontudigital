package com.prontudigital.auth_service.filter;

import com.prontudigital.auth_service.exception.InvalidTokenException;
import com.prontudigital.auth_service.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    // Paths que não precisam de autenticação JWT
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-resources/**",
            "/swagger-resources",
            "/swagger-resources/configuration/ui",
            "/swagger-resources/configuration/security",
            "/webjars/**",
            "/api-docs/**",
            "/api-docs",
            "/home",
            "/index",
            "/favicon.ico",
            "/error",
            "/api/auth/v1/register"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        AntPathMatcher pathMatcher = new AntPathMatcher();

        return PUBLIC_PATHS.stream()
                .anyMatch(p -> pathMatcher.match(p, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        log.debug("Processando requisição: {} {}", request.getMethod(), request.getRequestURI());
        try {
            String jwt = getJwtFromRequest(request);
            log.debug("Token extraído: {}", jwt != null ? "presente" : "ausente");

            if(Objects.isNull(jwt)) throw new InvalidTokenException("InvalidTokenException no filtro !!!");

            boolean valid = tokenProvider.validateToken(jwt);
            log.debug("Token válido: {}", valid);
            if (valid) {
                String username = tokenProvider.getUsernameFromToken(jwt);
                log.debug("Username do token: {}", username);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                log.debug("UserDetails carregado: {} - authorities: {}",
                        userDetails.getUsername(), userDetails.getAuthorities());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Autenticação definida no SecurityContext");
            } else {
                log.debug("Token inválido ou expirado");
            }
        } catch (Exception ex) {
            log.error("Exceção no filtro JWT: {}", ex.getMessage(), ex);
        }
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}