-- Configuracao da clinica atendida por esta instalacao.
--
-- O produto e distribuido em modelo SILO: cada cliente roda a propria stack,
-- com o proprio banco. Por isso a tabela guarda UMA linha, garantida pelo
-- CHECK em id = 1 — nao ha discriminador de tenant, e um INSERT extra falha
-- em vez de criar ambiguidade sobre "qual e a clinica".
--
-- O que antes era constante no codigo (PdfBuilder.NOME_CLINICA) passa a ser
-- editavel pelo ADMIN, para que cada instalacao seja configurada pela tela e
-- nao por recompilacao.
--
-- Os campos template_pdf_* estao previstos mas NAO usados: a geracao de PDF
-- continua programatica (OpenPDF). Ficam aqui para que a evolucao para
-- template enviavel nao exija alterar o schema depois.

CREATE TABLE configuracao_clinica (
    id                      BIGINT PRIMARY KEY DEFAULT 1,

    -- Identificacao exibida no topo dos documentos
    nome                    VARCHAR(150) NOT NULL,
    cnpj                    VARCHAR(14),
    telefone                VARCHAR(20),
    email                   VARCHAR(150),
    site                    VARCHAR(150),

    -- Endereco (mesma decomposicao usada em usuarios)
    cep                     VARCHAR(8),
    logradouro              VARCHAR(150),
    numero                  VARCHAR(20),
    complemento             VARCHAR(100),
    bairro                  VARCHAR(100),
    cidade                  VARCHAR(100),
    uf                      VARCHAR(2),

    -- Marca. A chave aponta para o ArmazenamentoService, igual aos anexos:
    -- o binario NAO fica no banco.
    logo_chave              VARCHAR(255),
    logo_tipo_conteudo      VARCHAR(100),

    -- Rodape livre dos documentos (ex.: "Responsavel tecnico: ... COREN ...")
    rodape_documentos       TEXT,

    -- Reservado: template de PDF enviado pelo admin. Sem uso no momento.
    template_pdf_chave      VARCHAR(255),
    template_pdf_versao     INTEGER,

    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP,

    CONSTRAINT ck_configuracao_clinica_linha_unica CHECK (id = 1)
);

-- Linha inicial: sem ela o primeiro GET teria de tratar "configuracao ausente"
-- em todo lugar. O nome vem do valor que estava fixo no PdfBuilder.
INSERT INTO configuracao_clinica (id, nome) VALUES (1, 'ProntuDigital');
