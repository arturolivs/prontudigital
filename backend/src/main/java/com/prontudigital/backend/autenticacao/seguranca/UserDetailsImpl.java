package com.prontudigital.backend.autenticacao.seguranca;

import com.prontudigital.backend.autenticacao.entidades.Usuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {
    private final Long id;
    private String username;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    private static final String PREFIXO_PERFIL = "ROLE_";

    public static UserDetailsImpl build(Usuario usuario) {
        List<GrantedAuthority> authorities = usuario.getPerfis().stream()
                .map(perfil -> new SimpleGrantedAuthority(PREFIXO_PERFIL + perfil.getNome()))
                .collect(Collectors.toList());

        return new UserDetailsImpl(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getPasswordHash(),
                authorities);
    }
}