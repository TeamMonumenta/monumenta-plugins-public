package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.shardhealth.ShardHealth;
import com.playmonumenta.networkrelay.shardhealth.g1.G1GcHealth;
import com.playmonumenta.plugins.bosses.bosses.SystemMonitorDisplayBoss;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.TableFormatter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.util.HSVLike;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

import static com.playmonumenta.plugins.Constants.Tags.REMOVE_ON_UNLOAD;

public class SpellSystemMonitorDisplay extends Spell {
	private static final int KIB_FACTOR = 1024;
	private static final int MIB_FACTOR = KIB_FACTOR * KIB_FACTOR;
	private static final int GIB_FACTOR = MIB_FACTOR * KIB_FACTOR;
	public static final DecimalFormat NUMBER_FORMATTER = new DecimalFormat("0.###");

	private static final ShardHealth YELLOW_HEALTH = ShardHealth.defaultTargetHealth();
	private static final Color BACKGROUND_COLOR = Color.fromARGB(255, 0x2E, 0x34, 0x36);

	private final Entity mBoss;
	private final boolean mIncludeGcInfo;

	public SpellSystemMonitorDisplay(final Entity boss, boolean includeGcInfo) {
		mBoss = boss;
		mIncludeGcInfo = includeGcInfo;
	}

	private static Component coloredStat(ShardHealth health, Function<ShardHealth, Double> func) {
		double value = func.apply(health);
		return Component.text(String.format("%.2f", value), mixColors(func.apply(YELLOW_HEALTH), value));
	}

	private static String formatByte(double bytes) {
		if (bytes == 0) {
			return "0";
		}

		if (bytes > GIB_FACTOR) {
			return NUMBER_FORMATTER.format(bytes / GIB_FACTOR) + " GiB";
		} else if (bytes > MIB_FACTOR) {
			return NUMBER_FORMATTER.format(bytes / MIB_FACTOR) + " MiB";
		} else if (bytes > KIB_FACTOR) {
			return NUMBER_FORMATTER.format(bytes / KIB_FACTOR) + " KiB";
		}

		return NUMBER_FORMATTER.format(bytes) + " B";
	}

	private TableFormatter.TabulationResult computeText() {
		final var shards = new ArrayList<>(NetworkRelayAPI.getOnlineShardNames());
		shards.sort(String::compareTo);

		List<List<Component>> table = new ArrayList<>();

		for (final var shard : shards) {
			ShardHealth shardHealth = MonumentaNetworkRelayIntegration.remoteShardHealth(shard);
			final var gcHealth = Objects.requireNonNullElse(
				shardHealth.gcHealth(),
				G1GcHealth.zeroHealth()
			);

			final var entries = new ArrayList<>(List.of(
				Component.text(shard),
				coloredStat(shardHealth, ShardHealth::healthScore),
				coloredStat(shardHealth, ShardHealth::memoryHealth),
				coloredStat(shardHealth, ShardHealth::tickHealth)
			));

			if (mIncludeGcInfo) {
				entries.addAll(List.of(
					Component.text(
						String.format(
							"%s/%s/%.2f",
							(int) gcHealth.oldGenCycleInInterval(),
							formatByte(gcHealth.averageOldGenFreed()),
							gcHealth.averageOldGenTime()
						)
					),
					Component.text(
						String.format(
							"%s/%s/%.2f",
							(int) gcHealth.youngGenCycleInInterval(),
							formatByte(gcHealth.averageYoungGenFreed()),
							gcHealth.averageYoungGenTime()
						)
					),
					Component.text(
						String.format(
							"%s/%s/%.2f",
							(int) gcHealth.concurrentCycleInInterval(),
							formatByte(gcHealth.averageConcurrentFreed()),
							gcHealth.averageConcurrentTime()
						)
					),
					Component.text(
						String.format(
							"%s/%s/%.2f",
							(int) gcHealth.overallCyclesInInterval(),
							formatByte(gcHealth.averageOverallFreed()),
							gcHealth.averageOverallTime()
						)
					)
				));
			}

			table.add(entries);
		}

		final var headers = new ArrayList<>(List.of("Shard", "Health", "Memory", "Idle Time"));

		if (mIncludeGcInfo) {
			headers.addAll(List.of("G1 Old", "G1 Young", "G1 Concurrent", "G1 Overall"));
		}

		return TableFormatter.tabulate(
			TextColor.color(BACKGROUND_COLOR.asRGB()),
			headers.stream().map(Component::text).toList(),
			table,
			" | "
		);
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
			textDisplay.setBackgroundColor(BACKGROUND_COLOR);
		}

		final var tabResult = computeText();
		textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
		textDisplay.setLineWidth(TableFormatter.UNIT_TO_PX * tabResult.width());
		textDisplay.text(Component.empty().font(NamespacedKey.fromString("minecraft:uniform")).append(tabResult.text()));
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
		return 10; // no need to fire this every tick...
	}
}
