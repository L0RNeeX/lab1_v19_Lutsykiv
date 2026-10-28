@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
if not exist "build\classes" mkdir "build\classes"
javac -encoding UTF-8 --release 21 -d build/classes src/main/java/Lab01.java
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp build/classes Lab01 %*
exit /b %errorlevel%
