package com.playmonumenta.plugins.utils;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class DungeonUtils {
	public static int[] getSpawnersBroken(Player p) {
		Location armorStandLoc = p.getWorld().getSpawnLocation(); // get the spawn location
		ArmorStand armorStand = null;
		for (Entity entity : armorStandLoc.getNearbyEntities(2, 2, 2)) { // get the entities at the spawn location
			if (entity.getType().equals(EntityType.ARMOR_STAND) && entity.getCustomName() != null && entity.getCustomName().equals("SpawnerBreaksArmorStand")) { //if it's our marker armorstand
				armorStand = (ArmorStand) entity;
			}
		}
		if (armorStand != null) {
			return new int[]{ScoreboardUtils.getScoreboardValue(armorStand, "SpawnerBreaks").orElse(0), ScoreboardUtils.getScoreboardValue(armorStand, "SpawnersTotal").orElse(0)};
		} else {
			return null;
		}
	}
}

