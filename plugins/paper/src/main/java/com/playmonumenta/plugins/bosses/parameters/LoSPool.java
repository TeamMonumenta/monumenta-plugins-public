package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.libraryofsouls.SoulPoolHistoryEntry;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
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

	public @Nullable Entity spawn(Location loc) {
		Map<Soul, Integer> mobsPool = getMobs();
		if (mobsPool != null) {
			for (Map.Entry<Soul, Integer> mob : mobsPool.entrySet()) {
				return mob.getKey().summon(loc);
			}
		}
		return null;
	}

	public static LoSPool fromString(String string) {
		return Parser.parseOrDefault(Parser.getParserMethod(LoSPool.class), string, new LibraryPool(""));
	}
}
