-- ============================================================
-- Rastreio de entrega das mensagens do WhatsApp
--
-- A Cloud API responde HTTP 200 assim que ACEITA a mensagem; a
-- entrega (ou a falha) so chega depois, de forma assincrona, pelo
-- webhook de status da Meta. Sem guardar o id que a Meta devolve no
-- envio nao ha como ligar esse callback a linha deste log — era por
-- isso que uma mensagem podia nunca chegar ao paciente com o status
-- gravado aqui parado em ENVIADO.
-- ============================================================

ALTER TABLE log_notificacoes_whatsapp
    ADD COLUMN mensagem_id  VARCHAR(128),
    ADD COLUMN entregue_em  TIMESTAMP,
    ADD COLUMN lido_em      TIMESTAMP,
    ADD COLUMN erro_codigo  VARCHAR(20),
    ADD COLUMN erro_detalhe TEXT;

COMMENT ON COLUMN log_notificacoes_whatsapp.mensagem_id IS
    'wamid devolvido pela Cloud API no envio; chave de correlacao do webhook de status';

-- Parcial: so as linhas realmente enviadas tem id, e a busca do
-- webhook sempre filtra por um valor nao nulo.
CREATE INDEX idx_log_notif_mensagem_id
    ON log_notificacoes_whatsapp(mensagem_id)
    WHERE mensagem_id IS NOT NULL;
