-- Popula as tabelas de autenticação com usuários e perfis (perfis)
-- Os UUIDs são gerados automaticamente pelo PostgreSQL

-- Inserir perfis padrão do sistema
INSERT INTO perfis (nome, descricao) VALUES
('ADMIN', 'Administrador do sistema com acesso total'),
('PROFISSIONAL', 'Profissional de enfermagem com acesso a agendamentos e pacientes'),
('PACIENTE', 'Paciente com acesso ao próprio perfil e agendamentos');

-- Inserir usuário ADMIN
INSERT INTO usuarios (username, email, password_hash, nome_completo) VALUES
('admin', 'admin@prontudigital.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Administrador do Sistema');

-- Associar perfil ADMIN ao usuário admin
INSERT INTO usuario_perfis (usuario_id, perfil_id) VALUES
(1, 1); -- admin (id 1) tem perfil ADMIN (id 1)

-- Inserir usuários NURSE (enfermeiros)
INSERT INTO usuarios (username, email, password_hash, nome_completo) VALUES
('enfermeiro.silva', 'enfermeiro.silva@prontudigital.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'Maria da Silva'),
('enfermeiro.santos', 'enfermeiro.santos@prontudigital.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye.KbJY7KxZYd6k6P2fTp.jusySWE.1mW', 'João Santos');

-- Associar perfil NURSE aos enfermeiros
INSERT INTO usuario_perfis (usuario_id, perfil_id) VALUES
(2, 2), -- enfermeiro.silva (id 2)
(3, 2); -- enfermeiro.santos (id 3)

-- Inserir usuários PATIENT (pacientes)
INSERT INTO usuarios (username, email, password_hash, nome_completo) VALUES
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

-- Associar perfil PATIENT aos pacientes
INSERT INTO usuario_perfis (usuario_id, perfil_id) VALUES
(4, 3),  -- paciente.oliveira
(5, 3),  -- paciente.souza
(6, 3),  -- paciente.rodrigues
(7, 3),  -- paciente.almeida
(8, 3),  -- paciente.ferreira
(9, 3),  -- paciente.costa
(10, 3), -- paciente.lima
(11, 3), -- paciente.martins
(12, 3), -- paciente.barbosa
(13, 3); -- paciente.ribeiro