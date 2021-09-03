package com.playmonumenta.plugins.depths;

import java.util.Collection;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.utils.FastUtils;

public class DepthsLoot {

	public static final NamespacedKey LOOT_TABLE_KEY = NamespacedKey.fromString("epic:r2/depths/loot/reward");
	public static final NamespacedKey RELIC_KEY = NamespacedKey.fromString("epic:r2/depths/loot/relicroll");
	public static final NamespacedKey GEODE_KEY = NamespacedKey.fromString("epic:r2/depths/loot/voidstained_geode");
	public static final NamespacedKey POME_KEY = NamespacedKey.fromString("epic:r2/delves/items/twisted_pome");
	public static final NamespacedKey TROPHY_KEY = NamespacedKey.fromString("epic:r2/delves/trophies/depths");

	public static final int RELIC_CHANCE = 250;

	public static final Vector LOOT_ROOM_LOOT_OFFSET = new Vector(-21, 5, 0);


	/**
	 * Generates loot at the loot room location for the given treasure score
	 * @param loc loot room spawn location
	 * @param treasureScore amount of loot to spawn
	 */
	public static void generateLoot(Location loc, int treasureScore, Player p, boolean trophy) {

		//Load the main reward table with ccs and depths mats and spawn it in

		LootTable moneyTable = Bukkit.getLootTable(LOOT_TABLE_KEY);

		LootContext context = new LootContext.Builder(loc).build();
		if (moneyTable != null) {
			for (int i = 0; i < treasureScore; i++) {
				Collection<ItemStack> loot = moneyTable.populateLoot(FastUtils.RANDOM, context);
				if (!loot.isEmpty()) {
					for (ItemStack item : loot) {
						Item lootOnGround = loc.getWorld().dropItem(loc, item);
						lootOnGround.setGlowing(true);
					}
				}

				if (i < 128) {
					ItemStack fillerBlocks = new ItemStack(Material.BLACKSTONE, 2);
					loc.getWorld().dropItem(loc, fillerBlocks);
				}

				if (i < 64) {
					ItemStack fillerFood = new ItemStack(Material.COOKED_PORKCHOP, 1);
					loc.getWorld().dropItem(loc, fillerFood);
				}
			}
		}

		Random r = new Random();

		//Roll for geodes
		LootTable geodeTable = Bukkit.getLootTable(GEODE_KEY);
		Collection<ItemStack> loot = geodeTable.populateLoot(FastUtils.RANDOM, context);
		if (!loot.isEmpty()) {
			for (int i = treasureScore; i >= 12; i -= 12) {
				for (ItemStack item : loot) {
					loc.getWorld().dropItem(loc, item);
				}
			}
			//Get num from 0-7
			int roll = r.nextInt(12);
			if (roll < treasureScore) {
				//Drop an extra geode
				for (ItemStack item : loot) {
					loc.getWorld().dropItem(loc, item);
				}
			}
		}

		//Roll for endless mode loot- subtract 54 from treasure score to compensate for base score from 3 boss wins

		LootTable pomeTable = Bukkit.getLootTable(POME_KEY);
		if (pomeTable != null) {
			for (int i = 0; i < treasureScore - 54; i++) {
				// 1/50 chance to drop a pome per treasure score in endless mode
				if (r.nextInt(50) == 0) {
					loot = pomeTable.populateLoot(FastUtils.RANDOM, context);
					if (!loot.isEmpty()) {
						for (ItemStack item : loot) {
							loc.getWorld().dropItem(loc, item);
						}
					}
				}
			}
		}

		//Delve trophy if max points
		if (trophy) {
			LootTable trophyTable = Bukkit.getLootTable(TROPHY_KEY);

			loot = trophyTable.populateLoot(FastUtils.RANDOM, context);
			if (!loot.isEmpty()) {
				for (ItemStack item : loot) {
					loc.getWorld().dropItem(loc, item);
				}
			}
		}


		//Roll for relics- treasure score / 250 chance (if above 250, guaranteed drop and subtract relic)

		LootTable relicTable = Bukkit.getLootTable(RELIC_KEY);

		if (relicTable == null) {
			return;
		}

		for (int score = treasureScore; score > 0; score -= RELIC_CHANCE) {
			int roll = r.nextInt(RELIC_CHANCE);
			if (roll < score) {
				//Drop random relic
				loot = relicTable.populateLoot(FastUtils.RANDOM, context);
				if (!loot.isEmpty()) {
					for (ItemStack item : loot) {
						Item lootOnGround = loc.getWorld().dropItem(loc, item);
						lootOnGround.setGlowing(true);
						p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
					}
				}
			}
		}
	}

}
