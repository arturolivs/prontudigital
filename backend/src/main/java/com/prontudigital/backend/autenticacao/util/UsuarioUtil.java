package com.prontudigital.backend.autenticacao.util;

import com.prontudigital.backend.autenticacao.dto.EnderecoDTO;
import com.prontudigital.backend.autenticacao.dto.UsuarioDTO;
import com.prontudigital.backend.autenticacao.entidades.Endereco;
import com.prontudigital.backend.autenticacao.entidades.Perfil;
import com.prontudigital.backend.autenticacao.entidades.Usuario;

import java.util.stream.Collectors;

public class UsuarioUtil {

    private UsuarioUtil() {}

    public static UsuarioDTO converterUsuarioParaDTO(Usuario usuario) {
        return UsuarioDTO.builder()
                .id(usuario.getId())
                .uuid(usuario.getUuid())
                .email(usuario.getEmail())
                .username(usuario.getUsername())
                .nomeCompleto(usuario.getNomeCompleto())
                .telefone(usuario.getTelefone())
                .cpf(usuario.getCpf())
                .dataNascimento(usuario.getDataNascimento())
                .endereco(converterEnderecoParaDTO(usuario.getEndereco()))
                .coren(usuario.getCoren())
                .especialidade(usuario.getEspecialidade())
                .ativo(usuario.getAtivo())
                .acessoAtivado(usuario.getAcessoAtivado())
                .perfis(usuario.getPerfis().stream()
                        .map(Perfil::getNome)
                        .collect(Collectors.toSet()))
                .criadoEm(usuario.getCriadoEm())
                .atualizadoEm(usuario.getAtualizadoEm())
                .build();
    }

    public static EnderecoDTO converterEnderecoParaDTO(Endereco endereco) {
        if (endereco == null || endereco.vazio()) {
            return null;
        }
        return EnderecoDTO.builder()
                .cep(endereco.getCep())
                .logradouro(endereco.getLogradouro())
                .numero(endereco.getNumero())
                .complemento(endereco.getComplemento())
                .bairro(endereco.getBairro())
                .cidade(endereco.getCidade())
                .uf(endereco.getUf())
                .build();
    }

    /**
     * Converte o endereco recebido na requisicao. Devolve {@code null} quando
     * o cliente nao mandou endereco algum, para nao sobrescrever o que ja
     * estava gravado com um objeto de campos nulos.
     */
    public static Endereco converterEnderecoDeDTO(EnderecoDTO dto) {
        if (dto == null) {
            return null;
        }
        return Endereco.builder()
                .cep(somenteDigitos(dto.cep()))
                .logradouro(normalizar(dto.logradouro()))
                .numero(normalizar(dto.numero()))
                .complemento(normalizar(dto.complemento()))
                .bairro(normalizar(dto.bairro()))
                .cidade(normalizar(dto.cidade()))
                .uf(dto.uf() == null ? null : normalizar(dto.uf().toUpperCase()))
                .build();
    }

    /** Guarda o CPF so com digitos — a mascara e responsabilidade da tela. */
    public static String normalizarCpf(String cpf) {
        return somenteDigitos(cpf);
    }

    public static String somenteDigitos(String valor) {
        if (valor == null) {
            return null;
        }
        String digitos = valor.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }

    public static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
