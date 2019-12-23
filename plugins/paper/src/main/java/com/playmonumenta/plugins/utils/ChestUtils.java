package com.playmonumenta.plugins.utils;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

public class ChestUtils {

	private static final int CHEST_LUCK_RADIUS = 128;

	public static void chestScalingLuck(Plugin plugin, Player player, Block block) {
		int chestLuck = ScoreboardUtils.getScoreboardValue(player, "ChestLuckToggle");
		if (chestLuck > 0) {
			int playerCount = PlayerUtils.playersInRange(player.getLocation(), CHEST_LUCK_RADIUS).size();

			int luckLevel;

			Random mRandom = new Random();
			double rand = mRandom.nextDouble();

			if (playerCount <= 1) {
				if (rand < 0.5) {
					luckLevel = -1;
				} else {
					luckLevel = 0;
				}
			} else if (playerCount == 2) {
				if (rand < 0.6) {
					luckLevel = 0;
				} else {
					luckLevel = 1;
				}
			} else if (playerCount == 3) {
				if (rand < 0.7) {
					luckLevel = 1;
				} else {
					luckLevel = 2;
				}
			} else {
				luckLevel = 2;
			}

			player.getPotionEffect(PotionEffectType.LUCK);

			if (luckLevel >= 0) {
				plugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, new PotionEffect(PotionEffectType.LUCK,
				                                3, luckLevel, true, false));
			}

			if (player.getEquipment() != null &&
			    player.getEquipment().getItemInMainHand() != null &&
			    player.getEquipment().getItemInMainHand().getType() == Material.COMPASS) {
				if (playerCount == 1) {
					MessagingUtils.sendActionBarMessage(plugin, player, playerCount + " player in range!");
				} else {
					MessagingUtils.sendActionBarMessage(plugin, player, playerCount + " players in range!");
				}
			}
		}
	}

	public static boolean isEmpty(Block block) {
		return block.getState() instanceof Chest && isEmpty((Chest)block.getState());
	}

	public static boolean isEmpty(Chest chest) {
		for (ItemStack slot : chest.getInventory()) {
			if (slot != null) {
				return false;
			}
		}
		return true;
	}
}
