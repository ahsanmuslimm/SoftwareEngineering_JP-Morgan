@echo off
echo.
echo =====================================================
echo   Midas Core - Incentive API Server
echo =====================================================
echo.
echo Starting server on http://localhost:8080
echo Endpoint: POST /incentive
echo.
echo This simulates the external Incentive API required
echo for JP Morgan simulation Tasks 4 and 5.
echo.
echo Press Ctrl+C to stop the server
echo =====================================================
echo.

powershell -ExecutionPolicy Bypass -File "%~dp0incentive-api-server.ps1"