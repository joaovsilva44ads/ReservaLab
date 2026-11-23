#!/bin/sh
set -eu
cd "$(dirname "$0")"
if [ ! -f lib/sqlite-jdbc-3.54.1.0.jar ] || [ ! -f lib/slf4j-api-2.0.17.jar ] || [ ! -f lib/slf4j-nop-2.0.17.jar ]; then
    echo 'Tentando preparar as bibliotecas JDBC...'
    java ferramentas/PrepararDependencias.java || echo 'Aviso: JDBC nao disponivel. O modo em memoria ainda pode funcionar.'
fi
mkdir -p build/classes
javac --release 17 -encoding UTF-8 -d build/classes src/main/java/br/edu/reservalab/*.java
java -cp 'build/classes:src/main/resources:lib/*' br.edu.reservalab.App
