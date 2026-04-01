@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0deploy-debug.ps1" %*
exit /b %ERRORLEVEL%

