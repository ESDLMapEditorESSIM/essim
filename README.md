# ESSIM — Energy System Simulator

ESSIM is a modular, JVM-based simulation engine for Energy System Scenario analysis. It consumes ESDL (Energy System Description Language) models and exposes a REST API for running simulations, inspecting results, and converting profiles.

This repository contains:
- `commons`: shared utilities (time/horizon, data structures, common logic)
- `essim-engine`: the REST server and simulation engine

Useful links:
- OpenAPI/Swagger UI (served by the engine): `/openapi/`
- API base path: `/essim`
- ESDL information: https://energytransition.gitbook.io/esdl/esdl-based-tools/essim
- ESDL Model Reference Documentation: https://energytransition.github.io/index.html


## Features
- Scenario-driven simulations (ESDL-based)
  - Import Energy System Description Language (ESDL) models and translate assets, carriers, and connections into the internal model.
  - Discrete time simulation with configurable horizon and time-step (see `commons` module utilities).
  - Balances carrier flows and respects transport constraints across the network.
- Simulation lifecycle via REST API (`/essim`)
  - Create simulations, start execution, check status/progress, retrieve results/artifacts, and delete when finished.
  - Built-in pagination and JSON payloads; errors reported with meaningful HTTP status codes.
- Power grid modeling and AC load-flow
  - Build a Powsybl network from ESDL: substations, voltage levels, lines, transformers.
  - Run load-flow per time step and collect flows, voltages, and thermal loading.
  - Cable/transformer parameterization via `CableSpec` and `TransformerSpec`.
  - Controller: `nl.tno.essim.loadflow.EssimLoadFlowController`.
- KPIs and analytics
  - Pluggable KPI modules compute scenario KPIs (e.g., energy balance, curtailment, losses, grid violations).
  - Managed by `nl.tno.essim.kpi.KPIManager` and exposed via REST.
- Profiles and time-series utilities
  - ESDL profile conversion, resampling and alignment to simulation time steps.
  - Embedding of profiles into ESDL.


## Prerequisites
- Java Development Kit 21+
- Apache Maven 3.9+
- A reachable MongoDB instance
- Optional: Docker, Docker Compose


## Build
From the project root:
```
mvn clean package
```
Artifacts:
- `essim-engine/target/essim.jar` — run this JAR to start the server


## Development
The project is a standard multi-module Maven build and works well in IntelliJ IDEA and Eclipse. Requirements: JDK 21 and Maven 3.9+.

- IntelliJ IDEA (Community or Ultimate):
  - Open the project: File > New > Project from Existing Sources… > select the root `pom.xml`.
  - Set Project SDK to JDK 21: File > Project Structure > Project > SDK.
  - Enable annotation processing (for Lombok): Settings > Build, Execution, Deployment > Compiler > Annotation Processors > check “Enable annotation processing”.
  - Build: use the Maven tool window or run `mvn clean package` in the terminal.
  - Run the server locally:
    - Create an Application run configuration with Main class `nl.tno.essim.ESSimMain`.
    - Set Environment variables (example):
      - `HTTP_SERVER_SCHEME=http`
      - `HTTP_SERVER_HOSTNAME=localhost`
      - `HTTP_SERVER_PORT=8080`
      - `MONGODB_HOST=localhost`
      - `MONGODB_PORT=27017`
    - Start the run configuration and browse `http://localhost:8080/openapi/`.

- Eclipse (with m2e):
  - Import: File > Import… > Maven > Existing Maven Projects, select the project root.
  - Configure JDK 21: Preferences > Java > Installed JREs, add/select a JDK 21 and set it as default.
  - Lombok/annotation processing:
    - Ensure m2e-apt is installed/enabled (Eclipse may prompt to install). Then enable: Preferences > Maven > Annotation Processing > “Automatically configure JDT APT”.
    - If your Eclipse installation requires it, install Lombok by running the `lombok.jar` installer and selecting your Eclipse installation.
  - Run the server: Run > Run Configurations… > Java Application, choose `nl.tno.essim.ESSimMain`, and add the same Environment variables as above.

Tips:
- API base: `http://localhost:8080/essim`
- Swagger UI: `http://localhost:8080/openapi/`
- Quick MongoDB: use the Docker Compose example below to start a local MongoDB for development.


## Run (Docker)
Build the Docker image after packaging the JAR:
```
mvn clean package

docker build -t <repo/essim>:<tag> essim-engine
```
Run the container (requires a MongoDB reachable from the container):
```
docker run --rm \
  -e HTTP_SERVER_SCHEME=http \
  -e HTTP_SERVER_HOSTNAME=0.0.0.0 \
  -e HTTP_SERVER_PORT=8080 \
  -e MONGODB_HOST=<mongodb-host> \
  -e MONGODB_PORT=27017 \
  -p 8080:8080 \
  <repo/essim>:<tag>
```
Then browse to:
- API: `http://localhost:8080/essim`
- Swagger UI: `http://localhost:8080/openapi/`

Tip: See `essim-engine/Dockerfile` for the full list of default environment variables used at runtime.


## Docker Compose (example)
This example uses a MongoDB container and the ESSIM engine:
```yaml
services:
  mongodb:
    image: mongo:7
    ports:
      - "27017:27017"

  essim:
    image: <repo/essim>:<tag>
    depends_on:
      - mongodb
    environment:
      HTTP_SERVER_SCHEME: http
      HTTP_SERVER_HOSTNAME: 0.0.0.0
      HTTP_SERVER_PORT: 8080
      MONGODB_HOST: mongodb
      MONGODB_PORT: 27017
    ports:
      - "8080:8080"
```


## REST API overview
The REST API is implemented under the base path `/essim`. Key groups include:
- Simulations: create, list, query status and KPIs
- Profile conversion: ESDL profile to time-series conversion
- Transport/load-flow and logs retrieval per simulation

Explore and try all endpoints via Swagger UI at `/openapi/`. The API description is generated from annotations in the code (e.g., `nl.tno.essim.rest` package).


## Project structure
- `/commons`: shared classes and utilities
- `/essim-engine`:
  - `src/main/java/nl/tno/essim/rest`: REST resources and server setup (Jetty + Jersey)
  - `src/main/resources/web/swagger-ui`: Swagger UI assets are unpacked here during build
  - `Dockerfile`: runtime image based on `eclipse-temurin:21-jdk`

The main entry point is `nl.tno.essim.ESSimMain`, which starts the HTTP server (`nl.tno.essim.rest.EssimHttpServer`).


## Testing
Run all tests:
```
mvn test
```


## Troubleshooting
- Server does not start, or exits immediately:
  - Ensure all required environment variables are set and valid numbers (e.g., `HTTP_SERVER_PORT`).
  - Check MongoDB connectivity (`MONGODB_HOST`/`MONGODB_PORT`).
- Cannot reach Swagger UI:
  - Confirm the server is running and the port is exposed.
  - Verify you’re browsing `/openapi/` (trailing slash is accepted).
- `java.lang.NoClassDefFoundError` or similar:
  - Make sure you run the packaged JAR from `essim-engine/target/essim.jar` produced by `mvn clean package`.


## Contributing
See `CONTRIBUTE.md` for guidelines.


## License
Licensed under the Apache License, Version 2.0. See `LICENSE` for details.
