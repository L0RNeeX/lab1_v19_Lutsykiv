#!/bin/sh
# Компіляція і запуск із кореня проєкту; аргументи передаються програмі.
set -eu
cd -- "$(dirname -- "$0")"
mkdir -p build/classes
javac -encoding UTF-8 --release 21 -d build/classes src/main/java/Lab01.java
exec java -Dfile.encoding=UTF-8 -cp build/classes Lab01 "$@"
