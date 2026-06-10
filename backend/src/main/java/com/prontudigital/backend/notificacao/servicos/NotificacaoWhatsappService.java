package com.prontudigital.backend.notificacao.servicos;

import com.prontudigital.backend.agendamento.entidades.Agendamento;

import java.util.UUID;

public interface NotificacaoWhatsappService {
    void enviarLembrete48h(Agendamento agendamento);
    void enviarSolicitacaoConfirmacao24h(Agendamento agendamento);
    void confirmarViaToken(UUID token);
    void recusarViaToken(UUID token);
    void processarNaoConfirmados();
}
