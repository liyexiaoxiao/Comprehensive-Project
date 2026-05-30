@echo off
setlocal EnableExtensions EnableDelayedExpansion
cd /d "%~dp0"
set "ROOT=%CD%"
set "BOOT_TIMEOUT_SECONDS=180"
set "OPTIONAL_TIMEOUT_SECONDS=20"

echo =========================================
echo EmotionHealing - Start All Backend Services
echo =========================================
echo.

call :load_supported_env "%ROOT%\.env"
call :prepare_jwt_secret

echo Cleaning existing backend processes...
call :stop_service "Python Backend" 5000
call :stop_service "AI Service" 5001
call :stop_service "Emotion Recognition" 5003
call :stop_service "API Gateway" 8080
call :stop_service "Music Service" 8081
call :stop_service "User Service" 8082
call :stop_service "Social Service" 8083
call :stop_service "Data Service" 8084
call :stop_service "Meditation Service" 8085

echo.
echo Cleaning stale build output...
call :clean_dir "%ROOT%\music-service\target" "Music Service target"

echo.
echo Starting services one by one and waiting for ports...
echo.

call :start_and_wait "Python Backend" 5000 "cd /d ""%ROOT%\backend"" && set JWT_SECRET=%JWT_SECRET% && python app.py"
call :start_and_wait "AI Service" 5001 "cd /d ""%ROOT%\AI-service\backend"" && set JWT_SECRET=%JWT_SECRET% && set AI_SERVICE_PORT=5001 && call pip install -r requirements.txt && python app.py"
call :start_and_wait "User Service" 8082 "cd /d ""%ROOT%\user-service"" && set JWT_SECRET=%JWT_SECRET% && call mvnw.cmd spring-boot:run"
call :start_and_wait "Music Service" 8081 "cd /d ""%ROOT%\music-service"" && call mvnw.cmd spring-boot:run"
call :start_and_wait "Social Service" 8083 "cd /d ""%ROOT%"" && call api-gateway\mvnw.cmd -f social-service\pom.xml spring-boot:run"
call :start_and_wait "Data Service" 8084 "cd /d ""%ROOT%"" && call api-gateway\mvnw.cmd -f data-service\pom.xml spring-boot:run"
call :start_and_wait "Meditation Service" 8085 "cd /d ""%ROOT%\meditation-service\meditation-service"" && call mvnw.cmd spring-boot:run"
call :start_and_wait "API Gateway" 8080 "cd /d ""%ROOT%\api-gateway"" && set JWT_SECRET=%JWT_SECRET% && call mvnw.cmd spring-boot:run"
call :start_optional_service "Emotion Recognition" 5003 "cd /d ""%ROOT%\AI-service\emotion_recog"" && set PORT=5003 && call pip install -r requirements.txt && python app.py"

echo.
echo Startup sequence finished. Verify any window that reported a timeout.
echo =========================================
pause
goto :eof

:load_supported_env
if not exist "%~1" (
  echo No .env file found. Using current shell environment only.
  goto :eof
)

echo Loading supported environment variables from .env ...
for /f "usebackq tokens=* delims=" %%L in ("%~1") do (
  set "LINE=%%L"
  if defined LINE if not "!LINE:~0,1!"=="#" call :set_supported_env_line "%%L"
)
echo Environment variables loaded.
goto :eof

:set_supported_env_line
for /f "tokens=1* delims==" %%A in ("%~1") do (
  if /i "%%~A"=="JWT_SECRET" set "%%~A=%%~B"
  if /i "%%~A"=="DASHSCOPE_API_KEY" set "%%~A=%%~B"
  if /i "%%~A"=="DASHSCOPE_BASE_URL" set "%%~A=%%~B"
  if /i "%%~A"=="DASHSCOPE_CAPTION_MODEL" set "%%~A=%%~B"
  if /i "%%~A"=="DASHSCOPE_MAX_AUDIO_BYTES" set "%%~A=%%~B"
  if /i "%%~A"=="AI_SERVICE_BASE_URLS" set "%%~A=%%~B"
)
goto :eof

:prepare_jwt_secret
if defined JWT_SECRET (
  if not "%JWT_SECRET:~31,1%"=="" (
    echo Using JWT secret from environment or .env.
    goto :eof
  )
  echo Configured JWT secret is shorter than 32 characters and is not secure enough for HS256.
  echo Falling back to a generated startup secret.
) else (
  echo No JWT secret configured. Generating a fresh JWT secret for this startup...
)
set "JWT_SECRET=emotion-healing-dev-jwt-secret-%RANDOM%-%RANDOM%-%RANDOM%-%RANDOM%-%RANDOM%-%RANDOM%-%RANDOM%-%RANDOM%"
goto :eof

:start_and_wait
set "SERVICE_NAME=%~1"
set "SERVICE_PORT=%~2"
set "SERVICE_COMMAND=%~3"
echo Starting %SERVICE_NAME% on port %SERVICE_PORT%...
start "%SERVICE_NAME%" cmd /k "%SERVICE_COMMAND%"
call :wait_for_port "%SERVICE_PORT%" "%SERVICE_NAME%" "%BOOT_TIMEOUT_SECONDS%"
goto :eof

:start_optional_service
set "SERVICE_NAME=%~1"
set "SERVICE_PORT=%~2"
set "SERVICE_COMMAND=%~3"
echo Starting optional service %SERVICE_NAME% on port %SERVICE_PORT%...
start "%SERVICE_NAME%" cmd /k "%SERVICE_COMMAND%"
call :wait_for_port "%SERVICE_PORT%" "%SERVICE_NAME%" "%OPTIONAL_TIMEOUT_SECONDS%"
goto :eof

:wait_for_port
set "WAIT_PORT=%~1"
set "WAIT_SERVICE=%~2"
set "WAIT_TIMEOUT=%~3"
set /a WAIT_ELAPSED=0

:wait_loop
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r /c:":%WAIT_PORT% .*LISTENING"') do (
  echo [OK] %WAIT_SERVICE% is listening on port %WAIT_PORT% ^(PID %%P^).
  goto :eof
)

if %WAIT_ELAPSED% GEQ %WAIT_TIMEOUT% (
  echo [WARN] %WAIT_SERVICE% did not open port %WAIT_PORT% within %WAIT_TIMEOUT% seconds.
  echo        Please inspect the "%WAIT_SERVICE%" window for startup errors.
  echo        Startup will continue for the remaining services.
  goto :eof
)

set /a WAIT_ELAPSED+=1
timeout /t 1 /nobreak >nul
goto wait_loop

:stop_service
set "SERVICE_NAME=%~1"
set "SERVICE_PORT=%~2"

echo Stopping %SERVICE_NAME%...
taskkill /FI "WINDOWTITLE eq %SERVICE_NAME%" /T /F >nul 2>&1
call :kill_port "%SERVICE_PORT%"
timeout /t 1 /nobreak >nul
goto :eof

:kill_port
set "TARGET_PORT=%~1"
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r /c:":%TARGET_PORT% .*LISTENING"') do (
  echo   Killing PID %%P on port %TARGET_PORT%...
  taskkill /PID %%P /T /F >nul 2>&1
)
goto :eof

:clean_dir
set "TARGET_DIR=%~1"
set "TARGET_NAME=%~2"
if exist "%TARGET_DIR%" (
  echo Removing %TARGET_NAME%...
  rmdir /s /q "%TARGET_DIR%"
)
goto :eof
