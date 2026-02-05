# Using ESSIM with PowSyBl

## Overview

ESSIM integrates with [PowSyBl (Power System Blocks)](https://www.powsybl.org/) to perform AC load-flow calculations on electricity networks defined in ESDL (Energy System Description Language). This integration enables detailed power grid analysis including voltage levels, power flows, reactive power, and thermal loading of network components.

## What is PowSyBl?

PowSyBl is an open-source framework for power system modeling and simulation. ESSIM uses PowSyBl to:
- Model electrical networks with substations, voltage levels, buses, lines, and transformers
- Run AC load-flow calculations using the OpenLoadFlow solver
- Compute power flows, voltages, and currents for each timestep
- Validate network constraints and identify potential violations

## Enabling PowSyBl Load Flow

### Via Java API

To enable PowSyBl load-flow in your simulation, set the `powsyblLoadFlow` flag to `true` in your simulation configuration:

```java
EssimSimulation sim = new EssimSimulation();
sim.setPowsyblLoadFlow(true);
```

### Via REST API

When submitting a simulation via the REST API, include `"powsyblLoadFlow": true` in your JSON payload:

```bash
curl -X POST http://localhost:8080/essim/simulation \
  -H "Content-Type: application/json" \
  -d '{
    "esdlContents": "<base64-encoded-esdl>",
    "startDate": "2019-01-01T00:00:00.000Z",
    "endDate": "2019-01-02T00:00:00.000Z",
    "scenarioID": "my-loadflow-scenario",
    "powsyblLoadFlow": true,
    "postgresURL": "postgresql://localhost:5432/essim"
  }'
```

**Note**: The `postgresURL` should be in the format `postgresql://host:port/database` (without the `jdbc:` prefix). ESSIM adds the `jdbc:` prefix automatically.

The API will return a simulation ID that you can use to monitor progress and retrieve results.

## Architecture

### Core Components

The PowSyBl integration is implemented in the `nl.tno.essim.loadflow` package:

- **`EssimLoadFlowController`** (`essim-engine/src/main/java/nl/tno/essim/loadflow/EssimLoadFlowController.java`): Main controller that translates ESDL models to PowSyBl networks and runs load-flow calculations
- **`CableSpec`** (`essim-engine/src/main/java/nl/tno/essim/loadflow/CableSpec.java`): Cable specifications including resistance, reactance, capacitance, and current limits
- **`TransformerSpec`** (`essim-engine/src/main/java/nl/tno/essim/loadflow/TransformerSpec.java`): Transformer specifications including rated voltages, impedances, and losses

### Network Creation Process

The controller creates a PowSyBl network from ESDL in the following steps:

1. **Substation Grouping**: Assets connected without cables between them are grouped into substations
2. **Voltage Levels**: Each electricity carrier (voltage level) gets its own voltage level within substations
3. **Bus Creation**: Buses are created for each voltage level to connect assets
4. **Asset Mapping**:
   - `Producer` → PowSyBl `Generator`
   - `Consumer` → PowSyBl `Generator` (with negative power for flexibility)
   - `Storage` → PowSyBl `Battery`
   - `Conversion` → PowSyBl `Generator`
   - `ElectricityCable` → PowSyBl `Line`
   - `Transformer` → PowSyBl `TwoWindingsTransformer`

## ESDL Requirements

### Electricity Commodities

All electricity carriers must be defined as `ElectricityCommodity` with voltage specified:

```xml
<carrier xsi:type="esdl:ElectricityCommodity" id="elec-110kV" name="Electricity 110kV">
    <voltage>110000.0</voltage>
</carrier>
```

### Cables

Electricity cables must:
- Have exactly 2 ports
- Specify a cable type via `assetType` attribute (see Cable Types below)
- Include length in meters

```xml
<asset xsi:type="esdl:ElectricityCable" id="cable1" name="Cable 1" assetType="243-AL1/39-ST1A 20.0" length="1000.0">
    <port xsi:type="esdl:InPort" id="in" carrier="elec-20kV"/>
    <port xsi:type="esdl:OutPort" id="out" carrier="elec-20kV"/>
</asset>
```

### Transformers

Transformers must:
- Have exactly 2 ports at different voltage levels
- Specify a transformer type via `assetType` attribute (see Transformer Types below)

```xml
<asset xsi:type="esdl:Transformer" id="trafo1" name="Transformer 1" assetType="63 MVA 110/20 kV">
    <port xsi:type="esdl:InPort" id="in" carrier="elec-110kV"/>
    <port xsi:type="esdl:OutPort" id="out" carrier="elec-20kV"/>
</asset>
```

### Producers, Consumers, and Storage

- **Producers**: Must specify `power` capacity (in Watts). Cannot have more than 1 port.
- **Consumers**: Cannot have more than 1 port.
- **Storage**: Must specify `maxChargeRate` and `maxDischargeRate` (in Watts). Cannot have more than 1 port.

**Note on Conversion Assets**: Conversion assets (like heat pumps or electrolyzers) CAN have multiple ports at different voltage levels or for different carriers. Only the port matching the electricity carrier being solved is used in the load-flow calculation (see `EssimLoadFlowController.java:493`).

## Cable Types

ESSIM includes specifications for standard cables from pandapower. Available cable types:

### Low Voltage (0.4 kV)
- `NAYY 4x50 SE`
- `NAYY 4x120 SE`
- `NAYY 4x150 SE`

### Medium Voltage (6-20 kV)
- `NA2XS2Y 1x70 RM/25 6/10 kV`
- `NA2XS2Y 1x95 RM/25 6/10 kV`
- `NA2XS2Y 1x120 RM/25 6/10 kV`
- `NA2XS2Y 1x150 RM/25 6/10 kV`
- `NA2XS2Y 1x185 RM/25 6/10 kV`
- `NA2XS2Y 1x240 RM/25 6/10 kV`
- `NA2XS2Y 1x70 RM/25 12/20 kV`
- `NA2XS2Y 1x95 RM/25 12/20 kV`
- `NA2XS2Y 1x120 RM/25 12/20 kV`
- `NA2XS2Y 1x150 RM/25 12/20 kV`
- `NA2XS2Y 1x185 RM/25 12/20 kV`
- `NA2XS2Y 1x240 RM/25 12/20 kV`

### High Voltage (110-380 kV)
- `48-AL1/8-ST1A 110.0`
- `94-AL1/15-ST1A 110.0`
- `122-AL1/20-ST1A 110.0`
- `149-AL1/24-ST1A 110.0`
- `184-AL1/30-ST1A 110.0`
- `243-AL1/39-ST1A 110.0` (default if no type specified)
- `305-AL1/39-ST1A 110.0`
- `490-AL1/64-ST1A 110.0`
- `679-AL1/86-ST1A 110.0`
- `N2XS(FL)2Y 1x120 RM/35 64/110 kV`
- `N2XS(FL)2Y 1x185 RM/35 64/110 kV`
- `N2XS(FL)2Y 1x240 RM/35 64/110 kV`
- `N2XS(FL)2Y 1x300 RM/35 64/110 kV`

And more... (see `CableSpec.java` for complete list)

## Transformer Types

Available transformer types:

### High Voltage Transformers
- `160 MVA 380/110 kV` (default if no type specified)
- `100 MVA 220/110 kV`

### Medium Voltage Transformers
- `63 MVA 110/20 kV`
- `40 MVA 110/20 kV`
- `25 MVA 110/20 kV`
- `63 MVA 110/10 kV`
- `40 MVA 110/10 kV`
- `25 MVA 110/10 kV`

### Low Voltage Transformers
- `0.25 MVA 20/0.4 kV`
- `0.4 MVA 20/0.4 kV`
- `0.63 MVA 20/0.4 kV`
- `0.25 MVA 10/0.4 kV`
- `0.4 MVA 10/0.4 kV`
- `0.63 MVA 10/0.4 kV`

See `TransformerSpec.java` for electrical parameters (impedances, losses, tap settings).

## Load Flow Solver Configuration

ESSIM uses PowSyBl's OpenLoadFlow solver with the following configuration (see `EssimLoadFlowController.java:651`):

- **Solver**: Newton-Raphson AC solver
- **Reactive limits**: Disabled
- **Distributed slack**: Enabled
- **Slack bus selection**: Most meshed bus
- **Maximum iterations**: 100

## Output and Results

### Observations

Load-flow calculations produce observations with the following fields (defined in `ObservationField`):

- **`LOADFLOW_POWER`**: Active power flow (W)
- **`LOADFLOW_ENERGY`**: Energy transferred (Wh)
- **`LOADFLOW_REACTIVE_POWER`**: Reactive power flow (VAr)
- **`LOADFLOW_CURRENT`**: Current (A) - for lines only

### Network Export

The controller exports the PowSyBl network to:
- **XML file**: `diagrams/essim-network-{simulationId}.xml` in system temp directory
- **Network diagram**: `diagrams/essim-network-{simulationId}-diagram.svg`
- **Single-line diagram**: `diagrams/essim-network-{simulationId}-single-line-diagram.svg`

## Dependencies

The following PowSyBl dependencies are required (from `essim-engine/pom.xml`):

```xml
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-config-classic</artifactId>
</dependency>
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-iidm-impl</artifactId>
</dependency>
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-ieee-cdf-converter</artifactId>
</dependency>
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-open-loadflow</artifactId>
</dependency>
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-single-line-diagram-core</artifactId>
</dependency>
<dependency>
    <groupId>com.powsybl</groupId>
    <artifactId>powsybl-network-area-diagram</artifactId>
</dependency>
```

## Example Usage

### Java API Example

See test examples in `essim-engine/src/test/java/nl/tno/essim/tests/LoadflowTest.java`:

```java
EssimSimulation sim = new EssimSimulation();
sim.setEsdlContents(esdl);
sim.setPowsyblLoadFlow(true);
sim.setStartDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 1, 0, 0, 0)));
sim.setEndDate(EssimTime.localDateTimeToDate(LocalDateTime.of(2019, 1, 2, 0, 0, 0)));

IMongoBackend mongo = new MongoBackendStub();
String simId = mongo.addSimulation(sim);
ESSimEngine essimEngine = new ESSimEngine("local-test", simId, sim, sim.getEsdlContents(), mongo);
essimEngine.startSimulation();
```

### REST API Workflow

The REST API provides a complete workflow for running simulations with PowSyBl load-flow:

#### 1. Submit a Simulation

```bash
curl -X POST http://localhost:8080/essim/simulation \
  -H "Content-Type: application/json" \
  -d '{
    "esdlContents": "<base64-encoded-esdl>",
    "startDate": "2019-01-01T00:00:00.000Z",
    "endDate": "2019-01-02T00:00:00.000Z",
    "scenarioID": "my-scenario",
    "powsyblLoadFlow": true,
    "influxURL": "http://localhost:8086",
    "postgresURL": "postgresql://localhost:5432/essim"
  }'
```

**Note**: The `postgresURL` format is `postgresql://host:port/database` (without `jdbc:` prefix).

Response:
```json
{
  "status": "CREATED",
  "id": "abc123-simulation-id"
}
```

#### 2. Monitor Simulation Status

```bash
curl http://localhost:8080/essim/simulation/abc123-simulation-id/status
```

Response:
```json
{
  "State": "COMPLETE",
  "Description": "Simulation completed successfully"
}
```

Possible states: `CREATED`, `RUNNING`, `COMPLETE`, `ERROR`

#### 3. Retrieve Simulation Results

Get the full simulation details:
```bash
curl http://localhost:8080/essim/simulation/abc123-simulation-id
```

Get transport network information (includes load-flow results):
```bash
curl http://localhost:8080/essim/simulation/abc123-simulation-id/transport
```

Get simulation logs:
```bash
curl http://localhost:8080/essim/simulation/abc123-simulation-id/logs
```

Get ESDL with embedded result profiles:
```bash
curl http://localhost:8080/essim/simulation/abc123-simulation-id/ESDLWithProfiles
```

#### 4. Retrieve KPI Results

List available KPIs:
```bash
curl http://localhost:8080/essim/simulation/abc123-simulation-id/kpi
```

Get specific KPI result:
```bash
curl http://localhost:8080/essim/simulation/abc123-simulation-id/kpi/{kpiId}
```

#### 5. Delete Simulation

```bash
curl -X DELETE http://localhost:8080/essim/simulation/abc123-simulation-id
```

### API Reference

All endpoints are documented in the **Swagger UI** available at:
```
http://localhost:8080/openapi/
```

The main simulation endpoints (`nl.tno.essim.rest.SimulationService`):
- `POST /essim/simulation` - Start a new simulation
- `GET /essim/simulation` - List all simulations
- `GET /essim/simulation/list` - Get simulation IDs only
- `GET /essim/simulation/{id}` - Get simulation details
- `GET /essim/simulation/{id}/status` - Get simulation status
- `GET /essim/simulation/{id}/transport` - Get transport networks (load-flow results)
- `GET /essim/simulation/{id}/kpi` - Get KPIs for simulation
- `GET /essim/simulation/{id}/logs` - Get simulation logs
- `DELETE /essim/simulation/{id}` - Delete simulation and data

## Limitations

- **Electricity only**: Only electricity networks are supported (carriers must be `ElectricityCommodity`)
- **Port restrictions for basic assets**: Producers, consumers, and storage cannot have more than 1 port. Having 2 or more ports will cause an error.
- **Multi-port support for conversions**: Conversion assets (e.g., heat pumps, electrolyzers) can have multiple ports, but only the port matching the electricity carrier being solved is used in load-flow
- **Consumer modeling**: Consumers are modeled as generators with negative power to allow flexibility in load-flow calculations
- **AC load-flow only**: Only AC load-flow is supported (no DC power flow)
- **Two-port requirement**: Cables and transformers must have exactly 2 ports

## Further Information

- PowSyBl documentation: https://www.powsybl.org/
- OpenLoadFlow solver: https://www.powsybl.org/pages/documentation/simulation/loadflow/
- ESDL documentation: https://energytransition.gitbook.io/esdl/
