@echo off
echo Starting Incentive API Server...
echo.
echo This provides the external incentive API required for Tasks 4 and 5
echo Endpoint: POST http://localhost:8080/incentive
echo.

cd /d "%~dp0"

if exist "IncentiveApiApp.class" (
    echo Running compiled version...
    java IncentiveApiApp
) else (
    echo Compiling and running...
    javac IncentiveApiApp.java
    if %errorlevel% equ 0 (
        java IncentiveApiApp
    ) else (
        echo Compilation failed. Please check Java installation.
        pause
    )
)