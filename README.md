# Pre-requisites

1. Java Development Kit (8+).
2. Maven. Download from [here] (https://maven.apache.org/download.cgi) and follow instructions [here] (https://maven.apache.org/install.html)
3. Docker. (If you want to build a docker image)


# Steps to create a Docker image from sources:

**Step 1)** From the current directory (`open-sourced-essim`), run the following command:
```
mvn clean package
```
This should create `essim.jar` that will be available in the `essim-engine\target\` folder and used in the next step.

**Step 2)** When Step 1 is successfully completed, run the following command to build the docker image from the same directory:
```
docker build -t [optional-repository/]<name-of-image>[:optional-image-version] essim-engine
```
**Step 3)** Done. You may now use this built image in your stack or push it to your favourite repository. <br>

*PS: Please take a look at `essim-engine/Dockerfile` to see what environment variables need to be defined.*