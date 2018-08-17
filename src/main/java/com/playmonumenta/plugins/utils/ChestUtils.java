package com.playmonumenta.plugins.utils;

import java.util.Random;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;

public class ChestUtils {

	private static final int CHEST_LUCK_RADIUS = 128;

	public static void chestScalingLuck(Plugin plugin, Player player, Block block) {
		int chestLuck = ScoreboardUtils.getScoreboardValue(player, "ChestLuckToggle");
		if (chestLuck > 0) {
			int playerCount = PlayerUtils.getNearbyPlayers(player.getLocation(), CHEST_LUCK_RADIUS).size();

			int luckLevel;

			if (playerCount <= 1) {
				luckLevel = -1;
			} else if (playerCount == 2) {
				Random mRandom = new Random();
				double rand = mRandom.nextDouble();

				if (rand < 0.6) {
					luckLevel = 1;
				} else {
					luckLevel = 0;
				}
			} else if (playerCount == 3) {
				luckLevel = 2;
			} else {
				luckLevel = 3;
			}

			player.getPotionEffect(PotionEffectType.LUCK);

			if (luckLevel >= 0) {
				plugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, new PotionEffect(PotionEffectType.LUCK,
				                                3, luckLevel, true, false));
			}

			int chestDebug = ScoreboardUtils.getScoreboardValue(player, "DEBUGChestLuck");
			if (chestDebug >= 0) {
				MessagingUtils.sendActionBarMessage(plugin, player, playerCount + " players in range!");
			}
		}
	}
}
