@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
if not exist "build\classes" mkdir "build\classes"
if not exist "build\test-classes" mkdir "build\test-classes"
javac -encoding UTF-8 --release 21 -d build/classes src/main/java/*.java
if errorlevel 1 exit /b 1
javac -encoding UTF-8 --release 21 -cp build/classes -d build/test-classes src/test/java/Lab02Checks.java
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp "build/classes;build/test-classes" Lab02Checks
exit /b %errorlevel%
