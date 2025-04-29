@echo off
setlocal enabledelayedexpansion

REM Check if .env file exists
if not exist .env (
    echo Error: .env file not found!
    echo Please create a .env file with your Spotify credentials.
    echo You can use the example.env as a template.
    exit /b 1
)

REM Load environment variables from .env file
for /f "tokens=*" %%a in (.env) do (
    set line=%%a
    if not "!line:~0,1!"=="#" (
        set !line!
    )
)

REM Verify Spotify credentials are set
if "%SPOTIFY_CLIENT_ID%"=="" (
    echo Error: SPOTIFY_CLIENT_ID is missing in .env file!
    exit /b 1
)

if "%SPOTIFY_CLIENT_SECRET%"=="" (
    echo Error: SPOTIFY_CLIENT_SECRET is missing in .env file!
    exit /b 1
)

echo Starting Lavalink with YouTubeify plugin...
echo Spotify credentials loaded from .env file.

REM Start Lavalink with environment variables available
java -jar Lavalink.jar