@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
if not exist "build\classes" mkdir "build\classes"
javac -encoding UTF-8 --release 21 -d build/classes src/main/java/*.java
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp build/classes Lab05 %*
exit /b %errorlevel%
