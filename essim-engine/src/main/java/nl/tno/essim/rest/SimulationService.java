package nl.tno.essim.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import esdl.*;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.influxdb.InfluxConnectionProvider;
import nl.tno.essim.influxdb.InfluxDBService;
import nl.tno.essim.kpi.KPIManager;
import nl.tno.essim.kpi.data.KPIStatus;
import nl.tno.essim.model.*;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.observation.consumers.PostgresObservationConsumer;
import nl.tno.essim.profiles.DataTableProfileDataHandler;
import nl.tno.essim.rest.util.MongoInject;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.utils.ESDLUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MarkerFactory;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@OpenAPIDefinition(
        info = @Info(
                title = "ESSIM API",
                version = "1.0",
                description = "API for managing ESSIM simulations"
        )
)
@Tag(name = "ESSIM", description = "Operations related to ESSIM Simulations")
@Path("/simulation")
@Slf4j
public class SimulationService {
    private static final String ESSIM_ID = System.getProperty(Commons.ESSIM_REVISION);
    @MongoInject
    private IMongoBackend mongo;

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Operation(
            summary = "Start a new simulation",
            description = "Initiates a new simulation with the provided ESDL",
            requestBody = @RequestBody(description = "Details of the simulation to be started", required = true, content = @Content(schema = @Schema(implementation = EssimSimulation.class))),
            responses = {
                    @ApiResponse(description = "The status of the simulation", content = @Content(schema = @Schema(implementation = KPIStatus.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content(schema = @Schema(implementation = ErrorStatus.class)))
            }
    )
    public Response postSimulation(EssimSimulation simulation) {
        ErrorStatus error = new ErrorStatus();
        error.setStatus(Status.ERROR);

        String esdlContents = simulation.getEsdlContents();
        FileWriter fw = null;
        String decodedContents = null;
        if (esdlContents == null || esdlContents.isEmpty()) {
            error.setDescription("Invalid ESDL file!");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        } else {
            try {
                decodedContents = new String(Base64.getDecoder().decode(esdlContents), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                log.debug("ESDL content is not Base64 encoded. Trying URL decoder...");
            }
            if (decodedContents == null) {
                decodedContents = URLDecoder.decode(esdlContents, StandardCharsets.UTF_8);
            }
        }

        if (mongo != null) {
            SimulationStatus status = new SimulationStatus();
            status.setState(Status.CREATED);
            status.setDescription("");
            simulation.setStatus(status);
            String simId = mongo.addSimulation(simulation);

            log.debug(MarkerFactory.getMarker(simId), "Simulation ID : {}", simId);


            String finalDecodedContents = decodedContents;
            Thread thread = new Thread(() -> {
                try {
                    Date now = new Date();
                    simulation.setSimRunDate(now);
                    ESSimEngine engine = new ESSimEngine(ESSIM_ID, simId, simulation, finalDecodedContents, mongo);
                    String grafanaDashboard = engine.createGrafanaDashboard();
                    if (grafanaDashboard != null) {
                        simulation.setDashboardURL(grafanaDashboard);
                    } else {
                        simulation.setDashboardURL("*headless simulation*");
                    }
                    mongo.updateSimulationData(simId, simulation);
                    mongo.getStatusMap().put(simId, engine);
                    engine.startSimulation();
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    log.error(MarkerFactory.getMarker(simId), "Error starting simulation", e);
                    error.setDescription("Internal error: " + sw);
                    mongo.updateSimulationStatus(simId, Status.ERROR, sw.toString());
                    mongo.updateStatus("Ready");
                }

            }, simId);
            thread.setUncaughtExceptionHandler(new EssimExceptionHandler(mongo));
            thread.start();

            CreatedStatus created = new CreatedStatus();
            created.setStatus(Status.CREATED);
            created.setId(simId);

            return Response.status(Response.Status.CREATED).entity(created).build();
        } else {
            error.setDescription("MongoDB internal error!");
        }

        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();

    }

    @POST
    @Path("/convert")
    @Produces("application/json")
    @Consumes("application/json")
    @Operation(
            summary = "Convert external profiles to time series",
            description = "Converts external profiles in the provided ESDL to time series profiles using the specified time parameters",
            requestBody = @RequestBody(
                    description = "ESDL profile conversion parameters",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ESDLProfileConversion.class))
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully converted profiles",
                            content = @Content(schema = @Schema(implementation = ObjectNode.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Invalid input parameters",
                            content = @Content(schema = @Schema(implementation = ErrorStatus.class))
                    )
            }
    )
    public Response convertExternalToTimeSeriesProfiles(ESDLProfileConversion conversion) {
        ErrorStatus error = new ErrorStatus();
        error.setStatus(Status.ERROR);
        try {
            String timeStep = conversion.getTimeStep();
            EssimDuration profileStep = ESSimEngine.SIMULATION_STEP;
            if (timeStep != null) {
                profileStep = ESSimEngine.parseTimeStep(timeStep);
            }
            EssimTime essimTime = new EssimTime(EssimTime.dateToLocalDateTime(conversion.getStartDate()), EssimTime.dateToLocalDateTime(conversion.getEndDate()), profileStep);
            XMIResourceImpl xmiResource = ESDLUtil.loadEcoreResource(new ByteArrayInputStream(Base64.getDecoder().decode(conversion.getEsdlContents())));
            ESDLUtil.convertExternalToTimeSeriesProfiles(xmiResource, essimTime);
            String newEsdl = ESDLUtil.ecoreResourceToString((EnergySystem) xmiResource.getContents().getFirst());
            ObjectNode statusJson = ESSimEngine.getObjectMapper().createObjectNode();
            statusJson.put("esdl", Base64.getEncoder().encodeToString(newEsdl.getBytes(StandardCharsets.UTF_8)));
            return Response.status(Response.Status.OK).entity(statusJson).build();

        } catch (Exception e) {
            log.error("Error converting ESDL to time series profiles", e);
            error.setDescription(e.getMessage());
        }
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }

    @GET
    @Produces("application/json")
    @Operation(
            summary = "Retrieve all simulations",
            description = "Fetches all simulations stored in the system. NOTE: This can return a large dataset.",
            responses = {
                    @ApiResponse(description = "A list of all simulations", content = @Content(array = @ArraySchema(schema = @Schema(implementation = EssimSimulation.class))))
            }
    )
    public Response getSimulations() {
        List<EssimSimulation> simulations = mongo.getSimulations();
        return Response.ok(simulations).build();
    }

    @GET
    @Path("/list")
    @Produces("application/json")
    @Operation(
            summary = "Retrieve simulation list",
            description = "Fetches a list of all simulation IDs stored in the system.",
            responses = {
                    @ApiResponse(description = "A list of all simulation IDs", content = @Content(array = @ArraySchema(schema = @Schema(implementation = String.class))))
            }
    )
    public Response getSimulationList() {
        List<String> simulations = mongo.getSimulationIdList();
        return Response.ok(simulations).build();
    }

    @GET
    @Path("/status")
    @Produces("text/plain")
    @Operation(
            summary = "Retrieve ESSIM status",
            description = "Gets the current status of the ESSIM simulation service",
            responses = {
                    @ApiResponse(description = "The current status of ESSIM", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "503", description = "Service is unavailable", content = @Content(schema = @Schema(implementation = String.class)))
            }
    )
    public Response getSimulationStatus() {
        String status = mongo.getStatus();
        if (status.equals("Busy")) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(status).build();
        }
        return Response.ok(status).build();
    }

    @GET
    @Path("/kpiModules")
    @Produces("application/json")
    @Operation(
            summary = "Retrieve available KPI modules",
            description = "Fetches a list of all available KPI modules connected to ESSIM",
            responses = {
                    @ApiResponse(description = "A list of available KPI modules", content = @Content(array = @ArraySchema(schema = @Schema(implementation = KPIModuleInfo.class))))
            }
    )
    public Response getSimulationKpiModules() {
        KPIManager.getInstance().queryStatus();
        List<KPIModuleInfo> allKPIModules = KPIManager.getInstance().getAllKPIModules();
        return Response.ok(allKPIModules).build();
    }

    @GET
    @Path("/{simulationId}")
    @Produces("application/json")
    @Operation(
            summary = "Retrieve a specific simulation",
            description = "Fetches the details of a specific simulation based on the provided simulation ID",
            responses = {
                    @ApiResponse(description = "Details of the requested simulation", content = @Content(schema = @Schema(implementation = EssimSimulation.class))),
                    @ApiResponse(responseCode = "404", description = "Simulation ID not found", content = @Content(schema = @Schema(implementation = ErrorStatus.class)))
            }
    )
    public Response getSimulationBySimulationId(
            @Parameter(description = "The id of the requested simulation", required = true)
            @PathParam("simulationId") String simulationId) {
        EssimSimulation simulation = mongo.getSimulation(simulationId);
        if (simulation == null) {
            ErrorStatus error = new ErrorStatus();
            error.setStatus(Status.ERROR);
            error.setDescription("SimulationID " + simulationId + " not found!");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        } else {
            return Response.ok(simulation).build();
        }
    }

    @PUT
    @Path("/{simulationId}")
    @Produces("application/json")
    @Consumes("application/json")
    @Operation(summary = "Add or update simulation",
            description = "Add or update a simulation in mongo",
            requestBody = @RequestBody(description = "Simulation description", content = @Content(schema = @Schema(implementation = EssimSimulation.class))),
            responses = {
                    @ApiResponse(description = "The simulation id", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "404", description = "Simulation id not found", content = @Content(schema = @Schema(implementation = ErrorStatus.class)))
            })
    public Response putSimulationBySimulationId(
            @Parameter(description = "The id of the simulation", required = true)
            @PathParam("simulationId") String simulationId, EssimSimulation entity) {
        if (mongo.getSimulation(simulationId) == null) {
            ErrorStatus error = new ErrorStatus();
            error.setStatus(Status.ERROR);
            error.setDescription("SimulationID " + simulationId + " not found!");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        } else {
            mongo.updateSimulationData(simulationId, entity);
            return Response.ok(simulationId).build();
        }
    }

    @DELETE
    @Path("/{simulationId}")
    @Produces("application/json")
    @Operation(
            summary = "Remove a specific simulation",
            description = "Removes a specific simulation including all simulation data.",
            responses = {
                    @ApiResponse(description = "The simulation id", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "404", description = "Simulation ID not found", content = @Content(schema = @Schema(implementation = ErrorStatus.class)))
            }
    )
    public Response deleteSimulationBySimulationId(
            @Parameter(description = "The id of the simulation", required = true)
            @PathParam("simulationId") String simulationId) {
        EssimSimulation simulation = mongo.getSimulation(simulationId);
        if (simulation == null) {
            ErrorStatus error = new ErrorStatus();
            error.setStatus(Status.ERROR);
            error.setDescription("SimulationID " + simulationId + " not found!");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        } else {
            String influxURL = simulation.getInfluxURL();
            String database = simulation.getScenarioID();
            InfluxDBService influx = InfluxConnectionProvider.getInfluxDBService(influxURL);
            influx.deleteEntriesWithTag(database, "simulationRun", simulationId);
            mongo.deleteSimulation(simulationId);
            ErrorStatus status = new ErrorStatus(Status.OK, simulationId);

            return Response.ok(status).build();
        }

    }


    @GET
    @Path("/{simulationId}/kpi")
    @Produces("application/json")
    @Operation(summary = "Get KPIs for simulation",
            description = "Get all the KPI information for a simulation",
            responses = {
                    @ApiResponse(description = "The list of KPIs", content = @Content(array = @ArraySchema(schema = @Schema(implementation = KPIModule.class)))),
                    @ApiResponse(responseCode = "404", description = "Simulation id not found", content = @Content(schema = @Schema(implementation = ErrorStatus.class)))
            })
    public Response getSimulationKpiBySimulationId(
            @Parameter(description = "The id of the simulation", required = true)
            @PathParam("simulationId") String simulationId) {
        String error = "";
        EssimSimulation simulation = mongo.getSimulation(simulationId);
        if (simulation == null) {
            error = "SimulationID " + simulationId + " not found!";
        } else {
            RemoteKPIModule kpiModule = simulation.getKpiModule();
            if (kpiModule == null) {
                error = "SimulationID " + simulationId + " had no KPIs defined!";
            } else {
                if (kpiModule.getModules() == null || kpiModule.getModules().isEmpty()) {
                    error = "SimulationID " + simulationId + " had no KPIs defined!";
                } else {
                    return Response.ok(kpiModule.getModules()).build();
                }
            }
        }
        return Response.status(Response.Status.NOT_FOUND).entity(error).build();
    }

    @GET
    @Path("/{simulationId}/kpi/{kpiId}")
    @Produces("application/json")
    @Operation(summary = "Get KPI result",
            description = "Get KPI result for a specific KPI of a simulation",
            responses = {
                    @ApiResponse(description = "The KPI result"),
                    @ApiResponse(responseCode = "404", description = "Simulation id not found", content = @Content(schema = @Schema(implementation = ErrorStatus.class)))
            })
    public Response getSimulationKpiBySimulationIdAndKpiId(
            @Parameter(description = "The id of the simulation", required = true)
            @PathParam("simulationId") String simulationId,
            @Parameter(description = "The id of the KPI module", required = true)
            @PathParam("kpiId") String kpiId) {
        ErrorStatus error = new ErrorStatus();
        error.setStatus(Status.ERROR);
        EssimSimulation simulation = mongo.getSimulation(simulationId);
        if (simulation == null) {
            error.setDescription("SimulationID " + simulationId + " not found!");
        } else {
            RemoteKPIModule kpiModule = simulation.getKpiModule();
            if (kpiModule == null) {
                error.setDescription("SimulationID " + simulationId + " had no KPIs defined!");
            } else {
                if (kpiModule.getModules() == null || kpiModule.getModules().isEmpty()) {
                    error.setDescription("SimulationID " + simulationId + " had no KPIs defined!");
                } else {
                    for (KPIModule module : kpiModule.getModules()) {
                        if (module.getId() != null) {
                            if (module.getId().equals(kpiId)) {
                                return Response.ok(getKPIResult(simulationId, module)).build();
                            }
                        }
                    }
                    error.setDescription("KPI ID " + kpiId + " undefined for SimulationID " + simulationId + "!");
                }
            }
        }
        return Response.status(Response.Status.NOT_FOUND).entity(error).build();
    }

    @GET
    @Path("/{simulationId}/status")
    @Produces("application/json")
    @Operation(summary = "Get simulation status",
            description = "Get the status of a simulation",
            responses = {
                    @ApiResponse(description = "The simulation status", content = @Content(schema = @Schema(implementation = SimulationStatus.class))),
                    @ApiResponse(responseCode = "404", description = "Simulation not found", content = @Content(schema = @Schema(implementation = ErrorStatus.class)))
            })
    public Response getSimulationStatusBySimulationId(
            @Parameter(description = "The id of the simulation", required = true)
            @PathParam("simulationId") String simulationId) {
        SimulationStatus simStatus = mongo.getSimulationStatus(simulationId);
        if (simStatus != null) {
            ObjectNode statusJson = ESSimEngine.getObjectMapper().createObjectNode();
            statusJson.put("State", simStatus.getState().toString());
            statusJson.put("Description", simStatus.getDescription());
            return Response.ok(statusJson).build();
        } else {
            ErrorStatus error = new ErrorStatus();
            error.setStatus(Status.ERROR);
            error.setDescription("SimulationID " + simulationId + " not found!");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
    }

    @GET
    @Path("/{simulationId}/load_animation")
    @Produces("application/json")
    public Response getSimulationLoadAnimationBySimulationId(@PathParam("simulationId") String simulationId) {
        return Response.status(Response.Status.NOT_FOUND).entity("Feature temporarily disabled!").build();
    }

    @GET
    @Path("/{simulationId}/transport")
    @Produces("application/json")
    @Operation(summary = "Get simulation transport networks",
            description = "Get the transport networks of a simulation",
            responses = {
                    @ApiResponse(description = "The list of transport networks for the simulation", content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransportNetwork.class)))),
                    @ApiResponse(responseCode = "404", description = "Simulation not found", content = @Content(schema = @Schema(implementation = ErrorStatus.class)))
            })
    public Response getSimulationTransportBySimulationId(
            @Parameter(description = "The id of the simulation", required = true)
            @PathParam("simulationId") String simulationId) {
        EssimSimulation simulation = mongo.getSimulation(simulationId);
        if (simulation != null) {
            return Response.ok(simulation.getTransport()).build();
        } else {
            ErrorStatus error = new ErrorStatus();
            error.setStatus(Status.ERROR);
            error.setDescription("SimulationID " + simulationId + " not found!");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
    }

    @GET
    @Path("/{simulationId}/transport/{index}")
    @Produces("application/json")
    @Operation(summary = "Get simulation transport network",
            description = "Get a specific transport network for a simulation",
            responses = {
                    @ApiResponse(description = "The transport network", content = @Content(schema = @Schema(implementation = TransportNetwork.class))),
                    @ApiResponse(responseCode = "404", description = "Simulation or transport not found", content = @Content(schema = @Schema(implementation = ErrorStatus.class)))
            })
    public Response getSimulationTransportBySimulationIdAndIndex(
            @Parameter(description = "The id of the simulation", required = true)
            @PathParam("simulationId") String simulationId,
            @PathParam("index") String index) {
        EssimSimulation simulation = mongo.getSimulation(simulationId);
        if (simulation != null) {
            List<TransportNetwork> transportNetworks = simulation.getTransport();
            try {
                int i = Integer.parseInt(index);
                TransportNetwork transportNetwork = transportNetworks.get(i);
                return Response.ok(transportNetwork).build();
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                ErrorStatus error = new ErrorStatus();
                error.setStatus(Status.ERROR);
                error.setDescription("Index " + index + " is not valid!");
                return Response.status(Response.Status.NOT_FOUND).entity(error).build();
            }
        } else {
            ErrorStatus error = new ErrorStatus();
            error.setStatus(Status.ERROR);
            error.setDescription("SimulationID " + simulationId + " not found!");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
    }

    private JsonNode getKPIResult(String simulationId, KPIModule module) {
        String moduleResultString = module.getResult();
        JsonNode result = null;
        if (moduleResultString != null) {
            try {
                result = ESSimEngine.getObjectMapper().readTree(moduleResultString);
                if (!result.has("status")) {
                    ((ObjectNode) result).put("status", "Success");
                }

            } catch (JsonProcessingException e) {
                log.warn("Could not parse json", e);
            }

        }
        if (result == null) {
            KPIStatus.SimulationProgress simulationProgress = KPIManager.getInstance().getKPIModuleInfo(module.getId(), simulationId);
            if (simulationProgress == null || simulationProgress.getStatusMessage() == null) {
                result = ESSimEngine.getObjectMapper().createObjectNode();
                ((ObjectNode) result).put("status", "Not yet started");
            } else {
                result = ESSimEngine.getObjectMapper().valueToTree(simulationProgress);
            }

        }
        ObjectNode objectNode = ESSimEngine.getObjectMapper().createObjectNode();
        objectNode.put(module.getId(), result);
        return objectNode;
    }

    @GET
    @Path("/{simulationId}/logs")
    @Produces("application/json")
    @Operation(summary = "Get simulation logs",
            description = "Get the logs for a specified simulation",
            responses = {
                    @ApiResponse(description = "The logs for the given simulation id"),
                    @ApiResponse(responseCode = "404", description = "Simulation or transport not found", content = @Content(schema = @Schema(implementation = ErrorStatus.class)))
            })
    public Response getSimulationLogs(
            @Parameter(description = "The id of the simulation", required = true)
            @PathParam("simulationId") String simulationId
    ) throws IOException {
        File file = new File("logs/simulation-logs/" + simulationId + ".log");
        if (!file.exists()) {
            ErrorStatus error = new ErrorStatus();
            error.setStatus(Status.ERROR);
            error.setDescription("No logs found for SimulationID " + simulationId + "!");
            return Response.status(Response.Status.NOT_FOUND).entity(error).build();
        }
        String logs = Files.readString(Paths.get(file.getAbsolutePath()));
        ObjectNode objectNode = ESSimEngine.getObjectMapper().createObjectNode();
        objectNode.put(simulationId, logs);
        return Response.ok(objectNode).build();
    }

    @GET
    @Path("/{simulationId}/ESDLWithProfiles")
    @Produces("application/json")
    public Response getESDLWithProfiles(
            @Parameter(description = "The id of the simulation", required = true)
            @PathParam("simulationId") String simulationId) {
        ErrorStatus error = new ErrorStatus();
        error.setStatus(Status.ERROR);
        try {
            EssimSimulation simulation = mongo.getSimulation(simulationId);
            XMIResourceImpl xmiResource = ESDLUtil.loadEcoreResource(new ByteArrayInputStream(Base64.getDecoder().decode(simulation.getEsdlContents())));
//        simulation.get
            EnergySystem energySystem = (EnergySystem) xmiResource.getContents().getFirst();
            Instance instance = energySystem.getInstance().getFirst();
            if (instance.getArea() == null) {
                throw new IllegalArgumentException("No area found in ESDL energy instance !");
            }
            Area mainArea = instance.getArea();

            String postgresURL = simulation.getPostgresURL();
            DatabaseConfiguration dbc = DataTableProfileDataHandler.getDatabaseConfiguration(energySystem, postgresURL, DatabaseTypeEnum.POSTGRESQL);
            QuantityAndUnitReference quantityAndUnitReference = getQuantityAndUnitReference(energySystem);

            ESSimEngine.determineSolverNetworks(mainArea, (connectedAssets, connectedCarriers, index) -> {
                String solverIdentifier = ESSimEngine.getSolverIdentifier(ESSimEngine.getEnergySystemId(energySystem), connectedCarriers, index);
                HashMap<String, DatabaseConfiguration> map = new HashMap<>();
                connectedAssets.keySet().forEach(energyAsset -> {
                    energyAsset.getPort().forEach(port -> {
                        String scenarioID = simulation.getScenarioID();

                        DataTableProfile dataTableProfile = EsdlFactory.eINSTANCE.createDataTableProfile();
                        dataTableProfile.setConfiguration(dbc);
                        dataTableProfile.setId(UUID.randomUUID().toString());
                        dataTableProfile.setTableName(PostgresObservationConsumer.convertToValidPostgresColumnName(solverIdentifier));
                        dataTableProfile.setSchema(PostgresObservationConsumer.convertToValidPostgresColumnName(scenarioID));
                        dataTableProfile.setColumnName("allocationenergy");
                        dataTableProfile.setTimeColumnName("time");
                        dataTableProfile.setFilter("tags->>'assetName' = '" + energyAsset.getName() + "' AND tags->>'simulationRun' = '" + simulationId + "'");
                        dataTableProfile.setProfileQuantityAndUnit(quantityAndUnitReference);
                        dataTableProfile.setProfileType(ProfileTypeEnum.ENERGY_IN_J);
                        dataTableProfile.setStartDate(simulation.getStartDate());
                        dataTableProfile.setEndDate(simulation.getEndDate());
                        dataTableProfile.setMultiplier(1);
//                        dataTableProfile.setInterpolationMethod(InterpolationMethodEnum.LINEAR);
                        port.getProfile().add(dataTableProfile);
                    });
                });
            });
            String newEsdl = ESDLUtil.ecoreResourceToString((EnergySystem) xmiResource.getContents().getFirst());
            ObjectNode statusJson = ESSimEngine.getObjectMapper().createObjectNode();
            statusJson.put("esdl", Base64.getEncoder().encodeToString(newEsdl.getBytes(StandardCharsets.UTF_8)));
            return Response.status(Response.Status.OK).entity(statusJson).build();

        } catch (Exception e) {
            log.error("Error converting ESDL to time series profiles", e);
            error.setDescription(e.getMessage());
        }
        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
    }

    @NotNull
    private QuantityAndUnitReference getQuantityAndUnitReference(EnergySystem energySystem) {
        QuantityAndUnitType quantityAndUnitType = EsdlFactory.eINSTANCE.createQuantityAndUnitType();
        quantityAndUnitType.setUnit(UnitEnum.JOULE);
        quantityAndUnitType.setDescription("Energy in J");
        quantityAndUnitType.setMultiplier(MultiplierEnum.NONE);
        quantityAndUnitType.setPerUnit(UnitEnum.NONE);
        quantityAndUnitType.setPhysicalQuantity(PhysicalQuantityEnum.ENERGY);
        quantityAndUnitType.setPerTimeUnit(TimeUnitEnum.NONE);
        quantityAndUnitType.setPerMultiplier(MultiplierEnum.NONE);
        quantityAndUnitType.setPerScope(QuantityAndUnitScopeEnum.UNDEFINED);
        if (energySystem.getEnergySystemInformation().getQuantityAndUnits() == null) {
            energySystem.getEnergySystemInformation().setQuantityAndUnits(EsdlFactory.eINSTANCE.createQuantityAndUnits());
            energySystem.getEnergySystemInformation().getQuantityAndUnits().setId(UUID.randomUUID().toString());
        }
        energySystem.getEnergySystemInformation().getQuantityAndUnits().getQuantityAndUnit().add(quantityAndUnitType);
        QuantityAndUnitReference quantityAndUnitReference = EsdlFactory.eINSTANCE.createQuantityAndUnitReference();
        quantityAndUnitReference.setReference(quantityAndUnitType);
        return quantityAndUnitReference;
    }


}
