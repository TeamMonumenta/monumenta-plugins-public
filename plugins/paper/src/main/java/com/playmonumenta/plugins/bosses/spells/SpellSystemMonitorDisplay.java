package com.playmonumenta.plugins.bosses.spells;

import static com.playmonumenta.plugins.Constants.Tags.REMOVE_ON_UNLOAD;

import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.plugins.bosses.bosses.SystemMonitorDisplayBoss;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.shardhealth.ShardHealth;
import java.util.TreeMap;
import java.util.TreeSet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.HSVLike;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

public class SpellSystemMonitorDisplay extends Spell {
	private static final String SHARD_HEADER = "Shard";
	private static final String HEALTH_HEADER = "Health";
	private static final String MEMORY_HEADER = "Memory";
	private static final String TICK_HEADER = "Idle Time";
	private static final int HEALTH_COL_LENGTH = HEALTH_HEADER.length();
	private static final int MEMORY_COL_LENGTH = MEMORY_HEADER.length();
	private static final int TICK_COL_LENGTH = TICK_HEADER.length();

	private final Entity mBoss;

	public SpellSystemMonitorDisplay(final Entity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		TextDisplay textDisplay = null;
		double closestDistanceSquared = Double.MAX_VALUE;
		for (TextDisplay testEntity : loc.getNearbyEntitiesByType(TextDisplay.class, 1.0)) {
			if (!testEntity.getScoreboardTags().contains(SystemMonitorDisplayBoss.identityTag)) {
				continue;
			}

			double distanceSquared = loc.distanceSquared(testEntity.getLocation());
			if (distanceSquared < closestDistanceSquared) {
				textDisplay = testEntity;
				closestDistanceSquared = distanceSquared;
			}
		}
		if (textDisplay == null) {
			textDisplay = loc.getWorld().spawn(loc, TextDisplay.class);
			textDisplay.addScoreboardTag(SystemMonitorDisplayBoss.identityTag);
			textDisplay.addScoreboardTag(REMOVE_ON_UNLOAD);
			textDisplay.setBackgroundColor(Color.fromARGB(255, 0x2E, 0x34, 0x36));
		}

		ShardHealth yellowHealth = ShardHealth.defaultTargetHealth();

		int maxShardNameLength = SHARD_HEADER.length();
		TreeSet<String> shardNames = new TreeSet<>(NetworkRelayAPI.getOnlineShardNames());
		for (String shardName : shardNames) {
			maxShardNameLength = Math.max(maxShardNameLength, shardName.length());
		}

		int lineWidth = 10 + maxShardNameLength + HEALTH_COL_LENGTH + MEMORY_COL_LENGTH + TICK_COL_LENGTH;
		// ...of course this is in pixels; just going to set it wider than needed
		textDisplay.setLineWidth(16 * lineWidth);

		Component header = Component.text("")
			.font(Key.key("minecraft:uniform"))
			.append(Component.text(String.format(
				"%" + maxShardNameLength + "s | %" + HEALTH_COL_LENGTH + "s | %" + MEMORY_COL_LENGTH +"s | %" + TICK_COL_LENGTH + "s",
				SHARD_HEADER,
				HEALTH_HEADER,
				MEMORY_HEADER,
				TICK_HEADER
			), NamedTextColor.GOLD))
			.append(Component.newline());

		TreeMap<String, Component> sortedShardLines = new TreeMap<>();
		for (String shardName : shardNames) {
			ShardHealth shardHealth = MonumentaNetworkRelayIntegration.remoteShardHealth(shardName);

			TextColor healthTextColor = mixColors(yellowHealth.healthScore(), shardHealth.healthScore());
			TextColor memoryTextColor = mixColors(yellowHealth.memoryHealth(), shardHealth.memoryHealth());
			TextColor tickTextColor = mixColors(yellowHealth.tickHealth(), shardHealth.tickHealth());

			Component shardComponent = Component.text(String.format("%" + maxShardNameLength + "s | ", shardName), healthTextColor)
				.append(Component.text(String.format("%" + HEALTH_COL_LENGTH + ".2f", 100 * shardHealth.healthScore())))
				.append(Component.text(" | "))
				.append(Component.text(String.format("%" + MEMORY_COL_LENGTH + ".2f", 100 * shardHealth.memoryHealth()), memoryTextColor))
				.append(Component.text(" | "))
				.append(Component.text(String.format("%" + TICK_COL_LENGTH + ".2f", 100 * shardHealth.tickHealth()), tickTextColor))
				;

			sortedShardLines.put(shardName, shardComponent);
		}

		textDisplay.text(header
			.append(Component.join(JoinConfiguration.newlines(), sortedShardLines.values())));
	}

	private static TextColor mixColors(
		double midValue,
		double actualValue
	) {
		if (actualValue <= midValue) {
			return mixColors(NamedTextColor.RED, NamedTextColor.YELLOW, 0.0, midValue, actualValue);
		} else {
			return mixColors(NamedTextColor.YELLOW, NamedTextColor.GREEN, midValue, 1.0, actualValue);
		}
	}

	private static TextColor mixColors(
		TextColor minColor,
		TextColor maxColor,
		double minValue,
		double maxValue,
		double actualValue
	) {
		double maxMinDiff = maxValue - minValue;
		if (maxMinDiff <= 0.0) {
			return minColor;
		}

		HSVLike minHSV = minColor.asHSV();
		float minH = minHSV.h();
		float minS = minHSV.s();
		float minV = minHSV.v();

		HSVLike maxHSV = maxColor.asHSV();
		float maxH = maxHSV.h();
		float maxS = maxHSV.s();
		float maxV = maxHSV.v();

		float percentMaxColor = (float) ((actualValue - minValue) / maxMinDiff);
		float percentMinColor = 1.0f - percentMaxColor;

		float h = percentMinColor * minH + percentMaxColor * maxH;
		float s = percentMinColor * minS + percentMaxColor * maxS;
		float v = percentMinColor * minV + percentMaxColor * maxV;

		return TextColor.color(HSVLike.hsvLike(h, s, v));
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
