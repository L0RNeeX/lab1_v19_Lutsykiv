#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
mkdir -p build/classes build/test-classes
javac -encoding UTF-8 --release 21 -d build/classes src/main/java/*.java
javac -encoding UTF-8 --release 21 -cp build/classes -d build/test-classes src/test/java/Lab02Checks.java
exec java -Dfile.encoding=UTF-8 -cp "build/classes:build/test-classes" Lab02Checks
