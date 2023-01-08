package com.playmonumenta.plugins.integrations;

import com.playmonumenta.libraryofsouls.LibraryOfSoulsAPI;
import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class LibraryOfSoulsIntegration {
	private static boolean ENABLED = false;

	public LibraryOfSoulsIntegration(Logger logger) {
		logger.info("Enabling LibraryOfSouls integration");
		ENABLED = true;
	}

	public static @Nullable Entity summon(Location loc, String soulName) {
		if (ENABLED) {
			return LibraryOfSoulsAPI.summon(loc, soulName);
		}
		return null;
	}

	public static Set<String> getSoulNames() {
		if (!ENABLED) {
			return Set.of();
		}
		return LibraryOfSoulsAPI.getSoulNames();
	}

	public static Set<String> getSoulLocations() {
		if (!ENABLED) {
			return Set.of();
		}
		return LibraryOfSoulsAPI.getSoulLocations();
	}

	public static Set<String> getPoolNames() {
		if (!ENABLED) {
			return Set.of();
		}
		return LibraryOfSoulsAPI.getSoulPoolNames();
	}

	public static Set<String> getGroupNames() {
		if (!ENABLED) {
			return Set.of();
		}
		return LibraryOfSoulsAPI.getSoulGroupNames();
	}

	public static Set<String> getPartyNames() {
		if (!ENABLED) {
			return Set.of();
		}
		return LibraryOfSoulsAPI.getSoulPartyNames();
	}

	public static Map<Soul, Integer> getPool(String pool) {
		if (!ENABLED) {
			return Map.of();
		}
		return LibraryOfSoulsAPI.getRandomSouls(pool, FastUtils.RANDOM);
	}
}
