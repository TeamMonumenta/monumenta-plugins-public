package com.playmonumenta.plugins.utils;

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

	public static final int CHEST_LUCK_RADIUS = 128;
	private static final double[] BONUS_ITEMS = {
			0,		// Dummy value, this is a player count indexed array
			0.5,
			1.7,
			2.6,
			3.3,
			3.8,
			4.2,
			4.4,
			4.5
	};

	public static void chestScalingLuck(Plugin plugin, Player player, Block block) {
		int chestLuck = ScoreboardUtils.getScoreboardValue(player, "ChestLuckToggle");
		if (chestLuck > 0) {
			int playerCount = PlayerUtils.playersInRange(player.getLocation(), CHEST_LUCK_RADIUS).size();
			double bonusItems = BONUS_ITEMS[Math.min(BONUS_ITEMS.length - 1, playerCount)];
			int luckLevel = (int) bonusItems;

			if (FastUtils.RANDOM.nextDouble() < bonusItems - luckLevel) {
				luckLevel++;
			}

			if (luckLevel > 0) {
				plugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, new PotionEffect(PotionEffectType.LUCK,
				                                3, luckLevel - 1, true, false));
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
