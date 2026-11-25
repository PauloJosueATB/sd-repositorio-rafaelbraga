@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out (
  echo Pasta out nao encontrada. Execute compilar.bat primeiro.
  exit /b 1
)
if "%~1"=="" (
  echo Uso: executar.bat ^<pacote.Classe^> [args...]
  echo Exemplos:
  echo   executar.bat filas.BrokerFilas 7001 dados-filas
  echo   executar.bat filas.ConsumidorVotacao localhost 7001 120
  echo   executar.bat filas.ClienteVotacaoFila localhost 7001 listar
  exit /b 1
)
set CLASSE=%~1
shift
java -Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8 -cp out %CLASSE% %1 %2 %3 %4 %5 %6 %7
