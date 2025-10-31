-- Inserir alguns blocos de tempo para teste
INSERT INTO time_blocks (professional_uuid, start_date_time, end_date_time, reason, type) VALUES
-- Blocos para o Profissional 1 (ID 2)
('62c65a33-ec4e-4976-95ca-385c83b4ec60', '2025-11-03 12:00:00', '2025-11-03 13:30:00', 'Horário de almoço', 'UNAVAILABLE'),
('62c65a33-ec4e-4976-95ca-385c83b4ec60', '2025-11-05 14:00:00', '2025-11-05 16:00:00', 'Reunião de equipe', 'TRAINING'),
('62c65a33-ec4e-4976-95ca-385c83b4ec60', '2025-11-10 08:00:00', '2025-11-10 10:00:00', 'Capacitação', 'TRAINING'),
('62c65a33-ec4e-4976-95ca-385c83b4ec60', '2025-11-17 13:00:00', '2025-11-17 15:00:00', 'Evento externo', 'UNAVAILABLE'),
('62c65a33-ec4e-4976-95ca-385c83b4ec60', '2025-11-25 07:00:00', '2025-11-25 12:00:00', 'Férias', 'VACATION'),

-- Blocos para o Profissional 2 (ID 3)
('404583d6-719f-4f26-842a-8b7389d3bca6', '2025-11-04 12:00:00', '2025-11-04 13:30:00', 'Almoço', 'UNAVAILABLE'),
('404583d6-719f-4f26-842a-8b7389d3bca6', '2025-11-11 09:00:00', '2025-11-11 11:00:00', 'Workshop', 'TRAINING'),
('404583d6-719f-4f26-842a-8b7389d3bca6', '2025-11-18 14:00:00', '2025-11-18 16:00:00', 'Consulta externa', 'UNAVAILABLE'),
('404583d6-719f-4f26-842a-8b7389d3bca6', '2025-11-26 08:00:00', '2025-11-26 17:00:00', 'Congresso médico', 'TRAINING');

-- Inserir alguns registros na lista de espera
INSERT INTO waiting_list (patient_uuid, professional_uuid, preferred_type, preferred_date, priority, status) VALUES
('105027fa-5110-44f6-a151-bcc51e28d7aa', '62c65a33-ec4e-4976-95ca-385c83b4ec60', 'CONSULTATION', '2025-11-28 09:00:00', 10, 'ACTIVE'),
('5ddbf15a-260a-49ba-afa2-0e4bf5a9595f', '62c65a33-ec4e-4976-95ca-385c83b4ec60', 'CONSULTATION', '2025-11-29 14:00:00', 50, 'ACTIVE'),
('37fff67e-c179-44a3-a60a-45a6d6f03fbf', '404583d6-719f-4f26-842a-8b7389d3bca6', 'FOLLOW_UP', '2025-11-30 10:00:00', 5, 'ACTIVE'),
('6f418466-ed47-42dc-bfea-1514e3d9e454', '62c65a33-ec4e-4976-95ca-385c83b4ec60', 'OTHER', '2025-12-01 11:00:00', 15, 'ACTIVE'),
('404bb3e2-6735-4c7e-88b8-03b9ec98f9f1', '404583d6-719f-4f26-842a-8b7389d3bca6', 'CONSULTATION', '2025-12-02 15:00:00', 8, 'ACTIVE');