@echo off
cd /d "%~dp0"
where py >nul 2>nul
if errorlevel 1 goto python
py -3 runner.py
goto end
:python
python runner.py
:end
pause
