# Trabalho 1 – Comunicação entre processos (Capítulo 4)

**Disciplina:** QXD0043 – Sistemas Distribuídos · UFC Campus Quixadá · **Professor:** Rafael Braga
**Alunos:** Paulo Josué de Almeida Teixeira Barros (567608) · Roberth Ravell (562366)

**Serviço remoto escolhido:** *Sistema de Votação Eletrônica*. O mesmo domínio (candidatos, eleitores, votos, notas…)
é usado nas cinco questões, de modo que os POJOs, os streams (Q2/Q3), o serviço remoto (Q4) e o sistema
de votações (Q5) se encaixam uns nos outros.

Somente a biblioteca padrão do Java é usada (**JDK 11 ou superior**; desenvolvido e testado com JDK 21).

---

## 1. Como compilar e executar

Execute sempre **de dentro desta pasta**.

| | Linux / macOS | Windows (use o **Prompt de Comando**) |
|---|---|---|
| Compilar | `bash compilar.sh` | `compilar.bat` |
| Executar | `bash executar.sh <pacote.Classe> [args]` | `executar.bat <pacote.Classe> [args]` |

Os scripts compilam para `out/` e executam com `-Djava.net.preferIPv4Stack=true` (necessário para o multicast
funcionar de forma previsível) e com UTF-8. O diretório de trabalho da execução é sempre esta pasta
(é de lá que o servidor lê `dados/`).

## 2. Estrutura

```
Trabalho 1 - Comunicacao entre processos/
├── compilar.sh / .bat, executar.sh / .bat
├── dados/                      candidatos.csv e usuarios.csv (a "Data" da Figura 1)
└── src/
    ├── modelo/                 Q1  POJOs + classes de modelo (serviços) + repositório em memória
    ├── streams/                Q2  EleicaoOutputStream    Q3  EleicaoInputStream
    │   └── teste/                  testes: System.out/in, arquivo, TCP + teste automatizado
    ├── rpc/                    Q4  serviço remoto: empacotamento de request/reply sobre TCP
    └── votacao/                Q5  sistema de votações
        ├── protocolo/              JSON, mensagens, constantes do protocolo, conversores POJO<->JSON
        ├── servidor/               servidor multi-threaded (TCP) + publicador multicast (UDP)
        ├── cliente/                clientes de terminal: eleitor e administrador
        └── teste/                  testes automatizados (JSON e integração)
```

---

## 3. Questão 1 – Serviço remoto, POJOs e classes de modelo

**POJOs** (`src/modelo`): `Candidato`, `Usuario` (+ enum `Perfil`), `Eleicao`, `Voto`, `NotaInformativa`,
`ItemResultado` e `Resultado`.

**Classes de modelo que implementam serviços:**

| Classe | Serviço | Operações |
|---|---|---|
| `ServicoVotacao` | usado pelos **eleitores** | `autenticar`, `listarCandidatos`, `votar`, `apurar`, `votacaoAberta`, `tempoRestanteMs` |
| `ServicoAdministracao` | usado pelos **administradores** | `adicionarCandidato`, `removerCandidato`, `criarNota` |

Apoio: `RepositorioEleicao` (dados em memória, thread-safe), `CarregadorDados` (lê os CSV de `dados/`),
`Formatadores` (texto de apuração/tempo) e `ServicoException` (erros de regra de negócio).

**Regras de negócio** (implementadas uma única vez em `modelo`, valendo para Q4 e Q5):
apenas eleitores votam · um voto por eleitor · votos só até o prazo · após o prazo o resultado (total, percentuais e
vencedor/empate) é liberado e a lista de candidatos fica congelada · não se remove candidato que já recebeu votos ·
o voto não é associado ao eleitor (só um comprovante aleatório – sigilo do voto).

---

## 4. Questão 2 – `EleicaoOutputStream` (subclasse de `OutputStream`)

Seguindo a convenção `PojoEscolhidoOutputStream` do enunciado, o POJO **`Eleicao`** deu origem a
`EleicaoOutputStream`, que envia os dados de um **array de `Candidato`**.

```java
new EleicaoOutputStream(Candidato[] candidatos, int quantidade, OutputStream destino)
```

Os parâmetros são: (i) o array, (ii) quantos objetos serão enviados (`0…candidatos.length`) e (iv) o destino.
O envio ocorre no construtor. `write(...)`, `flush()` e `close()` repassam ao destino, então o objeto continua sendo
um `OutputStream` comum.

**Formato dos bytes** (big-endian, `DataOutputStream`) – item (iii): *para cada objeto é enviado o número de bytes usados para gravar seus atributos* (3 atributos):

```
int  quantidade
repete `quantidade` vezes:
    int    tamanho           ← nº de bytes usados para gravar os atributos deste objeto
    bytes  [tamanho]:        int numero | UTF nome | UTF partido
```

Exemplo real (3 candidatos, 147 bytes; `--hex`): `00 00 00 03 | 00 00 00 2A | 00 00 00 0D 00 0C 41 64 61 20 …`

**Testes (b):**

```bash
bash executar.sh streams.teste.TesteSaidaPadrao --hex     # (i)   System.out  (sem --hex: bytes brutos)
bash executar.sh streams.teste.TesteSaidaArquivo          # (ii)  FileOutputStream -> candidatos.bin
bash executar.sh streams.teste.ServidorEleicaoTCP         # (iii) terminal 1: servidor remoto (porta 7000)
bash executar.sh streams.teste.TesteSaidaTCP              #       terminal 2: cliente que envia via TCP
```

## 5. Questão 3 – `EleicaoInputStream` (subclasse de `InputStream`)

```java
new EleicaoInputStream(InputStream origem)      // (a) recebe o InputStream de origem
Candidato[] lidos = stream.lerCandidatos();     // lê exatamente o formato gerado na Questão 2
```

A leitura não usa buffer próprio (consome só o necessário), o que permite embutir esses dados dentro de mensagens
maiores – é o que a Questão 4 faz. Validações protegem contra dados corrompidos (quantidades/tamanhos absurdos).

**Testes:**

```bash
# (b) origem = System.in  (a saída padrão de um programa vira a entrada do outro)
bash executar.sh streams.teste.TesteSaidaPadrao | bash executar.sh streams.teste.TesteEntradaPadrao
bash executar.sh streams.teste.TesteEntradaPadrao < candidatos.bin

# (c) origem = arquivo (FileInputStream) – rode antes o TesteSaidaArquivo
bash executar.sh streams.teste.TesteEntradaArquivo

# (d) servidor remoto TCP: o ServidorEleicaoTCP lê do socket com EleicaoInputStream
bash executar.sh streams.teste.ServidorEleicaoTCP         # terminal 1
bash executar.sh streams.teste.TesteSaidaTCP              # terminal 2
```

> No Windows use o **Prompt de Comando** para os pipes/redirecionamentos binários; o PowerShell converte os bytes em texto.

**Teste automatizado** (memória, arquivo e TCP em loopback, acentos, quantidade 0, dados truncados, atributos extras):
`bash executar.sh streams.teste.TesteRoundTrip`

---

## 6. Questão 4 – Serviço remoto cliente-servidor (serialização)

Serviço: consulta e votação remotas, via **sockets TCP** trocando fluxos de bytes. Os métodos remotos são
`listarCandidatos()`, `votar(login, senha, numero)`, `apurar()` e `tempoRestante()`.

| Quem | O quê | Onde |
|---|---|---|
| Cliente | **empacota** o request | `ClienteRPC.invocar` → `Empacotador.empacotarRequisicao` |
| Servidor | **desempacota** o request | `ServidorRPC.atender` → `Empacotador.desempacotarRequisicao` |
| Servidor | **empacota** o reply | `ServidorRPC.atender` → `Empacotador.empacotarResposta` |
| Cliente | **desempacota** o reply | `ClienteRPC.invocar` → `Empacotador.desempacotarResposta` |

Representação externa (binária, big-endian) e enquadramento no TCP (`int tamanho + bytes`), definidos em `Empacotador`:

```
Requisição: byte tipo(1) | int id | UTF metodo | int nArgs | valor[nArgs]
Resposta  : byte tipo(2) | int id | boolean sucesso | (sucesso ? valor : UTF erro)
valor     : tag + conteúdo →  0 nulo · 1 int · 2 long · 3 String · 4 boolean
                               5 Candidato[]  ← usa EleicaoOutputStream/EleicaoInputStream (Q2/Q3)
                               6 Resultado
```

O servidor atende cada cliente em uma thread e despacha as chamadas para `ServicoVotacao`.

```bash
bash executar.sh rpc.ServidorRPC 6000 60                            # terminal 1 (porta 6000, votação de 60 s)
bash executar.sh rpc.DemoClienteRPC localhost 6000 eleitor1 123 13  # terminal 2 (lista, vota, revota, tenta apurar)
bash executar.sh rpc.TesteRPC                                       # teste automatizado
```

---

## 7. Questão 5 – Sistema de votações distribuído

### Arquitetura (Figura 1 do enunciado)

```
   Eleitor ──┐   TCP (unicast): login, candidatos, voto     ┌───────────────────────────┐
   Eleitor ──┼──────────────────────────────────────────────►  ServidorVotacao          │  ┌──────────────┐
   Eleitor ──┘                                               │  (multi-threaded:        │──│ dados/*.csv  │
                                                             │   1 thread por cliente)  │  │ (Data)       │
   Administrador ── TCP: login, add/remove candidato, nota ─►│                          │  └──────────────┘
        ▲                                                    └────────────┬──────────────┘
        └──────────── UDP multicast (230.0.0.1:4446) ◄──── nota informativa ┘  (eleitores e admins escutam)
```

* **Unicast (TCP):** login, envio da lista de candidatos, votos, resultado e comandos de administração.
* **Multicast (UDP):** *exclusivamente* para as notas informativas. O administrador envia a nota por TCP; o servidor
  confere que ele é administrador e a retransmite ao grupo em um datagrama UDP. Todos os clientes logados escutam o grupo
  (o servidor informa grupo/porta na resposta do login).
* **Prazo:** o servidor é iniciado com uma duração. Ao terminar, rejeita novos votos, e uma tarefa agendada imprime
  no console **total de votos, percentuais e vencedor** (ou empate). Os clientes também podem pedir o `RESULTADO` (só liberado após o prazo).
* **Representação externa de dados: JSON** (uma das opções aceitas no enunciado). O parser/gerador está em
  `votacao/protocolo/Json.java`, sem dependências externas, então basta compilar e rodar.

### Protocolo

TCP: uma mensagem JSON por linha (UTF-8). Toda resposta traz `"ok": true|false` e, quando `false`, `"erro"`.

| Requisição | Resposta (além de `ok`) |
|---|---|
| `{"op":"LOGIN","login":"..","senha":".."}` | `nome, perfil, titulo, candidatos[], votacaoAberta, tempoRestanteMs, grupoMulticast, portaMulticast` |
| `{"op":"LISTAR_CANDIDATOS"}` | `candidatos[{numero,nome,partido}]` |
| `{"op":"VOTAR","candidato":13}` | `comprovante` |
| `{"op":"STATUS"}` | `titulo, votacaoAberta, tempoRestanteMs` |
| `{"op":"RESULTADO"}` | `resultado{totalVotos, itens[{candidato,votos,percentual}], vencedores[]}` |
| `{"op":"ADICIONAR_CANDIDATO","numero":..,"nome":..,"partido":..}` *(admin)* | `candidato` |
| `{"op":"REMOVER_CANDIDATO","numero":..}` *(admin)* | – |
| `{"op":"ENVIAR_NOTA","texto":".."}` *(admin)* | – |
| `{"op":"SAIR"}` | – |

UDP multicast (um datagrama): `{"tipo":"NOTA","autor":"admin","texto":"..","instante":1700000000000}`

### Executando (um terminal para cada item)

```bash
# 1) Servidor: [portaTCP=5000] [duracaoSeg=120] [grupo=230.0.0.1] [portaMulticast=4446]
bash executar.sh votacao.servidor.ServidorVotacao 5000 120

# 2) Eleitores (quantos quiser)      [host=localhost] [porta=5000]
bash executar.sh votacao.cliente.ClienteEleitor

# 3) Administrador
bash executar.sh votacao.cliente.ClienteAdministrador
```

Para usar **várias máquinas**, execute os clientes com o IP do servidor: `bash executar.sh votacao.cliente.ClienteEleitor 192.168.0.10 5000`.

**Usuários padrão** (`dados/usuarios.csv`): `admin` / `admin123` (administrador) e `eleitor1`…`eleitor5` / `123`.
**Candidatos** iniciais em `dados/candidatos.csv` (13, 22 e 45). Ambos os arquivos podem ser editados.

**Roteiro de demonstração:** suba o servidor com 120 s → entre com 2–3 eleitores → no administrador use *Enviar nota* e
veja a nota aparecer nos eleitores (multicast) → adicione um candidato e peça *Listar* no eleitor → vote → tente votar de novo
(recusado) → aguarde o prazo: o servidor imprime a apuração e o voto tardio é recusado.

**Se as notas não chegarem** (problemas de multicast são de rede, não do programa): mantenha todos na mesma rede local e sem
isolamento de clientes no Wi-Fi; libere UDP na porta do multicast no firewall; se houver várias placas/VPN, escolha a interface com
`JAVA_TOOL_OPTIONS="-Dmulticast.interface=wlan0"` (Windows: `set JAVA_TOOL_OPTIONS=-Dmulticast.interface=Wi-Fi`) antes de iniciar servidor e clientes.
Sem multicast o cliente avisa e o resto do sistema (TCP) continua funcionando.

### Testes automatizados

```bash
bash executar.sh votacao.teste.TesteJson         # gerador/parser JSON (escapes, acentos, entradas inválidas)
bash executar.sh votacao.teste.TesteIntegracao   # servidor real: login, admin, multicast UDP, 4 eleitores concorrentes,
                                                 # voto duplicado/tardio, prazo (~7 s), apuração e percentuais
```

## 8. Limitações conhecidas (trabalho didático)

* Senhas em texto puro nos CSV e tráfego sem TLS.
* Notas multicast não são autenticadas (qualquer host da LAN poderia enviar um datagrama ao grupo); por isso elas só são
  aceitas *pelo servidor* quando enviadas por um administrador autenticado via TCP, mas o canal UDP em si é aberto.
* O estado (votos, candidatos) fica em memória: reiniciar o servidor reinicia a votação.
