-- =============================================================
-- V10 - Agendamentos de exemplo
-- 22 agendamentos por enfermeiro (16 avaliações + 6 tratamentos),
-- distribuídos entre passado, semana corrente e agenda futura.
-- Depende de: V8 (usuários enfermeiro.silva e enfermeiro.santos)
-- =============================================================

DO $$
DECLARE
    -- UUIDs dos profissionais
    uuid_silva   UUID;
    uuid_santos  UUID;

    -- UUIDs dos pacientes
    uuid_oliveira  UUID;
    uuid_souza     UUID;
    uuid_rodrigues UUID;
    uuid_almeida   UUID;
    uuid_ferreira  UUID;
    uuid_costa     UUID;
    uuid_lima      UUID;
    uuid_martins   UUID;
    uuid_barbosa   UUID;
    uuid_ribeiro   UUID;

    -- IDs das avaliações que receberão tratamentos vinculados
    -- Silva
    id_a1 BIGINT; id_a2 BIGINT; id_a3 BIGINT; id_a4 BIGINT;
    id_a9 BIGINT; id_a10 BIGINT;
    -- Santos
    id_a17 BIGINT; id_a18 BIGINT; id_a19 BIGINT; id_a20 BIGINT;
    id_a25 BIGINT; id_a26 BIGINT;
BEGIN

    -- --------------------------------------------------------
    -- Lookup de UUIDs por username
    -- --------------------------------------------------------
    SELECT uuid INTO uuid_silva   FROM usuarios WHERE username = 'enfermeiro.silva';
    SELECT uuid INTO uuid_santos  FROM usuarios WHERE username = 'enfermeiro.santos';

    SELECT uuid INTO uuid_oliveira  FROM usuarios WHERE username = 'paciente.oliveira';
    SELECT uuid INTO uuid_souza     FROM usuarios WHERE username = 'paciente.souza';
    SELECT uuid INTO uuid_rodrigues FROM usuarios WHERE username = 'paciente.rodrigues';
    SELECT uuid INTO uuid_almeida   FROM usuarios WHERE username = 'paciente.almeida';
    SELECT uuid INTO uuid_ferreira  FROM usuarios WHERE username = 'paciente.ferreira';
    SELECT uuid INTO uuid_costa     FROM usuarios WHERE username = 'paciente.costa';
    SELECT uuid INTO uuid_lima      FROM usuarios WHERE username = 'paciente.lima';
    SELECT uuid INTO uuid_martins   FROM usuarios WHERE username = 'paciente.martins';
    SELECT uuid INTO uuid_barbosa   FROM usuarios WHERE username = 'paciente.barbosa';
    SELECT uuid INTO uuid_ribeiro   FROM usuarios WHERE username = 'paciente.ribeiro';

    -- ========================================================
    -- enfermeiro.silva (Maria da Silva) — 16 avaliações
    -- ========================================================

    -- Passado — REALIZADO
    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, concluido_em)
    VALUES (uuid_oliveira, uuid_silva, '2026-04-28 08:00', '2026-04-28 09:00', 'REALIZADO', 'AVALIACAO', '2026-04-28 09:05')
    RETURNING id INTO id_a1;

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, concluido_em)
    VALUES (uuid_souza, uuid_silva, '2026-04-28 09:00', '2026-04-28 10:00', 'REALIZADO', 'AVALIACAO', '2026-04-28 10:05')
    RETURNING id INTO id_a2;

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, concluido_em)
    VALUES (uuid_rodrigues, uuid_silva, '2026-05-05 08:00', '2026-05-05 09:00', 'REALIZADO', 'AVALIACAO', '2026-05-05 09:08')
    RETURNING id INTO id_a3;

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, concluido_em)
    VALUES (uuid_almeida, uuid_silva, '2026-05-05 14:00', '2026-05-05 15:00', 'REALIZADO', 'AVALIACAO', '2026-05-05 15:10')
    RETURNING id INTO id_a4;

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, concluido_em)
    VALUES (uuid_ferreira, uuid_silva, '2026-05-12 09:00', '2026-05-12 10:00', 'REALIZADO', 'AVALIACAO', '2026-05-12 10:02');

    -- Passado — NAO_COMPARECEU / CANCELADO
    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_costa, uuid_silva, '2026-05-12 14:00', '2026-05-12 15:00', 'NAO_COMPARECEU', 'AVALIACAO');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, concluido_em)
    VALUES (uuid_lima, uuid_silva, '2026-05-19 08:00', '2026-05-19 09:00', 'REALIZADO', 'AVALIACAO', '2026-05-19 09:05');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_martins, uuid_silva, '2026-05-19 14:00', '2026-05-19 15:00', 'CANCELADO', 'AVALIACAO');

    -- Hoje — CONFIRMADO
    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_barbosa, uuid_silva, '2026-05-26 08:00', '2026-05-26 09:00', 'CONFIRMADO', 'AVALIACAO')
    RETURNING id INTO id_a9;

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_ribeiro, uuid_silva, '2026-05-26 14:00', '2026-05-26 15:00', 'CONFIRMADO', 'AVALIACAO')
    RETURNING id INTO id_a10;

    -- Esta semana / próximas semanas — AGENDADO
    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_oliveira, uuid_silva, '2026-05-27 08:00', '2026-05-27 09:00', 'AGENDADO', 'AVALIACAO');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_souza, uuid_silva, '2026-05-27 14:00', '2026-05-27 15:00', 'AGENDADO', 'AVALIACAO');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_rodrigues, uuid_silva, '2026-06-02 09:00', '2026-06-02 10:00', 'AGENDADO', 'AVALIACAO');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_almeida, uuid_silva, '2026-06-03 08:00', '2026-06-03 09:00', 'AGENDADO', 'AVALIACAO');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_ferreira, uuid_silva, '2026-06-09 14:00', '2026-06-09 15:00', 'AGENDADO', 'AVALIACAO');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_costa, uuid_silva, '2026-06-10 08:00', '2026-06-10 09:00', 'AGENDADO', 'AVALIACAO');

    -- ========================================================
    -- enfermeiro.silva (Maria da Silva) — 6 tratamentos
    -- ========================================================

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, avaliacao_id, concluido_em)
    VALUES (uuid_oliveira, uuid_silva, '2026-05-06 08:00', '2026-05-06 09:00', 'REALIZADO', 'TRATAMENTO', id_a1, '2026-05-06 09:10');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, avaliacao_id, concluido_em)
    VALUES (uuid_souza, uuid_silva, '2026-05-06 09:00', '2026-05-06 10:00', 'REALIZADO', 'TRATAMENTO', id_a2, '2026-05-06 10:05');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, avaliacao_id, concluido_em)
    VALUES (uuid_rodrigues, uuid_silva, '2026-05-13 08:00', '2026-05-13 09:00', 'REALIZADO', 'TRATAMENTO', id_a3, '2026-05-13 09:08');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, avaliacao_id, concluido_em)
    VALUES (uuid_almeida, uuid_silva, '2026-05-20 08:00', '2026-05-20 09:00', 'REALIZADO', 'TRATAMENTO', id_a4, '2026-05-20 09:05');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, avaliacao_id)
    VALUES (uuid_barbosa, uuid_silva, '2026-05-28 09:00', '2026-05-28 10:00', 'CONFIRMADO', 'TRATAMENTO', id_a9);

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, avaliacao_id)
    VALUES (uuid_ribeiro, uuid_silva, '2026-06-04 08:00', '2026-06-04 09:00', 'AGENDADO', 'TRATAMENTO', id_a10);

    -- ========================================================
    -- enfermeiro.santos (João Santos) — 16 avaliações
    -- ========================================================

    -- Passado — REALIZADO
    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, concluido_em)
    VALUES (uuid_costa, uuid_santos, '2026-04-28 09:30', '2026-04-28 10:30', 'REALIZADO', 'AVALIACAO', '2026-04-28 10:35')
    RETURNING id INTO id_a17;

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, concluido_em)
    VALUES (uuid_lima, uuid_santos, '2026-04-28 14:30', '2026-04-28 15:30', 'REALIZADO', 'AVALIACAO', '2026-04-28 15:35')
    RETURNING id INTO id_a18;

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, concluido_em)
    VALUES (uuid_martins, uuid_santos, '2026-05-05 09:30', '2026-05-05 10:30', 'REALIZADO', 'AVALIACAO', '2026-05-05 10:40')
    RETURNING id INTO id_a19;

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, concluido_em)
    VALUES (uuid_barbosa, uuid_santos, '2026-05-05 14:30', '2026-05-05 15:30', 'REALIZADO', 'AVALIACAO', '2026-05-05 15:38')
    RETURNING id INTO id_a20;

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, concluido_em)
    VALUES (uuid_ribeiro, uuid_santos, '2026-05-12 09:30', '2026-05-12 10:30', 'REALIZADO', 'AVALIACAO', '2026-05-12 10:32');

    -- Passado — NAO_COMPARECEU / CANCELADO
    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_oliveira, uuid_santos, '2026-05-12 14:30', '2026-05-12 15:30', 'NAO_COMPARECEU', 'AVALIACAO');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, concluido_em)
    VALUES (uuid_souza, uuid_santos, '2026-05-19 09:30', '2026-05-19 10:30', 'REALIZADO', 'AVALIACAO', '2026-05-19 10:35');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_rodrigues, uuid_santos, '2026-05-19 14:30', '2026-05-19 15:30', 'CANCELADO', 'AVALIACAO');

    -- Hoje — CONFIRMADO
    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_almeida, uuid_santos, '2026-05-26 09:30', '2026-05-26 10:30', 'CONFIRMADO', 'AVALIACAO')
    RETURNING id INTO id_a25;

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_ferreira, uuid_santos, '2026-05-26 14:30', '2026-05-26 15:30', 'CONFIRMADO', 'AVALIACAO')
    RETURNING id INTO id_a26;

    -- Esta semana / próximas semanas — AGENDADO
    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_costa, uuid_santos, '2026-05-27 09:30', '2026-05-27 10:30', 'AGENDADO', 'AVALIACAO');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_lima, uuid_santos, '2026-05-27 14:30', '2026-05-27 15:30', 'AGENDADO', 'AVALIACAO');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_martins, uuid_santos, '2026-06-02 09:30', '2026-06-02 10:30', 'AGENDADO', 'AVALIACAO');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_barbosa, uuid_santos, '2026-06-03 09:30', '2026-06-03 10:30', 'AGENDADO', 'AVALIACAO');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_ribeiro, uuid_santos, '2026-06-09 09:30', '2026-06-09 10:30', 'AGENDADO', 'AVALIACAO');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo)
    VALUES (uuid_oliveira, uuid_santos, '2026-06-10 09:30', '2026-06-10 10:30', 'AGENDADO', 'AVALIACAO');

    -- ========================================================
    -- enfermeiro.santos (João Santos) — 6 tratamentos
    -- ========================================================

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, avaliacao_id, concluido_em)
    VALUES (uuid_costa, uuid_santos, '2026-05-06 09:30', '2026-05-06 10:30', 'REALIZADO', 'TRATAMENTO', id_a17, '2026-05-06 10:38');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, avaliacao_id, concluido_em)
    VALUES (uuid_lima, uuid_santos, '2026-05-06 14:30', '2026-05-06 15:30', 'REALIZADO', 'TRATAMENTO', id_a18, '2026-05-06 15:35');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, avaliacao_id, concluido_em)
    VALUES (uuid_martins, uuid_santos, '2026-05-13 09:30', '2026-05-13 10:30', 'REALIZADO', 'TRATAMENTO', id_a19, '2026-05-13 10:40');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, avaliacao_id, concluido_em)
    VALUES (uuid_barbosa, uuid_santos, '2026-05-20 09:30', '2026-05-20 10:30', 'REALIZADO', 'TRATAMENTO', id_a20, '2026-05-20 10:35');

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, avaliacao_id)
    VALUES (uuid_almeida, uuid_santos, '2026-05-28 09:30', '2026-05-28 10:30', 'CONFIRMADO', 'TRATAMENTO', id_a25);

    INSERT INTO agendamentos (paciente_uuid, profissional_uuid, inicio_em, fim_em, status, tipo, avaliacao_id)
    VALUES (uuid_ferreira, uuid_santos, '2026-06-04 09:30', '2026-06-04 10:30', 'AGENDADO', 'TRATAMENTO', id_a26);

END $$;
