#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
if [ ! -d out ]; then
  echo "Pasta 'out' não encontrada. Rode ./compilar.sh primeiro." >&2
  exit 1
fi
if [ $# -lt 1 ]; then
  echo "Uso: $0 <pacote.Classe> [args...]"
  echo "Exemplos:"
  echo "  $0 rmi.ServidorVotacaoRMI 1099 120"
  echo "  $0 rmi.ClienteVotacaoRMI localhost 1099"
  echo "  $0 rmi.teste.TesteEmpacotador"
  echo "  $0 rmi.teste.TesteRMI"
  exit 1
fi
CLASSE="$1"; shift
java -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Djava.rmi.server.hostname=localhost -cp out "$CLASSE" "$@"
