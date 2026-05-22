@echo off
setlocal
cd /d "%~dp0"
set "ROOT=%CD%"

echo =========================================
echo EmotionHealing - Start All Backend Services
echo =========================================
echo.
echo Cleaning existing backend processes on service ports...

call :stop_port 5000 Python Backend
call :stop_port 5001 AI Service
call :stop_port 5003 Emotion Recognition
call :stop_port 8080 API Gateway
call :stop_port 8081 Music Service
call :stop_port 8082 User Service
call :stop_port 8083 Social Service
call :stop_port 8084 Data Service
call :stop_port 8085 Meditation Service

echo.
echo Cleaning stale build output...
call :clean_dir "%ROOT%\music-service\target" Music Service target

echo.
echo Generating a fresh JWT secret for this startup...
set "JWT_SECRET=emotion-healing-dev-jwt-secret-%RANDOM%-%RANDOM%-%RANDOM%-%RANDOM%-%RANDOM%-%RANDOM%-%RANDOM%-%RANDOM%"
echo JWT secret prepared.

echo.
echo Starting Python Backend...
start "Python Backend" cmd /k "cd /d ""%ROOT%\backend"" && call pip install -r requirements.txt && python app.py"

echo Starting AI Service...
start "AI Service" cmd /k "cd /d ""%ROOT%\AI-service\backend"" && call pip install -r requirements.txt && set AI_SERVICE_PORT=5001 && python app.py"

echo Starting Emotion Recognition Service...
start "Emotion Recognition" cmd /k "cd /d ""%ROOT%\AI-service\emotion_recog"" && call pip install -r requirements.txt && set PORT=5003 && python app.py"

echo Starting API Gateway...
start "API Gateway" cmd /k "cd /d ""%ROOT%\api-gateway"" && set JWT_SECRET=%JWT_SECRET% && call mvnw.cmd spring-boot:run"

echo Starting User Service...
start "User Service" cmd /k "cd /d ""%ROOT%\user-service"" && set JWT_SECRET=%JWT_SECRET% && call mvnw.cmd spring-boot:run"

echo Starting Music Service...
start "Music Service" cmd /k "cd /d ""%ROOT%\music-service"" && call mvnw.cmd spring-boot:run"

echo Starting Social Service...
start "Social Service" cmd /k "cd /d ""%ROOT%"" && call api-gateway\mvnw.cmd -f social-service\pom.xml spring-boot:run"

echo Starting Meditation Service...
start "Meditation Service" cmd /k "cd /d ""%ROOT%\meditation-service\meditation-service"" && call mvnw.cmd spring-boot:run"

echo Starting Data Service...
start "Data Service" cmd /k "cd /d ""%ROOT%"" && call api-gateway\mvnw.cmd -f data-service\pom.xml spring-boot:run"

echo.
echo All backend services are starting in separate windows.
echo =========================================
pause
goto :eof

:stop_port
set "PORT=%~1"
set "SERVICE_NAME=%~2"
for /f "tokens=5" %%P in ('netstat -ano ^| findstr /r /c:":%PORT% .*LISTENING"') do (
  echo Stopping %SERVICE_NAME% on port %PORT% ^(PID %%P^)...
  taskkill /PID %%P /F >nul 2>&1
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
