@echo off
setlocal

REM Build all service images
docker compose build
IF ERRORLEVEL 1 (
  echo Build failed.
  exit /b 1
)

REM Start the 6 containers
docker compose up -d
IF ERRORLEVEL 1 (
  echo Compose up failed.
  exit /b 1
)

echo Services are starting. Use: docker compose ps
endlocal

