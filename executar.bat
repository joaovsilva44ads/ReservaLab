@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>&1
if errorlevel 1 (
  echo ERRO: Java nao encontrado. Instale JDK 17 ou superior.
  pause
  exit /b 1
)
where javac >nul 2>&1
if errorlevel 1 (
  echo ERRO: javac nao encontrado. Instale o JDK completo, nao somente o JRE.
  pause
  exit /b 1
)
set "PRECISA_JDBC=0"
if not exist "lib\sqlite-jdbc-3.54.1.0.jar" set "PRECISA_JDBC=1"
if not exist "lib\slf4j-api-2.0.17.jar" set "PRECISA_JDBC=1"
if not exist "lib\slf4j-nop-2.0.17.jar" set "PRECISA_JDBC=1"
if "%PRECISA_JDBC%"=="1" (
  echo Preparando as bibliotecas JDBC para uso posterior...
  java "ferramentas\PrepararDependencias.java"
  if errorlevel 1 echo Aviso: nao foi possivel baixar JDBC. O modo em memoria ainda pode funcionar.
)
if not exist "build\classes" mkdir "build\classes"
set "SRC=src\main\java\br\edu\reservalab"
javac --release 17 -encoding UTF-8 -d "build\classes" "%SRC%\App.java" "%SRC%\Equipamento.java" "%SRC%\RegraException.java" "%SRC%\RepositorioMemoria.java" "%SRC%\RepositorioReservas.java" "%SRC%\RepositorioSqlite.java" "%SRC%\Reserva.java"
if errorlevel 1 (
  echo Falha na compilacao.
  pause
  exit /b 1
)
echo Abrindo http://localhost:8777
java -cp "build\classes;src\main\resources;lib\*" br.edu.reservalab.App
if errorlevel 1 (
  echo O servidor encerrou com um erro. Confira as mensagens acima.
  pause
  exit /b 1
)
