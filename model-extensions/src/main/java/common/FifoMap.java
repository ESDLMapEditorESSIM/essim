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

import java.util.Iterator;
import java.util.LinkedHashMap;

public class FifoMap<K, V> extends LinkedHashMap<K, V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7904261827444352387L;
	int max;

	public FifoMap(int max) {
		super(max + 1);
		this.max = max;

	}

	@Override
	public V put(K key, V value) {
		V forReturn = super.put(key, value);
		if (super.size() > max) {
			removeEldest();
		}

		return forReturn;
	}

	private void removeEldest() {
		Iterator<K> iterator = this.keySet().iterator();
		if (iterator.hasNext()) {
			this.remove(iterator.next());
		}
	}

}