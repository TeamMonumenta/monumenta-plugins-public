package com.playmonumenta.plugins.rush;

import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

public class RushReward {
	private static final NamespacedKey ADE_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:r2/dungeons/rushdown/a_dis_energy");
	private static final NamespacedKey DE_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:r2/dungeons/rushdown/dis_energy");
	private static final NamespacedKey CCS_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:r2/items/currency/compressed_crystalline_shard");
	private static final NamespacedKey HCS_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:r2/items/currency/hyper_crystalline_shard");
	private static final NamespacedKey ROSE_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:r2/dungeons/rushdown/replica_rose_wool");

	private static final double SCALING_BOOST = 0.0025;

	public static void generateLoot(Location lootLoc, int round) {
		if (--round < 1) { // Exclude loot from the round you died on
			return;
		}

		double totalMobs = 0;

		for (int i = 1; i <= round; i++) {
			double[] mobCountByWave = RushManager.calculateMobCount(round, false);
			for (double v : mobCountByWave) {
				totalMobs += (RushManager.WAVE_PER_ROUND + RushManager.LAST_WAVE_INCREASE - 1) * 0.425 * v
					* Math.max(1, 1 + SCALING_BOOST * (round - RushManager.SCALING_ROUND));
			}
		}
		int totalReward = (int) totalMobs;

		attemptPayOut(ADE_LOOT_TABLE, totalReward / 100, lootLoc);
		attemptPayOut(DE_LOOT_TABLE, totalReward % 100, lootLoc);

		for (int i = 0; i < Math.min(256, totalReward); i++) {
			lootLoc.getWorld().dropItem(lootLoc, new ItemStack(Material.FIRE_CORAL_BLOCK));
		}

		// Misc. loot reduced by some amount

		totalReward /= 8;
		attemptPayOut(HCS_LOOT_TABLE, totalReward / 64, lootLoc);
		attemptPayOut(CCS_LOOT_TABLE, totalReward % 64, lootLoc);

	}

	public static void roseWoolDrop(Location lootLoc) {
		attemptPayOut(ROSE_LOOT_TABLE, 1, lootLoc);
	}

	private static void attemptPayOut(NamespacedKey namespacedKey, int count, Location loc) {
		LootTable lootTable = Bukkit.getLootTable(namespacedKey);
		NamedTextColor color;
		if (namespacedKey.equals(ADE_LOOT_TABLE) || namespacedKey.equals(DE_LOOT_TABLE)) {
			color = NamedTextColor.RED;
		} else if (namespacedKey.equals(ROSE_LOOT_TABLE)) {
			color = NamedTextColor.DARK_RED;
		} else {
			color = NamedTextColor.WHITE;
		}

		if (lootTable != null) {
			ItemStack item = lootTable.populateLoot(FastUtils.RANDOM, new LootContext.Builder(loc).build()).iterator().next();
			for (int i = 0; i < count; i++) {
				Item loot = loc.getWorld().dropItem(loc, item);
				GlowingManager.startGlowing(loot, color, 10000, 0);
			}
		}
	}

}
