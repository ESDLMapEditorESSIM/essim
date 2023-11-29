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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProfileCache extends TimerTask {

	private static final long CACHE_INVALIDATION_PERIOD = TimeUnit.HOURS.toMillis(2); // 2 Hours
	private static ProfileCache instance;
	private final HashMap<Integer, TimeSeriesDataCache> dataCaches = new HashMap<>();

	private ProfileCache() {
		new Timer("ProfileCacheCleanupTimer").scheduleAtFixedRate(this, 0, TimeUnit.MINUTES.toMillis(10)); // Check every 10 minutes
	}

	public static synchronized ProfileCache getInstance() {
		if (instance == null) {
			instance = new ProfileCache();
		}
		return instance;
	}

	@Override
	public synchronized void run() {
		long now = System.currentTimeMillis();
		for (Iterator<Entry<Integer, TimeSeriesDataCache>> it = dataCaches.entrySet().iterator(); it.hasNext();) {
			Entry<Integer, TimeSeriesDataCache> entry = it.next();
			if (entry.getValue().getLastAccessed() + CACHE_INVALIDATION_PERIOD <= now) {
				log.debug("Profile cache entry {} has expired, removing", Integer.toHexString(entry.getKey()));
				it.remove();
			}
		}
	}

	public synchronized void cache(String query, TimeSeriesDataCache dataCache) {
		int queryHash = getQueryHash(query);
		log.debug("Add profile cache entry {} for query '{}'", Integer.toHexString(queryHash), query);
		dataCaches.put(queryHash, dataCache);
	}

	public synchronized TimeSeriesDataCache getDataCache(String query) {
		int queryHash = getQueryHash(query);
		log.debug("Fetch profile cache entry {} for query '{}'", Integer.toHexString(queryHash), query);
		TimeSeriesDataCache cache = dataCaches.get(queryHash);
		return cache;
	}

	private static int getQueryHash(String query) {
		int hash = Hashing.murmur3_32().newHasher().putString(query, Charsets.UTF_8).hash().asInt();
		return hash;
	}
}
