package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.libraryofsouls.SoulPoolHistoryEntry;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public abstract class LoSPool {

	public static final LoSPool EMPTY = new LibraryPool("");

	public static class LibraryPool extends LoSPool {

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
		ParseResult<LoSPool> result = fromReader(new StringReader(string), "");
		if (result.getResult() == null) {
			Plugin.getInstance().getLogger().warning("Failed to parse '" + string + "' as LoSPool");
			Thread.dumpStack();
			return new LibraryPool("");
		}

		return result.getResult();
	}

	/*
	 * Parses an MobPool at the next position in the StringReader.
	 * If this item parses successfully:
	 *   The returned ParseResult will contain a non-null getResult() and a null getTooltip()
	 *   The reader will be advanced to the next character past this EffectsList value.
	 * Else:
	 *   The returned ParseResult will contain a null getResult() and a non-null getTooltip()
	 *   The reader will not be advanced
	 */
	public static ParseResult<LoSPool> fromReader(StringReader reader, String hoverDescription) {
		if (reader.advance("[")) {
			Set<String> soulNames = LibraryOfSoulsIntegration.getSoulNames();
			Map<String, Integer> parsedSouls = new HashMap<>();
			while (true) {
				String soulName = reader.readOneOf(soulNames);
				if (soulName == null) {
					List<Tooltip<String>> suggArgs = new ArrayList<>(soulNames.size());
					String soFar = reader.readSoFar();
					for (String valid : soulNames) {
						suggArgs.add(Tooltip.ofString(soFar + valid, hoverDescription));
					}
					return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
				}
				int weight = 100;
				if (reader.advance(":")) {
					Long readWeight = reader.readLong();
					if (readWeight == null) {
						return ParseResult.of(Tooltip.arrayOf(Tooltip.ofString(reader.readSoFar() + "100", "Weight of the soul in the pool")));
					}
					weight = (int) (long) readWeight;
				}
				parsedSouls.put(soulName, weight);
				if (reader.advance("]")) {
					return ParseResult.of(new InlinePool(parsedSouls));
				}
				if (!reader.advance(",")) {
					return ParseResult.of(Tooltip.arrayOf(
						Tooltip.ofString(reader.readSoFar() + ":", "define the soul's weight in the pool"),
						Tooltip.ofString(reader.readSoFar() + ",", ""),
						Tooltip.ofString(reader.readSoFar() + "]", "")));
				}
			}
		}
		Set<String> mobsPool = LibraryOfSoulsIntegration.getPoolNames();
		String mobPool = reader.readOneOf(mobsPool);
		if (mobPool == null) {
			List<Tooltip<String>> suggArgs = new ArrayList<>(mobsPool.size() + 1);
			String soFar = reader.readSoFar();
			suggArgs.add(Tooltip.ofString(soFar + "[", hoverDescription));
			for (String valid : mobsPool) {
				suggArgs.add(Tooltip.ofString(soFar + valid, hoverDescription));
			}
			return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
		}

		return ParseResult.of(new LibraryPool(mobPool));
	}

}
