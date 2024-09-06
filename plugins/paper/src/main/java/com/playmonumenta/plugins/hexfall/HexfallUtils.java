package com.playmonumenta.plugins.hexfall;

import com.playmonumenta.plugins.bosses.bosses.hexfall.HyceneaRageOfTheWolf;
import com.playmonumenta.plugins.bosses.bosses.hexfall.Ruten;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class HexfallUtils {

	public static boolean playerInRuten(Player player) {
		return player.getScoreboardTags().contains("RutenFighter");
	}

	public static boolean playerInHycenea(Player player) {
		return player.getScoreboardTags().contains("HyceneaFighter");
	}

	public static boolean playerInBoss(Player player) {
		return playerInRuten(player) || playerInHycenea(player);
	}

	public static List<Player> getPlayersInRuten(Location spawnLoc) {
		return PlayerUtils.playersInXZRange(spawnLoc, Ruten.detectionRange, true).stream().filter(HexfallUtils::playerInRuten).collect(Collectors.toList());
	}

	public static List<Player> getPlayersInHycenea(Location spawnLoc) {
		return PlayerUtils.playersInXZRange(spawnLoc, HyceneaRageOfTheWolf.detectionRange, true).stream().filter(HexfallUtils::playerInHycenea).collect(Collectors.toList());
	}

	public static List<Player> playersInBossInXZRange(Location loc, double range, boolean includeNonTargetable) {
		return PlayerUtils.playersInXZRange(loc, range, includeNonTargetable).stream().filter(HexfallUtils::playerInBoss).collect(Collectors.toList());
	}

	public static void clearPlatformAndAbove(Location loc) {
		List<Block> blocksToRemove = new ArrayList<>();
		for (int height = 24; height >= -1; height--) {
			for (int x = -7; x <= 7; x++) {
				for (int z = -7; z <= 7; z++) {
					if ((x == -7 || x == 7) && (z == -7 || z == 7)) {
						continue;
					}
					Location l = loc.clone().add(x, height, z);
					Block block = l.getBlock();
					Material material = block.getType();
					if (material != Material.BARRIER && material != Material.BEDROCK && material != Material.LIGHT) {
						blocksToRemove.add(l.getBlock());
					}
				}
			}
		}

		for (Block block : blocksToRemove) {
			block.setType(Material.AIR, false);
		}
	}
}
