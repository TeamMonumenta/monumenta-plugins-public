package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.scriptedquests.zones.Zone;
import java.util.Optional;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ZoneUtils {
	public enum ZoneProperty {
		PLOTS_POSSIBLE("Plots Possible"),
		PLOT("Plot"),
		SHOPS_POSSIBLE("Shops Possible"),
		NO_MOBILITY_ABILITIES("No Mobility Abilities"),
		NO_PORTABLE_STORAGE("No Portable Storage"),
		SPECTATE_AVAILABLE("Spectate Available"),
		RESIST_5("Resistance V"),
		DISABLE_GRAVES("Disable Graves"),
		SPEED_2("Speed II"),
		SATURATION_2("Saturation II"),
		MASK_SPEED("Mask Speed"),
		MASK_JUMP_BOOST("Mask Jump Boost"),
		NO_EQUIPMENT_DAMAGE("No Equipment Damage"),
		NO_NATURAL_SPAWNS("No Natural Spawns"),
		NO_TRAPDOOR_CLICKS("No Trapdoor Clicks"),
		NO_DOOR_CLICKS("No Door Clicks"),
		BIG_DOOR_DOWN_CCW("Big Door Down is CCW"),
		BIG_DOOR_DOWN_CW("Big Door Down is CW"),
		NO_VEHICLES("No Vehicles"),
		ADVENTURE_MODE("Adventure Mode"),
		NO_EXPLOSIONS("No Explosions"),
		WINTER_SNOWBALLS_ONLY("Winter Snowballs Only"),
		NO_POTIONS("No Potions"),
		NO_FALL_DAMAGE("No Fall Damage"),
		RESTRICTED("Restricted"),
		BROOMSTICK_ENABLED("Broomstick Enabled"),
		BLOCKBREAK_DISABLED("Blockbreak Disabled"),
		BONE_MEAL_DISABLED("Bone Meal Disabled"),
		LAND_BOAT_POSSIBLE("Land Boat Possible"),
		NO_QUICK_BUILDING("NoQuickBuilding");

		private final String mPropertyName;

		ZoneProperty(String propertyName) {
			mPropertyName = propertyName;
		}

		public String getPropertyName() {
			return mPropertyName;
		}
	}

	public static boolean isInPlot(Entity entity) {
		return isInPlot(entity.getLocation());
	}

	public static boolean isInPlot(Location loc) {
		return inPlot(loc, ServerProperties.getIsTownWorld());
	}

	public static boolean inPlot(Entity entity, boolean isTownWorld) {
		return inPlot(entity.getLocation(), isTownWorld);
	}

	public static boolean inPlot(Location loc, boolean isTownWorld) {
		if (hasZoneProperty(loc, ZoneProperty.PLOT)) {
			return true;
		}
		if (!isTownWorld &&
		    !hasZoneProperty(loc, ZoneProperty.PLOTS_POSSIBLE)) {
			return false;
		}

		return isSurvivalModeInPlots(loc);
	}

	private static boolean isSurvivalModeInPlots(Location loc) {
		Material mat = loc.getWorld().getBlockAt(loc.getBlockX(), 10, loc.getBlockZ()).getType();
		return mat == Material.SPONGE;
	}

	public static boolean playerCanInteractWithBlock(Player player, Block block, boolean alwaysInPlots) {
		return playerCanInteractWithBlock(player, block.getLocation(), alwaysInPlots);
	}

	// True when the player is allowed to break/place blocks in the location
	// Must be in survival mode, attempting to interact in an adventure mode area that is not a survival mode plots area to be false
	// Does not include "interactions" like trapdoors/chests/etc
	public static boolean playerCanInteractWithBlock(Player player, Location loc, boolean alwaysInPlots) {
		return player.getGameMode() != GameMode.SURVIVAL || !ZoneUtils.hasZoneProperty(loc, ZoneProperty.ADVENTURE_MODE) || (alwaysInPlots && ServerProperties.getIsTownWorld());
	}

	// Check that the player can break/place blocks
	public static boolean playerCanMineBlock(Player player, Location loc) {
		GameMode gameMode = player.getGameMode();
		if (gameMode.equals(GameMode.ADVENTURE) || gameMode.equals(GameMode.SPECTATOR)) {
			return false;
		}
		if (hasZoneProperty(loc, ZoneProperty.ADVENTURE_MODE)) {
			if (isInPlot(loc)) {
				return true;
			}
			return false;
		}
		return true;
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

	public static Optional<Zone> getZone(Location loc) {
		return getZone(loc, "default");
	}

	public static Optional<Zone> getZone(Location loc, String layerName) {
		com.playmonumenta.scriptedquests.Plugin scriptedQuestsPlugin;
		scriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin)Bukkit.getPluginManager().getPlugin("ScriptedQuests");

		if (scriptedQuestsPlugin == null || scriptedQuestsPlugin.mZoneManager == null) {
			return Optional.empty();
		}

		@Nullable Zone zone = scriptedQuestsPlugin.mZoneManager.getZone(loc, layerName);
		if (zone == null) {
			return Optional.empty();
		} else {
			return Optional.of(zone);
		}
	}
}
