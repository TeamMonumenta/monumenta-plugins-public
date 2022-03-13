package com.playmonumenta.plugins.integrations;

import com.playmonumenta.libraryofsouls.LibraryOfSoulsAPI;
import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

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

	public static @Nullable Set<String> getSoulNames() {
		if (ENABLED) {
			return LibraryOfSoulsAPI.getSoulNames();
		}
		return null;
	}

	public static @Nullable Set<String> getSoulLocations() {
		if (ENABLED) {
			return LibraryOfSoulsAPI.getSoulLocations();
		}
		return null;
	}

	public static @Nullable Set<String> getPoolNames() {
		if (ENABLED) {
			return LibraryOfSoulsAPI.getSoulPoolNames();
		}
		return null;
	}

	public static @Nullable Set<String> getGroupNames() {
		if (ENABLED) {
			return LibraryOfSoulsAPI.getSoulGroupNames();
		}
		return null;
	}

	public static @Nullable Set<String> getPartyNames() {
		if (ENABLED) {
			return LibraryOfSoulsAPI.getSoulPartyNames();
		}
		return null;
	}

	public static @Nullable Map<Soul, Integer> getPool(String pool) {
		if (ENABLED) {
			return LibraryOfSoulsAPI.getRandomSouls(pool, FastUtils.RANDOM);
		}
		return null;
	}
}
