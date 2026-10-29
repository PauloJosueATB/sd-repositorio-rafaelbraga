#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
rm -rf out
mkdir -p out
find src -name "*.java" > /tmp/fontes_trabalho2.txt
javac -encoding UTF-8 -d out @/tmp/fontes_trabalho2.txt
rm -f /tmp/fontes_trabalho2.txt
echo "Compilação concluída: classes em ./out"
