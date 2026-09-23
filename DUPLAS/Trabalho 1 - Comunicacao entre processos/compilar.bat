@echo off
chcp 65001 >nul
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
rem O launcher do Java no Windows expande os curingas (*.java)
javac -encoding UTF-8 -d out -sourcepath src src\modelo\*.java src\streams\*.java src\streams\teste\*.java src\rpc\*.java src\votacao\protocolo\*.java src\votacao\servidor\*.java src\votacao\cliente\*.java src\votacao\teste\*.java
if errorlevel 1 (
    echo Erro de compilacao.
    exit /b 1
)
echo Compilacao concluida: classes em .\out
