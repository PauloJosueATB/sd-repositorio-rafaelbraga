@echo off
chcp 65001 >nul
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
javac -encoding UTF-8 -d out src\modelo\*.java src\protocolo\*.java src\filas\*.java
if errorlevel 1 exit /b 1
echo Compilacao concluida: classes em .\out
