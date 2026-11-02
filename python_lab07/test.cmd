@echo off
setlocal
cd /d "%~dp0"
set "PYTHONPATH=%~dp0src;%~dp0..\python_lab06\src"
if exist ".venv\Scripts\python.exe" (
    ".venv\Scripts\python.exe" -m pytest -v tests ..\python_lab06\tests
) else (
    py -3 -m pytest -v tests ..\python_lab06\tests
)
exit /b %errorlevel%
