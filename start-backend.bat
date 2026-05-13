@echo off
echo =========================================
echo EmotionHealing - Start All Backend Services
echo =========================================

echo Starting Python Backend...
start "Python Backend" cmd /k "cd backend && call pip install -r requirements.txt && python app.py"

echo Starting API Gateway...
start "API Gateway" cmd /k "cd api-gateway && mvnw spring-boot:run"

echo Starting User Service...
start "User Service" cmd /k "cd user-service && mvnw spring-boot:run"

echo Starting Music Service...
start "Music Service" cmd /k "cd music-service && mvnw spring-boot:run"

echo Starting Social Service...
start "Social Service" cmd /k "cd social-service && mvnw spring-boot:run"

echo Starting Meditation Service...
start "Meditation Service" cmd /k "cd meditation-service\meditation-service && mvnw spring-boot:run"

echo Starting Data Service...
start "Data Service" cmd /k "cd data-service && mvnw spring-boot:run"

echo.
echo All backend services are starting in separate windows.
echo =========================================
pause