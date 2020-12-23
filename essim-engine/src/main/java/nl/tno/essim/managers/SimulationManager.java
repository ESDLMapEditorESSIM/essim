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
import java.time.Month;
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
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.commons.ISimulationManager;
import nl.tno.essim.commons.IStatusProvider;
import nl.tno.essim.commons.Simulatable;
import nl.tno.essim.model.Status;
import nl.tno.essim.mongo.MongoBackend;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.transportsolver.TransportSolver;

@Slf4j
public class SimulationManager implements ISimulationManager, IStatusProvider {

	private static long TIME_OUT_IN_SEC;
	private String simulationId;
	private LocalDateTime startDateTime;
	private LocalDateTime endDateTime;
	private EssimTime time;
	private ThreadPoolExecutor simulationExecutor;
	private CountDownLatch barrier;
	private Duration precheckTime;
	private AtomicInteger solverTypeIndex;
	private int numOfSolvers;
	@Getter
	private double status;
	@Getter
	public String description;
	private List<IObservationConsumer> observationConsumers;
	private Month currentMonth;
	private ESSimEngine engine;
	private HashMap<Integer, List<TransportSolver>> solverBlock;
	private List<Simulatable> otherSims;
	@Getter
	private boolean started;
	private MongoBackend mongo;
	private ScheduledExecutorService statusUpdater;

	public SimulationManager(ESSimEngine engine, String simulationId, LocalDateTime startDateTime,
			LocalDateTime endDateTime, EssimDuration simStepLength) {
		String timeout = System.getenv("PROFILE_QUERY_TIMEOUT");
		if(timeout == null) {
			timeout = "45";
		}
		TIME_OUT_IN_SEC = Long.parseLong(timeout);
		this.engine = engine;
		this.simulationId = simulationId;
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		solverTypeIndex = new AtomicInteger();
		solverBlock = new HashMap<Integer, List<TransportSolver>>();
		otherSims = new ArrayList<Simulatable>();
		time = new EssimTime(startDateTime, endDateTime, simStepLength);
		this.precheckTime = Duration.of(0, ChronoUnit.SECONDS);
		this.mongo = MongoBackend.getInstance();

		simulationExecutor = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		statusUpdater = Executors.newScheduledThreadPool(1);
		statusUpdater.scheduleAtFixedRate(statusUpdaterService, 0, 1, TimeUnit.SECONDS);

		log.debug("Simulation " + simulationId + " is initialised!");
	}

	private Runnable statusUpdaterService = new Runnable() {
		@Override
		public void run() {
			mongo.updateSimulationStatus(simulationId, Status.RUNNING, String.valueOf(status));
		}
	};

	/**
	 * @return the startDateTime
	 */
	@Override
	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}

	/**
	 * @return the endDateTime
	 */
	@Override
	public LocalDateTime getEndDateTime() {
		return endDateTime;
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

	public void startSimulation() {
		started = true;
		try {
			// Run init() for all simulatables
			barrier = new CountDownLatch(numOfSolvers);
			for (List<TransportSolver> simulatables : solverBlock.values()) {
				for (TransportSolver solver : simulatables) {
					simulationExecutor.submit(new ExceptionRunnable(() -> {
						solver.init(time);
					}));
				}
			}
			// Wait until everybody is initialised
			boolean result = barrier.await(TIME_OUT_IN_SEC, TimeUnit.SECONDS);
			if (!result) {
				throw new IllegalStateException("Error in Simulation Init: " + description);
			}

			Instant startTime = Instant.now();

			log.debug("Starting simulation");
			// Step through until end of simulation
			long start = time.getTime().toEpochSecond(ZoneOffset.UTC);
			long end = endDateTime.toEpochSecond(ZoneOffset.UTC);
			while (time.getTime().isBefore(endDateTime) || time.getTime().isEqual(endDateTime)) {
				Month thisMonth = time.getTime().getMonth();
				if (thisMonth != currentMonth) {
					log.debug("Next timestep {}", time.getTime().toString());
					currentMonth = thisMonth;
				}

				long step = time.getTime().toEpochSecond(ZoneOffset.UTC);
				status = ((double) (step - start)) / ((double) (end - start));

				for (Integer simulatableType : solverBlock.keySet()) {
					List<TransportSolver> simulatablesOfSameType = solverBlock.get(simulatableType);
					barrier = new CountDownLatch(simulatablesOfSameType.size());
					for (Simulatable simulatable : simulatablesOfSameType) {
						simulationExecutor.submit(new ExceptionRunnable(() -> {
							simulatable.step(time);
						}));
					}

					barrier.await();
				}

				barrier = new CountDownLatch(otherSims.size());
				for (Simulatable simulatable : otherSims) {
					simulationExecutor.submit(new ExceptionRunnable(() -> {
						simulatable.step(time);
					}));
				}
				barrier.await();

				time = time.nextTimeStep();
			}

			barrier = new CountDownLatch(numOfSolvers);
			for (Integer simTypeNumber : solverBlock.keySet()) {
				for (Simulatable simulatable : solverBlock.get(simTypeNumber)) {
					simulationExecutor.submit(new ExceptionRunnable(() -> {
						simulatable.stop();
					}));
				}
			}
			barrier.await();

			log.debug("Please wait while the data is flushed to the database. This could take a few seconds...");

			if (observationConsumers != null) {
				for (IObservationConsumer consumer : observationConsumers) {
					consumer.cleanup();
				}
			}

			Instant endTime = Instant.now();
			String simDuration = Duration.between(startTime, endTime).plus(precheckTime).toString();
			log.debug("Simulation finished and took {}", simDuration);

			log.debug("Done!");
			statusUpdater.shutdownNow();
			mongo.updateSimulationStatus(simulationId, Status.COMPLETE, "Finished in " + simDuration);
			mongo.updateStatus("Ready");
			engine.setFeatureCollections();
			simulationExecutor.shutdownNow();
		} catch (Exception e) {
			statusUpdater.shutdownNow();
			log.error("Error in scheduled runnable", e);
			status = -1;
			description = e.getMessage();
			mongo.updateSimulationStatus(simulationId, Status.ERROR, String.valueOf(description));
			mongo.updateStatus("Ready");
			simulationExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}

	}

	/**
	 * Wraps a runnable with support for a barrier/latch and exception handling
	 * 
	 * @author werkmane
	 *
	 */
	public class ExceptionRunnable implements Runnable {
		private Runnable runnable;

		public ExceptionRunnable(Runnable r) {
			this.runnable = r;
		}

		@Override
		public void run() {
			try {
				runnable.run();
				barrier.countDown();
			} catch (Exception e) {
				statusUpdater.shutdownNow();
				log.error("Error in scheduled runnable", e);
				status = -1;
				description = e.getMessage();
				mongo.updateSimulationStatus(simulationId, Status.ERROR, String.valueOf(description));
//				mongo.updateStatus("Ready");
//				simulationExecutor.shutdownNow();
//				Thread.currentThread().interrupt();
			}
		}

	}

	public void startSimulation(Duration precheckTime) throws InterruptedException {
		this.precheckTime = precheckTime;
		startSimulation();
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
