@echo off
setlocal
set "PYTHONPATH=%~dp0src"
cd /d "%~dp0"
if exist ".venv\Scripts\python.exe" (
    ".venv\Scripts\python.exe" -m kzp_lab07.cli data\input.csv reports\summary.txt %*
) else (
    py -3 -m kzp_lab07.cli data\input.csv reports\summary.txt %*
)
exit /b %errorlevel%
