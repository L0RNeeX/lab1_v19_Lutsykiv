@echo off
setlocal
set "PYTHONPATH=%~dp0src"
cd /d "%~dp0"
if exist ".venv\Scripts\python.exe" (
    ".venv\Scripts\python.exe" -m pytest -q
) else (
    py -3 -m pytest -q
)
exit /b %errorlevel%
