echo ============================
echo Building Docker image
echo ============================
COMMIT_SHA=`git rev-parse --short HEAD`
docker build --no-cache -t ci.tno.nl/essim/open-sourced-essim/essim-engine:master -t ci.tno.nl/essim/open-sourced-essim/essim-engine:$COMMIT_SHA .
