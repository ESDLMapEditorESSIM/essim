package nl.tno.essim.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
    public Response getSimulation() {
        List<EssimSimulation> simulations = mongo.getSimulations();
        return Response.ok(simulations).build();
    }

    @GET
    @Path("/status")
    @Produces("application/json")
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
    public Response getSimulationKpiModules() {
        KPIManager.getInstance().queryStatus();
        List<KPIModuleInfo> allKPIModules = KPIManager.getInstance().getAllKPIModules();
        return Response.ok(allKPIModules).build();
    }

    @GET
    @Path("/{simulationId}")
    @Produces("application/json")
    public Response getSimulationBySimulationId(
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
    public Response putSimulationBySimulationId(
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
    public Response getSimulationKpiBySimulationId(
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
                    ArrayNode arrayNode = ESSimEngine.getObjectMapper().createArrayNode();
                    for (KPIModule module : kpiModule.getModules()) {
                        if (module.getId() != null) {
                            arrayNode.add(getKPIResult(simulationId, module));
                        }
                    }
                    log.trace("Sending {} ", arrayNode.toString());
                    return Response.ok(arrayNode).build();
                }
            }
        }
        return Response.status(Response.Status.NOT_FOUND).entity(error).build();
    }

    @GET
    @Path("/{simulationId}/kpi/{kpiId}")
    @Produces("application/json")
    public Response getSimulationKpiBySimulationIdAndKpiId(
            @PathParam("simulationId") String simulationId, @PathParam("kpiId") String kpiId) {
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
    public Response getSimulationStatusBySimulationId(@PathParam("simulationId") String simulationId) {
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
    public Response getSimulationTransportBySimulationId(
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
    public Response getSimulationTransportBySimulationIdAndIndex(@PathParam("simulationId") String simulationId, @PathParam("index") String index) {
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
