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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProfileCache extends TimerTask {

	private static final long CACHE_INVALIDATION_PERIOD = TimeUnit.DAYS.toMillis(14); // 2 Weeks
	private static ProfileCache instance;
	private final HashMap<ProfileCacheKey, TimeSeriesDataCache> dataCaches = new HashMap<>();

	private ProfileCache() {
		// Check every 10 minutes
		new Timer("ProfileCacheCleanupTimer").scheduleAtFixedRate(this, 0, TimeUnit.MINUTES.toMillis(10));
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
		for (Iterator<Entry<ProfileCacheKey, TimeSeriesDataCache>> i = dataCaches.entrySet().iterator(); i.hasNext();) {
			Entry<ProfileCacheKey, TimeSeriesDataCache> entry = i.next();
			if (entry.getValue().getLastAccessed() + CACHE_INVALIDATION_PERIOD <= now) {
				log.debug("Profile cache entry {} has expired, removing", entry.getKey());
				i.remove();
			}
		}
		log.debug("Profile cache now contains {} entries", dataCaches.size());
	}

	public synchronized void cache(ProfileCacheKey profileCacheKey, TimeSeriesDataCache dataCache) {
		log.debug("Add profile cache entry {}", profileCacheKey);
		dataCaches.put(profileCacheKey, dataCache);
	}

	public synchronized TimeSeriesDataCache getDataCache(ProfileCacheKey profileCacheKey) {
		log.debug("Fetch profile cache entry {} for query '{}'", profileCacheKey, profileCacheKey);
		TimeSeriesDataCache cache = dataCaches.get(profileCacheKey);
		return cache;
	}
}
