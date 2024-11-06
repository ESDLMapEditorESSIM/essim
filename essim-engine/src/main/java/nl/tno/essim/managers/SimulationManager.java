/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

package nl.tno.essim.managers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.commons.ISimulationManager;
import nl.tno.essim.commons.IStatusProvider;
import nl.tno.essim.commons.Simulatable;
import nl.tno.essim.loadflow.EssimLoadFlowController;
import nl.tno.essim.model.Status;
import nl.tno.essim.mongo.IMongoBackend;
import nl.tno.essim.mso.MSOClient;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.transportsolver.TransportSolver;

@Slf4j
public class SimulationManager implements ISimulationManager, IStatusProvider {

	private final static long TIME_OUT_IN_SEC = getTimeout();
	private String simulationId;
	private EssimTime time;
	private ThreadPoolExecutor simulationExecutor;
	private AtomicInteger solverTypeIndex;
	private int numOfSolvers;
	@Getter
	private double status;
	@Getter
	public String description;
	private List<IObservationConsumer> observationConsumers;
	private HashMap<Integer, List<TransportSolver>> solverBlock;
	private List<Simulatable> otherSims;
	@Getter
	private boolean started;
	private IMongoBackend mongo;
	private String interruptedCause;
	private ScheduledExecutorService statusUpdater;
	private boolean interrupted;
	private MSOClient msoClient;
	private CountDownLatch msoBarrier;
	private long modelDeployTimeout;
	@Getter
	@Setter
	private EssimLoadFlowController loadFlowController;

	private static long getTimeout() {
		String timeout = System.getenv("PROFILE_QUERY_TIMEOUT");
		if (timeout == null) {
			return 45;
		}
		return Long.parseLong(timeout);
	}

	public SimulationManager(String simulationId, LocalDateTime startDateTime, LocalDateTime endDateTime,
			EssimDuration simStepLength, IMongoBackend mongo) {
		this.mongo = mongo;
		this.simulationId = simulationId;
		solverTypeIndex = new AtomicInteger();
		solverBlock = new HashMap<Integer, List<TransportSolver>>();
		otherSims = new ArrayList<Simulatable>();
		time = new EssimTime(startDateTime, endDateTime, simStepLength);
		interrupted = false;
		interruptedCause = "";

		simulationExecutor = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		statusUpdater = Executors.newScheduledThreadPool(1);
		statusUpdater.scheduleAtFixedRate(
				() -> mongo.updateSimulationStatus(simulationId, Status.RUNNING, String.valueOf(status)), 0, 1,
				TimeUnit.SECONDS);

		log.debug("Simulation " + simulationId + " is initialised!");
	}

	/**
	 * @return the startDateTime
	 */
	@Override
	public LocalDateTime getStartDateTime() {
		return time.getSimulationStartTime();
	}

	/**
	 * @return the endDateTime
	 */
	@Override
	public LocalDateTime getEndDateTime() {
		return time.getSimulationEndTime();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see nl.tno.dido.simulation.ISimulationManager#getName())
	 */
	@Override
	public String getName() {
		return simulationId;
	}

	@Override
	public void addSolvers(List<TransportSolver> solvers) {
		synchronized (solverBlock) {
			solverBlock.put(solverTypeIndex.incrementAndGet(), solvers);
			this.numOfSolvers += solvers.size();
		}
	}

	public void addOtherSimulatable(Simulatable sim) {
		synchronized (otherSims) {
			otherSims.add(sim);
		}
	}

	public void interrupt(String cause) {
		interrupted = true;
		interruptedCause = cause;
	}

	public void startSimulation() {
		startSimulation(Duration.of(0, ChronoUnit.SECONDS));
	}

	public void startSimulation(Duration precheckTime) {
		try {
			if (msoClient != null) {
				log.debug("Waiting for {} seconds for external models to be deployed.", modelDeployTimeout);
				boolean result = msoBarrier.await(modelDeployTimeout, TimeUnit.SECONDS);
				if (!result) {
					throw new IllegalStateException("Error in Simulation Init: " + description);
				}
			}

			started = true;
			// Run init() for all simulatables
			initializeSolvers();

			Instant startTime = Instant.now();

			log.info("Starting simulation {}", simulationId);
			// Step through until end of simulation
			long start = time.getTime().toEpochSecond(ZoneOffset.UTC);
			long end = time.getSimulationEndTime().toEpochSecond(ZoneOffset.UTC);
			double log_stat = 0.0;
			while (time.getTime().isBefore(time.getSimulationEndTime())
					|| time.getTime().isEqual(time.getSimulationEndTime())) {
				if (interrupted) {
					throw new InterruptedException(interruptedCause);
				}

				long step = time.getTime().toEpochSecond(ZoneOffset.UTC);
				status = ((double) (step - start)) / ((double) (end - start));
				if (status >= log_stat) {
					log.debug("Next timestep {} [{} %]", time.getTime().toString(),
							String.format("%.0f", status * 100));
					log_stat = log_stat + 0.1;
				}
				runStep();
				runOtherSimulatables();
				time = time.nextTimeStep();
			}
			stopSolvers();

			if (observationConsumers != null) {
				log.debug("Please wait while the data is flushed to the database. This could take a few seconds...");
				for (IObservationConsumer consumer : observationConsumers) {
					consumer.cleanup();
				}
			}

			if (msoClient != null) {
				log.debug("Simulation done. Waiting for external models to terminate.");
				msoClient.simulationDone();
			}

			Instant endTime = Instant.now();
			String simDuration = Duration.between(startTime, endTime).plus(precheckTime).toString();
			log.debug("Simulation finished and took {}", simDuration);
			mongo.updateSimulationStatus(simulationId, Status.COMPLETE, "Finished in " + simDuration);
			mongo.updateStatus("Ready");
			shutdown();
		} catch (Exception e) {
			statusUpdater.shutdownNow();
			log.error("Error in scheduled runnable", e);
			status = -1;
			description = e.getMessage();
			mongo.updateSimulationStatus(simulationId, Status.ERROR, String.valueOf(description));
			mongo.updateStatus("Ready");
			shutdown();
			Thread.currentThread().interrupt();
		}

	}

	private void stopSolvers() throws InterruptedException {
		CountDownLatch barrier = new CountDownLatch(numOfSolvers);
		log.trace("Stopping {} solvers", numOfSolvers);
		for (Integer simTypeNumber : solverBlock.keySet()) {
			for (Simulatable simulatable : solverBlock.get(simTypeNumber)) {
				simulationExecutor.submit(new ExceptionRunnable(barrier, () -> {
					simulatable.stop();
				}));
			}
		}
		barrier.await();
	}

	private void runOtherSimulatables() throws InterruptedException {
		// log.trace("Running {} other simulatables", otherSims.size());
		CountDownLatch barrier = new CountDownLatch(otherSims.size());
		for (Simulatable simulatable : otherSims) {
			simulationExecutor.submit(new ExceptionRunnable(barrier, () -> {
				simulatable.step(time);
			}));
		}
		barrier.await();
	}

	private void runStep() throws InterruptedException {
		for (Integer simulatableType : solverBlock.keySet()) {
			List<TransportSolver> simulatablesOfSameType = solverBlock.get(simulatableType);
//					log.trace("Running {} steps", simulatablesOfSameType.size());
			CountDownLatch barrier = new CountDownLatch(simulatablesOfSameType.size());
			for (Simulatable simulatable : simulatablesOfSameType) {
				simulationExecutor.submit(new ExceptionRunnable(barrier, () -> {
					simulatable.step(time);
				}));
			}
			barrier.await();
		}
		if (loadFlowController != null) {
			loadFlowController.run();
		}
	}

	private void initializeSolvers() throws InterruptedException {
		log.debug("Initializing {} solvers", numOfSolvers);
		CountDownLatch barrier = new CountDownLatch(numOfSolvers);
		for (List<TransportSolver> simulatables : solverBlock.values()) {
			for (TransportSolver solver : simulatables) {
				simulationExecutor.submit(new ExceptionRunnable(barrier, () -> {
					solver.init(time);
				}));
			}
		}
		// Wait until everybody is initialised
		boolean result = barrier.await(TIME_OUT_IN_SEC, TimeUnit.SECONDS);
		if (!result) {
			throw new IllegalStateException("Error in Simulation Init: " + description);
		}
	}

	public void shutdown() {
		statusUpdater.shutdownNow();
		simulationExecutor.shutdownNow();
	}

	public void setMSOClient(MSOClient msoClient, CountDownLatch msoBarrier, long modelDeployTimeout) {
		this.msoClient = msoClient;
		this.msoBarrier = msoBarrier;
		this.modelDeployTimeout = modelDeployTimeout;
	}

	/**
	 * Wraps a runnable with support for a barrier/latch and exception handling
	 * 
	 * @author werkmane
	 *
	 */
	public class ExceptionRunnable implements Runnable {
		private Runnable runnable;
		private CountDownLatch countDownLatch;

		public ExceptionRunnable(CountDownLatch barrier, Runnable r) {
			countDownLatch = barrier;
			this.runnable = r;
		}

		@Override
		public void run() {
			try {
				runnable.run();
				countDownLatch.countDown();
			} catch (Throwable e) {
				statusUpdater.shutdownNow();
				log.error("Error in scheduled runnable", e);
				status = -1;
				description = e.getMessage();
				mongo.updateSimulationStatus(simulationId, Status.ERROR, String.valueOf(description));
				mongo.updateStatus("Ready");
				simulationExecutor.shutdownNow();
//				Thread.currentThread().interrupt();
			}
		}

	}

	@Override
	public void setObservationConsumers(List<IObservationConsumer> consumers) {
		this.observationConsumers = consumers;
	}

	public void addObservationConsumer(IObservationConsumer consumer) {
		if (observationConsumers == null) {
			observationConsumers = new ArrayList<IObservationConsumer>();
		}
		observationConsumers.add(consumer);
	}
}
