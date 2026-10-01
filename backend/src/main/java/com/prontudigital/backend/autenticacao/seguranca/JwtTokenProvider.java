package com.prontudigital.backend.autenticacao.seguranca;

import com.prontudigital.backend.compartilhado.mensagens.Mensagens;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtTokenProvider {

    private final UserDetailsServiceImpl userDetailsService;
    private Key secretKey;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-expiration-ms}")
    private Long accessExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private Long refreshExpirationMs;

    public JwtTokenProvider(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * Tamanho minimo do segredo, em bytes.
     *
     * HS512 exige chave de 512 bits pela RFC 7518, secao 3.2. O
     * {@code Keys.hmacShaKeyFor} aceita qualquer chave a partir de 256 bits,
     * entao um segredo curto passa aqui e so estoura la no {@code signWith} —
     * a aplicacao sobe inteira e falha apenas no primeiro login, com um erro
     * que nao aponta para a configuracao. Falhar no boot torna a causa obvia.
     */
    private static final int TAMANHO_MINIMO_SEGREDO_BYTES = 64;

    @PostConstruct
    public void init() {
        byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < TAMANHO_MINIMO_SEGREDO_BYTES) {
            throw new IllegalStateException(Mensagens.get(
                    "jwt.segredo-curto", bytes.length * 8,
                    TAMANHO_MINIMO_SEGREDO_BYTES));
        }

        this.secretKey = Keys.hmacShaKeyFor(bytes);
    }

    public String generateAccessToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        List<String> perfis = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("perfis", perfis)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return (bearerToken != null && bearerToken.startsWith("Bearer "))
                ? bearerToken.substring(7)
                : null;
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (Exception ex) {
            log.error("Token validation error: {}", ex.getMessage());
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public Authentication getAuthentication(String token) {
        String username = getUsernameFromToken(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
}
