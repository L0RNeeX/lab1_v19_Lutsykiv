#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
mkdir -p build/classes
javac -encoding UTF-8 --release 21 -d build/classes src/main/java/*.java
exec java -Dfile.encoding=UTF-8 -cp build/classes Lab04 "$@"
