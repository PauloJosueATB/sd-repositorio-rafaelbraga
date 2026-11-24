# Diagrama de classes — Trabalho 4

## Imagens renderizadas pelo Mermaid

### Fluxo de votação por filas

![Classes do fluxo de votação por filas](DIAGRAMA_CLASSES.png)

### Modelo da eleição

![Classes do modelo da eleição](DIAGRAMA_CLASSES_MODELO.png)

<details>
<summary>Ver código Mermaid</summary>

## Fluxo de votação por filas

```mermaid
classDiagram
    direction LR

    class ClienteVotacaoFila {
        +main(args)
        -executar(broker, args)
        -consultar(broker, correlationId) boolean
    }
    class ClienteFilas {
        -String host
        -int porta
        +executar(comando)
    }
    class BrokerFilas {
        -Path raiz
        -enfileirar(comando)
        -desenfileirar(comando)
        -devolverExpiradas()
        -finalizar(comando, confirmar)
        -buscarResposta(comando)
    }
    class Entrega {
        -String recibo
        -long expiraEm
    }
    class ConsumidorVotacao {
        +main(args)
        -consumir(broker, servico)
        -processar(body, servico, id)
    }
    class CarregadorDados {
        +carregar(pasta, duracaoSegundos) RepositorioEleicao
    }
    class ServicoVotacao {
        +autenticar(login, senha) Usuario
        +listarCandidatos()
        +votar(usuario, numeroCandidato, idRequisicao) Voto
        +apurar() Resultado
        +tempoRestanteMs() long
    }
    class RepositorioEleicao {
        -Path arquivoEstado
        +abrirEstado(arquivo)
        +buscarVotoPorRequisicao(id) Voto
        +registrarVoto(id, login, voto)
    }
    class Json {
        +ler(texto) Object
        +escrever(valor) String
    }
    class ArquivoAtomico {
        +gravar(destino, conteudo)
    }

    ClienteVotacaoFila ..> ClienteFilas : publica e consulta
    ConsumidorVotacao ..> ClienteFilas : retira, publica e confirma
    ClienteFilas ..> BrokerFilas : TCP com JSON
    BrokerFilas "1" *-- "0..*" Entrega : controla recibos
    ConsumidorVotacao ..> CarregadorDados : carrega dados iniciais
    CarregadorDados ..> RepositorioEleicao : cria e popula
    ConsumidorVotacao ..> ServicoVotacao : executa o pedido
    ServicoVotacao --> "1" RepositorioEleicao : usa
    ClienteVotacaoFila ..> Json
    ClienteFilas ..> Json
    BrokerFilas ..> Json
    BrokerFilas ..> ArquivoAtomico : grava mensagens
    RepositorioEleicao ..> Json
    RepositorioEleicao ..> ArquivoAtomico : grava o estado
```

A ligação **TCP com JSON** representa comunicação entre processos. O cliente e o consumidor usam instâncias de `ClienteFilas` para se conectar ao broker. `Entrega` é uma classe interna do broker. Ela guarda o recibo e o prazo de confirmação de uma retirada.

## Modelo da eleição

```mermaid
classDiagram
    direction LR

    class RepositorioEleicao
    class ServicoVotacao
    class Eleicao {
        -int id
        -String titulo
        -long inicio
        -long fim
    }
    class Usuario {
        -String login
        -String nome
        -Perfil perfil
    }
    class Perfil {
        <<enumeration>>
        ELEITOR
        ADMINISTRADOR
    }
    class Candidato {
        -int numero
        -String nome
        -String partido
    }
    class Voto {
        -String comprovante
        -int numeroCandidato
        -long instante
    }
    class Resultado {
        -int totalVotos
    }
    class ItemResultado {
        -int votos
        -double percentual
    }
    class NotaInformativa {
        -String autor
        -String texto
        -long instante
    }

    ServicoVotacao --> "1" RepositorioEleicao
    ServicoVotacao ..> Resultado : apura
    RepositorioEleicao --> "1" Eleicao : guarda
    RepositorioEleicao --> "0..*" Usuario : guarda
    RepositorioEleicao --> "0..*" Candidato : guarda
    RepositorioEleicao --> "0..*" Voto : guarda
    RepositorioEleicao --> "0..*" NotaInformativa : guarda
    Usuario --> "1" Perfil
    Resultado --> "0..*" ItemResultado : detalha
    Resultado --> "0..*" Candidato : vencedores
    ItemResultado --> "1" Candidato : candidato
```

O voto referencia o candidato pelo campo `numeroCandidato`. O repositório também mantém o controle de quem já votou. Os diagramas mostram os atributos e métodos relevantes ao fluxo de votação e à apuração.

</details>
