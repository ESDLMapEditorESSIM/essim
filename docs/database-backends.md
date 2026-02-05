# Database Backends: PostgreSQL vs InfluxDB

## Overview

ESSIM supports two database backends for storing simulation results and reading input profiles:
- **InfluxDB** - A time-series database optimized for high-write workloads
- **PostgreSQL** - A relational database with TimescaleDB extension support for time-series data

Both backends can be used for:
1. **Storing simulation results** (observations, power flows, energy balances, etc.)
2. **Reading input profiles** (load curves, generation profiles, price profiles, etc.)

## ⚠️ Important: InfluxDB Version Compatibility

**ESSIM currently uses InfluxDB 1.x client library (`influxdb-java` version 2.24), which is outdated and no longer actively maintained.**

**Recommendation: Use PostgreSQL instead of InfluxDB for new projects.**

### Why PostgreSQL is Preferred

1. **InfluxDB 1.x is End-of-Life**: InfluxDB 1.x reached end-of-life, and the Java client library is no longer maintained
2. **InfluxDB 2.x Breaking Changes**: InfluxDB 2.x uses a completely different API and is not compatible with the current ESSIM implementation
3. **PostgreSQL Maturity**: PostgreSQL is a mature, actively maintained database with excellent long-term support

### Current Status

- ESSIM works with **InfluxDB 1.x only** (tested with 1.8.x)
- InfluxDB 2.x is **not supported**
- The `influxdb-java` client version 2.24 is used (last release for InfluxDB 1.x)

### Future Direction

New users should:
- ✅ Use **PostgreSQL** for all new ESSIM projects
- ✅ Consider TimescaleDB extension for better time-series performance
- ❌ Avoid InfluxDB unless you have an existing InfluxDB 1.x installation

Existing users with InfluxDB 1.x:
- Continue using InfluxDB 1.x if it's working for you
- Plan migration to PostgreSQL for long-term support
- Consider using both databases during transition period

## Comparison

### PostgreSQL (Recommended)

**Strengths:**
- ✅ **Actively maintained** with long-term support
- ✅ Full SQL support with complex queries
- ✅ ACID compliance and transactions
- ✅ Better for relational data and joins
- ✅ More familiar to most developers
- ✅ Can store both time-series and metadata in one database
- ✅ Stored procedures support (PL/pgSQL)
- ✅ TimescaleDB extension available for enhanced time-series performance
- ✅ Excellent tooling and ecosystem

**Use Cases:**
- **All new ESSIM projects** (recommended default choice)
- Complex analytical queries across simulation results
- Integration with existing PostgreSQL infrastructure
- When you need complex joins and aggregations
- Any frequency data (from seconds to daily resolution)
- When long-term support and maintenance are important

**Implementation:**
- Observations: `nl.tno.essim.observation.consumers.PostgresObservationConsumer`
- Profiles: `nl.tno.essim.profiles.PostgresProfileCacheSupplier`
- Metadata: `nl.tno.essim.mongo.PostgresMetadataProvider`

### InfluxDB (Legacy Support Only)

⚠️ **Note: InfluxDB support uses outdated 1.x version. Not recommended for new projects.**

**Strengths:**
- Built-in time-series functions and aggregations
- Efficient storage with compression
- Native support for tags and fields

**Limitations:**
- ❌ **Uses InfluxDB 1.x only** (end-of-life)
- ❌ Java client library (`influxdb-java` 2.24) is no longer maintained

**Use Cases:**
- Legacy ESSIM installations already using InfluxDB 1.x
- When you have existing InfluxDB 1.x infrastructure
- **Not recommended for new projects**

**Implementation:**
- Observations: `nl.tno.essim.observation.consumers.InfluxDBObservationConsumer`
- Profiles: `nl.tno.essim.profiles.InfluxDBProfileDataHandler`
- Service: `nl.tno.essim.influxdb.InfluxDBService`

## Configuration

### Via Java API

```java
EssimSimulation sim = new EssimSimulation();

// For InfluxDB
sim.setInfluxURL("http://localhost:8086");

// For PostgreSQL (without jdbc: prefix - ESSIM adds it automatically)
sim.setPostgresURL("postgresql://localhost:5432/essim");

// You can use both simultaneously
sim.setInfluxURL("http://localhost:8086");
sim.setPostgresURL("postgresql://localhost:5432/essim");
```

**Important**: The PostgreSQL URL format is `postgresql://host:port/database` **without** the `jdbc:` prefix. ESSIM automatically prepends `jdbc:` when creating the connection (see `ESSimEngine.java:317`).

### Via REST API

```bash
curl -X POST http://localhost:8080/essim/simulation \
  -H "Content-Type: application/json" \
  -d '{
    "esdlContents": "<base64-encoded-esdl>",
    "startDate": "2019-01-01T00:00:00.000Z",
    "endDate": "2019-01-02T00:00:00.000Z",
    "scenarioID": "my-scenario",
    "influxURL": "http://localhost:8086",
    "postgresURL": "postgresql://localhost:5432/essim"
  }'
```

**Note**: Use `postgresql://` format (without `jdbc:` prefix).

### Via Environment Variables

When running the ESSIM engine, you can set default database URLs:

```bash
export INFLUX_URL=http://localhost:8086
export POSTGRES_URL=postgresql://localhost:5432/essim

# Start ESSIM
java -jar essim-engine/target/essim.jar
```

**Note**: Use `postgresql://` format (not `jdbc:postgresql://`).

## Using Both Databases Simultaneously

ESSIM can write to both databases at the same time. This is useful for:
- **InfluxDB**: Fast time-series queries and Grafana dashboards
- **PostgreSQL**: Complex analysis and long-term storage

Simply configure both URLs in your simulation:

```java
sim.setInfluxURL("http://localhost:8086");
sim.setPostgresURL("postgresql://localhost:5432/essim");  // without jdbc: prefix
```

## Storing Simulation Results

### Data Model

Both databases store observations with:
- **Time**: Timestamp of the observation
- **Tags**: Metadata (assetId, assetName, carrierId, carrierName, simulationRun, etc.)
- **Fields**: Numerical values (power, energy, voltage, current, etc.)

### InfluxDB Schema

Data is stored in measurements (one per transport solver/network):

```
Measurement: TransportSolver_ElectricityNet_0
  Tags:
    - simulationRun: "abc123-simulation-id"
    - assetId: "producer1"
    - assetName: "Solar PV"
    - assetClass: "Producer"
    - carrierId: "elec-110kV"
    - carrierName: "Electricity 110kV"
    - Year: "2019"
  Fields:
    - allocationPower: 1500000.0
    - allocationEnergy: 5400000000.0
    - loadflow_power: 1498500.0
    - loadflow_reactive_power: 125000.0
  Time: 2019-01-01T12:00:00Z
```

Query example:
```sql
SELECT "allocationPower", "loadflow_power"
FROM "TransportSolver_ElectricityNet_0"
WHERE "assetId" = 'producer1'
  AND time >= '2019-01-01T00:00:00Z'
  AND time <= '2019-01-02T00:00:00Z'
```

### PostgreSQL Schema

Data is stored in schemas (one per scenario) with tables (one per transport solver):

```sql
-- Schema: my_scenario
-- Table: transportsolver_electricitynet_0

CREATE TABLE my_scenario.transportsolver_electricitynet_0 (
    id BIGSERIAL PRIMARY KEY NOT NULL,
    time TIMESTAMP,
    tags JSONB,
    bidcurve JSONB,
    allocationpower DOUBLE PRECISION,
    allocationenergy DOUBLE PRECISION,
    loadflow_power DOUBLE PRECISION,
    loadflow_reactive_power DOUBLE PRECISION,
    -- ... other observation fields
);
```

Query example:
```sql
SELECT time, allocationpower, loadflow_power
FROM my_scenario.transportsolver_electricitynet_0
WHERE tags->>'assetId' = 'producer1'
  AND time >= '2019-01-01 00:00:00'
  AND time <= '2019-01-02 00:00:00';
```

### Observation Fields

Common fields stored in both databases (see `nl.tno.essim.observation.ObservationField`):

- **ALLOCATION_POWER** - Allocated power (W)
- **ALLOCATION_ENERGY** - Allocated energy (Wh)
- **LOADFLOW_POWER** - Load-flow calculated power (W)
- **LOADFLOW_ENERGY** - Load-flow calculated energy (Wh)
- **LOADFLOW_REACTIVE_POWER** - Reactive power (VAr)
- **LOADFLOW_CURRENT** - Current (A)
- **COST** - Cost (currency units)
- **MARGINAL_COST** - Marginal cost
- **SIMULATION_TIME** - Simulation execution time (ms)

## Reading Input Profiles

Both databases support reading time-series profiles as input to simulations.

### ESDL Profile Configuration

#### InfluxDB Profile

```xml
<profile xsi:type="esdl:InfluxDBProfile"
         id="profile1"
         host="localhost"
         port="8086"
         database="energy_profiles"
         measurement="solar_generation"
         field="power_kw"
         multiplier="1000.0"
         startDate="2019-01-01T00:00:00.000+00:00"
         endDate="2019-12-31T23:59:59.000+00:00">
    <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="Power in W"/>
</profile>
```

Optional filters:
```xml
<profile xsi:type="esdl:InfluxDBProfile"
         filters="AND &quot;location&quot; = 'site1' AND &quot;panel_type&quot; = 'monocrystalline'"
         ...>
```

#### PostgreSQL Profile

```xml
<profile xsi:type="esdl:DatabaseProfile"
         id="profile2"
         host="localhost"
         port="5432"
         database="energy_profiles"
         measurement="solar_generation"
         schema="public"
         field="power_kw"
         multiplier="1000.0"
         startDate="2019-01-01T00:00:00.000+00:00"
         endDate="2019-12-31T23:59:59.000+00:00">
    <profileQuantityAndUnit xsi:type="esdl:QuantityAndUnitReference" reference="Power in W"/>
</profile>
```

Optional filters (SQL WHERE clause):
```xml
<profile xsi:type="esdl:DatabaseProfile"
         filters="location = 'site1' AND panel_type = 'monocrystalline'"
         ...>
```

### Profile Caching

Both implementations cache profile data to avoid repeated database queries:

- **Cache key**: Based on URI, database/schema, measurement/table, field, and filters
- **Cache duration**: Profiles are cached for the duration of the simulation
- **Memory efficiency**: Only requested time ranges are loaded

Implementation:
- `nl.tno.essim.profiles.ProfileCache` - Shared cache for all profiles
- `nl.tno.essim.profiles.TimeSeriesDataCache` - In-memory time-series data

### Profile Data Requirements

Both databases expect:
- A **time column** with timestamps
- A **value column** with the profile data
- Optional **filter columns** for selecting specific data

Time column names:
- InfluxDB: Uses built-in `time` field
- PostgreSQL: Defaults to `time`, configurable via `DatabaseProfile.timeColumn`

## Authentication

### Credentials File

Both backends support credentials from a file at `~/.essim-credentials`:

```properties
# InfluxDB
localhost:8086.username=admin
localhost:8086.password=influxpassword

# PostgreSQL (use the URL without jdbc: prefix as the key)
postgresql://localhost:5432/essim.username=essim_user
postgresql://localhost:5432/essim.password=pgpassword
```

Format: `<server-or-url>.<username|password>=<value>`

**Note**: For PostgreSQL, use the URL format **without** `jdbc:` prefix (i.e., `postgresql://...` not `jdbc:postgresql://...`).

Implementation: `nl.tno.essim.commons.CredentialUtils`

### Default Credentials

If no credentials file is found:
- **InfluxDB**: No authentication (assumes `auth-enabled = false` in InfluxDB config)
- **PostgreSQL**: Uses `username=essim`, `password=pgpassword`

## Grafana Integration

ESSIM can automatically generate Grafana dashboards for visualizing simulation results.

### InfluxDB Datasource

```java
String grafanaDashboard = essimEngine.createGrafanaDashboard();
```

Generates dashboard with:
- Datasource: InfluxDB at configured URL
- Database: Scenario ID
- Queries: Pre-configured panels for power flows, energy balances, etc.

Implementation: `nl.tno.essim.grafana.InfluxDBDataSourceGenerator`

### PostgreSQL Datasource

Similar dashboard generation for PostgreSQL:
- Datasource: PostgreSQL at configured URL
- Schema: Scenario ID
- Queries: SQL-based panels

Implementation: `nl.tno.essim.grafana.PostgresDataSourceGenerator`

## Performance Considerations

### InfluxDB

**Batch Writing:**
- Enabled by default: 10,000 points per batch or 10 seconds
- Gzip compression enabled
- Asynchronous writes with background thread

```java
// From InfluxDBObservationConsumer.java:64
influxDB.enableBatch(10000, 10, TimeUnit.SECONDS, threadFactory).enableGzip();
```

**Query Performance:**
- Fast for time-range queries
- Efficient aggregations (mean, sum, etc.)
- Good for high-frequency data

### PostgreSQL

**Batch Writing:**
- Uses a single-threaded executor for sequential writes
- Batches observations in memory before writing
- Supports stored procedures for complex operations

**Schema Naming:**
- Scenario names are converted to valid PostgreSQL identifiers
- Invalid characters replaced with underscores
- Maximum 63 characters (PostgreSQL identifier limit)

Implementation: `PostgresObservationConsumer.convertToValidPostgresColumnName()`

## Troubleshooting


### Performance Issues

**InfluxDB:**
- Increase batch size for faster writes
- Add indexes/retention policies as needed
- Monitor memory usage (InfluxDB can be memory-intensive)

**PostgreSQL:**
- Create indexes on time column and frequently queried tags
- Use `EXPLAIN ANALYZE` to optimize slow queries
- Consider partitioning large tables by time
- **Recommended**: Install TimescaleDB extension for better time-series performance

## TimescaleDB Extension

[TimescaleDB](https://www.timescale.com/) is a PostgreSQL extension that enhances time-series performance while maintaining full SQL compatibility. For best performance, an extra insertion timestamp should be added to allow compression of older data.

### Why Use TimescaleDB?

- ✅ **Better performance** for time-series queries (10-100x faster than vanilla PostgreSQL)
- ✅ **Automatic partitioning** (called "hypertables") based on time
- ✅ **Compression** to reduce storage requirements (up to 90% reduction)
- ✅ **Continuous aggregates** for pre-computed rollups
- ✅ **Full SQL compatibility** - works with existing ESSIM code
- ✅ **Free and open source** (Apache 2.0 license)

### Enable TimescaleDB for ESSIM

After installation, enable the extension in your database:

```sql
-- Connect to your ESSIM database
psql -d essim

-- Enable TimescaleDB extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Convert existing tables to hypertables (do this after first simulation creates tables)
-- Replace 'my_scenario' with your scenario name and 'transportsolver_electricitynet_0' with your table name
SELECT create_hypertable('my_scenario.transportsolver_electricitynet_0', 'time',
                         if_not_exists => TRUE,
                         migrate_data => TRUE);

-- Enable compression (optional, recommended for older data)
ALTER TABLE my_scenario.transportsolver_electricitynet_0 SET (
  timescaledb.compress,
  timescaledb.compress_segmentby = 'tags'
);

-- Create compression policy (compress data older than 7 days)
SELECT add_compression_policy('my_scenario.transportsolver_electricitynet_0', INTERVAL '7 days');
```

### Using TimescaleDB with ESSIM

**No code changes required!** ESSIM will work automatically with TimescaleDB-enabled tables. The URL format remains the same:

```java
sim.setPostgresURL("postgresql://localhost:5432/essim");
```

### Performance Tips with TimescaleDB

1. **Create hypertables immediately after first simulation**
2. **Enable compression** for data older than a few days
3. **Use continuous aggregates** for frequently queried rollups:
   ```sql
   CREATE MATERIALIZED VIEW hourly_power_avg
   WITH (timescaledb.continuous) AS
   SELECT time_bucket('1 hour', time) AS hour,
          tags->>'assetId' as asset_id,
          AVG(allocationpower) as avg_power
   FROM my_scenario.transportsolver_electricitynet_0
   GROUP BY hour, asset_id;
   ```

4. **Add refresh policy** for continuous aggregates:
   ```sql
   SELECT add_continuous_aggregate_policy('hourly_power_avg',
     start_offset => INTERVAL '3 hours',
     end_offset => INTERVAL '1 hour',
     schedule_interval => INTERVAL '1 hour');
   ```

### Further Information

- TimescaleDB documentation: https://docs.timescale.com/
- Hypertables guide: https://docs.timescale.com/use-timescale/latest/hypertables/
- Compression guide: https://docs.timescale.com/use-timescale/latest/compression/

## Best Practices

1. **Choose the right backend:**
   - ✅ **For new projects: Always use PostgreSQL** (InfluxDB 1.x is end-of-life)
   - ✅ PostgreSQL handles all use cases well (high-frequency data, complex queries, etc.)
   - ✅ Consider TimescaleDB extension for enhanced time-series performance
   - ⚠️ Only use InfluxDB if you have existing InfluxDB 1.x infrastructure

2. **Profile data:**
   - Store frequently-used profiles in the database
   - Use filters to select specific subsets of data
   - Set appropriate start/end dates to limit data loaded

3. **Credentials:**
   - Use the credentials file for security
   - Don't hardcode passwords in ESDL files
   - Use different credentials for different environments

4. **Database maintenance:**
   - **InfluxDB**: Set up retention policies for old data
   - **PostgreSQL**: Regular VACUUM and ANALYZE operations
   - Monitor database size and performance

5. **Testing:**
   - Test with small datasets first
   - Verify profile data is correct before large simulations
   - Check database connectivity before starting long simulations

## Further Information

- InfluxDB documentation: https://docs.influxdata.com/
- PostgreSQL documentation: https://www.postgresql.org/docs/
- TimescaleDB (PostgreSQL extension): https://docs.timescale.com/
- Grafana documentation: https://grafana.com/docs/
