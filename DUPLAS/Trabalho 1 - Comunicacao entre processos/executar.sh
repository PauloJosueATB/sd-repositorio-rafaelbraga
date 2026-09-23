#!/usr/bin/env bash
# Executa uma classe já compilada (pasta out/). O diretório de trabalho passa a ser a pasta do trabalho,
# de modo que dados/ e os arquivos gerados (ex.: candidatos.bin) ficam sempre no mesmo lugar.
# Uso: bash executar.sh <pacote.Classe> [argumentos...]
#   ex.: bash executar.sh votacao.servidor.ServidorVotacao 5000 120
cd "$(dirname "$0")" || exit 1
if [ $# -lt 1 ]; then
    echo "Uso: bash executar.sh <pacote.Classe> [argumentos...]"
    exit 1
fi
# preferIPv4Stack: necessário para o multicast funcionar de forma previsível em várias plataformas
exec java -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp out "$@"
