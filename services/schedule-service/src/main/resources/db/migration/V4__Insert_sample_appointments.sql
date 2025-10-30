-- Popula o banco com agendamentos de exemplo para Novembro/2025
-- 2 profissionais, 10 pacientes, agendamentos nos dias úteis

-- Primeiro, se necessário, alterar a constraint para incluir todos os tipos
-- ALTER TABLE appointments DROP CONSTRAINT IF EXISTS chk_appointment_type;
-- ALTER TABLE appointments ADD CONSTRAINT chk_appointment_type
-- CHECK (type IN ('CONSULTATION', 'OTHER', 'FOLLOW_UP', 'URGENT', 'OTHER'));

-- Inserir agendamentos para o mês de Novembro/2025
INSERT INTO appointments (patient_id, professional_id, notes, start_date_time, end_date_time, status, type, created_at) VALUES

-- PROFISSIONAL 1 - NOVEMBRO/2025

-- Segunda-feira, 03/11/2025
(1, 1, 'Consulta de rotina - Checkup anual', '2025-11-03 08:00:00', '2025-11-03 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(2, 1, 'Acompanhamento pós-operatório', '2025-11-03 09:30:00', '2025-11-03 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(3, 1, 'Consulta urgente - Dor abdominal', '2025-11-03 11:00:00', '2025-11-03 12:00:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(4, 1, 'Retorno mensal - Controle medicação', '2025-11-03 14:00:00', '2025-11-03 15:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(5, 1, 'Primeira consulta - Avaliação inicial', '2025-11-03 15:30:00', '2025-11-03 16:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(6, 1, 'Procedimento menor - Curativo', '2025-11-03 17:00:00', '2025-11-03 17:30:00', 'SCHEDULED', 'OTHER', NOW()),

-- Terça-feira, 04/11/2025
(7, 1, 'Consulta de rotina - Hipertensão', '2025-11-04 08:00:00', '2025-11-04 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(8, 1, 'Avaliação de exames', '2025-11-04 09:30:00', '2025-11-04 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(1, 1, 'Controle diabetes', '2025-11-04 11:00:00', '2025-11-04 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(2, 1, 'Consulta preventiva', '2025-11-04 14:00:00', '2025-11-04 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(3, 1, 'Acompanhamento crônico', '2025-11-04 15:30:00', '2025-11-04 16:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(4, 1, 'Vacinação anual', '2025-11-04 17:00:00', '2025-11-04 17:30:00', 'SCHEDULED', 'OTHER', NOW()),

-- Quarta-feira, 05/11/2025
(5, 1, 'Consulta nova - Queixa principal', '2025-11-05 08:00:00', '2025-11-05 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(6, 1, 'Retorno semanal', '2025-11-05 09:30:00', '2025-11-05 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(7, 1, 'Emergência - Febre alta', '2025-11-05 11:00:00', '2025-11-05 12:00:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(8, 1, 'Acompanhamento tratamento', '2025-11-05 14:00:00', '2025-11-05 15:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(9, 1, 'Consulta de rotina', '2025-11-05 15:30:00', '2025-11-05 16:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(10, 1, 'Curativo complexo', '2025-11-05 17:00:00', '2025-11-05 18:00:00', 'SCHEDULED', 'OTHER', NOW()),

-- Quinta-feira, 06/11/2025
(1, 1, 'Controle pressão arterial', '2025-11-06 08:00:00', '2025-11-06 09:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(2, 1, 'Consulta nova paciente', '2025-11-06 09:30:00', '2025-11-06 10:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(3, 1, 'Retorno 15 dias', '2025-11-06 11:00:00', '2025-11-06 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(4, 1, 'Avaliação pré-operatória', '2025-11-06 14:00:00', '2025-11-06 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(5, 1, 'Procedimento - Sutura', '2025-11-06 15:30:00', '2025-11-06 16:30:00', 'SCHEDULED', 'OTHER', NOW()),
(6, 1, 'Consulta rápida', '2025-11-06 17:00:00', '2025-11-06 17:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),

-- Sexta-feira, 07/11/2025
(7, 1, 'Acompanhamento mensal', '2025-11-07 08:00:00', '2025-11-07 09:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(8, 1, 'Consulta urgente - Dor torácica', '2025-11-07 09:30:00', '2025-11-07 10:30:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(9, 1, 'Retorno tratamento', '2025-11-07 11:00:00', '2025-11-07 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(10, 1, 'Primeira consulta', '2025-11-07 14:00:00', '2025-11-07 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(1, 1, 'Controle medicação', '2025-11-07 15:30:00', '2025-11-07 16:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(2, 1, 'Procedimento - Retirada pontos', '2025-11-07 17:00:00', '2025-11-07 17:30:00', 'SCHEDULED', 'OTHER', NOW()),

-- Segunda-feira, 10/11/2025
(3, 1, 'Consulta de rotina', '2025-11-10 08:00:00', '2025-11-10 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(4, 1, 'Acompanhamento pós-alta', '2025-11-10 09:30:00', '2025-11-10 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(5, 1, 'Emergência - Alergia', '2025-11-10 11:00:00', '2025-11-10 12:00:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(6, 1, 'Consulta preventiva', '2025-11-10 14:00:00', '2025-11-10 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(7, 1, 'Retorno trimestral', '2025-11-10 15:30:00', '2025-11-10 16:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(8, 1, 'Vacinação', '2025-11-10 17:00:00', '2025-11-10 17:30:00', 'SCHEDULED', 'OTHER', NOW()),

-- Terça-feira, 11/11/2025
(9, 1, 'Consulta nova', '2025-11-11 08:00:00', '2025-11-11 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(10, 1, 'Acompanhamento', '2025-11-11 09:30:00', '2025-11-11 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(1, 1, 'Controle crônico', '2025-11-11 11:00:00', '2025-11-11 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(2, 1, 'Avaliação de dor', '2025-11-11 14:00:00', '2025-11-11 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(3, 1, 'Procedimento - Curativo', '2025-11-11 15:30:00', '2025-11-11 16:30:00', 'SCHEDULED', 'OTHER', NOW()),
(4, 1, 'Consulta rápida retorno', '2025-11-11 17:00:00', '2025-11-11 17:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),

-- Quarta-feira, 12/11/2025
(5, 1, 'Checkup anual', '2025-11-12 08:00:00', '2025-11-12 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(6, 1, 'Acompanhamento pós-cirurgia', '2025-11-12 09:30:00', '2025-11-12 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(7, 1, 'Urgência - Infecção', '2025-11-12 11:00:00', '2025-11-12 12:00:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(8, 1, 'Controle medicação', '2025-11-12 14:00:00', '2025-11-12 15:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(9, 1, 'Primeira consulta idoso', '2025-11-12 15:30:00', '2025-11-12 16:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(10, 1, 'Procedimento menor', '2025-11-12 17:00:00', '2025-11-12 17:30:00', 'SCHEDULED', 'OTHER', NOW()),

-- Quinta-feira, 13/11/2025
(1, 1, 'Retorno quinzenal', '2025-11-13 08:00:00', '2025-11-13 09:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(2, 1, 'Consulta nova - Gestante', '2025-11-13 09:30:00', '2025-11-13 10:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(3, 1, 'Acompanhamento tratamento', '2025-11-13 11:00:00', '2025-11-13 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(4, 1, 'Avaliação pré-cirúrgica', '2025-11-13 14:00:00', '2025-11-13 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(5, 1, 'Sutura removida', '2025-11-13 15:30:00', '2025-11-13 16:30:00', 'SCHEDULED', 'OTHER', NOW()),
(6, 1, 'Consulta breve', '2025-11-13 17:00:00', '2025-11-13 17:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),

-- Sexta-feira, 14/11/2025
(7, 1, 'Controle mensal', '2025-11-14 08:00:00', '2025-11-14 09:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(8, 1, 'Emergência - Trauma', '2025-11-14 09:30:00', '2025-11-14 10:30:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(9, 1, 'Retorno tratamento longo', '2025-11-14 11:00:00', '2025-11-14 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(10, 1, 'Consulta nova familiar', '2025-11-14 14:00:00', '2025-11-14 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(1, 1, 'Ajuste medicação', '2025-11-14 15:30:00', '2025-11-14 16:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(2, 1, 'Procedimento final', '2025-11-14 17:00:00', '2025-11-14 17:30:00', 'SCHEDULED', 'OTHER', NOW()),

-- Segunda-feira, 17/11/2025
(3, 1, 'Rotina checkup', '2025-11-17 08:00:00', '2025-11-17 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(4, 1, 'Pós-alta hospitalar', '2025-11-17 09:30:00', '2025-11-17 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(5, 1, 'Urgência respiratória', '2025-11-17 11:00:00', '2025-11-17 12:00:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(6, 1, 'Preventiva cancer', '2025-11-17 14:00:00', '2025-11-17 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(7, 1, 'Retorno especializado', '2025-11-17 15:30:00', '2025-11-17 16:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(8, 1, 'Aplicação medicação', '2025-11-17 17:00:00', '2025-11-17 17:30:00', 'SCHEDULED', 'OTHER', NOW()),

-- Terça-feira, 18/11/2025
(9, 1, 'Nova consulta urgente', '2025-11-18 08:00:00', '2025-11-18 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(10, 1, 'Acompanhamento continuo', '2025-11-18 09:30:00', '2025-11-18 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(1, 1, 'Controle diabetes avançado', '2025-11-18 11:00:00', '2025-11-18 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(2, 1, 'Avaliação resultados', '2025-11-18 14:00:00', '2025-11-18 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(3, 1, 'Procedimento complexo', '2025-11-18 15:30:00', '2025-11-18 16:30:00', 'SCHEDULED', 'OTHER', NOW()),
(4, 1, 'Retorno rápido', '2025-11-18 17:00:00', '2025-11-18 17:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),

-- Quarta-feira, 19/11/2025
(5, 1, 'Consulta preventiva idoso', '2025-11-19 08:00:00', '2025-11-19 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(6, 1, 'Pós-operatório controle', '2025-11-19 09:30:00', '2025-11-19 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(7, 1, 'Emergência cardiovascular', '2025-11-19 11:00:00', '2025-11-19 12:00:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(8, 1, 'Acompanhamento terapia', '2025-11-19 14:00:00', '2025-11-19 15:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(9, 1, 'Primeira consulta infantil', '2025-11-19 15:30:00', '2025-11-19 16:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(10, 1, 'Procedimento emergencial', '2025-11-19 17:00:00', '2025-11-19 18:00:00', 'SCHEDULED', 'OTHER', NOW()),

-- Quinta-feira, 20/11/2025
(1, 1, 'Retorno pós-tratamento', '2025-11-20 08:00:00', '2025-11-20 09:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(2, 1, 'Consulta gestacional', '2025-11-20 09:30:00', '2025-11-20 10:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(3, 1, 'Acompanhamento medicação', '2025-11-20 11:00:00', '2025-11-20 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(4, 1, 'Avaliação cirúrgica', '2025-11-20 14:00:00', '2025-11-20 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(5, 1, 'Procedimento rotina', '2025-11-20 15:30:00', '2025-11-20 16:30:00', 'SCHEDULED', 'OTHER', NOW()),
(6, 1, 'Consulta final dia', '2025-11-20 17:00:00', '2025-11-20 17:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),

-- Sexta-feira, 21/11/2025
(7, 1, 'Controle semanal', '2025-11-21 08:00:00', '2025-11-21 09:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(8, 1, 'Urgência abdominal', '2025-11-21 09:30:00', '2025-11-21 10:30:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(9, 1, 'Retorno pós-alta', '2025-11-21 11:00:00', '2025-11-21 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(10, 1, 'Consulta familiar nova', '2025-11-21 14:00:00', '2025-11-21 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(1, 1, 'Ajuste tratamento', '2025-11-21 15:30:00', '2025-11-21 16:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(2, 1, 'Procedimento final semana', '2025-11-21 17:00:00', '2025-11-21 17:30:00', 'SCHEDULED', 'OTHER', NOW()),

-- Segunda-feira, 24/11/2025
(3, 1, 'Rotina mensal', '2025-11-24 08:00:00', '2025-11-24 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(4, 1, 'Pós-cirurgia acompanhamento', '2025-11-24 09:30:00', '2025-11-24 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(5, 1, 'Emergência trauma', '2025-11-24 11:00:00', '2025-11-24 12:00:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(6, 1, 'Consulta preventiva adulto', '2025-11-24 14:00:00', '2025-11-24 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(7, 1, 'Retorno tratamento', '2025-11-24 15:30:00', '2025-11-24 16:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(8, 1, 'Procedimento aplicação', '2025-11-24 17:00:00', '2025-11-24 17:30:00', 'SCHEDULED', 'OTHER', NOW()),

-- Terça-feira, 25/11/2025
(9, 1, 'Nova consulta idoso', '2025-11-25 08:00:00', '2025-11-25 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(10, 1, 'Acompanhamento continuado', '2025-11-25 09:30:00', '2025-11-25 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(1, 1, 'Controle hipertensão', '2025-11-25 11:00:00', '2025-11-25 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(2, 1, 'Avaliação sintomas', '2025-11-25 14:00:00', '2025-11-25 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(3, 1, 'Procedimento curativo', '2025-11-25 15:30:00', '2025-11-25 16:30:00', 'SCHEDULED', 'OTHER', NOW()),
(4, 1, 'Retorno breve', '2025-11-25 17:00:00', '2025-11-25 17:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),

-- Quarta-feira, 26/11/2025
(5, 1, 'Checkup completo', '2025-11-26 08:00:00', '2025-11-26 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(6, 1, 'Pós-operatório final', '2025-11-26 09:30:00', '2025-11-26 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(7, 1, 'Urgência infecciosa', '2025-11-26 11:00:00', '2025-11-26 12:00:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(8, 1, 'Acompanhamento terapia final', '2025-11-26 14:00:00', '2025-11-26 15:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(9, 1, 'Consulta nova familiar', '2025-11-26 15:30:00', '2025-11-26 16:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(10, 1, 'Procedimento rotina final', '2025-11-26 17:00:00', '2025-11-26 17:30:00', 'SCHEDULED', 'OTHER', NOW()),

-- Quinta-feira, 27/11/2025
(1, 1, 'Retorno mensal final', '2025-11-27 08:00:00', '2025-11-27 09:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(2, 1, 'Consulta gestação', '2025-11-27 09:30:00', '2025-11-27 10:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(3, 1, 'Acompanhamento crônico final', '2025-11-27 11:00:00', '2025-11-27 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(4, 1, 'Avaliação pré-cirurgia final', '2025-11-27 14:00:00', '2025-11-27 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(5, 1, 'Procedimento último', '2025-11-27 15:30:00', '2025-11-27 16:30:00', 'SCHEDULED', 'OTHER', NOW()),
(6, 1, 'Consulta encerramento', '2025-11-27 17:00:00', '2025-11-27 17:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),

-- PROFISSIONAL 2 - NOVEMBRO/2025

-- Segunda-feira, 03/11/2025 - Profissional 2
(7, 2, 'Consulta especializada - Prof 2', '2025-11-03 08:00:00', '2025-11-03 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(8, 2, 'Acompanhamento - Prof 2', '2025-11-03 09:30:00', '2025-11-03 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(9, 2, 'Urgência - Prof 2', '2025-11-03 11:00:00', '2025-11-03 12:00:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(10, 2, 'Retorno - Prof 2', '2025-11-03 14:00:00', '2025-11-03 15:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(1, 2, 'Primeira consulta - Prof 2', '2025-11-03 15:30:00', '2025-11-03 16:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(2, 2, 'Procedimento - Prof 2', '2025-11-03 17:00:00', '2025-11-03 17:30:00', 'SCHEDULED', 'OTHER', NOW()),

-- Terça-feira, 04/11/2025 - Profissional 2
(3, 2, 'Rotina - Prof 2', '2025-11-04 08:00:00', '2025-11-04 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(4, 2, 'Avaliação - Prof 2', '2025-11-04 09:30:00', '2025-11-04 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(5, 2, 'Controle - Prof 2', '2025-11-04 11:00:00', '2025-11-04 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(6, 2, 'Preventiva - Prof 2', '2025-11-04 14:00:00', '2025-11-04 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(7, 2, 'Acompanhamento - Prof 2', '2025-11-04 15:30:00', '2025-11-04 16:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(8, 2, 'Vacinação - Prof 2', '2025-11-04 17:00:00', '2025-11-04 17:30:00', 'SCHEDULED', 'OTHER', NOW()),

-- Quarta-feira, 05/11/2025 - Profissional 2
(9, 2, 'Nova consulta - Prof 2', '2025-11-05 08:00:00', '2025-11-05 09:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(10, 2, 'Retorno - Prof 2', '2025-11-05 09:30:00', '2025-11-05 10:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(1, 2, 'Emergência - Prof 2', '2025-11-05 11:00:00', '2025-11-05 12:00:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(2, 2, 'Acompanhamento - Prof 2', '2025-11-05 14:00:00', '2025-11-05 15:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(3, 2, 'Consulta - Prof 2', '2025-11-05 15:30:00', '2025-11-05 16:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(4, 2, 'Procedimento - Prof 2', '2025-11-05 17:00:00', '2025-11-05 18:00:00', 'SCHEDULED', 'OTHER', NOW()),

-- Quinta-feira, 06/11/2025 - Profissional 2
(5, 2, 'Controle - Prof 2', '2025-11-06 08:00:00', '2025-11-06 09:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(6, 2, 'Consulta nova - Prof 2', '2025-11-06 09:30:00', '2025-11-06 10:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(7, 2, 'Retorno - Prof 2', '2025-11-06 11:00:00', '2025-11-06 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(8, 2, 'Avaliação - Prof 2', '2025-11-06 14:00:00', '2025-11-06 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(9, 2, 'Procedimento - Prof 2', '2025-11-06 15:30:00', '2025-11-06 16:30:00', 'SCHEDULED', 'OTHER', NOW()),
(10, 2, 'Consulta rápida - Prof 2', '2025-11-06 17:00:00', '2025-11-06 17:30:00', 'SCHEDULED', 'CONSULTATION', NOW()),

-- Sexta-feira, 07/11/2025 - Profissional 2
(1, 2, 'Acompanhamento - Prof 2', '2025-11-07 08:00:00', '2025-11-07 09:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(2, 2, 'Urgência - Prof 2', '2025-11-07 09:30:00', '2025-11-07 10:30:00', 'SCHEDULED', 'CONSULTATION', NOW()), -- Alterado de URGENT para CONSULTATION
(3, 2, 'Retorno - Prof 2', '2025-11-07 11:00:00', '2025-11-07 12:00:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(4, 2, 'Primeira consulta - Prof 2', '2025-11-07 14:00:00', '2025-11-07 15:00:00', 'SCHEDULED', 'CONSULTATION', NOW()),
(5, 2, 'Controle - Prof 2', '2025-11-07 15:30:00', '2025-11-07 16:30:00', 'SCHEDULED', 'FOLLOW_UP', NOW()),
(6, 2, 'Procedimento - Prof 2', '2025-11-07 17:00:00', '2025-11-07 17:30:00', 'SCHEDULED', 'OTHER', NOW());

-- Inserir alguns blocos de tempo para teste
INSERT INTO time_blocks (professional_id, start_date_time, end_date_time, reason, type, created_at) VALUES
-- Blocos para o Profissional 1
(1, '2025-11-03 12:00:00', '2025-11-03 13:30:00', 'Horário de almoço', 'UNAVAILABLE', NOW()),
(1, '2025-11-05 14:00:00', '2025-11-05 16:00:00', 'Reunião de equipe', 'TRAINING', NOW()),
(1, '2025-11-10 08:00:00', '2025-11-10 10:00:00', 'Capacitação', 'TRAINING', NOW()),
(1, '2025-11-17 13:00:00', '2025-11-17 15:00:00', 'Evento externo', 'UNAVAILABLE', NOW()),
(1, '2025-11-25 07:00:00', '2025-11-25 12:00:00', 'Férias', 'VACATION', NOW()),

-- Blocos para o Profissional 2
(2, '2025-11-04 12:00:00', '2025-11-04 13:30:00', 'Almoço', 'UNAVAILABLE', NOW()),
(2, '2025-11-11 09:00:00', '2025-11-11 11:00:00', 'Workshop', 'TRAINING', NOW()),
(2, '2025-11-18 14:00:00', '2025-11-18 16:00:00', 'Consulta externa', 'UNAVAILABLE', NOW()),
(2, '2025-11-26 08:00:00', '2025-11-26 17:00:00', 'Congresso médico', 'TRAINING', NOW());

-- Inserir alguns registros na lista de espera
INSERT INTO waiting_list (patient_id, professional_id, preferred_type, preferred_date, priority, status, created_at) VALUES
(1, 1, 'CONSULTATION', '2025-11-28 09:00:00', 10, 'ACTIVE', NOW()),
(2, 1, 'CONSULTATION', '2025-11-29 14:00:00', 50, 'ACTIVE', NOW()), -- Alterado de URGENT para CONSULTATION
(3, 2, 'FOLLOW_UP', '2025-11-30 10:00:00', 5, 'ACTIVE', NOW()),
(4, 1, 'OTHER', '2025-12-01 11:00:00', 15, 'ACTIVE', NOW()),
(5, 2, 'CONSULTATION', '2025-12-02 15:00:00', 8, 'ACTIVE', NOW());