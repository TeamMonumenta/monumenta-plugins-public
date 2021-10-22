package com.playmonumenta.plugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.playmonumenta.plugins.server.properties.ServerProperties;

public class ZoneUtils {
	public enum ZoneProperty {
		PLOTS_POSSIBLE("Plots Possible"),
		SHOPS_POSSIBLE("Shops Possible"),
		NO_MOBILITY_ABILITIES("No Mobility Abilities"),
		NO_PORTABLE_STORAGE("No Portable Storage"),
		SPECTATE_AVAILABLE("Spectate Available"),
		RESIST_5("Resistance V"),
		SPEED_2("Speed II"),
		SATURATION_2("Saturation II"),
		MASK_SPEED("Mask Speed"),
		MASK_JUMP_BOOST("Mask Jump Boost"),
		NO_EQUIPMENT_DAMAGE("No Equipment Damage"),
		NO_NATURAL_SPAWNS("No Natural Spawns"),
		NO_TRAPDOOR_CLICKS("No Trapdoor Clicks"),
		BIG_DOOR_DOWN_CCW("Big Door Down is CCW"),
		BIG_DOOR_DOWN_CW("Big Door Down is CW"),
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

	public static boolean isInPlot(@NotNull Player player) {
		return isInPlot(player.getLocation());
	}

	public static boolean isInPlot(@NotNull Location loc) {
		return inPlot(loc, ServerProperties.getIsTownWorld());
	}

	public static boolean inPlot(Entity entity, boolean isTownWorld) {
		return inPlot(entity.getLocation(), isTownWorld);
	}

	public static boolean inPlot(Location loc, boolean isTownWorld) {
		if (!isTownWorld &&
		    !hasZoneProperty(loc, ZoneProperty.PLOTS_POSSIBLE)) {
			return false;
		}

		return isSurvivalModeInPlots(loc);
	}

	public static boolean isSurvivalModeInPlots(Location loc) {
		Material mat = loc.getWorld().getBlockAt(loc.getBlockX(), 10, loc.getBlockZ()).getType();
		return mat == Material.SPONGE;
	}

	public static boolean playerCanInteractWithBlock(Player player, Block block) {
		return playerCanInteractWithBlock(player, block.getLocation());
	}

	// True when the player is allowed to break/place blocks in the location
	// Must be in survival mode, attempting to interact in an adventure mode area that is not a survival mode plots area to be false
	// Does not include "interactions" like trapdoors/chests/etc
	public static boolean playerCanInteractWithBlock(Player player, Location loc) {
		return player.getGameMode() != GameMode.SURVIVAL || !ZoneUtils.hasZoneProperty(loc, ZoneProperty.ADVENTURE_MODE) || ServerProperties.getIsTownWorld();
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
