# Backup e restauração (RNF02)

Dois scripts, um par indivisível:

| Script | O que faz |
|---|---|
| `backup.sh` | Exporta banco + anexos, verifica, aplica retenção, envia cópia externa |
| `restore.sh` | Valida (`--teste`) ou restaura (`--real`) um backup |

## Por que o backup tem duas partes

Os anexos do prontuário (RF15) **não ficam no banco**. O `ArmazenamentoService`
grava os arquivos no volume `prontudigital_anexos_prod` e o banco guarda apenas a
chave de armazenamento. Restaurar só o banco produz um prontuário cujos downloads
falham; restaurar só os anexos produz arquivos que ninguém alcança.

Por isso os dois são feitos juntos, **nesta ordem: banco primeiro, anexos depois**.
Os dois dumps não são atômicos entre si, então algo gravado durante o processo cai
em um e não no outro. Nesta ordem o pior caso é um arquivo órfão (ocupa espaço, não
quebra nada). Na ordem inversa, o pior caso seria um registro sem arquivo — download
quebrado na tela do paciente.

## Instalação no servidor

```bash
cd /caminho/do/projeto/docker/prod
chmod +x scripts/*.sh
sudo mkdir -p /var/backups/prontudigital
```

Agendamento diário às 3h, via cron do root:

```bash
sudo crontab -e
```

```cron
0 3 * * * /caminho/do/projeto/docker/prod/scripts/backup.sh >> /var/log/prontudigital-backup.log 2>&1
```

O script devolve exit code diferente de zero quando algo falha — útil se você
plugar um monitor (Healthchecks.io, Uptime Kuma) no fim da linha do cron.

## Cópia externa — não é opcional

Sem isso, o backup vive **no mesmo servidor que ele deveria proteger**. Um disco
perdido leva os dados e as cópias junto. Configure no `docker/prod/.env`:

```bash
# Exemplo com rclone (Backblaze B2, Google Drive, S3...)
COMANDO_COPIA_EXTERNA="rclone sync /var/backups/prontudigital remoto:prontudigital"

# Ou para outro servidor por SSH
COMANDO_COPIA_EXTERNA="rsync -az --delete /var/backups/prontudigital/ user@host:/backups/prontudigital/"
```

Se a variável não estiver definida, o backup ainda roda, mas avisa em toda execução.

## Testar o backup — mensalmente

```bash
./scripts/restore.sh --teste /var/backups/prontudigital/diarios/2026-07-25_030000
```

Restaura o banco num database temporário e extrai os anexos numa pasta descartável.
**Não para a aplicação e não altera nada.** Ao final mostra a contagem de linhas por
tabela e cruza os registros de `anexos` com os arquivos presentes no volume — é assim
que se descobre um backup dessincronizado antes de precisar dele.

Backup nunca restaurado é suposição, não garantia. Coloque esse teste no calendário.

## Restaurar de verdade

```bash
./scripts/restore.sh --real /var/backups/prontudigital/diarios/2026-07-25_030000
```

Operação destrutiva: exige digitar `RESTAURAR` por extenso. Antes de destruir
qualquer coisa, o script tira uma cópia do estado atual em
`/var/backups/prontudigital/pre-restore/<data>` — se o backup restaurado se revelar
o errado, ainda há de onde voltar.

A aplicação fica parada durante o processo. Se algum passo falhar, ela **continua
parada de propósito**, para não subir sobre um banco meio restaurado.

## Retenção

Padrão: 14 diários + 8 semanais (domingo). Ajuste no `.env` ou por variável:

```bash
RETENCAO_DIAS=30 RETENCAO_SEMANAIS=12 ./scripts/backup.sh
```

A limpeza roda **depois** da verificação do backup novo — um backup antigo nunca é
apagado antes de existir um novo que comprovadamente presta.

## Dimensionamento

Com 2 profissionais, o banco fica na casa de dezenas de MB por anos. Os anexos
dominam: limite de 10 MB por arquivo (RF15). Estimando 20 anexos/mês, algo como
2 GB/ano no pior caso. Com a retenção padrão, reserve ~10× o tamanho atual do volume.

## LGPD

O backup contém dados clínicos e CPF. Onde ele repousa está sujeito às mesmas
obrigações do banco de produção:

- O destino da cópia externa precisa ser criptografado e de acesso restrito.
- `RETENCAO_*` é decisão de negócio, não técnica — reter indefinidamente contraria a
  limitação de finalidade.
- Quando o **RNF01** (criptografia em repouso) entrar, os dumps saem com os campos já
  cifrados; a chave passa a ser parte do plano de recuperação. **Backup sem a chave é
  um arquivo inútil** — guarde-a separada dos backups, e não só dentro do `.env` do
  servidor.
