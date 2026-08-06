package com.prontudigital.backend.agendamento.utils;

import com.prontudigital.backend.agendamento.utils.ExpedienteEfetivo.Janela;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExpedienteEfetivo")
class ExpedienteEfetivoTest {

    private static Janela janela(int inicio, int fim) {
        return new Janela(LocalTime.of(inicio, 0), LocalTime.of(fim, 0));
    }

    @Test
    @DisplayName("sem bloqueio o expediente sai inteiro")
    void semBloqueio() {
        List<Janela> resultado =
                ExpedienteEfetivo.subtrair(List.of(janela(8, 18)), List.of());

        assertEquals(List.of(janela(8, 18)), resultado);
    }

    @Test
    @DisplayName("bloqueio no meio parte a janela em duas")
    void bloqueioNoMeio() {
        List<Janela> resultado =
                ExpedienteEfetivo.subtrair(List.of(janela(8, 18)), List.of(janela(12, 13)));

        assertEquals(List.of(janela(8, 12), janela(13, 18)), resultado);
    }

    @Test
    @DisplayName("bloqueio identico ao expediente nao deixa sobra")
    void coberturaExata() {
        assertTrue(ExpedienteEfetivo.cobreTudo(List.of(janela(8, 18)), List.of(janela(8, 18))));
    }

    @Test
    @DisplayName("bloqueio maior que o expediente tambem cobre tudo")
    void coberturaComFolga() {
        assertTrue(ExpedienteEfetivo.cobreTudo(List.of(janela(8, 18)), List.of(janela(7, 20))));
    }

    @Test
    @DisplayName("dois bloqueios parciais somados cobrem o dia")
    void coberturaEmDuasPartes() {
        assertTrue(ExpedienteEfetivo.cobreTudo(
                List.of(janela(8, 18)),
                List.of(janela(8, 13), janela(13, 18))));
    }

    @Test
    @DisplayName("bloqueio encostado na borda nao remove nada")
    void bloqueioAdjacente() {
        List<Janela> resultado =
                ExpedienteEfetivo.subtrair(List.of(janela(8, 12)), List.of(janela(12, 18)));

        assertEquals(List.of(janela(8, 12)), resultado);
    }

    @Test
    @DisplayName("expediente em duas janelas so e anulado quando ambas somem")
    void duasJanelasDeExpediente() {
        List<Janela> expediente = List.of(janela(8, 12), janela(14, 18));

        assertFalse(ExpedienteEfetivo.cobreTudo(expediente, List.of(janela(8, 12))));
        assertTrue(ExpedienteEfetivo.cobreTudo(expediente, List.of(janela(8, 12), janela(14, 18))));
    }

    @Test
    @DisplayName("expediente vazio conta como dia sem janela util")
    void expedienteVazio() {
        assertTrue(ExpedienteEfetivo.cobreTudo(List.of(), List.of()));
    }
}
