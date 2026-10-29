@echo off
chcp 65001 >nul
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > fontes_trabalho2.txt
javac -encoding UTF-8 -d out @fontes_trabalho2.txt
del fontes_trabalho2.txt
echo Compilacao concluida: classes em .\out
