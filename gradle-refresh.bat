@echo off
SETLOCAL ENABLEDELAYEDEXPANSION

REM Save the current directory
set ROOT_DIR=%CD%

echo ====================================
echo Refreshing Gradle for all projects...
echo ====================================

pushd "%ROOT_DIR%\user-service"
call gradlew.bat --refresh-dependencies
popd

pushd "%ROOT_DIR%\request-service"
call gradlew.bat --refresh-dependencies
popd

pushd "%ROOT_DIR%\club-service"
call gradlew.bat --refresh-dependencies
popd

pushd "%ROOT_DIR%\frontend"
call gradlew.bat --refresh-dependencies
popd

echo ====================================
echo ✅ Gradle refresh complete for all projects
echo ====================================

ENDLOCAL
exit /b
