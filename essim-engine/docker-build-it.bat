@ECHO OFF
ECHO ============================
ECHO Building JAR
ECHO ============================
call mvn clean package -DskipTest
ECHO ============================
ECHO Building Docker image
ECHO ============================
for /f "delims=" %%i in ('git rev-parse --short HEAD') do set COMMIT_SHA=%%i
ECHO %COMMIT_SHA% > version.txt
docker build --no-cache -t ci.tno.nl/essim/open-sourced-essim/essim-engine:master -t ci.tno.nl/essim/open-sourced-essim/essim-engine:%COMMIT_SHA% .