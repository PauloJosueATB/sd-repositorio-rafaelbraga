@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out (
  echo Pasta "out" nao encontrada. Rode compilar.bat primeiro.
  exit /b 1
)
if "%~1"=="" (
  echo Uso: executar.bat ^<pacote.Classe^> [args...]
  echo Exemplos:
  echo   executar.bat rmi.ServidorVotacaoRMI 1099 120
  echo   executar.bat rmi.ClienteVotacaoRMI localhost 1099
  exit /b 1
)
set CLASSE=%~1
shift
java -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -Djava.rmi.server.hostname=localhost -cp out %CLASSE% %1 %2 %3 %4 %5
