CREATE TABLE perfis (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
    nome VARCHAR(20) NOT NULL,
    descricao VARCHAR(200),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP
);

CREATE INDEX idx_perfis_name ON perfis(nome);

INSERT INTO perfis (nome, descricao) VALUES
('ADMIN', 'Administrador do sistema com acesso total'),
('PROFISSIONAL', 'Profissional de enfermagem com acesso a agendamentos e pacientes'),
('PACIENTE', 'Paciente com acesso ao proprio perfil e agendamentos');
