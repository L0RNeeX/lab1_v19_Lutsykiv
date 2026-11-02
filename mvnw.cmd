@echo off
setlocal
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0.mvn\wrapper\Invoke-Maven.ps1" %*
exit /b %errorlevel%
