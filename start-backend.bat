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
start "" /b cmd /c "call :stop_service_no_pause ""Python Backend"" 5000 >nul 2>&1"
start "" /b cmd /c "call :stop_service_no_pause ""AI Service"" 5001 >nul 2>&1"
start "" /b cmd /c "call :stop_service_no_pause ""Emotion Recognition"" 5003 >nul 2>&1"
start "" /b cmd /c "call :stop_service_no_pause ""API Gateway"" 8080 >nul 2>&1"
start "" /b cmd /c "call :stop_service_no_pause ""Music Service"" 8081 >nul 2>&1"
start "" /b cmd /c "call :stop_service_no_pause ""User Service"" 8082 >nul 2>&1"
start "" /b cmd /c "call :stop_service_no_pause ""Social Service"" 8083 >nul 2>&1"
start "" /b cmd /c "call :stop_service_no_pause ""Data Service"" 8084 >nul 2>&1"
start "" /b cmd /c "call :stop_service_no_pause ""Meditation Service"" 8085 >nul 2>&1"
timeout /t 2 /nobreak >nul

echo Cleaning stale build output...
call :clean_dir "%ROOT%\music-service\target" "Music Service target"

echo.
echo =========================================
echo Launching ALL services in parallel...
echo =========================================
echo.

:: ---------- Phase 1: fire all services simultaneously ----------

start "Python Backend" cmd /c "cd /d "%ROOT%\backend" && set JWT_SECRET=%JWT_SECRET% && python app.py"
start "AI Service" cmd /c "cd /d "%ROOT%\AI-service\backend" && set JWT_SECRET=%JWT_SECRET% && set AI_SERVICE_PORT=5001 && pip install -r requirements.txt >nul 2>&1 && python app.py"
start "User Service" cmd /c "cd /d "%ROOT%\user-service" && set JWT_SECRET=%JWT_SECRET% && call mvnw.cmd spring-boot:run"
start "Music Service" cmd /c "cd /d "%ROOT%\music-service" && call mvnw.cmd spring-boot:run"
start "Social Service" cmd /c "cd /d "%ROOT%" && call api-gateway\mvnw.cmd -f social-service\pom.xml spring-boot:run"
start "Data Service" cmd /c "cd /d "%ROOT%" && call api-gateway\mvnw.cmd -f data-service\pom.xml spring-boot:run"
start "Meditation Service" cmd /c "cd /d "%ROOT%\meditation-service\meditation-service" && call mvnw.cmd spring-boot:run"
start "API Gateway" cmd /c "cd /d "%ROOT%\api-gateway" && set JWT_SECRET=%JWT_SECRET% && call mvnw.cmd spring-boot:run"
start "Emotion Recognition" cmd /c "cd /d "%ROOT%\AI-service\emotion_recog" && set PORT=5003 && pip install -r requirements.txt >nul 2>&1 && python app.py"

echo All services launched. Waiting for ports...
echo.

:: ---------- Phase 2: wait for all ports (they boot in parallel, so this is fast) ----------

call :wait_for_port 5000 "Python Backend" "%BOOT_TIMEOUT_SECONDS%"
call :wait_for_port 5001 "AI Service" "%BOOT_TIMEOUT_SECONDS%"
call :wait_for_port 8082 "User Service" "%BOOT_TIMEOUT_SECONDS%"
call :wait_for_port 8081 "Music Service" "%BOOT_TIMEOUT_SECONDS%"
call :wait_for_port 8083 "Social Service" "%BOOT_TIMEOUT_SECONDS%"
call :wait_for_port 8084 "Data Service" "%BOOT_TIMEOUT_SECONDS%"
call :wait_for_port 8085 "Meditation Service" "%BOOT_TIMEOUT_SECONDS%"
call :wait_for_port 8080 "API Gateway" "%BOOT_TIMEOUT_SECONDS%"
call :wait_for_port 5003 "Emotion Recognition" "%OPTIONAL_TIMEOUT_SECONDS%"

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
  goto :eof
)

set /a WAIT_ELAPSED+=1
timeout /t 1 /nobreak >nul
goto wait_loop

:stop_service_no_pause
set "SERVICE_NAME=%~1"
set "SERVICE_PORT=%~2"
taskkill /FI "WINDOWTITLE eq %SERVICE_NAME%" /T /F >nul 2>&1
call :kill_port "%SERVICE_PORT%"
goto :eof

:kill_port
set "TARGET_PORT=%~1"
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r /c:":%TARGET_PORT% .*LISTENING"') do (
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
