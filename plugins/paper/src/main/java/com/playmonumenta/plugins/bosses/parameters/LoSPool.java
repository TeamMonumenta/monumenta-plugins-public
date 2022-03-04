package com.playmonumenta.plugins.bosses.parameters;

import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import dev.jorel.commandapi.Tooltip;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class LoSPool {

	private String mPoolName;

	public LoSPool(String poolName) {
		mPoolName = poolName;
	}


	public @Nullable Entity spawn(Location loc) {
		Map<Soul, Integer> mobsPool = LibraryOfSoulsIntegration.getPool(mPoolName);
		if (mobsPool != null) {
			for (Map.Entry<Soul, Integer> mob : mobsPool.entrySet()) {
				return mob.getKey().summon(loc);
			}
		}

		return null;

	}

	public static final LoSPool EMPTY = new LoSPool("");

	@Override
	public String toString() {
		return mPoolName;
	}

	public static LoSPool fromString(String string) {
		ParseResult<LoSPool> result = fromReader(new StringReader(string), "");
		if (result.getResult() == null) {
			Plugin.getInstance().getLogger().warning("Failed to parse '" + string + "' as LoSPool");
			Thread.dumpStack();
			return new LoSPool("");
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
		Set<String> mobsPool = LibraryOfSoulsIntegration.getPoolNames();
		@Nullable String mobPool = reader.readOneOf(mobsPool);
		if (mobPool == null) {
			List<Tooltip<String>> suggArgs = new ArrayList<>(mobsPool.size());
			String soFar = reader.readSoFar();
			for (String valid : mobsPool) {
				suggArgs.add(Tooltip.of(soFar + valid, hoverDescription));
			}
			return ParseResult.of(suggArgs.toArray(Tooltip.arrayOf()));
		}

		return ParseResult.of(new LoSPool(mobPool));
	}

}
