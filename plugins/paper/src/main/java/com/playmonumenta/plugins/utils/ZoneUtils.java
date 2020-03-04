package com.playmonumenta.plugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class ZoneUtils {
	public enum ZoneProperty {
		PLOTS_POSSIBLE("Plots Possible"),
		NO_MOBILITY_ABILITIES("No Mobility Abilities"),
		SPECTATE_AVAILABLE("Spectate Available"),
		RESIST_5("Resistance V"),
		SPEED_2("Speed II"),
		SATURATION_2("Saturation II"),
		MASK_SPEED("Mask Speed"),
		MASK_JUMP_BOOST("Mask Jump Boost"),
		NO_EQUIPMENT_DAMAGE("No Equipment Damage"),
		NO_NATURAL_SPAWNS("No Natural Spawns"),
		NO_VEHICLES("No Vehicles"),
		ADVENTURE_MODE("Adventure Mode"),
		RESTRICTED("Restricted");

		private final String mPropertyName;

		ZoneProperty(String propertyName) {
			mPropertyName = propertyName;
		}

		private String getPropertyName() {
			return mPropertyName;
		}
	}

	public static boolean inPlot(Location loc, boolean isTownWorld) {
		if (!isTownWorld &&
		    !hasZoneProperty(loc, ZoneProperty.PLOTS_POSSIBLE)) {
			return false;
		}

		Material mat = loc.getWorld().getBlockAt(loc.getBlockX(), 10, loc.getBlockZ()).getType();
		return mat == Material.SPONGE;
	}

	public static boolean inPlot(Entity entity, boolean isTownWorld) {
		return inPlot(entity.getLocation(), isTownWorld);
	}

	public static boolean hasZoneProperty(Entity entity, ZoneProperty property) {
		return hasZoneProperty(entity.getLocation(), property);
	}

	public static boolean hasZoneProperty(Location loc, ZoneProperty property) {
		return hasZoneProperty(loc.toVector(), property);
	}

	public static boolean hasZoneProperty(Vector loc, ZoneProperty property) {
		com.playmonumenta.scriptedquests.Plugin scriptedQuestsPlugin;
		scriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin)Bukkit.getPluginManager().getPlugin("ScriptedQuests");

		return scriptedQuestsPlugin.mZoneManager.hasProperty(loc, "default", property.getPropertyName());
	}
}
