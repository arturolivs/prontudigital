-- Popula a tabela de agendamentos para Novembro/2025
-- 2 profissionais (IDs 2 e 3), 10 pacientes (IDs 4-13)
-- UUIDs serão gerados automaticamente pelo PostgreSQL

-- Agendamentos para o mês de Novembro/2025

-- PROFISSIONAL 1 (ID 2 - Maria da Silva) - NOVEMBRO/2025

-- Segunda-feira, 03/11/2025
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-03 08:00:00', '2025-11-03 09:00:00', 2, 4, 'CONSULTATION', 'SCHEDULED', 'Consulta de rotina - Checkup anual'),
('2025-11-03 09:30:00', '2025-11-03 10:30:00', 2, 5, 'FOLLOW_UP', 'SCHEDULED', 'Acompanhamento pós-operatório'),
('2025-11-03 11:00:00', '2025-11-03 12:00:00', 2, 6, 'CONSULTATION', 'SCHEDULED', 'Consulta urgente - Dor abdominal'),
('2025-11-03 14:00:00', '2025-11-03 15:00:00', 2, 7, 'FOLLOW_UP', 'SCHEDULED', 'Retorno mensal - Controle medicação'),
('2025-11-03 15:30:00', '2025-11-03 16:30:00', 2, 8, 'CONSULTATION', 'SCHEDULED', 'Primeira consulta - Avaliação inicial'),
('2025-11-03 17:00:00', '2025-11-03 17:30:00', 2, 9, 'OTHER', 'SCHEDULED', 'Procedimento menor - Curativo');

-- Terça-feira, 04/11/2025
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-04 08:00:00', '2025-11-04 09:00:00', 2, 10, 'CONSULTATION', 'SCHEDULED', 'Consulta de rotina - Hipertensão'),
('2025-11-04 09:30:00', '2025-11-04 10:30:00', 2, 11, 'FOLLOW_UP', 'SCHEDULED', 'Avaliação de exames'),
('2025-11-04 11:00:00', '2025-11-04 12:00:00', 2, 4, 'FOLLOW_UP', 'SCHEDULED', 'Controle diabetes'),
('2025-11-04 14:00:00', '2025-11-04 15:00:00', 2, 5, 'CONSULTATION', 'SCHEDULED', 'Consulta preventiva'),
('2025-11-04 15:30:00', '2025-11-04 16:30:00', 2, 6, 'FOLLOW_UP', 'SCHEDULED', 'Acompanhamento crônico'),
('2025-11-04 17:00:00', '2025-11-04 17:30:00', 2, 7, 'OTHER', 'SCHEDULED', 'Vacinação anual');

-- Quarta-feira, 05/11/2025
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-05 08:00:00', '2025-11-05 09:00:00', 2, 8, 'CONSULTATION', 'SCHEDULED', 'Consulta nova - Queixa principal'),
('2025-11-05 09:30:00', '2025-11-05 10:30:00', 2, 9, 'FOLLOW_UP', 'SCHEDULED', 'Retorno semanal'),
('2025-11-05 11:00:00', '2025-11-05 12:00:00', 2, 10, 'CONSULTATION', 'SCHEDULED', 'Emergência - Febre alta'),
('2025-11-05 14:00:00', '2025-11-05 15:00:00', 2, 11, 'FOLLOW_UP', 'SCHEDULED', 'Acompanhamento tratamento'),
('2025-11-05 15:30:00', '2025-11-05 16:30:00', 2, 12, 'CONSULTATION', 'SCHEDULED', 'Consulta de rotina'),
('2025-11-05 17:00:00', '2025-11-05 18:00:00', 2, 13, 'OTHER', 'SCHEDULED', 'Curativo complexo');

-- Quinta-feira, 06/11/2025
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-06 08:00:00', '2025-11-06 09:00:00', 2, 4, 'FOLLOW_UP', 'SCHEDULED', 'Controle pressão arterial'),
('2025-11-06 09:30:00', '2025-11-06 10:30:00', 2, 5, 'CONSULTATION', 'SCHEDULED', 'Consulta nova paciente'),
('2025-11-06 11:00:00', '2025-11-06 12:00:00', 2, 6, 'FOLLOW_UP', 'SCHEDULED', 'Retorno 15 dias'),
('2025-11-06 14:00:00', '2025-11-06 15:00:00', 2, 7, 'CONSULTATION', 'SCHEDULED', 'Avaliação pré-operatória'),
('2025-11-06 15:30:00', '2025-11-06 16:30:00', 2, 8, 'OTHER', 'SCHEDULED', 'Procedimento - Sutura'),
('2025-11-06 17:00:00', '2025-11-06 17:30:00', 2, 9, 'CONSULTATION', 'SCHEDULED', 'Consulta rápida');

-- Sexta-feira, 07/11/2025
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-07 08:00:00', '2025-11-07 09:00:00', 2, 10, 'FOLLOW_UP', 'SCHEDULED', 'Acompanhamento mensal'),
('2025-11-07 09:30:00', '2025-11-07 10:30:00', 2, 11, 'CONSULTATION', 'SCHEDULED', 'Consulta urgente - Dor torácica'),
('2025-11-07 11:00:00', '2025-11-07 12:00:00', 2, 12, 'FOLLOW_UP', 'SCHEDULED', 'Retorno tratamento'),
('2025-11-07 14:00:00', '2025-11-07 15:00:00', 2, 13, 'CONSULTATION', 'SCHEDULED', 'Primeira consulta'),
('2025-11-07 15:30:00', '2025-11-07 16:30:00', 2, 4, 'FOLLOW_UP', 'SCHEDULED', 'Controle medicação'),
('2025-11-07 17:00:00', '2025-11-07 17:30:00', 2, 5, 'OTHER', 'SCHEDULED', 'Procedimento - Retirada pontos');

-- Segunda-feira, 10/11/2025
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-10 08:00:00', '2025-11-10 09:00:00', 2, 6, 'CONSULTATION', 'SCHEDULED', 'Consulta de rotina'),
('2025-11-10 09:30:00', '2025-11-10 10:30:00', 2, 7, 'FOLLOW_UP', 'SCHEDULED', 'Acompanhamento pós-alta'),
('2025-11-10 11:00:00', '2025-11-10 12:00:00', 2, 8, 'CONSULTATION', 'SCHEDULED', 'Emergência - Alergia'),
('2025-11-10 14:00:00', '2025-11-10 15:00:00', 2, 9, 'CONSULTATION', 'SCHEDULED', 'Consulta preventiva'),
('2025-11-10 15:30:00', '2025-11-10 16:30:00', 2, 10, 'FOLLOW_UP', 'SCHEDULED', 'Retorno trimestral'),
('2025-11-10 17:00:00', '2025-11-10 17:30:00', 2, 11, 'OTHER', 'SCHEDULED', 'Vacinação');

-- Terça-feira, 11/11/2025
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-11 08:00:00', '2025-11-11 09:00:00', 2, 12, 'CONSULTATION', 'SCHEDULED', 'Consulta nova'),
('2025-11-11 09:30:00', '2025-11-11 10:30:00', 2, 13, 'FOLLOW_UP', 'SCHEDULED', 'Acompanhamento'),
('2025-11-11 11:00:00', '2025-11-11 12:00:00', 2, 4, 'FOLLOW_UP', 'SCHEDULED', 'Controle crônico'),
('2025-11-11 14:00:00', '2025-11-11 15:00:00', 2, 5, 'CONSULTATION', 'SCHEDULED', 'Avaliação de dor'),
('2025-11-11 15:30:00', '2025-11-11 16:30:00', 2, 6, 'OTHER', 'SCHEDULED', 'Procedimento - Curativo'),
('2025-11-11 17:00:00', '2025-11-11 17:30:00', 2, 7, 'FOLLOW_UP', 'SCHEDULED', 'Consulta rápida retorno');

-- Quarta-feira, 12/11/2025
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-12 08:00:00', '2025-11-12 09:00:00', 2, 8, 'CONSULTATION', 'SCHEDULED', 'Checkup anual'),
('2025-11-12 09:30:00', '2025-11-12 10:30:00', 2, 9, 'FOLLOW_UP', 'SCHEDULED', 'Acompanhamento pós-cirurgia'),
('2025-11-12 11:00:00', '2025-11-12 12:00:00', 2, 10, 'CONSULTATION', 'SCHEDULED', 'Urgência - Infecção'),
('2025-11-12 14:00:00', '2025-11-12 15:00:00', 2, 11, 'FOLLOW_UP', 'SCHEDULED', 'Controle medicação'),
('2025-11-12 15:30:00', '2025-11-12 16:30:00', 2, 12, 'CONSULTATION', 'SCHEDULED', 'Primeira consulta idoso'),
('2025-11-12 17:00:00', '2025-11-12 17:30:00', 2, 13, 'OTHER', 'SCHEDULED', 'Procedimento menor');

-- Quinta-feira, 13/11/2025
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-13 08:00:00', '2025-11-13 09:00:00', 2, 4, 'FOLLOW_UP', 'SCHEDULED', 'Retorno quinzenal'),
('2025-11-13 09:30:00', '2025-11-13 10:30:00', 2, 5, 'CONSULTATION', 'SCHEDULED', 'Consulta nova - Gestante'),
('2025-11-13 11:00:00', '2025-11-13 12:00:00', 2, 6, 'FOLLOW_UP', 'SCHEDULED', 'Acompanhamento tratamento'),
('2025-11-13 14:00:00', '2025-11-13 15:00:00', 2, 7, 'CONSULTATION', 'SCHEDULED', 'Avaliação pré-cirúrgica'),
('2025-11-13 15:30:00', '2025-11-13 16:30:00', 2, 8, 'OTHER', 'SCHEDULED', 'Sutura removida'),
('2025-11-13 17:00:00', '2025-11-13 17:30:00', 2, 9, 'CONSULTATION', 'SCHEDULED', 'Consulta breve');

-- Sexta-feira, 14/11/2025
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-14 08:00:00', '2025-11-14 09:00:00', 2, 10, 'FOLLOW_UP', 'SCHEDULED', 'Controle mensal'),
('2025-11-14 09:30:00', '2025-11-14 10:30:00', 2, 11, 'CONSULTATION', 'SCHEDULED', 'Emergência - Trauma'),
('2025-11-14 11:00:00', '2025-11-14 12:00:00', 2, 12, 'FOLLOW_UP', 'SCHEDULED', 'Retorno tratamento longo'),
('2025-11-14 14:00:00', '2025-11-14 15:00:00', 2, 13, 'CONSULTATION', 'SCHEDULED', 'Consulta nova familiar'),
('2025-11-14 15:30:00', '2025-11-14 16:30:00', 2, 4, 'FOLLOW_UP', 'SCHEDULED', 'Ajuste medicação'),
('2025-11-14 17:00:00', '2025-11-14 17:30:00', 2, 5, 'OTHER', 'SCHEDULED', 'Procedimento final');

-- PROFISSIONAL 2 (ID 3 - João Santos) - NOVEMBRO/2025

-- Segunda-feira, 03/11/2025 - Profissional 2
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-03 08:00:00', '2025-11-03 09:00:00', 3, 6, 'CONSULTATION', 'SCHEDULED', 'Consulta especializada - Prof 2'),
('2025-11-03 09:30:00', '2025-11-03 10:30:00', 3, 7, 'FOLLOW_UP', 'SCHEDULED', 'Acompanhamento - Prof 2'),
('2025-11-03 11:00:00', '2025-11-03 12:00:00', 3, 8, 'CONSULTATION', 'SCHEDULED', 'Urgência - Prof 2'),
('2025-11-03 14:00:00', '2025-11-03 15:00:00', 3, 9, 'FOLLOW_UP', 'SCHEDULED', 'Retorno - Prof 2'),
('2025-11-03 15:30:00', '2025-11-03 16:30:00', 3, 10, 'CONSULTATION', 'SCHEDULED', 'Primeira consulta - Prof 2'),
('2025-11-03 17:00:00', '2025-11-03 17:30:00', 3, 11, 'OTHER', 'SCHEDULED', 'Procedimento - Prof 2');

-- Terça-feira, 04/11/2025 - Profissional 2
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-04 08:00:00', '2025-11-04 09:00:00', 3, 12, 'CONSULTATION', 'SCHEDULED', 'Rotina - Prof 2'),
('2025-11-04 09:30:00', '2025-11-04 10:30:00', 3, 13, 'FOLLOW_UP', 'SCHEDULED', 'Avaliação - Prof 2'),
('2025-11-04 11:00:00', '2025-11-04 12:00:00', 3, 4, 'FOLLOW_UP', 'SCHEDULED', 'Controle - Prof 2'),
('2025-11-04 14:00:00', '2025-11-04 15:00:00', 3, 5, 'CONSULTATION', 'SCHEDULED', 'Preventiva - Prof 2'),
('2025-11-04 15:30:00', '2025-11-04 16:30:00', 3, 6, 'FOLLOW_UP', 'SCHEDULED', 'Acompanhamento - Prof 2'),
('2025-11-04 17:00:00', '2025-11-04 17:30:00', 3, 7, 'OTHER', 'SCHEDULED', 'Vacinação - Prof 2');

-- Quarta-feira, 05/11/2025 - Profissional 2
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-05 08:00:00', '2025-11-05 09:00:00', 3, 8, 'CONSULTATION', 'SCHEDULED', 'Nova consulta - Prof 2'),
('2025-11-05 09:30:00', '2025-11-05 10:30:00', 3, 9, 'FOLLOW_UP', 'SCHEDULED', 'Retorno - Prof 2'),
('2025-11-05 11:00:00', '2025-11-05 12:00:00', 3, 10, 'CONSULTATION', 'SCHEDULED', 'Emergência - Prof 2'),
('2025-11-05 14:00:00', '2025-11-05 15:00:00', 3, 11, 'FOLLOW_UP', 'SCHEDULED', 'Acompanhamento - Prof 2'),
('2025-11-05 15:30:00', '2025-11-05 16:30:00', 3, 12, 'CONSULTATION', 'SCHEDULED', 'Consulta - Prof 2'),
('2025-11-05 17:00:00', '2025-11-05 18:00:00', 3, 13, 'OTHER', 'SCHEDULED', 'Procedimento - Prof 2');

-- Quinta-feira, 06/11/2025 - Profissional 2
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-06 08:00:00', '2025-11-06 09:00:00', 3, 4, 'FOLLOW_UP', 'SCHEDULED', 'Controle - Prof 2'),
('2025-11-06 09:30:00', '2025-11-06 10:30:00', 3, 5, 'CONSULTATION', 'SCHEDULED', 'Consulta nova - Prof 2'),
('2025-11-06 11:00:00', '2025-11-06 12:00:00', 3, 6, 'FOLLOW_UP', 'SCHEDULED', 'Retorno - Prof 2'),
('2025-11-06 14:00:00', '2025-11-06 15:00:00', 3, 7, 'CONSULTATION', 'SCHEDULED', 'Avaliação - Prof 2'),
('2025-11-06 15:30:00', '2025-11-06 16:30:00', 3, 8, 'OTHER', 'SCHEDULED', 'Procedimento - Prof 2'),
('2025-11-06 17:00:00', '2025-11-06 17:30:00', 3, 9, 'CONSULTATION', 'SCHEDULED', 'Consulta rápida - Prof 2');

-- Sexta-feira, 07/11/2025 - Profissional 2
INSERT INTO appointments (start_date_time, end_date_time, professional_id, patient_id, type, status, notes) VALUES
('2025-11-07 08:00:00', '2025-11-07 09:00:00', 3, 10, 'FOLLOW_UP', 'SCHEDULED', 'Acompanhamento - Prof 2'),
('2025-11-07 09:30:00', '2025-11-07 10:30:00', 3, 11, 'CONSULTATION', 'SCHEDULED', 'Urgência - Prof 2'),
('2025-11-07 11:00:00', '2025-11-07 12:00:00', 3, 12, 'FOLLOW_UP', 'SCHEDULED', 'Retorno - Prof 2'),
('2025-11-07 14:00:00', '2025-11-07 15:00:00', 3, 13, 'CONSULTATION', 'SCHEDULED', 'Primeira consulta - Prof 2'),
('2025-11-07 15:30:00', '2025-11-07 16:30:00', 3, 4, 'FOLLOW_UP', 'SCHEDULED', 'Controle - Prof 2'),
('2025-11-07 17:00:00', '2025-11-07 17:30:00', 3, 5, 'OTHER', 'SCHEDULED', 'Procedimento - Prof 2');