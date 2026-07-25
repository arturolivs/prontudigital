package com.prontudigital.backend.autenticacao.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Endereco do usuario (RF04). Embutido em {@code usuarios} — a clinica
 * atende tambem em domicilio, entao o endereco e do cadastro e nao de
 * um atendimento especifico.
 *
 * <p>O CEP e guardado somente com digitos.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Endereco {

    @Column(name = "cep", length = 8)
    private String cep;

    @Column(name = "logradouro", length = 150)
    private String logradouro;

    @Column(name = "numero", length = 20)
    private String numero;

    @Column(name = "complemento", length = 100)
    private String complemento;

    @Column(name = "bairro", length = 100)
    private String bairro;

    @Column(name = "cidade", length = 100)
    private String cidade;

    @Column(name = "uf", length = 2)
    private String uf;

    /** Verdadeiro quando nenhum campo foi preenchido. */
    public boolean vazio() {
        return cep == null && logradouro == null && numero == null
                && complemento == null && bairro == null
                && cidade == null && uf == null;
    }
}
