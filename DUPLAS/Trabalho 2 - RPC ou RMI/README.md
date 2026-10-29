# Trabalho 2 — Remote Procedure Call (RPC) via RMI

Reimplementação da **Questão 4 do Trabalho 1** (serviço remoto de votação) usando
**Java RMI**, seguindo o protocolo requisição-resposta da Seção 5.2 / Figura 5.4
do livro-texto. **Nenhum socket é criado neste código** — todo o transporte é
feito pela Java RMI.

## Como compilar e executar

| Linux/macOS | Windows |
|---|---|
| `./compilar.sh` | `compilar.bat` |
| `./executar.sh <pacote.Classe> [args]` | `executar.bat <pacote.Classe> [args]` |

```bash
./executar.sh rmi.ServidorVotacaoRMI 1099 120     # servidor: porta 1099, prazo 120 s
./executar.sh rmi.ClienteVotacaoRMI localhost 1099  # cliente
```

Usuários de teste (`dados/usuarios.csv`): `admin`/`admin123` (administrador) e
`eleitor1`..`eleitor5`/`123` (eleitores).

## Mapeamento para a Figura 5.4 do livro-texto

| Papel na figura | Classe deste trabalho |
|---|---|
| Client | `ClienteVotacaoRMI` (processo cliente, modo texto) |
| Client stub + `doOperation` | `Requisitor.doOperation` |
| Communication module (cliente/servidor) | runtime da própria Java RMI (nenhum socket nosso) |
| Server | `ServidorVotacaoRMI` (processo servidor) |
| Dispatcher + Skeleton + `getRequest`/`sendReply` | `Invocador` |
| Service (procedimentos de serviço) | `modelo.ServicoVotacao` e `modelo.ServicoAdministracao` (reaproveitados do Trabalho 1) |
| Request-reply message (`messageType`, `requestId`, `objectReference`, `methodID`, `arguments`) | `ComunicadorRemoto.invocarRemota(int methodId, byte[] argumentos)` — o `requestId`/`messageType` ficam a cargo da própria RMI; `objectReference` é o `RemoteObjectRef` que guarda o stub |

`doOperation`, `getRequest` e `sendReply` existem no código com as assinaturas
adaptadas (permitido pelo enunciado): como a Java RMI é síncrona e entrega a
requisição diretamente ao método remoto, `getRequest`/`sendReply` em `Invocador`
explicitam esse papel sem reabrir nenhum socket — o transporte real acontece
dentro da própria chamada RMI.

## Requisitos da aplicação — onde estão

- **≥ 4 classes entidade:** `Candidato`, `Usuario`, `Voto`, `Eleicao`,
  `ItemResultado`, `Resultado`, `NotaInformativa` (pacote `modelo`, reaproveitadas
  do Trabalho 1).
- **≥ 2 agregações ("tem-um"):** `Invocador` tem um `ServicoVotacao` e um
  `ServicoAdministracao`; `ServicoVotacao`/`ServicoAdministracao` têm um
  `RepositorioEleicao`; `RepositorioEleicao` tem as coleções de `Usuario`,
  `Candidato`, `Voto`.
- **≥ 2 extensões ("é-um"):** `ComunicadorRemoto extends Remote`; `Invocador
  extends UnicastRemoteObject`; `ServicoException extends Exception`.
- **≥ 4 métodos de invocação remota:** `AUTENTICAR`, `LISTAR_CANDIDATOS`,
  `VOTAR`, `APURAR`, `TEMPO_RESTANTE_MS`, `VOTACAO_ABERTA`,
  `ADICIONAR_CANDIDATO`, `REMOVER_CANDIDATO`, `SAIR` (constantes em
  `OperacoesRemotas`, despachadas em `Invocador.despachar`).
- **Passagem por referência para objetos remotos:** o cliente obtém, uma única
  vez, o stub do objeto remoto (`Registry.lookup`) e guarda essa referência em
  `RemoteObjectRef`; todas as chamadas seguintes (login, listar, votar, apurar...)
  usam essa mesma referência — nunca uma cópia do objeto de serviço.
- **Passagem por valor para objetos locais, com representação externa de dados:**
  `Candidato`, `Usuario` e `Resultado` nunca trafegam como objetos Java
  serializados nativamente. Eles são convertidos para `Map`/`List` por
  `protocolo.Conversores` e depois para texto **JSON** por `protocolo.Json`
  (o mesmo parser/gerador próprio do Trabalho 1, sem bibliotecas externas) antes
  de virar `byte[]` em `rmi.Empacotador`.

## Protocolo (mensagens)

Requisição: `invocarRemota(int methodId, byte[] argumentos)`, onde `argumentos`
é um objeto JSON, por exemplo:
```json
{"token":"3d9b...", "numero":13}
```
Resposta (sempre com `ok`):
```json
{"ok":true, "comprovante":"826bb95d-..."}
{"ok":false, "erro":"Você já votou nesta eleição."}
```

Como o protocolo é sem estado (cada chamada RMI é independente), `AUTENTICAR`
devolve um **token de sessão**; as demais chamadas devem enviá-lo em `token`.
É o **servidor** quem associa o token ao perfil (eleitor/administrador) — o
cliente nunca pode se autopromover a administrador alterando a mensagem.

## Testes automatizados

```bash
./executar.sh rmi.teste.TesteEmpacotador   # ida e volta do empacotamento JSON
./executar.sh rmi.teste.TesteRMI           # integração: registro RMI real em loopback
```

`TesteRMI` sobe um `Registry` de verdade, publica o `Invocador` e testa, através
do `Requisitor`: operação sem login, login errado/certo, listar candidatos,
votar, voto duplicado e em candidato inexistente, controle de perfil (eleitor
não administra, administrador não vota), adicionar/remover candidato,
apuração bloqueada antes do prazo e liberada depois, voto tardio recusado,
total/percentuais da apuração e um `methodId` desconhecido.

## Limitações conhecidas

Mesmas do Trabalho 1: senha em texto puro nos CSVs, sem TLS, estado em
memória (reiniciar o servidor reinicia a eleição). O token de sessão não
expira sozinho — some apenas quando o cliente chama `SAIR` ou o servidor é
reiniciado.
