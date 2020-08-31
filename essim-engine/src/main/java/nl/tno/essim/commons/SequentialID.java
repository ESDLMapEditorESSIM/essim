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
