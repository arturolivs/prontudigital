# Acesso SSH à VPS — ProntuDigital

> Detalha a **§3.0** (usuário de deploy e chaves) e a **§3.6** (endurecimento
> do SSH) do [`DEPLOY.md`](./DEPLOY.md), ajustadas à VPS atual: Ubuntu 22.04,
> SSH na porta **22022** e entrega inicial só com `root` e senha.
>
> Ao final, o servidor aceita apenas login **por chave**, apenas com o usuário
> `deploy`, e bane quem insiste em errar.

---

## 0. Antes de começar

| Item | Valor |
|---|---|
| Sua máquina | Windows, com o PowerShell (o cliente OpenSSH já vem no Windows 10/11) |
| VPS | `<IP-DA-VPS>`, porta SSH `22022` |
| Usuário inicial | `root` com senha (do painel do provedor) |
| Usuário final | `deploy`: com `sudo`, no grupo `docker`, entrando só por chave |

> **Rede de segurança:** se em algum passo você ficar trancado do lado de
> fora, o botão **"Abrir console"** do painel do provedor dá acesso direto à
> máquina, sem SSH. Por ali dá para desfazer qualquer passo desta página.

---

## 1. Como a chave SSH funciona

Uma chave SSH é um **par de arquivos**:

| Arquivo | Onde fica | Pode ser compartilhado? |
|---|---|---|
| **Privada** (sem extensão) | Só na máquina de quem entra | **Nunca.** Equivale a uma senha |
| **Pública** (`.pub`) | No servidor, em `~/.ssh/authorized_keys` | Sim. Sozinha, ela não abre nada |

Ao conectar, o servidor desafia o cliente, e só quem tem a privada
correspondente consegue responder. Nenhuma senha trafega, então não há o que
adivinhar nem interceptar.

**A chave é gerada na máquina de quem vai entrar, nunca na VPS.** A privada
não deve sair de onde nasceu (a exceção é a do GitHub, que vai para um secret
e depois é apagada daqui).

---

## 2. Gerar as chaves — **na sua máquina**

No **PowerShell do seu computador**:

```powershell
# Chave 1: para VOCÊ administrar a VPS.
# Vai pedir uma frase-senha: use uma. Se o arquivo vazar, ela ainda protege.
ssh-keygen -t ed25519 -C "admin-prontudigital" -f $HOME\.ssh\prontu_admin

# Chave 2: para o GITHUB ACTIONS fazer o deploy.
# Sem frase-senha (-N '""'): o Actions não tem como digitar uma.
ssh-keygen -t ed25519 -C "github-actions-prontudigital" -f $HOME\.ssh\pd_deploy -N '""'
```

Resultado em `C:\Users\<voce>\.ssh\`:

```
prontu_admin       ← privada, sua. Não sai daqui
prontu_admin.pub   ← pública  → vai para a VPS
pd_deploy          ← privada  → vai para o secret VPS_SSH_KEY no GitHub
pd_deploy.pub      ← pública  → vai para a VPS
```

**Por que duas chaves:** donos diferentes. Para revogar o acesso do GitHub,
basta apagar a linha dele no `authorized_keys`, sem perder o seu.

---

## 3. Criar o usuário `deploy` — **na VPS, como root**

Último acesso com a senha do root:

```powershell
ssh -p 22022 root@<IP-DA-VPS>
```

```bash
# porta em que o sshd REALMENTE escuta — confirme que é 22022
ss -tlnp | grep sshd

apt update && apt upgrade -y

adduser deploy             # a senha definida aqui é usada só no `sudo`
usermod -aG sudo deploy

# opcional: elimina o aviso "sudo: unable to resolve host ..."
echo "127.0.1.1 $(hostname -f 2>/dev/null || hostname) $(hostname -s)" >> /etc/hosts

exit
```

> O grupo `docker` entra depois, na §3.1 do `DEPLOY.md`, quando o Docker
> estiver instalado (o grupo só existe a partir daí).

---

## 4. Instalar as chaves públicas — **na sua máquina**

Grava as duas `.pub` no `authorized_keys` do `deploy`, com dono e permissões
corretos. Pede a senha do root pela última vez:

```powershell
type $HOME\.ssh\prontu_admin.pub, $HOME\.ssh\pd_deploy.pub |
  ssh -p 22022 root@<IP-DA-VPS> "mkdir -p /home/deploy/.ssh && cat >> /home/deploy/.ssh/authorized_keys && chown -R deploy:deploy /home/deploy/.ssh && chmod 700 /home/deploy/.ssh && chmod 600 /home/deploy/.ssh/authorized_keys"
```

As permissões não são detalhe: com `.ssh` ou `authorized_keys` legível por
outros usuários, o `sshd` **ignora a chave em silêncio** e volta a pedir senha.

### 4.1 Testar

```powershell
ssh -p 22022 -i $HOME\.ssh\prontu_admin deploy@<IP-DA-VPS>
```

| O que pediu | Significado |
|---|---|
| A **frase-senha da chave** (`Enter passphrase for key ...`) | ✅ A chave foi aceita |
| A **senha do servidor** (`deploy@...'s password:`) | ❌ A chave **não** foi aceita. **Não siga para a §6**: confira o §4 |

Teste também a chave do GitHub, que precisa entrar sem pedir nada:

```powershell
ssh -p 22022 -i $HOME\.ssh\pd_deploy deploy@<IP-DA-VPS> "echo ok"
```

### 4.2 Atalho (opcional)

Crie `C:\Users\<voce>\.ssh\config`:

```
Host prontu
    HostName <IP-DA-VPS>
    Port 22022
    User deploy
    IdentityFile ~/.ssh/prontu_admin
```

A partir daí, `ssh prontu` basta. Os exemplos abaixo usam o atalho.

---

## 5. Cadastrar no GitHub

Em *Settings › Secrets and variables › Actions › Secrets*. Detalhes em
[`CICD.md`](./CICD.md) §2.

| Secret | Valor |
|---|---|
| `VPS_SSH_KEY` | Conteúdo **inteiro** de `pd_deploy`, a privada |
| `VPS_KNOWN_HOSTS` | Saída inteira de `ssh-keyscan -p 22022 <IP-DA-VPS>` |

```powershell
# copia a chave privada para a área de transferência
Get-Content $HOME\.ssh\pd_deploy -Raw | Set-Clipboard

# impressão digital do servidor
ssh-keyscan -p 22022 <IP-DA-VPS>
```

As linhas do `ssh-keyscan` saem como `[<IP-DA-VPS>]:22022 ssh-ed25519 ...`, e
os colchetes precisam ficar. Sem eles (gerada na porta 22), o deploy falha com
`Host key verification failed`.

Depois de cadastrar, apague `pd_deploy` (a privada) da sua máquina: a cópia que
importa está no GitHub. Para trocar a chave no futuro, gere outra.

> **Rebuild da VPS muda a impressão digital.** O "Recriar servidor" do painel
> reinstala o sistema e gera chaves de host novas: refaça o `VPS_KNOWN_HOSTS`.

---

## 6. Endurecer o SSH — a §3.6

**Só depois de o teste do §4.1 funcionar.** O objetivo é que ninguém mais
entre com senha. Robôs tentam senhas em todo servidor SSH exposto, o tempo
todo.

### 6.1 Por que não editar o `sshd_config` direto

No Ubuntu 22.04, a primeira linha útil do `/etc/ssh/sshd_config` é:

```
Include /etc/ssh/sshd_config.d/*.conf
```

E no `sshd` **vale o primeiro valor encontrado** para cada opção. Imagens de
VPS costumam trazer ali um `50-cloud-init.conf` com
`PasswordAuthentication yes`, que é lido **antes** do resto do arquivo
principal. Um `sed` no `sshd_config` não tem efeito nenhum, e a senha continua
ligada sem ninguém perceber.

Por isso a configuração vai num arquivo próprio, com prefixo `00-`, para ser
lido primeiro:

```bash
ls /etc/ssh/sshd_config.d/      # veja o que o provedor deixou ali
```

### 6.2 Aplicar

Na VPS, logado como `deploy`:

```bash
sudo tee /etc/ssh/sshd_config.d/00-prontudigital.conf >/dev/null <<'EOF'
# Endurecimento do SSH — doc/ACESSO_SSH.md §6
# Prefixo 00- para ser lido antes de 50-cloud-init.conf: no sshd vale o
# primeiro valor encontrado.
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin no
PubkeyAuthentication yes
EOF

# -t valida a sintaxe; com erro, o restart não acontece
sudo sshd -t && sudo systemctl restart ssh
```

| Linha | Efeito |
|---|---|
| `PasswordAuthentication no` | Login por senha desligado |
| `KbdInteractiveAuthentication no` | Fecha o outro caminho de pedir senha (via PAM), que contorna a linha acima |
| `PermitRootLogin no` | Ninguém entra como root por SSH. Entra-se como `deploy` e usa-se `sudo` |
| `PubkeyAuthentication yes` | Garante o login por chave ligado |

A **porta não muda**: este arquivo não declara `Port`, então continua valendo a
22022 configurada pelo provedor.

### 6.3 Conferir o que realmente vale

```bash
sudo sshd -T | grep -Ei '^(port|passwordauthentication|kbdinteractiveauthentication|permitrootlogin|pubkeyauthentication) '
```

Esperado:

```
port 22022
permitrootlogin no
pubkeyauthentication yes
passwordauthentication no
kbdinteractiveauthentication no
```

O `sshd -T` mostra a configuração **efetiva**, depois de juntar todos os
arquivos. É a única forma confiável de saber o que está valendo.

### 6.4 Testar — **num segundo terminal, sem fechar o atual**

```powershell
ssh prontu                                  # precisa ENTRAR
ssh -p 22022 root@<IP-DA-VPS>               # precisa RECUSAR
ssh -p 22022 -o PubkeyAuthentication=no deploy@<IP-DA-VPS>   # precisa RECUSAR
```

Os dois últimos devem responder `Permission denied (publickey)`, **sem pedir
senha**. Se pedirem senha, o §6.2 não pegou: rode o §6.3 e veja qual opção
ficou `yes`.

Só feche a primeira sessão depois de os três testes darem o resultado esperado.

---

## 7. fail2ban — na porta certa

O fail2ban lê o log do SSH e bane por um tempo o IP que erra o login várias
vezes. **Por padrão ele bloqueia a porta 22.** Com o SSH na 22022, ele baniria o
IP na porta errada, e o atacante continuaria tentando como se nada tivesse
acontecido.

```bash
sudo apt install -y fail2ban

sudo tee /etc/fail2ban/jail.local >/dev/null <<'EOF'
# doc/ACESSO_SSH.md §7 — SSH fora da porta padrão
[sshd]
enabled  = true
port     = 22022
maxretry = 5
findtime = 10m
bantime  = 1h
EOF

sudo systemctl enable --now fail2ban
sudo systemctl restart fail2ban
sudo fail2ban-client status sshd
```

| Opção | Efeito |
|---|---|
| `port = 22022` | Bane na porta em que o SSH realmente está |
| `maxretry = 5` / `findtime = 10m` | 5 falhas em 10 minutos disparam o banimento |
| `bantime = 1h` | O IP fica bloqueado por 1 hora |

Com senha desligada, ninguém entra adivinhando. O fail2ban serve para cortar o
ruído no log e o consumo de CPU das tentativas.

**Banido por engano** (errou a chave várias vezes do seu próprio IP):

```bash
# pelo console do painel, ou de outra rede
sudo fail2ban-client set sshd unbanip <SEU-IP>
```

---

## 8. Problemas comuns

| Sintoma | Causa provável | Correção |
|---|---|---|
| Pede a senha do servidor em vez da frase-senha da chave | Chave pública não está no `authorized_keys`, ou permissões erradas | Refaça o §4. Na VPS: `ls -la ~/.ssh` deve mostrar `drwx------` e `-rw-------`, com dono `deploy` |
| `Permission denied (publickey)` com a sua chave | Arquivo errado no `-i`, ou o `config` aponta para outra chave | `ssh -v prontu` mostra quais chaves foram oferecidas |
| `Connection timed out` | Porta errada, ou o `ufw` sem a 22022 | `DEPLOY.md` §3.5 |
| `Connection refused` | O `sshd` não subiu após o restart | Pelo console: `sudo sshd -t` mostra o erro de sintaxe |
| `REMOTE HOST IDENTIFICATION HAS CHANGED` | A VPS foi recriada (rebuild) | `ssh-keygen -R "[<IP-DA-VPS>]:22022"` e conecte de novo. Refaça também o `VPS_KNOWN_HOSTS` |
| Senha continua funcionando após o §6 | Um `.conf` em `sshd_config.d/` é lido antes e liga a senha | `sudo sshd -T` (§6.3) e `ls /etc/ssh/sshd_config.d/` |
| `sudo: unable to resolve host` | Nome da máquina ausente no `/etc/hosts` | Linha opcional do §3. O aviso é inofensivo |

---

## 9. Checklist

- [ ] Duas chaves geradas na sua máquina: `prontu_admin` (com frase-senha) e `pd_deploy`
- [ ] Usuário `deploy` criado, no grupo `sudo`
- [ ] As duas chaves públicas no `authorized_keys` do `deploy`, com `700`/`600`
- [ ] Login como `deploy` por chave testado com **as duas** chaves
- [ ] `VPS_SSH_KEY` e `VPS_KNOWN_HOSTS` cadastrados no GitHub; `pd_deploy` apagada da máquina local
- [ ] `00-prontudigital.conf` criado, e `sshd -T` mostrando `passwordauthentication no` e `permitrootlogin no`
- [ ] Root e login por senha **recusados** (§6.4)
- [ ] fail2ban ativo, com `port = 22022`
