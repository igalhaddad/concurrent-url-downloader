@echo off
REM Hopper URL Downloader - Windows Batch Script
REM Usage: download.bat [config-file]
REM Example: download.bat src/main/resources/example-config.json

setlocal

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 21 or higher
    exit /b 1
)

REM Check if JAR file exists
set JAR_FILE=target\url-downloader-0.0.1-SNAPSHOT.jar
if not exist "%JAR_FILE%" (
    echo Error: JAR file not found: %JAR_FILE%
    echo Please run 'mvn clean package' first to build the application
    exit /b 1
)

REM Set default config file if not provided
set CONFIG_FILE=%1
if "%CONFIG_FILE%"=="" (
    set CONFIG_FILE=src\main\resources\example-config.json
)

REM Check if config file exists
if not exist "%CONFIG_FILE%" (
    echo Error: Configuration file not found: %CONFIG_FILE%
    echo Please provide a valid JSON configuration file
    echo Usage: download.bat [config-file]
    exit /b 1
)

echo Starting Hopper URL Downloader...
echo Using configuration: %CONFIG_FILE%
echo.

REM Run the application
java -jar "%JAR_FILE%" download --config "%CONFIG_FILE%"

endlocal
