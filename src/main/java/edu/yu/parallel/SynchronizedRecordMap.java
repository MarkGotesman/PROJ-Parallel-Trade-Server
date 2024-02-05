package edu.yu.parallel;

import java.util.Collections;
import java.util.Map;

public class SynchronizedRecordMap<K, V> {
	private Map<K,V> map;
	SynchronizedRecordMap(Map<K,V> map) {
		this.map = map; // the map should begin with all keys that are needed for future use
	}
	public Map<K,V> getMap() {
		return Collections.unmodifiableMap(map); // NOTE: these seem unsafe as they expose the mutatable objects which are stored in the map (e.g., an Integer object used as the value) and could be modified by a client
	}

	public V getVal(K key) {
		return this.map.get(key); // NOTE: these seem unsafe as they expose the mutatable objects which are stored in the map (e.g., an Integer object used as the value) and could be modified by a client
	}

	public V getValOrDefault(K key, V def) {
		return this.map.getOrDefault(key, def);
	}

	public synchronized boolean setVal(K key, V valAssumed, V valNew) {
		V valCurrent = this.map.get(key);
		boolean assumption = (!map.containsKey(key)) || (valCurrent.equals(valAssumed));
		if (assumption == true) this.map.put(key, valNew);
		return assumption;
	} 
}
