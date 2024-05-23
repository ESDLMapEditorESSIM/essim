package nl.tno.essim.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.commons.Commons;
import nl.tno.essim.kpi.KPIManager;
import nl.tno.essim.kpi.data.KPIStatus;
import nl.tno.essim.model.*;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.rest.util.MongoInject;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Tag(name = "ESSIM")
@Path("/simulation")
@Slf4j
public class SimulationService {
    private static final String ESSIM_ID = System.getProperty(Commons.ESSIM_REVISION);
    @MongoInject
    private IMongoBackend mongo;

    @POST
    @Produces("application/json")
    @Consumes("application/json")
    @Operation(summary = "Start a new simulation",
            description = "Start a new Simulation with the provided ESDL",
            requestBody = @RequestBody(description = "Simulation description", content = @Content(schema = @Schema(implementation = EssimSimulation.class))),
            responses = {
                    @ApiResponse(description = "The simulation status", content = @Content(schema = @Schema(implementation = KPIStatus.class))),
                    @ApiResponse(responseCode = "400", description = "Error", content = @Content(schema = @Schema(implementation = ErrorStatus.class)))
            })
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

        String simId = null;
        try {
            if (mongo != null) {
                SimulationStatus status = new SimulationStatus();
                status.setState(Status.CREATED);
                status.setDescription("");
                simulation.setStatus(status);
                simId = mongo.addSimulation(simulation);

                log.debug("Simulation ID : {}", simId);

                Date now = new Date();
                simulation.setSimRunDate(now);
                ESSimEngine engine = new ESSimEngine(ESSIM_ID, simId, simulation, decodedContents, mongo);
                String grafanaDashboard = engine.createGrafanaDashboard();
                if (grafanaDashboard != null) {
                    simulation.setDashboardURL(grafanaDashboard);
                } else {
                    simulation.setDashboardURL("*headless simulation*");
                }
                mongo.updateSimulationData(simId, simulation);

                Thread thread = new Thread(engine::startSimulation, simId);
                thread.setUncaughtExceptionHandler(new EssimExceptionHandler(mongo));
                thread.start();

                mongo.getStatusMap().put(simId, engine);

                CreatedStatus created = new CreatedStatus();
                created.setStatus(Status.CREATED);
                created.setId(simId);

                return Response.status(Response.Status.CREATED).entity(created).build();
            } else {
                error.setDescription("MongoDB internal error!");
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            log.error(e.getMessage(), e);
            error.setDescription("Internal error: " + sw.toString());
            mongo.updateSimulationStatus(simId, Status.ERROR, sw.toString());
            mongo.updateStatus("Ready");
        }

        return Response.status(Response.Status.BAD_REQUEST).entity(error).build();

    }

    @GET
    @Produces("application/json")
    @Operation(summary = "Get all simulations",
            description = "Get all simulations. BE CAREFUL! this can be a lot of data!",
            responses = {
                    @ApiResponse(description = "The list of simulations", content = @Content(array = @ArraySchema(schema = @Schema(implementation = EssimSimulation.class)))),
            })
    public Response getSimulations() {
        List<EssimSimulation> simulations = mongo.getSimulations();
        return Response.ok(simulations).build();
    }

    @GET
    @Path("/status")
    @Produces("text/plain")
    @Operation(summary = "Get the status of ESSIM",
            description = "Get the status of ESSIM",
            responses = {
                    @ApiResponse(description = "The simulation status", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "503", description = "Service unavailable", content = @Content(schema = @Schema(implementation = String.class)))

            })
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
    @Operation(summary = "Get a list of available KPI modules",
            description = "Get a list of available KPI modules",
            responses = {
                    @ApiResponse(description = "List of KPI modules", content = @Content(array = @ArraySchema(schema = @Schema(implementation = KPIModuleInfo.class)))),
            })
    public Response getSimulationKpiModules() {
        KPIManager.getInstance().queryStatus();
        List<KPIModuleInfo> allKPIModules = KPIManager.getInstance().getAllKPIModules();
        return Response.ok(allKPIModules).build();
    }

    @GET
    @Path("/{simulationId}")
    @Produces("application/json")
    @Operation(summary = "Get a simulation",
            description = "Get the information of a single simulation",
            responses = {
                    @ApiResponse(description = "The simulation information", content = @Content(schema = @Schema(implementation = EssimSimulation.class))),
                    @ApiResponse(responseCode = "404", description = "Simulation id not found", content = @Content(schema = @Schema(implementation = ErrorStatus.class)))
            })
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

}
