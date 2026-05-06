# ProntuDigital — Docker

## Estrutura

```
prontudigital/
├── backend/
│   ├── Dockerfile.dev
│   ├── Dockerfile.prod
│   ├── application-dev.yaml      → src/main/resources/
│   └── application-prod.yaml     → src/main/resources/
├── gateway/
│   ├── Dockerfile.dev
│   └── Dockerfile.prod
├── frontend-web/
│   ├── Dockerfile.dev
│   ├── Dockerfile.prod
│   └── nginx.conf
├── docker/
│   ├── dev/
│   │   ├── docker-compose.yml
│   │   └── .env.example
│   └── prod/
│       ├── docker-compose.yml
│       └── .env.example
└── .gitignore
```

---

## Desenvolvimento

### Primeira vez

```bash
cd docker/dev
cp .env.example .env        # edite se necessário
docker compose up
```

### Comandos úteis

```bash
# Sobe tudo em background
docker compose up -d

# Acompanha logs de um serviço
docker compose logs -f backend

# Reinicia apenas o backend
docker compose restart backend

# Sobe apenas o banco (útil para rodar o backend pela IDE)
docker compose up postgres

# Derruba tudo e remove volumes (reset completo)
docker compose down -v
```

### Acessos em desenvolvimento

| Serviço   | URL                              |
|-----------|----------------------------------|
| Frontend  | http://localhost:3000            |
| Gateway   | http://localhost:9090            |
| Backend   | http://localhost:8080            |
| Swagger   | http://localhost:8080/swagger-ui.html |
| Postgres  | localhost:5432                   |

---

## Produção

### Primeira vez

```bash
cd docker/prod
cp .env.example .env

# Edite .env com valores reais — especialmente:
# POSTGRES_PASSWORD, JWT_SECRET, CORS_ALLOWED_ORIGINS

docker compose up -d
```

### Gerar JWT_SECRET seguro

```bash
openssl rand -hex 64
```

### Atualizar para nova versão

```bash
docker compose build --no-cache
docker compose up -d
```

### Ver logs

```bash
docker compose logs -f
docker compose logs -f backend
```

### Backup do banco

```bash
docker exec prontudigital-db pg_dump -U $POSTGRES_USER $POSTGRES_DB > backup.sql
```

---

## Segmentação de rede (produção)

```
Internet
    │
    ▼
[frontend :80]
    │  frontend-network
    ▼
[gateway :9090]
    │  backend-network
    ▼
[backend :8080]
    │  backend-network
    ▼
[postgres :5432]
```

O banco de dados **nunca** é acessível fora da rede interna Docker.
