# Converting ESDL external profiles to embedded time series profiles

This guide explains how to convert an ESDL that contains `ExternalProfile` entries into an ESDL where those profiles are embedded as `TimeSeriesProfile` entries. You can do this in two ways:

- Using the ESSIM web service API
- Directly in code (Java), using the utilities provided by ESSIM

After conversion, the resulting ESDL contains complete time-series values per port for the requested period and time step, so it can be processed without access to the original external data source.

---

## 1) Using the web service API

- Endpoint: `POST /simulation/convert`
- Consumes: `application/json`
- Produces: `application/json`

### Request body schema
The API expects the payload that maps to `nl.tno.essim.model.ESDLProfileConversion`:

- `esdlContents` (string, required): The ESDL contents, Base64-encoded.
- `startDate` (string, required): Simulation start timestamp in ISO‑8601 with milliseconds and timezone. Pattern: `yyyy-MM-dd'T'HH:mm:ss.SSSXXX` (e.g. `2026-01-01T00:00:00.000Z` or `2026-01-01T01:00:00.000+01:00`).
- `endDate` (string, required): Simulation end timestamp, same format as above.
- `timeStep` (string, optional): Time step as `<amount><unit>` where unit ∈ `{s, m, h, d, M, y}`. Examples: `15m`, `1h`, `30s`, `1d`, `1M`, `1y`. If omitted or unsupported, defaults to `1h`.

Notes:
- Dates are inclusive of `startDate` and iterate in steps of `timeStep` until before `endDate`.
- The server reads each external profile over each time slice and writes values into an embedded `TimeSeriesProfile` with the provided/interpolated units.

### Response (200)
```
{
  "esdl": "<Base64-encoded ESDL contents>"
}
```
On error, the API returns HTTP 400 with an `ErrorStatus` payload.

### cURL example
```bash
# Encode your input ESDL file once
ESDL_B64=$(base64 -w0 ./input.esdl)

# Submit the conversion request
curl -sS -X POST \
  http://<essim-host>/simulation/convert \
  -H 'Content-Type: application/json' \
  -d "{\
    \"esdlContents\": \"$ESDL_B64\",\
    \"startDate\": \"2026-01-01T00:00:00.000Z\",\
    \"endDate\":   \"2026-01-07T00:00:00.000Z\",\
    \"timeStep\":   \"15m\"\
  }" \
| jq -r .esdl \
| base64 -d > ./output-converted.esdl
```

### Java (HTTP client) example
```java
import java.net.http.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

String esdl = Files.readString(Path.of("./input.esdl"));
String esdlB64 = Base64.getEncoder().encodeToString(esdl.getBytes());
String payload = "{" +
  "\"esdlContents\":\"" + esdlB64 + "\"," +
  "\"startDate\":\"2026-01-01T00:00:00.000Z\"," +
  "\"endDate\":\"2026-01-07T00:00:00.000Z\"," +
  "\"timeStep\":\"15m\"}";

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
  .uri(URI.create("http://<essim-host>/simulation/convert"))
  .header("Content-Type", "application/json")
  .POST(HttpRequest.BodyPublishers.ofString(payload))
  .build();

HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
String esdlOutB64 = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
  .readTree(response.body()).get("esdl").asText();
Files.writeString(Path.of("./output-converted.esdl"), new String(Base64.getDecoder().decode(esdlOutB64)));
```

---

## 2) Converting directly in code (Java)

You can embed profiles without calling the web service, using the utilities provided by ESSIM.

Key classes/methods:
- `nl.tno.essim.utils.ESDLUtil.loadEcoreResource(InputStream)`
- `nl.tno.essim.utils.ESDLUtil.convertExternalToTimeSeriesProfiles(XMIResourceImpl, EssimTime)`
- `nl.tno.essim.utils.ESDLUtil.ecoreResourceToString(EObject)`
- `nl.tno.essim.time.EssimTime`
- `nl.tno.essim.time.EssimDuration`

Optional helper:
- `nl.tno.essim.utils.ESDLUtil.getProfilesStartAndEnd(XMIResourceImpl)` can scan the ESDL and return the earliest start and latest end across all profiles; you can use it to decide a simulation window when unsure.

### Java example
```java
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.utils.ESDLUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import esdl.EnergySystem;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// 1) Load the ESDL
String input = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of("./input.esdl")), StandardCharsets.UTF_8);
XMIResourceImpl xmi = ESDLUtil.loadEcoreResource(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

// 2) Decide simulation window & timestep
LocalDateTime start = LocalDateTime.parse("2026-01-01T00:00:00");
LocalDateTime end   = LocalDateTime.parse("2026-01-07T00:00:00");
EssimDuration step  = EssimDuration.of(15, ChronoUnit.MINUTES); // e.g., 15m
EssimTime essimTime = new EssimTime(start, end, step);

// Alternatively, derive window from profiles already in the ESDL
// LocalDateTime[] win = ESDLUtil.getProfilesStartAndEnd(xmi);
// EssimTime essimTime = new EssimTime(win[0], win[1], EssimDuration.of(1, ChronoUnit.HOURS));

// 3) Convert all ExternalProfile entries to embedded TimeSeriesProfile
ESDLUtil.convertExternalToTimeSeriesProfiles(xmi, essimTime);

// 4) Serialize back to ESDL text
String output = ESDLUtil.ecoreResourceToString((EnergySystem) xmi.getContents().getFirst());
java.nio.file.Files.writeString(java.nio.file.Path.of("./output-converted.esdl"), output, StandardCharsets.UTF_8);
```

---

## What the conversion does
- Iterates over all `EnergyAsset` ports; for each `ExternalProfile`, creates a `TimeSeriesProfile`.
- Sets the `TimeSeriesProfile` `timestep` (in seconds) and `startDateTime` from the provided `EssimTime`.
- Copies `profileType`, `interpolationMethod`, and converts/standardizes quantity and unit:
  - If the original profile uses a `QuantityAndUnitReference`, it is preserved.
  - If it uses an inline `QuantityAndUnitType`, the converter standardizes it and, if necessary, adds it to `EnergySystemInformation.quantityAndUnits` and references it.
- Uses the profile data source (via `ProfileDataManager`) to read values for each time slice `[t, t + timeStep)` and fills `ProfileElement`s accordingly.
- Replaces the original `ExternalProfile` with the new `TimeSeriesProfile` in the ESDL.

## Time step notation (API)
The `timeStep` field accepts `<amount><unit>` with units:
- `s` (seconds), `m` (minutes), `h` (hours), `d` (days), `M` (months), `y` (years)
If the notation is not recognized, the service logs a warning and defaults to `1h`.

## Troubleshooting
- "No area found in ESDL energy instance!": Ensure your ESDL `EnergySystem.instance.area` is present.
- "No Energy System Information specified!": Ensure `EnergySystem.energySystemInformation` with carriers/units is present. The converter may add missing `QuantityAndUnitType` definitions when needed.
- Empty output or unchanged profiles: Verify the input ESDL actually contains ports with `ExternalProfile`s; profiles already in `TimeSeriesProfile` or `DateTimeProfile` format will be left as is.
- Value scaling/units: The converter standardizes to the referenced quantity/unit. If values seem unexpectedly scaled, check the original unit and multipliers.

## References
- REST: `nl.tno.essim.rest.SimulationService#convertExternalToTimeSeriesProfiles` (`POST /simulation/convert`)
- Model: `nl.tno.essim.model.ESDLProfileConversion`
- Utility: `nl.tno.essim.utils.ESDLUtil#convertExternalToTimeSeriesProfiles`
- Helper: `nl.tno.essim.utils.ESDLUtil#getProfilesStartAndEnd`
