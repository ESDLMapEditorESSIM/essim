# Working with External Models

ESSIM supports integrating external models for two main purposes:
1.  **Asset Logic**: Replacing the default internal logic of an ESDL `EnergyAsset` with an external model.
2.  **KPI Calculation**: Using external microservices to calculate Key Performance Indicators (KPIs) based on simulation results (.

---

## MSO-based External Models

External models for assets are managed via a Model Service Orchestration (MSO) controller. This allows ESSIM to trigger the deployment of external models (typically in Docker containers) and synchronize with them during the simulation steps.

### Architecture

The interaction follows this flow:
1.  **Deployment**: ESSIM sends a `DeployModels` message to the MSO controller via MQTT.
2.  **Lifecycle**: The MSO controller starts the external models and ESSIM waits for a `ModelsReady` signal.
3.  **Simulation Loop**: For each time step, ESSIM communicates with the external model via MQTT to exchange bid curves and energy allocations.
4.  **Termination**: When the simulation finishes, ESSIM sends a `SimulationDone` message to the MSO controller to tear down the models.

### Configuration

#### ESSIM Simulation JSON
To enable MSO, include the `mso` and `nodeConfig` sections in your simulation configuration:

```json
{
  "mso": {
    "msoID": "my-mso-controller",
    "mqttHost": "mqtt-broker.example.com",
    "mqttPort": 1883
  },
  "nodeConfig": [
    {
      "esdlNodeId": "building_123",
      "mqttHost": "mqtt-broker.example.com",
      "mqttPort": 1883,
      "mqttTopic": "essim/external_model/building_123",
      "config": {
        "parameter_1": 42
      }
    }
  ]
}
```

-   **`mso`**: Configures the connection to the MSO controller.
-   **`nodeConfig`**: Specifies which ESDL assets should use external logic. The `esdlNodeId` must match the ID of an `EnergyAsset` in the ESDL model.

### Communication Protocol

#### Lifecycle (Protobuf)
Lifecycle messages use Protobuf and are exchanged on the following topics:
-   **Publish (ESSIM → MSO)**:
    -   `/lifecycle/essim/mso/{simulationId}/DeployModels`: Requests deployment of models.
    -   `/lifecycle/essim/mso/{simulationId}/SimulationDone`: Signal that simulation is finished.
-   **Subscribe (MSO → ESSIM)**:
    -   `/lifecycle/mso/essim/{simulationId}/ModelsReady`: Signal that all models are started and ready.

#### Simulation Steps (JSON / Binary)
During the simulation, ESSIM communicates with the `RemoteLogicNode` via the configured `mqttTopic`:

1.  **Initial Configuration (JSON)**:
    Topic: `{mqttTopic}/node/{nodeId}/config`
    Payload: Includes ESDL snippet and custom `config` from `nodeConfig`.

2.  **Bid Curve Request (JSON)**:
    Topic: `{mqttTopic}/node/{nodeId}/createBid`
    Payload: `{"timeStamp": 12345678, "timeStepInSeconds": 3600, "minPrice": 0, "maxPrice": 1, ...}`

3.  **Bid Curve Response (Binary)**:
    ESSIM subscribes to: `.../simulation/{nodeId}/{carrierId}/bid`
    Payload format:
    -   `timestep` (8-byte long)
    -   Sequence of `(price, energy)` pairs (8-byte double each).

4.  **Energy Allocation (JSON)**:
    Topic: `{mqttTopic}/node/{nodeId}/allocate`
    Payload: `{"timeStamp": 12345678, "price": 0.5, "carrierId": "electricity"}`

---
