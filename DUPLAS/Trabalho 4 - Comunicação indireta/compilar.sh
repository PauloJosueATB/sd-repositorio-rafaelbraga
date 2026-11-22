#!/bin/sh
set -e
cd "$(dirname "$0")"
rm -rf out
mkdir out
find src -name '*.java' -print > fontes_trabalho4.txt
javac -encoding UTF-8 -d out @fontes_trabalho4.txt
rm fontes_trabalho4.txt
echo "Compilacao concluida: classes em ./out"
