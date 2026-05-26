package com.prontudigital.backend.autenticacao.seguranca;

import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.excecoes.NaoAutenticadoException;
import com.prontudigital.backend.autenticacao.repositorios.UsuarioRepository;
import com.prontudigital.backend.autenticacao.util.UsuarioUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsuarioContexto {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDTO getUsuarioAtual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            throw new NaoAutenticadoException("Nenhum usuário autenticado na sessão atual");
        }

        return usuarioRepository.findByUsername(auth.getName())
                .map(UsuarioUtil::converterUsuarioParaDTO)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado: " + auth.getName()));
    }
}