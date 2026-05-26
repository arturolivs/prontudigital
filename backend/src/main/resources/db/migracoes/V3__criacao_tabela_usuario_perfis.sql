CREATE TABLE usuario_perfis (
    usuario_id BIGINT NOT NULL,
    perfil_id BIGINT NOT NULL,
    atribuido_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (usuario_id, perfil_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    FOREIGN KEY (perfil_id) REFERENCES perfis(id) ON DELETE CASCADE
);

CREATE INDEX idx_usuario_perfis_usuario_id ON usuario_perfis(usuario_id);
CREATE INDEX idx_usuario_perfis_perfil_id ON usuario_perfis(perfil_id);