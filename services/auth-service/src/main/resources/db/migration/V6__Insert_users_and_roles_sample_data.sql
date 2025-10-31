-- Popula as tabelas de autenticação com usuários e roles
-- Inclui UUIDs gerados automaticamente pelo PostgreSQL

-- Inserir roles padrão do sistema (UUIDs serão gerados automaticamente)
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Administrador do sistema com acesso total'),
('NURSE', 'Profissional de enfermagem com acesso a agendamentos e pacientes'),
('PATIENT', 'Paciente com acesso ao próprio perfil e agendamentos');

-- Inserir usuário ADMIN
INSERT INTO users (user_name, email, password_hash, full_name) VALUES
('admin', 'admin@prontudigital.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Administrador do Sistema');

-- Associar role ADMIN ao usuário admin
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1); -- admin (id 1) tem role ADMIN (id 1)

-- Inserir usuários NURSE (Enfermeiros/Profissionais)
INSERT INTO users (user_name, email, password_hash, full_name) VALUES
('enfermeiro.silva', 'enfermeiro.silva@prontudigital.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Maria da Silva'),
('enfermeiro.santos', 'enfermeiro.santos@prontudigital.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'João Santos');

-- Associar role NURSE aos profissionais
INSERT INTO user_roles (user_id, role_id) VALUES
(2, 2), -- enfermeiro.silva (id 2) tem role NURSE (id 2)
(3, 2); -- enfermeiro.santos (id 3) tem role NURSE (id 2)

-- Inserir usuários PATIENT (Pacientes)
INSERT INTO users (user_name, email, password_hash, full_name) VALUES
('paciente.oliveira', 'carlos.oliveira@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Carlos Oliveira'),
('paciente.souza', 'ana.souza@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Ana Souza'),
('paciente.rodrigues', 'paula.rodrigues@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Paula Rodrigues'),
('paciente.almeida', 'marcos.almeida@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Marcos Almeida'),
('paciente.ferreira', 'juliana.ferreira@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Juliana Ferreira'),
('paciente.costa', 'ricardo.costa@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Ricardo Costa'),
('paciente.lima', 'fernanda.lima@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Fernanda Lima'),
('paciente.martins', 'roberto.martins@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Roberto Martins'),
('paciente.barbosa', 'patricia.barbosa@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Patrícia Barbosa'),
('paciente.ribeiro', 'lucas.ribeiro@email.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Lucas Ribeiro');

-- Associar role PATIENT aos pacientes
INSERT INTO user_roles (user_id, role_id) VALUES
(4, 3),  -- paciente.oliveira (id 4)
(5, 3),  -- paciente.souza (id 5)
(6, 3),  -- paciente.rodrigues (id 6)
(7, 3),  -- paciente.almeida (id 7)
(8, 3),  -- paciente.ferreira (id 8)
(9, 3),  -- paciente.costa (id 9)
(10, 3), -- paciente.lima (id 10)
(11, 3), -- paciente.martins (id 11)
(12, 3), -- paciente.barbosa (id 12)
(13, 3); -- paciente.ribeiro (id 13)