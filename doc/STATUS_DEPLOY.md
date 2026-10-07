# Status do deploy — VPS da Purple Clin

> Situação em **07/10/2026**. Guia completo em [`DEPLOY.md`](./DEPLOY.md); este
> arquivo só registra onde a instalação parou e o que falta. O IP fica fora do
> repositório (pasta `deploy/`, no `.gitignore`) — aqui ele aparece como
> `<IP-DA-VPS>`.

---

## 1. Resumo

O servidor está configurado até o firewall (§3.5), mas **não há acesso SSH a
partir da máquina de trabalho**: a conexão na porta `22022` dá timeout. A VPS
está correta — o bloqueio está **antes** dela (rede local ou provedor). Até
isso ser resolvido, nada a partir do §3.0 (chaves) avança, e todo o trabalho
na VPS precisa ser feito pelo **console web do painel**.

---

## 2. O que já foi feito

| Passo | Estado | Observação |
|---|---|---|
| `apt update && apt upgrade` (§3.0) | ✅ | Feito pelo console. Na tela do *needrestart* nenhum serviço foi marcado — falta um `reboot` para aplicar tudo |
| sshd escutando na `22022` | ✅ | `ss -tlnp` mostra `0.0.0.0:22022` e `[::]:22022` |
| `ufw` (§3.5) | ✅ | Ativo: `22022/tcp`, `80,443/tcp`, `443/udp` (v4 e v6). A regra errada `80,443/udp` foi removida |
| Usuário `deploy` (§3.0) | ⚠️ a confirmar | `id deploy` no console |
| Chaves `prontu_admin` / `pd_deploy` geradas na máquina local | ⚠️ a confirmar | O comando que as copia para a VPS falhou (§3) |
| `iptables` da imagem Oracle (§3.5) | ❌ não verificado | `iptables -L INPUT -n --line-numbers` |

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

**Causas possíveis — nesta ordem**

1. **Rede local** bloqueando saída para porta não padrão (e interceptando a 80).
2. **Provedor**: firewall do painel / *security list* / anti-DDoS, possivelmente
   tendo bloqueado o IP público de origem após as tentativas.

---

## 4. Próximos passos

### 4.1 Destravar o SSH (bloqueante)

- [ ] Ligar o PC no roteador do celular e rodar
      `Test-NetConnection <IP-DA-VPS> -Port 22022`
  - `True` → é a rede local: pedir ao responsável pela rede para liberar saída
    TCP para `<IP-DA-VPS>:22022`, ou fazer o setup por outra rede
  - `False` → é o provedor: seguir o próximo item
- [ ] Procurar firewall / *security list* no painel do provedor e liberar
      `22022/tcp`, `80/tcp`, `443/tcp` e `443/udp`
- [ ] Se não houver nada no painel, abrir chamado no suporte informando o IP
      público de origem (`curl ifconfig.me` na máquina local)

### 4.2 Pelo console, enquanto o SSH não volta

- [ ] `reboot` para aplicar o upgrade (bibliotecas e kernel)
- [ ] `id deploy` — se não existir: `adduser deploy && usermod -aG sudo deploy`
- [ ] `iptables -L INPUT -n --line-numbers` — se houver `REJECT ...
      icmp-host-prohibited`: `apt purge -y netfilter-persistent
      iptables-persistent && reboot` (§3.5). **Não** usar `iptables -F`

### 4.3 Depois que o SSH funcionar

- [ ] Copiar as chaves para `/home/deploy/.ssh/authorized_keys` e entrar
      **sem senha** com `prontu_admin` (§3.0, `ACESSO_SSH.md` §1–§5)
- [ ] Docker ≥ v2.24 do Compose (§3.1)
- [ ] Swap de 2 GB (§3.2)
- [ ] Fuso `America/Sao_Paulo` (§3.3)
- [ ] `daemon.json` com rotação de log — **antes** do primeiro `up` (§3.4)
- [ ] SSH sem senha, sem root, `fail2ban` com `port = 22022` (§3.6) — só
      depois de o login por chave funcionar, e testando em outra sessão
- [ ] `/opt/prontudigital` e `/var/backups/prontudigital` do usuário `deploy` (§3.7)
- [ ] Clone do repositório (§4)
- [ ] `.env` e `secrets/` gerados na VPS (§5)
- [ ] DNS: registro `A` de `purpleclin` no registro.br apontando para a VPS
- [ ] Secrets e variables do CD no GitHub, com `VPS_PORT=22022` e
      `VPS_KNOWN_HOSTS` de `ssh-keyscan -p 22022 <IP-DA-VPS>` (`CICD.md` §2)
- [ ] Merge de `config-deploy` na `main` para o CD fazer a primeira subida (§6.1)
- [ ] Verificação, endurecimento e backup (§7, §8, §9) e checklist de corte (§11)
