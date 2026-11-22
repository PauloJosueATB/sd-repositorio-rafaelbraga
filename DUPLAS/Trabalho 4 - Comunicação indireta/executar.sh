#!/bin/sh
set -e
cd "$(dirname "$0")"
if [ ! -d out ]; then echo "Pasta out nao encontrada. Execute ./compilar.sh primeiro."; exit 1; fi
if [ "$#" -eq 0 ]; then
  echo "Uso: ./executar.sh <pacote.Classe> [args...]"
  echo "Exemplos:"
  echo "  ./executar.sh filas.BrokerFilas 7001 dados-filas"
  echo "  ./executar.sh filas.ConsumidorVotacao localhost 7001 120"
  echo "  ./executar.sh filas.ClienteVotacaoFila localhost 7001 listar"
  exit 1
fi
java -Dfile.encoding=UTF-8 -cp out "$@"
