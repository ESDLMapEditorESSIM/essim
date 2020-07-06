@ECHO OFF
ECHO ============================
ECHO Building Docker image
ECHO ============================
docker build -t 10.30.2.1:5000/essim-engine:latest .
ECHO ============================
ECHO Pushing Docker image
ECHO ============================
docker push 10.30.2.1:5000/essim-engine:latest