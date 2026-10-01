# Segredos de produção

Três arquivos, um segredo por arquivo, **sem quebra de linha no fim**. O
`docker-compose.prod.yml` monta cada um em `/run/secrets` dentro do container,
somente leitura, e recusa subir se algum não existir.

Lá dentro eles trocam de nome: o backend lê `/run/secrets` como config tree do
Spring, onde o nome do arquivo é o nome da propriedade — `db.password` aparece
como `spring.datasource.password` e `spring.flyway.password`, `jwt.secret` como
`app.jwt.secret`, `admin.senha` como `app.bootstrap-admin.senha`. Quem manda
nesse mapeamento é o bloco `secrets:` do compose; aqui os nomes são livres.

O conteúdo deste diretório está no `.gitignore` — só este README é versionado.

| Arquivo | Quem lê | Para quê |
|---|---|---|
| `db.password` | `postgres` (via `POSTGRES_PASSWORD_FILE`) e `backend` | Senha do usuário do banco. **Só é usada na criação do volume**; trocar depois exige `ALTER USER` |
| `jwt.secret` | `backend` | Assinatura dos tokens (HS512 — use 64 caracteres ou mais). Trocar desloga todo mundo |
| `admin.senha` | `backend` | Senha do primeiro ADMIN, usada uma única vez, no boot em que o banco ainda não tem nenhum ADMIN |

## Criar

```bash
cd /opt/prontudigital/docker/prod
mkdir -p secrets && chmod 700 secrets

# -n é o que importa: sem ele o arquivo termina em \n e a senha vira "senha\n"
printf '%s' "$(openssl rand -base64 24)" > secrets/db.password
printf '%s' "$(openssl rand -base64 64)" > secrets/jwt.secret
printf '%s' "$(openssl rand -base64 18)" > secrets/admin.senha

chmod 600 secrets/*
cat secrets/admin.senha   # anote num gerenciador: é a senha do primeiro acesso
```

## Depois do primeiro acesso

Esvazie o arquivo da senha do ADMIN — **esvazie, não apague**: o Compose exige
que ele exista.

```bash
: > secrets/admin.senha
docker compose -f docker-compose.prod.yml up -d backend   # recria o container
```

O backend trata senha vazia como "não criar ADMIN nenhum" — e a partir do
segundo boot ele já não criaria, porque o banco deixou de estar sem ADMIN.

## Backup

Estes arquivos **não** entram no backup do `scripts/backup.sh`, que cuida de
banco e anexos. Guarde-os num gerenciador de senhas: perder o `jwt.secret` só
desloga todo mundo, mas perder o `db.password` de um volume já criado custa um
`ALTER USER` no banco para voltar a conectar.
