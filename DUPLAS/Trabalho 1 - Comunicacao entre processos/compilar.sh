#!/usr/bin/env bash
# Compila todos os fontes de src/ para out/ (requer JDK 11 ou superior).
# Uso: bash compilar.sh
set -e
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
find src -name '*.java' > out/fontes.txt
javac -encoding UTF-8 -d out @out/fontes.txt
echo "Compilação concluída: classes em ./out"
