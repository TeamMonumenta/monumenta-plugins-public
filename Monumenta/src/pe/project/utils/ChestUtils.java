package pe.project.utils;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.Plugin;
import pe.project.managers.potion.PotionManager.PotionID;

public class ChestUtils {

	private static final int CHEST_LUCK_RADIUS = 32;

	public static void chestScalingLuck(Plugin plugin, Player player, Block block) {
		int chestLuck = ScoreboardUtils.getScoreboardValue(player, "ChestLuckToggle");
		if (chestLuck > 0) {
			int playerCount = PlayerUtils.getNearbyPlayers(player.getLocation(), CHEST_LUCK_RADIUS).size();

			int luckLevel;
			switch (playerCount) {
			case 0:
				luckLevel = 0;
				break;
			case 1:
				luckLevel = 0;
				break;
			case 2:
				luckLevel = 1;
				break;
			case 3:
				luckLevel = 3;
				break;
			default:
				luckLevel = 4;
				break;
			}

			player.getPotionEffect(PotionEffectType.LUCK);

			if (luckLevel > 0) {
				plugin.mPotionManager.addPotion(player, PotionID.SAFE_ZONE, new PotionEffect(PotionEffectType.LUCK,
				                                3, luckLevel, true, false));
			}

			int chestDebug = ScoreboardUtils.getScoreboardValue(player, "DEBUGChestLuck");
			if (chestDebug > 0) {
				int dll = luckLevel + 1;
				MessagingUtils.sendActionBarMessage(plugin, player, "Generated " + dll + " extra items!");
			}
		}
	}
}
