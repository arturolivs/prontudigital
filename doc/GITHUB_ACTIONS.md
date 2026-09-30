# Os workflows do GitHub Actions, linha a linha

> **Leitura comentada** de [`.github/workflows/ci.yml`](../.github/workflows/ci.yml)
> e [`.github/workflows/cd.yml`](../.github/workflows/cd.yml): o que cada bloco
> faz e, principalmente, **por que aquele comando e não outro**.
>
> Este documento explica os arquivos. Quem quiser *configurar* o repositório
> (secrets, variables, chaves, deploy key) vai para [`CICD.md`](./CICD.md); quem
> quiser *operar* a VPS vai para [`DEPLOY.md`](./DEPLOY.md). Escrito lendo o
> estado do repositório em **23/09/2026**.

---

## 0. O desenho, antes dos detalhes

Dois arquivos, um chamando o outro:

```
push em branch (≠ main) ou PR
   └─ CI ......... testes do backend │ lint+tipos do frontend
                   build das imagens │ validação do compose
                   (nada é publicado, nada é implantado)

push na main
   └─ CD
       ├─ preparar ... decide a tag, confere a variable DOMINIO
       ├─ ci ......... chama o MESMO ci.yml (workflow_call)
       ├─ imagens .... constrói e publica no GHCR (sha-abc1234 + latest)
       └─ implantar .. ssh → git checkout <sha> → scripts/deploy.sh <tag>
```

Três decisões estruturais explicam quase todo o resto:

1. **O CI é um arquivo só, usado nos dois caminhos.** O CD não repete os jobs
   de teste: ele invoca `ci.yml` como *workflow reutilizável*. Assim não existe
   caminho em que algo chegue a produção sem passar por teste, e não há dois
   YAMLs para manter em sincronia.
2. **O build acontece no runner do GitHub, nunca na VPS.** `docker compose up
   --build` na máquina de produção roda Maven e `next build` ali, consome mais
   memória que o runtime inteiro e derruba uma VPS de 4 GB por OOM. O runner é
   descartável, mais folgado que a VPS e não atende ninguém enquanto trabalha —
   é lá que o build pesado mora. A VPS só faz `pull`.
3. **O trabalho de deploy mora num script do repositório**, não no YAML. O
   workflow só chama `docker/prod/scripts/deploy.sh` por SSH. Isso é o que
   permite repetir o deploy à mão, do jeito exato, quando o Actions está fora
   do ar.

---

## 1. Vocabulário mínimo

Quem já conhece Actions pode pular. Os termos aparecem o tempo todo daqui
para baixo:

| Termo | O que é |
|---|---|
| **workflow** | Um arquivo `.yml` em `.github/workflows/`. Tem gatilhos (`on:`) e um ou mais jobs |
| **job** | Um bloco de trabalho. Roda numa **máquina virtual limpa**, em paralelo com os outros por padrão. Nada passa de um job para o outro a não ser por `outputs` ou artefatos |
| **step** | Um passo dentro do job. Ou `run:` (comando de shell) ou `uses:` (uma ação pronta de terceiros) |
| **runner** | A VM que executa o job. `ubuntu-latest` aqui, sempre nova |
| **`needs:`** | Dependência entre jobs. Sem isso eles correriam todos juntos |
| **`vars.X`** | Valor **não secreto** do repositório, visível nos logs (`DOMINIO`) |
| **`secrets.X`** | Valor secreto, mascarado nos logs (`VPS_SSH_KEY`) |
| **`GITHUB_TOKEN`** | Credencial criada automaticamente para cada execução e **expirada ao fim dela**. É o que publica no GHCR — não existe senha de registro guardada em lugar nenhum |

O detalhe do "job roda em máquina limpa" não é trivial: é por isso que todo job
começa com `actions/checkout@v4` (a VM não tem o código) e por que o cache
precisa ser declarado (a VM não tem `~/.m2` nem `node_modules`).

---

## 2. `ci.yml` — verificação

### 2.1 Quando roda

```yaml
on:
  pull_request:
  push:
    branches-ignore:
      - main
  workflow_call:
```

Três gatilhos, e o terceiro é o que importa:

- `pull_request` — todo PR é verificado antes do merge.
- `push: branches-ignore: [main]` — qualquer branch de trabalho. A `main`
  é **excluída de propósito**: push nela dispara o CD, que já chama este
  arquivo. Sem o `branches-ignore`, cada merge rodaria a bateria duas vezes.
- `workflow_call` — declara que este workflow pode ser chamado por outro. É o
  que torna a linha `uses: ./.github/workflows/ci.yml` do CD legal.

### 2.2 Concorrência e permissões

```yaml
concurrency:
  group: ci-${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

Push seguido de push na mesma branch cancela a execução anterior. Só interessa
o resultado do último commit, e minuto de runner gasto com código já
substituído é desperdício. O `group` inclui a ref para que branches diferentes
não cancelem umas às outras.

```yaml
permissions:
  contents: read
```

O `GITHUB_TOKEN` desta execução só consegue **ler** o repositório. Nada aqui
escreve tag, comentário ou pacote, então nada aqui recebe permissão para isso.
No CD a permissão de escrita aparece só no job que publica imagem (§3.4) — é o
mesmo princípio aplicado por job.

### 2.3 Job `backend` — os testes

```yaml
defaults:
  run:
    working-directory: backend
```

Evita repetir `cd backend &&` em todo `run:` do job. Só vale para `run:`, não
para ações — é por isso que o `upload-artifact` mais abaixo usa o caminho
completo `backend/target/surefire-reports/`.

```yaml
- uses: actions/setup-java@v4
  with:
    distribution: temurin
    java-version: '21'          # casa com <java.version> do pom
    cache: maven
```

`temurin` é a mesma distribuição da imagem `eclipse-temurin:21-jdk` usada no
`Dockerfile.prod` — testar num JDK e produzir noutro é pedir divergência. O
`cache: maven` guarda o `~/.m2` entre execuções; sem ele cada push rebaixa
dezenas de MB do Maven Central.

```yaml
- name: Tornar o mvnw executável
  run: chmod +x mvnw
```

Este é o passo que mais confunde quem chega. O Git guarda o bit de execução no
modo do arquivo, e um commit feito no Windows grava `100644` (sem o `+x`) —
confirme com `git ls-files -s backend/mvnw`. No runner Linux, `./mvnw` sem esse
bit falha com **exit 126 (permission denied)**. O `Dockerfile.prod` tem a mesma
linha, pelo mesmo motivo. A alternativa definitiva é
`git update-index --chmod=+x backend/mvnw`, um commit só; enquanto isso não
acontece, o `chmod` fica.

```yaml
- name: Testes
  run: ./mvnw -B -ntp verify
```

- `-B` (*batch mode*) — desliga a saída interativa e colorida. Em log de CI,
  cor vira lixo de escape.
- `-ntp` (*no transfer progress*) — corta as milhares de linhas de "Downloading
  ...", que tornam o log ilegível.
- `verify`, e não `test` — `verify` roda a fase de testes **e** empacota e
  executa as verificações do ciclo (`failsafe`, plugins de qualidade). Se algo
  só quebra no empacotamento, quebra aqui, não no build da imagem.

Repare que este é o **único lugar onde os testes rodam**: o
`backend/Dockerfile.prod` compila com `./mvnw package -DskipTests -q`. A
divisão é proposital — o Dockerfile produz o artefato, o CI garante que ele
presta. Se os testes rodassem também na imagem, cada build pagaria a conta duas
vezes.

```yaml
- name: Publicar relatórios do Surefire
  if: failure()
  uses: actions/upload-artifact@v4
  with:
    name: surefire-reports
    path: backend/target/surefire-reports/
    retention-days: 7
```

`if: failure()` — só anexa os relatórios **quando algo quebrou**. Em execução
verde, o XML do Surefire não interessa a ninguém e ocuparia cota de artefato.
`retention-days: 7` pelo mesmo motivo: relatório de teste velho não serve para
nada, e o padrão da plataforma são 90 dias.

### 2.4 Job `frontend` — lint e tipos

```yaml
- uses: actions/setup-node@v4
  with:
    node-version: '20'          # mesma major da imagem node:20-alpine
    cache: npm
    cache-dependency-path: frontend-web/package-lock.json
```

O `cache-dependency-path` é obrigatório aqui porque o `package-lock.json` não
está na raiz do repositório; sem apontar o caminho, a ação procuraria na raiz,
não acharia e o cache nunca aqueceria.

```yaml
- run: npm ci
```

`npm ci`, não `npm install`. O `ci` instala **exatamente** o que está no
`package-lock.json`, apaga o `node_modules` antes e falha se o lock estiver
dessincronizado do `package.json`. O `install` pode resolver versões novas
silenciosamente — o que torna o resultado do CI dependente do dia em que ele
rodou.

```yaml
- run: npm run lint
- run: npm run type-check
```

Dois passos separados, e não um `&&`: assim a interface do Actions mostra qual
dos dois falhou sem ninguém abrir o log.

**O que não está aqui:** `next build`. Ele já roda dentro do
`frontend-web/Dockerfile.prod`, no job `imagens`. Rodar de novo seria pagar
duas vezes pela mesma compilação — e a que vale é a que vira imagem.

### 2.5 Job `imagens` — build sem publicar

```yaml
strategy:
  fail-fast: false
  matrix:
    include:
      - servico: backend
        contexto: backend
        dockerfile: backend/Dockerfile.prod
      - servico: frontend
        contexto: frontend-web
        dockerfile: frontend-web/Dockerfile.prod
```

A *matrix* gera dois jobs a partir de um bloco. `fail-fast: false` é a parte
deliberada: por padrão, a falha de uma combinação cancela as outras. Aqui isso
seria ruim — se o backend quebra, ainda vale saber se o frontend também
quebrou, em vez de descobrir no próximo push.

```yaml
- uses: docker/setup-buildx-action@v3
- uses: docker/build-push-action@v6
  with:
    push: false
    cache-from: type=gha,scope=${{ matrix.servico }}
    cache-to: type=gha,scope=${{ matrix.servico }},mode=max
```

`push: false` — no CI a pergunta é só "ainda constrói?". Publicar é papel do
CD, que roda depois dos testes e só na `main`.

O Buildx existe para o `type=gha`: o cache de camadas fica no armazenamento do
próprio Actions. Sem ele, cada PR refaz `./mvnw dependency:go-offline` e
`npm ci` do zero, e são vários minutos. `scope` separado por serviço para que o
backend não invalide o cache do frontend. `mode=max` guarda também as camadas
intermediárias do estágio de build — as caras — e não só as da imagem final.

### 2.6 Job `compose` — validar o que a VPS vai executar

```yaml
- name: Preencher .env e segredos falsos
  run: |
    set -euo pipefail
    cat > .env <<'ENV'
    DOMINIO=exemplo.invalid
    ...
    ENV
    mkdir -p secrets
    for arquivo in db.password jwt.secret admin.senha; do
      printf '%s' "ci" > "secrets/$arquivo"
    done
```

O `docker-compose.prod.yml` declara as variáveis obrigatórias como
`${VAR:?mensagem}`, então ele **se recusa a ser lido** sem um `.env`
preenchido. Os valores são descartáveis de propósito — servem só para a
interpolação resolver. `exemplo.invalid` usa o TLD reservado pela RFC 2606:
não existe, não resolve, não bate em ninguém por acidente.

`set -euo pipefail` no começo do bloco: aborta no primeiro erro (`-e`), trata
variável não definida como erro (`-u`) e propaga falha no meio de um *pipe*
(`-o pipefail`). Sem isso, um passo de shell pode falhar no meio e ainda assim
terminar com sucesso.

O heredoc é `<<'ENV'` com aspas — aspas simples impedem o shell de expandir
`$`. Sem elas, qualquer `$` no conteúdo viraria variável vazia.

```yaml
- run: docker compose -f docker-compose.prod.yml config >/dev/null
```

`config` faz o Compose ler, interpolar e validar o arquivo inteiro, imprimindo
o resultado — que vai para `/dev/null` porque o que interessa é o **código de
saída**. Pega erro de YAML, variável obrigatória removida do
`.env.prod.example` e serviço mal referenciado antes de a VPS pegar.

```yaml
- name: Sobreposição não pode manter build local
  run: |
    if docker compose -f docker-compose.prod.yml -f docker-compose.ghcr.yml \
         config | grep -qE '^\s+build:'; then
      echo "::error::docker-compose.ghcr.yml deixou um bloco build: no resultado"
      exit 1
    fi
```

O teste mais específico do repositório, e o mais fácil de não entender. A
sobreposição `docker-compose.ghcr.yml` usa `build: !reset null` para **apagar**
o bloco `build:` herdado do arquivo base. Se essa remoção parar de funcionar —
alguém mexe na sobreposição, ou a VPS roda um Compose anterior à v2.24, que não
conhece `!reset` — o Compose volta a enxergar um contexto de build e **a VPS
compila durante o deploy**, que é exatamente o desastre que todo este arranjo
existe para evitar. O `grep` no resultado do `config` é o que transforma essa
regra em teste automático.

`::error::` é a sintaxe de anotação do Actions: a mensagem aparece destacada no
resumo da execução, e não perdida no meio do log.

---

## 3. `cd.yml` — publicar e implantar

### 3.1 Quando roda

```yaml
on:
  push:
    branches: [main]
    paths-ignore:
      - '**.md'
      - 'doc/**'
      - 'testes-carga/**'
      - '*.jpg'
      - '*.png'
  workflow_dispatch:
    inputs:
      tag:
        description: 'Tag já publicada no GHCR (ex.: sha-3d0989f). Vazio = construir a partir deste commit.'
```

`paths-ignore` evita um deploy inteiro — build, publicação, reinício dos
containers — para corrigir uma vírgula num documento. Nada em `**.md` ou
`doc/**` entra na imagem.

O `workflow_dispatch` com o campo `tag` é o botão de **voltar atrás**:
informando uma tag que já existe no GHCR, o workflow pula CI e build e só
implanta. É a diferença entre um rollback de segundos e um de dez minutos.

```yaml
concurrency:
  group: cd-producao
  cancel-in-progress: false
```

Ao contrário do CI, aqui **não** se cancela. Um `docker compose up -d`
interrompido no meio deixa a pilha em estado misto: backend novo, frontend
velho, ou containers parados. Dois pushes seguidos entram em fila; o segundo
espera o primeiro terminar.

### 3.2 Job `preparar` — decidir antes de gastar

```yaml
outputs:
  tag: ${{ steps.dados.outputs.tag }}
  registro: ${{ steps.dados.outputs.registro }}
```

Jobs rodam em máquinas separadas e não compartilham variáveis. `outputs` é o
canal: o que este job calcula, os outros leem com
`needs.preparar.outputs.tag`.

```bash
if [ -n "${TAG_INFORMADA:-}" ]; then
  tag="$TAG_INFORMADA"
else
  tag="sha-$(printf '%s' "$GITHUB_SHA" | cut -c1-7)"
fi
```

Ou a tag veio do `workflow_dispatch`, ou ela é derivada do commit: `sha-` mais
os 7 primeiros caracteres do SHA. Tag derivada do commit é o que torna
"qual versão está no ar?" uma pergunta com resposta exata — `sha-3d0989f`
aponta para um commit, não para um "latest" que muda debaixo do pé.

```bash
registro="ghcr.io/$(printf '%s' "$DONO" | tr '[:upper:]' '[:lower:]')"
```

O GHCR só aceita caminho em minúsculas, e o nome de usuário do GitHub pode ter
maiúscula. Sem o `tr`, um dono chamado `ArturOlivs` produziria
`ghcr.io/ArturOlivs/...` e o push falharia com um erro pouco explicativo.

```bash
echo "tag=$tag" >> "$GITHUB_OUTPUT"
echo "Tag: \`$tag\` — registro: \`$registro\`" >> "$GITHUB_STEP_SUMMARY"
```

Dois arquivos que o Actions expõe por variável de ambiente: escrever em
`$GITHUB_OUTPUT` publica o valor para os outros jobs; escrever em
`$GITHUB_STEP_SUMMARY` renderiza Markdown na página da execução. O segundo é
cortesia com quem vai auditar o deploy depois.

```yaml
- name: Conferir a variável DOMINIO
  if: ${{ !inputs.tag }}
```

Esta checagem paga por si sozinha. O `frontend-web/Dockerfile.prod` declara
`ARG NEXT_PUBLIC_BASE_URL=http://localhost:9090` como padrão, e todas as URLs
de API do frontend derivam dele **em tempo de build**. Com a variable `DOMINIO`
vazia, a imagem sai apontando para `localhost:9090`, o build passa, o deploy
passa, os healthchecks passam — e a falha só aparece no navegador do usuário,
em forma de tela que não carrega nada. Falhar no primeiro job custa 10
segundos; descobrir em produção custa um rollback.

O `if: ${{ !inputs.tag }}` dispensa a checagem no deploy manual por tag: ali
não há build, a imagem já existe com o domínio certo embutido.

### 3.3 Job `ci` — reaproveitar em vez de repetir

```yaml
ci:
  if: ${{ !inputs.tag }}
  uses: ./.github/workflows/ci.yml
```

Chama o arquivo inteiro do CI. A condição repete a lógica acima: implantar uma
tag já publicada não precisa rodar testes de novo — aquela imagem já passou por
eles quando foi construída.

### 3.4 Job `imagens` — construir e publicar

```yaml
permissions:
  contents: read
  packages: write
```

A permissão de escrita em pacotes aparece **só aqui**, e não no topo do
arquivo. Se um passo comprometido rodar em qualquer outro job, ele não tem
como publicar imagem.

```yaml
- name: Login no GHCR
  uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}
```

Não existe secret de registro neste repositório. O `GITHUB_TOKEN` é criado para
esta execução e morre com ela — uma credencial que não pode vazar de um mês
para o outro porque não dura um mês.

```yaml
    tags: |
      ${{ needs.preparar.outputs.registro }}/prontudigital-${{ matrix.servico }}:${{ needs.preparar.outputs.tag }}
      ${{ needs.preparar.outputs.registro }}/prontudigital-${{ matrix.servico }}:latest
```

Duas tags para a mesma imagem. A `sha-abc1234` é a que o deploy usa e a que
permite voltar atrás; a `latest` é conveniência para quem inspeciona o pacote
na interface do GitHub. O deploy **nunca** usa `latest` — "a última" não é uma
versão para a qual se possa retornar.

```yaml
    labels: |
      org.opencontainers.image.source=${{ github.server_url }}/${{ github.repository }}
      org.opencontainers.image.revision=${{ github.sha }}
```

Rótulos do padrão OCI. O `source` é o que faz o GitHub ligar o pacote ao
repositório na interface; o `revision` grava o commit exato **dentro da
imagem**, então `docker inspect` responde de onde aquele binário veio mesmo
que a tag tenha se perdido.

```yaml
    build-args: |
      NEXT_PUBLIC_BASE_URL=https://${{ vars.DOMINIO }}
```

O argumento só tem efeito no frontend — o `Dockerfile.prod` do backend nem
declara esse `ARG`, e passá-lo ali é inofensivo. É a alternativa a manter dois
blocos de build quase iguais.

### 3.5 Job `implantar` — a única parte que toca a produção

```yaml
if: >-
  always()
  && needs.preparar.result == 'success'
  && (needs.imagens.result == 'success' || needs.imagens.result == 'skipped')
```

Sem `always()`, um job pulado arrastaria este junto: no deploy manual por tag,
`imagens` é pulado de propósito, e o comportamento padrão do Actions trataria
isso como "não continue". A condição diz, explicitamente: siga se a preparação
deu certo e se as imagens foram publicadas **ou** propositalmente puladas.

```yaml
environment:
  name: producao
  url: https://${{ vars.DOMINIO }}
```

Declarar o *environment* dá três coisas: o link "View deployment" no commit e
no PR, o histórico de implantações na aba do repositório e — o principal — o
gancho para exigir **aprovação humana** antes do job, em
`Settings > Environments > producao > Required reviewers`. Em prontuário
eletrônico, costuma valer.

```bash
mkdir -p ~/.ssh && chmod 700 ~/.ssh
printf '%s\n' "$CHAVE" > ~/.ssh/id_deploy
chmod 600 ~/.ssh/id_deploy
printf '%s\n' "$HOSTS_CONHECIDOS" > ~/.ssh/known_hosts
```

`chmod 700` no diretório e `600` na chave porque o cliente OpenSSH **recusa**
usar uma chave privada que outros usuários possam ler ("UNPROTECTED PRIVATE KEY
FILE").

O `known_hosts` vem de um secret, e não de um `ssh-keyscan` feito na hora.
Essa é a diferença entre verificar a identidade do servidor e aceitar qualquer
chave que aparecer: um `ssh-keyscan` no momento da conexão confia em quem
responder, o que abre a porta para *man-in-the-middle* justamente no canal que
carrega o token do registro. Como gerar o valor está em `CICD.md` §2.

Repare que os valores chegam por `env:` e não interpolados dentro do `run:`.
Interpolar `${{ secrets.X }}` direto no corpo do script faz o Actions colar o
conteúdo no texto do comando — uma chave com caractere especial pode quebrar o
shell, e o valor aparece na linha de comando.

```bash
printf '%s' "$TOKEN" | ssh ... "docker login ghcr.io -u '$ATOR' --password-stdin"
```

O token vai por **stdin** nas duas pontas. Como argumento, ele apareceria na
tabela de processos da VPS (`ps aux`), visível para qualquer usuário logado
durante o deploy. `--password-stdin` existe exatamente para isso — o `docker
login -p senha` até funciona, mas emite um aviso dizendo para não fazer isso.

```bash
cd '$CAMINHO'; git fetch --prune origin; git checkout --detach '$COMMIT'
```

A VPS precisa do `docker-compose.prod.yml`, do `Caddyfile` e dos scripts **na
mesma versão da imagem** que vai subir — um backend novo com um `Caddyfile`
velho é uma combinação que ninguém testou. `--detach` (HEAD destacado) em vez
de `checkout main` porque a cópia da VPS não é ambiente de trabalho de
ninguém: ela aponta para um commit exato, não acompanha uma branch. `--prune`
remove referências de branches que já sumiram do remoto.

```bash
ssh ... "REGISTRO='$REGISTRO' '$CAMINHO/docker/prod/scripts/deploy.sh' '$TAG'"
```

E aqui o workflow acaba. Backup, `pull`, `up -d`, espera de saúde, teste de
fumaça e rollback são todos do `deploy.sh` — descritos em `CICD.md` §4. Manter
essa lógica num script versionado, e não em YAML, é o que permite rodar
exatamente o mesmo deploy à mão quando o Actions está indisponível. YAML de
workflow não se executa em lugar nenhum além do Actions; `bash` se executa em
qualquer lugar.

```yaml
- name: Encerrar a sessão da VPS no GHCR
  if: always()
  run: ssh ... "docker logout ghcr.io" || true
```

`if: always()` — o logout precisa acontecer **inclusive quando o deploy falha**,
que é justamente quando esquecer credencial na máquina é pior. O `|| true` no
fim impede que uma falha no logout marque o deploy inteiro como quebrado: a
essa altura o trabalho já terminou, e o token expira sozinho de qualquer forma.

```yaml
- name: Resumo
  if: always()
  run: |
    {
      echo "### Deploy"
      echo "- Tag: \`$TAG\`"
      ...
    } >> "$GITHUB_STEP_SUMMARY"
```

Fecha a execução com tag, endereço e resultado na página do workflow — inclusive
em caso de falha, quando a informação vale mais.

---

## 4. Padrões que se repetem, e o motivo

| Padrão | Onde aparece | Por quê |
|---|---|---|
| `set -euo pipefail` | Todo bloco `run:` de várias linhas | Sem isso, um comando pode falhar no meio do bloco e o passo ainda terminar verde |
| Segredo via `env:`, nunca interpolado no script | Todo passo do `implantar` | Interpolação cola o valor no texto do comando: quebra com caractere especial e vaza no `ps` |
| `permissions` mínimo, elevado por job | Topo dos dois arquivos, `packages: write` só em `imagens` | Menor privilégio: um passo comprometido só consegue o que aquele job precisava |
| Tag derivada do commit, nunca `latest` no deploy | `preparar`, `deploy.sh` | "A última" não é uma versão para a qual se possa voltar |
| Cache declarado (`cache: maven`, `type=gha`) | Todos os jobs de build | A VM nasce vazia toda vez; sem cache, cada execução baixa tudo de novo |
| Falhar cedo e barato | `Conferir a variável DOMINIO` | Erro de configuração pego no primeiro job custa segundos; pego em produção custa rollback |

---

## 5. Mexer nos workflows sem quebrar produção

- **Alteração no `ci.yml` se testa sozinha:** empurre numa branch qualquer que
  não seja a `main` e o próprio push executa o arquivo modificado.
- **Alteração no `cd.yml` não.** O gatilho é a `main` — o arquivo só roda
  depois do merge. Antes de mexer, vale ter em mente que o campo `tag` do
  *Run workflow* é a saída de emergência: ele implanta uma imagem que já existe,
  sem depender do caminho de build que você acabou de alterar.
- **O YAML é validado pelo próprio GitHub**, não pelo CI: um erro de sintaxe
  faz o workflow nem aparecer na lista de execuções, o que parece "não
  disparou" e na verdade é "não compilou". Em caso de silêncio inesperado,
  confira a aba *Actions* por um aviso de workflow inválido.
- **O `paths-ignore` do CD é uma faca de dois gumes:** editar só documentação
  não implanta, o que é o comportamento desejado — mas também significa que um
  commit que mude apenas `.md` **não** leva a VPS a atualizar seu `git
  checkout`. Ela sincroniza no próximo deploy de verdade.

---

## 6. Quando algo falha

A tabela de sintomas e causas prováveis está em [`CICD.md`](./CICD.md) §6, para
não viver em dois lugares. Dois atalhos de diagnóstico que pertencem a esta
página:

- **Falhou antes de qualquer log útil?** Olhe o job `preparar`: ele é barato de
  propósito e concentra as checagens de configuração.
- **Falhou no `implantar`?** O erro que interessa quase nunca está no log do
  Actions, e sim na VPS: o `deploy.sh` imprime `docker compose ps` e as últimas
  80 linhas do backend antes de restaurar a versão anterior. Essa saída aparece
  no log do passo `Deploy`, abaixo da linha de erro.
