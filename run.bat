@echo off
SETLOCAL ENABLEDELAYEDEXPANSION

REM Save the current directory
set ROOT_DIR=%CD%

REM Handle clean argument
IF "%1"=="clean" (
    echo Cleaning up...
    pushd request-service && call gradlew.bat clean && popd
    pushd user-service && call gradlew.bat clean && popd
    pushd frontend && call gradlew.bat clean && popd
    pushd club-service && call gradlew.bat clean && popd
    pushd project-service && call gradlew.bat clean && popd
    goto :eof
)

REM Start each microservice in the background with a unique window title
start "user-service" /b cmd /c "cd /d %ROOT_DIR%\user-service && call gradlew.bat bootRun"
start "request-service" /b cmd /c "cd /d %ROOT_DIR%\request-service && call gradlew.bat bootRun"
start "club-service" /b cmd /c "cd /d %ROOT_DIR%\club-service && call gradlew.bat bootRun"
start "project-service" /b cmd /c "cd /d %ROOT_DIR%\project-service && call gradlew.bat bootRun"

REM Run frontend in the current window
pushd frontend
call gradlew.bat bootRun
popd

REM Cleanup after frontend stops
:cleanup
echo Killing all microservices...
for %%T in ("user-service","request-service","club-service","project-service") do (
    for /f "tokens=2" %%P in ('tasklist /v /fi "imagename eq java.exe" /fi "windowtitle eq %%~T*" ^| findstr /i java.exe') do (
        taskkill /F /PID %%P >nul 2>&1
    )
)
ENDLOCAL
exit /b
