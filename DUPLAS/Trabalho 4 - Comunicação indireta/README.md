# Trabalho 4 — Comunicação Indireta com Filas de Mensagens

## 1. Objetivo e justificativa

Este trabalho evolui o sistema de votação dos Trabalhos 1 e 2 para uma arquitetura de **filas de mensagens persistentes**. O cliente passa a publicar ações em um broker e não conhece endereço, processo ou estado do consumidor. Essa opção é adequada ao cenário porque os votos são operações assíncronas que podem ser postadas mesmo quando o serviço de votação está temporariamente indisponível. O intermediário também separa entrada dos eleitores da aplicação de domínio e permite controlar falhas sem acoplamento ponto a ponto.

O modelo da votação foi reaproveitado dos trabalhos anteriores: `RepositorioEleicao`, `ServicoVotacao`, `Usuario`, `Candidato` e `Voto`. A principal mudança arquitetural está no transporte: não há chamada direta de método entre cliente e servidor.

## 2. Arquitetura e fluxo

```text
Cliente produtor -> Broker TCP -> fila votacao-requests -> Consumidor votacao
                                      |                         |
                                      +-----------------------v
                                     fila votacao-responses <-+ 
                                                           | Cliente consulta por correlationId
```

- **BrokerFilas:** intermediário TCP/JSON na porta 7001. Mantém as mensagens em arquivos `.pending` e `.processing` no diretório `dados-filas`.
- **ConsumidorVotacao:** recebe mensagens, executa o serviço da votação e publica uma resposta. No consumidor não existe referência nem endereço IP do cliente.
- **ClienteVotacaoFila:** conhece somente o broker. Posta ações e procura sua resposta pelo `correlationId`.
- **ClienteFilas:** implementa o protocolo mínimo `ENQUEUE`, `DEQUEUE`, `ACK`, `NACK` e `POLL_RESPONSE`.

A fila usa despacho FIFO aproximado pelo nome sequencial dos arquivos. O produtor não envia diretamente ao receptor final: o único requisito espacial conhecido é o endereço do broker.

## 3. Persistência e robustez

Uma mensagem é gravada como JSON em disco antes o broker confirmar o `ENQUEUE`. Ao retirar da fila, o arquivo muda de `.pending` para `.processing`. Somente um `ACK` remove definitivamente a mensagem. Um `NACK` ou reinício do broker devolve arquivos `.processing` para `.pending`, evitando perda em caso de queda do consumidor.

Consequentemente:

- **Desacoplamento espacial:** a aplicação cliente não possui IP/porta do consumidor.
- **Desacoplamento temporal:** o produtor continua postando enquanto o consumidor está desligado; as mensagens aguardam no disco.
- **Recuperação:** ao reiniciar o broker, mensagens pendentes e em processamento são recuperadas.
- **Entrega:** o ACK evita que a requisição desapareça antes do processamento; a implementação não garante exatamente uma vez, mas evita perda em falhas simples e usa `correlationId` para relacionar resposta e pedido.

Reiniciar o consumidor perde temporariamente o estado em memória da eleição, pois `RepositorioEleicao` é carregado novamente dos CSVs. Para produção seriam necessários replay e persistência de votos já aceitos, idempotência por `correlationId` e um mecanismo de transação/outbox.

## 4. Compilação e execução

### Windows

```bat
cd "Trabalho 4 - Comunicação indireta"
compilar.bat

executar.bat filas.BrokerFilas 7001 dados-filas
executar.bat filas.ConsumidorVotacao localhost 7001 120
executar.bat filas.ClienteVotacaoFila localhost 7001 listar
executar.bat filas.ClienteVotacaoFila localhost 7001 votar eleitor1 123 13
executar.bat filas.ClienteVotacaoFila localhost 7001 apurar
```

### Linux/macOS

```sh
cd "Trabalho 4 - Comunicação indireta"
./compilar.sh

./executar.sh filas.BrokerFilas 7001 dados-filas
./executar.sh filas.ConsumidorVotacao localhost 7001 120
./executar.sh filas.ClienteVotacaoFila localhost 7001 listar
./executar.sh filas.ClienteVotacaoFila localhost 7001 votar eleitor1 123 13
./executar.sh filas.ClienteVotacaoFila localhost 7001 apurar
```

Credenciais de teste: `eleitor1` a `eleitor5`, senha `123`, ou `admin`/`admin123`.

## 5. Demonstração prática de falha e recuperação

1. Inicie o broker.
2. **Não** inicie o consumidor.
3. Execute `listar` ou `votar` no cliente. O comando deve ser confirmado pelo broker mesmo sem consumidor.
4. Verifique a persistência em `dados-filas/votacao-requests`.
5. Inicie o consumidor. Ele processa a mensagem pendente e a resposta aparece em `votacao-responses`.
6. Reinicie o consumidor ou derrube-o após uma retirada. Mensagens `.processing` são devolvidas para `.pending` na próxima inicialização do broker.

Para demonstrar o desacoplamento do consumidor, outra possibilidade é iniciar o cliente com outro terminal e alterar o consumidor da porta original. A única configuração necessária no cliente permanece sendo a porta/host do broker.

## 6. Análise de desempenho e complexidade

O intermediário cria trabalho extra: persistência em disco, serialização JSON, novas etapas de rede, polling e ACK. Isso aumenta latência em comparação com uma chamada RMI direta e reduz vazão se cada operação fizer uma gravação síncrona. A simplicidade também é limitada: o broker não replica dados, não usa confirmação distribuída e o estado da eleição não está persistido.

As perdas podem ser mitigadas por: gravação em lote, nomes de arquivo sequenciais, cache do último ACK, indexação por fila, réplica dos arquivos do broker, limite de tamanho da fila, back-pressure e múltiplos consumidores. Em produção, um framework como RabbitMQ ou Kafka forneceria replicação, durabilidade, dead-letter queues e escalonamento. O grupo optou por manter o broker didático em Java puro para que persistência, ACK e recuperação possam ser demonstrados sem dependências externas.
