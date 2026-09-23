@echo off
chcp 65001 >nul
cd /d "%~dp0"
if "%~1"=="" (
    echo Uso: executar.bat ^<pacote.Classe^> [argumentos]
    exit /b 1
)
java -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp out %*
