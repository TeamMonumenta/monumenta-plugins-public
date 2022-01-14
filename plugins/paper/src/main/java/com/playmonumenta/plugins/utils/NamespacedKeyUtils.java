package com.playmonumenta.plugins.utils;

import org.bukkit.NamespacedKey;

public abstract class NamespacedKeyUtils {

	private NamespacedKeyUtils() {
	}

	/**
	 * Same as {@link NamespacedKey#fromString(String)}, but throws an IllegalArgumentException (instead of returning null) if the passed string is badly formatted.
	 */
	public static NamespacedKey fromString(String s) throws IllegalArgumentException {
		NamespacedKey key = NamespacedKey.fromString(s);
		if (key == null) {
			throw new IllegalArgumentException("Invalid namespaced key '" + s + "'");
		}
		return key;
	}

}
