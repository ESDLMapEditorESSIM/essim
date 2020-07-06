package nl.tno.essim.managers;

import java.io.PrintWriter;
import java.io.StringWriter;
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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nl.tno.essim.ESSimEngine;
import nl.tno.essim.commons.ISimulationManager;
import nl.tno.essim.commons.IStatusProvider;
import nl.tno.essim.commons.Simulatable;
import nl.tno.essim.observation.IObservationConsumer;
import nl.tno.essim.time.EssimDuration;
import nl.tno.essim.time.EssimTime;
import nl.tno.essim.transportsolver.TransportSolver;

@Slf4j
public class SimulationManager implements ISimulationManager, IStatusProvider {

	private String name;
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

	public SimulationManager(ESSimEngine engine, String name, LocalDateTime startDateTime, LocalDateTime endDateTime,
			EssimDuration simStepLength) {
		this.engine = engine;
		this.name = name;
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
		solverTypeIndex = new AtomicInteger();
		solverBlock = new HashMap<Integer, List<TransportSolver>>();
		otherSims = new ArrayList<Simulatable>();
		time = new EssimTime(startDateTime, endDateTime, simStepLength);
		this.precheckTime = Duration.of(0, ChronoUnit.SECONDS);

		simulationExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime()
				.availableProcessors());

		log.debug("Simulation " + name + " is initialised!");
	}

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
		return name;
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
			barrier.await();

			Instant startTime = Instant.now();

			log.debug("Starting simulation");
			// Step through until end of simulation
			long start = time.getTime()
					.toEpochSecond(ZoneOffset.UTC);
			long end = endDateTime.toEpochSecond(ZoneOffset.UTC);
			while (time.getTime()
					.isBefore(endDateTime)
					|| time.getTime()
							.isEqual(endDateTime)) {
				Month thisMonth = time.getTime()
						.getMonth();
				if (thisMonth != currentMonth) {
					log.debug("Next timestep {}", time.getTime()
							.toString());
					currentMonth = thisMonth;
				}

				long step = time.getTime()
						.toEpochSecond(ZoneOffset.UTC);
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
			log.debug("Simulation finished and took {}", Duration.between(startTime, endTime)
					.plus(precheckTime)
					.toString());

			log.debug("Done!");
			engine.setFeatureCollections();
			simulationExecutor.shutdown();
		} catch (InterruptedException e) {
			log.error("Error in scheduled runnable", e);
			status = -1;
			description = e.getMessage();
			Thread.currentThread()
					.interrupt();
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
				log.error("Error in scheduled runnable", e);
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				status = -1;
				description = sw.toString();
				Thread.currentThread()
						.interrupt();
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
