package com.prontudigital.backend.agendamento.utils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Subtracao das janelas de bloqueio do expediente de um dia da semana.
 *
 * <p>Expediente ({@code horarios_trabalho}) e bloqueio recorrente
 * ({@code bloqueios_recorrentes}) sao cadastrados por telas diferentes e nenhuma
 * das duas enxergava a outra, o que permitia gravar um par que se autoanula —
 * expediente de sabado 08:00-18:00 com folga recorrente de sabado 08:00-18:00.
 * A configuracao nao quebrava o agendamento (o bloqueio vence, porque as duas
 * regras sao aplicadas em AND), mas deixava o sabado inteiro inagendavel
 * enquanto a tela continuava anunciando que havia expediente.
 *
 * <p>Sobreposicao parcial e legitima e continua permitida: o almoco das 12:00 as
 * 13:00 dentro do expediente das 08:00 as 18:00 e justamente o caso de uso do
 * bloqueio recorrente. O que se recusa e a cobertura total, quando nao sobra
 * nenhuma janela util no dia.
 */
public final class ExpedienteEfetivo {

    private ExpedienteEfetivo() {
    }

    /**
     * Intervalo de horas dentro de um dia, meio-aberto: {@code [inicio, fim)}.
     *
     * <p>Meio-aberto porque um bloqueio que termina 12:00 nao colide com um
     * expediente que comeca 12:00 — e a mesma convencao que
     * {@code AgendamentoServiceImpl.validarBloqueioRecorrente} usa ao comparar
     * o agendamento com as regras.
     */
    public record Janela(LocalTime inicio, LocalTime fim) {
    }

    /**
     * O que sobra do expediente depois de descontar os bloqueios.
     *
     * <p>Lista vazia significa dia sem nenhuma janela util — ou porque o
     * expediente ja era vazio, ou porque os bloqueios cobrem tudo.
     */
    public static List<Janela> subtrair(List<Janela> expediente, List<Janela> bloqueios) {
        List<Janela> restante = new ArrayList<>();
        for (Janela janela : expediente) {
            restante.addAll(subtrairDeUma(janela, bloqueios));
        }
        return restante;
    }

    /** Verdadeiro quando os bloqueios nao deixam nenhum minuto util no expediente. */
    public static boolean cobreTudo(List<Janela> expediente, List<Janela> bloqueios) {
        return subtrair(expediente, bloqueios).isEmpty();
    }

    /**
     * Recorta uma janela com cada bloqueio, um de cada vez.
     *
     * <p>Um bloqueio no meio parte a janela em duas, por isso o resultado
     * parcial e realimentado a cada iteracao em vez de comparar o bloqueio
     * sempre com a janela original.
     */
    private static List<Janela> subtrairDeUma(Janela janela, List<Janela> bloqueios) {
        List<Janela> restante = new ArrayList<>();
        restante.add(janela);

        for (Janela bloqueio : bloqueios) {
            List<Janela> proximo = new ArrayList<>();
            for (Janela atual : restante) {
                boolean intersecta = bloqueio.inicio().isBefore(atual.fim())
                        && atual.inicio().isBefore(bloqueio.fim());
                if (!intersecta) {
                    proximo.add(atual);
                    continue;
                }
                // As bordas so viram janela quando sobra intervalo de fato:
                // comparar com isBefore descarta os pedacos de duracao zero.
                if (atual.inicio().isBefore(bloqueio.inicio())) {
                    proximo.add(new Janela(atual.inicio(), bloqueio.inicio()));
                }
                if (bloqueio.fim().isBefore(atual.fim())) {
                    proximo.add(new Janela(bloqueio.fim(), atual.fim()));
                }
            }
            restante = proximo;
        }

        return restante;
    }
}
