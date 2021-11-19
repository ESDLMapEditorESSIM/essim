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

package common;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProfileCache extends TimerTask {

	private static final long CACHE_INVALIDATION_PERIOD = 2 * 60 * 60 * 1000; // 2 hours
	private static ProfileCache instance;
	private ConcurrentHashMap<Integer, TimeSeriesDataCache> dataCaches;
	private static AtomicInteger counter;

	private ProfileCache() {
		dataCaches = new ConcurrentHashMap<Integer, TimeSeriesDataCache>();
		counter = new AtomicInteger(0);
		new Timer("Timer").scheduleAtFixedRate(this, 0, CACHE_INVALIDATION_PERIOD);
	}

	public static synchronized ProfileCache getInstance() {
		if (instance == null) {
			synchronized (ProfileCache.class) {
				instance = new ProfileCache();
			}
		}
		counter.incrementAndGet();
		return instance;
	}

	@Override
	public void run() {
		// Invalidate Cache if nobody is querying
		log.debug("Trying to invalidate profile cache... Number of profile accessors: " + counter.get());
		if (counter.get() <= 0) {
			log.debug("Cleared!");
			dataCaches.clear();
		}
	}

	public synchronized void cache(String query, TimeSeriesDataCache dataCache) {
		dataCaches.put(getQueryHash(query), dataCache);
	}

	public synchronized boolean isCached(String query) {
		return dataCaches.containsKey(getQueryHash(query));
	}

	public synchronized TimeSeriesDataCache getDataCache(String query) {
		return dataCaches.get(getQueryHash(query));
	}

	private int getQueryHash(String query) {
		int hash = Hashing.murmur3_32().newHasher().putString(query, Charsets.UTF_8).hash().asInt();
		return hash;
	}

	public synchronized void giveUp() {
		counter.decrementAndGet();
	}
}
