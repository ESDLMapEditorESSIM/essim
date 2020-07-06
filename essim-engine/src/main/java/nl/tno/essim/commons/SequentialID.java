package nl.tno.essim.commons;

import java.util.concurrent.atomic.AtomicInteger;

public class SequentialID {
	
	private static AtomicInteger counter;
	private static SequentialID instance;

	public synchronized static SequentialID getInstance() {
		if(instance == null) {
			instance = new SequentialID();
		}
		return instance;
	}
	
	private SequentialID() {
		counter = new AtomicInteger(0);
	}
	
	public int getNewIndex() {
		return counter.incrementAndGet();
	}
}
