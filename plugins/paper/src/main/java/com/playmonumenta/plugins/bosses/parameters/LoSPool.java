package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.libraryofsouls.SoulPoolHistoryEntry;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public abstract class LoSPool {

	public static class LibraryPool extends LoSPool {
		public static final LoSPool EMPTY = new LibraryPool("");

		private final String mPoolName;

		public LibraryPool(String poolName) {
			mPoolName = poolName;
		}

		@Override
		protected @Nullable Map<Soul, Integer> getMobs() {
			if (mPoolName.isEmpty()) {
				return null;
			}
			return LibraryOfSoulsIntegration.getPool(mPoolName);
		}

		@Override
		public String toString() {
			return mPoolName;
		}

	}

	public static class InlinePool extends LoSPool {

		private final SoulPoolHistoryEntry mEntry;

		public InlinePool(Map<String, Integer> souls) {
			mEntry = new SoulPoolHistoryEntry("synthetic_pool", -1, "", souls);
		}

		public InlinePool(String soul) {
			this(Map.of(soul, 1));
		}

		@Override
		protected @Nullable Map<Soul, Integer> getMobs() {
			return mEntry.getRandomSouls(FastUtils.RANDOM);
		}

		@Override
		public String toString() {
			return "inline_pool";
		}

	}

	protected abstract @Nullable Map<Soul, Integer> getMobs();

	/**
	 * Spawns a random mob from the pool, according to weights given
	 * (Do not use with a pool containing parties; it will always spawn exactly 1 mob)
	 *
	 * @param loc the location to spawn the mob
	 * @return The mob spawned
	 */
	public @Nullable Entity spawn(Location loc) {
		Map<Soul, Integer> mobsPool = getMobs();
		if (mobsPool != null) {
			for (Map.Entry<Soul, Integer> mob : mobsPool.entrySet()) {
				return mob.getKey().summon(loc);
			}
		}
		return null;
	}

	/**
	 * Spawns a random mob/party from the pool, according to weights given
	 * (Use this for pools containing parties)
	 *
	 * @param loc the location to spawn all the mobs
	 * @return A list of all mobs spawned
	 */
	public List<Entity> spawnAll(Location loc) {
		return spawnAll(loc, null);
	}

	/**
	 * Spawns a random mob/party from the pool, according to weights given
	 * (Use this for pools containing parties)
	 *
	 * @param loc the location to spawn all the mobs
	 * @param offset the maximum offset in each direction
	 * @return A list of all mobs spawned
	 */
	public List<Entity> spawnAll(Location loc, @Nullable Vector offset) {
		List<Entity> mobs = new ArrayList<>();
		Map<Soul, Integer> mobsPool = getMobs();
		if (mobsPool != null) {
			for (Map.Entry<Soul, Integer> mob : mobsPool.entrySet()) {
				for (int i = 0; i < mob.getValue(); i++) {
					Location spawnLocation = loc.clone();
					if (offset != null) {
						spawnLocation.add(FastUtils.randomDoubleInRange(-offset.getX(), offset.getX()), FastUtils.randomDoubleInRange(-offset.getY(), offset.getY()), FastUtils.randomDoubleInRange(-offset.getZ(), offset.getZ()));
					}
					mobs.add(mob.getKey().summon(spawnLocation));
				}
			}
		}
		return mobs;
	}

	public static LoSPool fromString(String string) {
		return Parser.parseOrDefault(Parser.getParserMethod(LoSPool.class), string, new LibraryPool(""));
	}
}
