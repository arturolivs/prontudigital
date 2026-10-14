# Status do deploy — VPS da Purple Clin

> Situação em **09/10/2026**. Guia completo em [`DEPLOY.md`](./DEPLOY.md); este
> arquivo só registra onde a instalação parou e o que falta. O IP e os prints
> ficam fora do repositório (pastas `deploy/` e `temp/`, no `.gitignore`) —
> aqui ele aparece como `<IP-DA-VPS>`.

---

## 1. Resumo

O servidor está configurado até o firewall (§3.5), com o `ufw` subindo sozinho
após reboot, mas **não há acesso SSH a partir da rede do escritório**: a
conexão na porta `22022` dá timeout. A VPS está correta — o bloqueio está
**antes** dela.

**Atualização 09/10:** pela rede do celular o SSH conecta — a causa é a rede
do escritório (§3). Passos por SSH (§4.4) vão pelo celular até a rede ser
liberada; o resto segue pelo console (§4.2).

---

## 2. O que já foi feito

| Passo | Estado | Observação |
|---|---|---|
| `apt update && apt upgrade` (§3.0) | ✅ | Feito pelo console. Na tela do *needrestart* nenhum serviço foi marcado |
| sshd escutando na `22022` | ✅ | `ss -tlnp` mostra `0.0.0.0:22022` e `[::]:22022` |
| `ufw` (§3.5) | ✅ | Ativo: `22022/tcp`, `80,443/tcp`, `443/udp` (v4 e v6). A regra errada `80,443/udp` foi removida |
| `reboot` após o upgrade | ✅ | 08/10/2026 |
| Usuário `deploy` (§3.0) | ✅ | `uid=1000`, no grupo `sudo` |
| Chave `prontu_admin` no `authorized_keys` do `deploy` | ✅ | 09/10, pela rede do celular. Login por chave ok (a frase-senha antiga foi perdida e a chave, regerada) |
| Chave `pd_deploy` (CD) | ✅ | `"echo ok"` respondeu sem pedir nada |
| `iptables` da imagem Oracle (§3.5) | ✅ | Cadeia `INPUT` vazia, política `ACCEPT` — sem `REJECT` da imagem, nada a remover |
| `ufw` ativo após o reboot | ✅ | Estava `inactive`/`disabled` após o reboot. Corrigido com `systemctl enable ufw && ufw enable`: `INPUT` com política `DROP` e cadeias `ufw-*` |

---

## 3. Bloqueio atual — SSH dá timeout

**Sintoma**

```
ssh: connect to host <IP-DA-VPS> port 22022: Connection timed out
```

**Diagnóstico feito**

| Teste | Resultado | Conclusão |
|---|---|---|
| `ss -tlnp \| grep sshd` na VPS | escutando na `22022` | sshd ok |
| `ufw status numbered` | `22022/tcp` liberada | firewall da VPS ok |
| `tcpdump -ni any tcp port 22022` na VPS durante o teste | **nenhum pacote** | a conexão não chega à VPS |
| `Test-NetConnection -Port 22022` da máquina local | timeout | — |
| Ping da máquina local | respondia no começo, depois passou a falhar | algo mudou fora da VPS |
| Porta 80 da máquina local | TCP "abre", mas o HTTP não responde | indício de proxy/firewall na rede local |
| `iptables -L INPUT` após o reboot | vazia, política `ACCEPT` | a VPS não filtra nada — o timeout é externo |

| `Test-NetConnection -Port 22022` pela rede do celular (09/10) | **`True`** | VPS e provedor ok |

**Causa confirmada: a rede local do escritório** bloqueia a saída para a porta
`22022` (e intercepta a 80). Não afeta o CD — o GitHub Actions conecta de
fora — nem os usuários do sistema, que só usam 80/443. Afeta apenas o acesso
administrativo por SSH a partir do escritório.

---

## 4. Próximos passos

### 4.1 Destravar o SSH (bloqueante)

- [x] Teste pela rede do celular — `True`: o bloqueio é a rede local
- [ ] Pedir ao responsável pela rede do escritório a liberação da saída TCP
      para `<IP-DA-VPS>:22022`. Até lá, todo passo por SSH (§4.4) é feito
      **pela rede do celular** (ou outra rede)

### 4.2 Pelo console, sem depender do SSH

Feito até aqui, como root:

- [x] `reboot` para aplicar o upgrade (bibliotecas e kernel)
- [x] `id deploy` — existe e está no grupo `sudo`
- [x] `iptables -L INPUT -n --line-numbers` — sem regras da imagem Oracle
- [x] `ufw` habilitado no boot (`systemctl enable ufw && ufw enable`)
- [x] `ufw` subindo sozinho após `reboot` — confirmado
- [ ] `ufw status numbered` — confirmar que as regras `22022/tcp`,
      `80,443/tcp` e `443/udp` continuam lá

A seguir, logado como **`deploy`** (senha do `adduser`, `sudo` quando
preciso), na ordem do `DEPLOY.md`:

- [ ] Docker + `docker compose version` ≥ v2.24 (§3.1)
- [ ] Swap de 2 GB (§3.2)
- [ ] `timedatectl set-timezone America/Sao_Paulo` (§3.3)
- [ ] `/etc/docker/daemon.json` com rotação de log — **antes** de qualquer
      `up` (§3.4)
- [ ] `/opt/prontudigital` e `/var/backups/prontudigital` pertencendo ao
      `deploy` (§3.7)
- [ ] `git clone` em `/opt/prontudigital` (§4)
- [ ] `.env` com `chmod 600` e os três arquivos em `secrets/`, gerados na
      própria VPS (§5)

> ⚠️ **Não fazer o §3.6 ainda.** Ele desliga o login por senha — sem o login
> por chave funcionando, o único acesso passa a ser o console.

### 4.3 Fora da VPS, em paralelo

- [ ] DNS: registro `A` de `purpleclin` no registro.br apontando para a VPS
      (fazer cedo, por causa da propagação)
- [ ] Secrets e variables do CD no GitHub, com `VPS_PORT=22022` (`CICD.md` §2)
      — tudo menos o `VPS_KNOWN_HOSTS`, que depende do SSH

### 4.4 Por SSH (rede do celular enquanto o escritório bloqueia)

- [x] Copiar as chaves para `/home/deploy/.ssh/authorized_keys` e entrar
      **sem senha** com `prontu_admin` (§3.0, `ACESSO_SSH.md` §1–§5)
- [x] Testar a `pd_deploy`: `"echo ok"` sem pedir frase-senha nem senha
- [x] `ssh-keyscan` gerado — rodar **na VPS**; o do Windows falha com
      `unsupported KEX method sntrup761x25519`
- [ ] Cadastrar secrets, variables e o environment `producao` no GitHub —
      passo a passo com os valores reais em `temp/SECRETS_GITHUB.md` (local,
      fora do git)
- [ ] Conferir se o último login do `deploy` antes de 09/10 (02/10, de
      `192.207.206.203`) foi seu — `last -i deploy`
- [ ] `VPS_KNOWN_HOSTS` no GitHub com a saída de
      `ssh-keyscan -p 22022 <IP-DA-VPS>` (`CICD.md` §2)
- [ ] SSH sem senha, sem root, `fail2ban` com `port = 22022` (§3.6) —
      testando em **outra sessão** antes de fechar a atual
- [ ] Merge de `config-deploy` na `main` para o CD fazer a primeira subida (§6.1)
- [ ] Verificação, endurecimento e backup (§7, §8, §9) e checklist de corte (§11)
