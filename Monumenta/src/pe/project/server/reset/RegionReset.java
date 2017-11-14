package pe.project.server.reset;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import net.md_5.bungee.api.ChatColor;
import pe.project.Main;
import pe.project.utils.ScoreboardUtils;

public class RegionReset {
	public static void handle(Main plugin, Player player) {
		if (player != null) {
			int version = ScoreboardUtils.getScoreboardValue(player, "version");
			if (version != plugin.mServerVersion) {
				ScoreboardUtils.setScoreboardValue(player, "version", plugin.mServerVersion);

				_resetScoreboards(player);

				player.teleport(player.getWorld().getSpawnLocation());
				player.sendMessage(ChatColor.GREEN + "The terrain has been reset since you've last played and you've been moved to safety.");

				player.removePotionEffect(PotionEffectType.LUCK);
			}
		}
	}

	private static void _resetScoreboards(Player player) {
		//	Reset Dungeon related scoreboards.
		ScoreboardUtils.setScoreboardValue(player, "D1Access", 0);
		ScoreboardUtils.setScoreboardValue(player, "D2Access", 0);
		ScoreboardUtils.setScoreboardValue(player, "D3Access", 0);
		ScoreboardUtils.setScoreboardValue(player, "D4Access", 0);
		ScoreboardUtils.setScoreboardValue(player, "D5Access", 0);
		ScoreboardUtils.setScoreboardValue(player, "DB1Access", 0);
		ScoreboardUtils.setScoreboardValue(player, "D1Finished", 0);
		ScoreboardUtils.setScoreboardValue(player, "D2Finished", 0);
		ScoreboardUtils.setScoreboardValue(player, "D3Finished", 0);
		ScoreboardUtils.setScoreboardValue(player, "D4Finished", 0);
		ScoreboardUtils.setScoreboardValue(player, "D5Finished", 0);
	}
}
