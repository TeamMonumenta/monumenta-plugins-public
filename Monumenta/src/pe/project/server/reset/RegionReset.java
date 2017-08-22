package pe.project.server.reset;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import net.md_5.bungee.api.ChatColor;
import pe.project.Constants;
import pe.project.Main;
import pe.project.utils.ScoreboardUtils;

public class RegionReset {
	public static void handle(Main plugin, Player player) {
		if (player != null) {
			new BukkitRunnable() {
				Integer tick = 0;
				public void run() {
					if (++tick == 20) {
						int version = ScoreboardUtils.getScoreboardValue(player, "version");
						if (version != plugin.mServerVersion) {
							ScoreboardUtils.setScoreboardValue(player, "version", plugin.mServerVersion);
							
							_resetScoreboards(player);
							
							player.teleport(new Location(player.getWorld(), Constants.RESET_POINT.mX, Constants.RESET_POINT.mY, Constants.RESET_POINT.mZ));
							player.sendMessage(ChatColor.GREEN + "The terrain has been reset since you've last played and you've been moved to safety.");
							
							player.removePotionEffect(PotionEffectType.LUCK);
						}
						
						this.cancel();
					}
				}
			}.runTaskTimer(plugin, 0, 1);
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
