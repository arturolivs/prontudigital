-- Inserir alguns blocos de tempo para teste
INSERT INTO time_blocks (professional_id, start_date_time, end_date_time, reason, type) VALUES
-- Blocos para o Profissional 1 (ID 2)
(2, '2025-11-03 12:00:00', '2025-11-03 13:30:00', 'Horário de almoço', 'UNAVAILABLE'),
(2, '2025-11-05 14:00:00', '2025-11-05 16:00:00', 'Reunião de equipe', 'TRAINING'),
(2, '2025-11-10 08:00:00', '2025-11-10 10:00:00', 'Capacitação', 'TRAINING'),
(2, '2025-11-17 13:00:00', '2025-11-17 15:00:00', 'Evento externo', 'UNAVAILABLE'),
(2, '2025-11-25 07:00:00', '2025-11-25 12:00:00', 'Férias', 'VACATION'),

-- Blocos para o Profissional 2 (ID 3)
(3, '2025-11-04 12:00:00', '2025-11-04 13:30:00', 'Almoço', 'UNAVAILABLE'),
(3, '2025-11-11 09:00:00', '2025-11-11 11:00:00', 'Workshop', 'TRAINING'),
(3, '2025-11-18 14:00:00', '2025-11-18 16:00:00', 'Consulta externa', 'UNAVAILABLE'),
(3, '2025-11-26 08:00:00', '2025-11-26 17:00:00', 'Congresso médico', 'TRAINING');

-- Inserir alguns registros na lista de espera
INSERT INTO waiting_list (patient_id, professional_id, preferred_type, preferred_date, priority, status) VALUES
(4, 2, 'CONSULTATION', '2025-11-28 09:00:00', 10, 'ACTIVE'),
(5, 2, 'CONSULTATION', '2025-11-29 14:00:00', 50, 'ACTIVE'),
(6, 3, 'FOLLOW_UP', '2025-11-30 10:00:00', 5, 'ACTIVE'),
(7, 2, 'OTHER', '2025-12-01 11:00:00', 15, 'ACTIVE'),
(8, 3, 'CONSULTATION', '2025-12-02 15:00:00', 8, 'ACTIVE');