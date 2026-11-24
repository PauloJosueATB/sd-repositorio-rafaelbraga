# Trabalho 4 — Comunicação Indireta

## Relatório técnico

### Escolha e integração

Adotamos a **Opção C — Filas de Mensagens** para evoluir o sistema de votação dos Trabalhos 1 e 2. O produtor envia solicitações de listagem, votação e apuração ao broker TCP/JSON. O consumidor retira as solicitações, aplica as regras de votação e publica as respostas em outra fila. O produtor usa o `correlationId` para encontrar a resposta.

O desacoplamento espacial ocorre porque o produtor configura apenas o endereço do broker e o consumidor também se conecta somente ao broker. O desacoplamento temporal ocorre porque o broker grava as solicitações em disco antes de confirmar a postagem. Assim, o produtor pode encerrar sua execução com o consumidor desligado e consultar o resultado em outra sessão.

### Persistência e recuperação

O broker move a mensagem de `.pending` para `.processing` na retirada. Cada entrega recebe um recibo e tem cinco segundos para confirmação. Se o consumidor cair, uma nova tentativa de retirada após esse prazo devolve a mensagem à fila. Quando o broker reinicia, recupera as mensagens em processamento. Um `ACK` antigo é rejeitado se a mensagem já tiver sido entregue novamente.

O consumidor grava o voto, o controle de quem já votou e o prazo da eleição em disco. Em seguida publica a resposta e confirma a solicitação. Uma reentrega do mesmo pedido de voto devolve o comprovante original. As respostas ficam disponíveis para consultas posteriores. O tempo da eleição começa na primeira inicialização daquele diretório de estado. O prazo é avaliado no processamento e permanece igual após reiniciar.

### Desempenho e gerenciamento

A passagem pelo broker inclui serialização JSON, conexões adicionais, gravações em disco e confirmações. A consulta periódica acrescenta até cerca de 500 ms de espera por retirada e 300 ms por consulta automática de resposta, além do tempo de rede e processamento. A listagem de arquivos da fila e a regravação do estado da eleição a cada voto aumentam o custo conforme o volume cresce. A análise é qualitativa.

Notificações de disponibilidade reduziriam o polling. Um índice por identificador e um armazenamento transacional reduziriam varreduras e regravações. A instalação didática usa um broker e um consumidor por eleição. Replicação do broker e retenção com prazo para respostas seriam passos para uma implantação maior. Use dados de teste porque o protocolo TCP e as solicitações em disco contêm credenciais sem criptografia.

## Demonstração prática

Use Java 11 ou superior e execute os comandos na pasta deste trabalho. No Windows, use `compilar.bat` e substitua `sh executar.sh` por `executar.bat`.

1. Compile com `sh compilar.sh`. Em um terminal, inicie somente o broker.

   ```sh
   sh executar.sh filas.BrokerFilas 7001 dados-filas-demo
   ```

2. Em outro terminal, envie um voto com o consumidor desligado. Copie o `correlationId` impresso e confira o arquivo `.pending` em `dados-filas-demo/votacao-requests`.

   ```sh
   sh executar.sh filas.ClienteVotacaoFila localhost 7001 enfileirar votar eleitor1 123 13
   ```

3. Inicie o consumidor em um terceiro terminal. Consulte o comprovante em outra execução, substituindo `ID_COPIADO` pelo identificador da etapa anterior.

   ```sh
   sh executar.sh filas.ConsumidorVotacao localhost 7001 60 estado-eleicao-demo
   sh executar.sh filas.ClienteVotacaoFila localhost 7001 consultar ID_COPIADO
   ```

4. Pare o consumidor com Ctrl+C e poste outro voto com `eleitor2 123 22`. Suba o consumidor com o mesmo comando antes do fim dos 60 segundos e consulte o segundo comprovante. Após o fim do prazo, execute `sh executar.sh filas.ClienteVotacaoFila localhost 7001 apurar` e confira os dois votos.

Para repetir com uma nova eleição, escolha novos diretórios vazios para o broker e para o estado do consumidor. Mantenha os mesmos diretórios quando quiser demonstrar recuperação. Os comandos sem `enfileirar` aguardam a resposta por até cerca de 30 segundos.
