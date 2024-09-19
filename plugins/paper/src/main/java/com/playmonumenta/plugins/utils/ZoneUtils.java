package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.scriptedquests.zones.Zone;
import java.util.List;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

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
		DISABLE_REDSTONE_INTERACTIONS("Disable Redstone Interactions"),
		SPEED_2("Speed II"),
		SATURATION_2("Saturation II"),
		MASK_SPEED("Mask Speed"),
		MASK_JUMP_BOOST("Mask Jump Boost"),
		NO_EQUIPMENT_DAMAGE("No Equipment Damage"),
		NO_NATURAL_SPAWNS("No Natural Spawns"),
		NO_TRAPDOOR_CLICKS("No Trapdoor Clicks"),
		NO_BERRY_BUSH_CLICKS("No Berry Bush Clicks"),
		NO_SLEEPING("No Sleeping"),
		NO_DOOR_CLICKS("No Door Clicks"),
		BIG_DOOR_DOWN_CCW("Big Door Down is CCW"),
		BIG_DOOR_DOWN_CW("Big Door Down is CW"),
		NO_VEHICLES("No Vehicles"),
		NO_PLAYER_VEHICLES("No Player Vehicles"),
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
		NO_QUICK_BUILDING("NoQuickBuilding"),
		OVERWORLD_BLOCK_RESET("OverworldBlockReset"),
		LOOTROOM("Lootroom"),
		MONUMENT("Monument"),
		FESTIVE_TESSERACT_DISABLED("Festive Tesseract Disabled"),
		BLITZ("Blitz"),
		NO_PLACING_BOATS("No Placing Boats"),
		ABYSSAL_FORCED("Abyssal Forced"),
		ANTI_SPEED("Anti Speed"),
		LOOTING_LIMITER_DISABLED("Looting Limiter Disabled"),
		RAISE_GRAVE_ABOVE_ZONE("Raise Grave Above Zone"),
		PORTAL_GUN_ENABLED("Portal Gun Enabled"),
		PRECIOUS_BLOCK_DROPS_DISABLED("Precious Block Drops Disabled"),
		ITEM_FRAMES_EDITABLE("Item Frames Editable"),
		NO_VIRTUAL_INVENTORIES("No Virtual Inventories"),
		NO_SPECTATOR_ON_RESPAWN("No Spectator On Respawn"),
		NO_SPECTATOR_ON_DEATH("No Spectator On Death"),
		NO_BUFF_DURATION_LOSS_ON_DEATH("No Buff Duration Loss On Death"),
		NO_PLACING_CONTAINERS("No Placing Containers"),
		;

		private final String mPropertyName;

		ZoneProperty(String propertyName) {
			mPropertyName = propertyName;
		}

		public String getPropertyName() {
			return mPropertyName;
		}
	}

	public static List<Material> PRECIOUS_BLOCKS = List.of(Material.IRON_BLOCK, Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.NETHERITE_BLOCK);

	// Returns if the player is expected to be in Survival Mode or Adventure Mode for their given circumstances
	public static GameMode expectedGameMode(Player player) {
		GameMode currentGameMode = player.getGameMode();
		Location location = player.getLocation();

		/*
		 * Checks for the plots shards, where plots may be present, but there isn't a zone for each plot.
		 * This depends on the presence of sponge at y=10 and meeting the requirements to own a plot.
		 * There's also an exception to allow building just outside a plot.
		 */
		if (hasZoneProperty(player, ZoneProperty.PLOTS_POSSIBLE)) {
			boolean isInPlot = inPlot(location, ServerProperties.getIsTownWorld());
			if (!isInPlot) {
				return GameMode.ADVENTURE;
			} else if (
				currentGameMode == GameMode.ADVENTURE
					&& location.getY() > ServerProperties.getPlotSurvivalMinHeight()
					&& ScoreboardUtils.getScoreboardValue(player, AbilityUtils.TOTAL_LEVEL).orElse(0) >= 5
			) {
				return GameMode.SURVIVAL;
			} else {
				return currentGameMode;
			}
		}

		/*
		 * Everything after this point covers everywhere else in the game:
		 * - Player plots have a zone covering the plot bounds
		 * - Everything else uses the adventure mode zone property, or its absence
		 */
		if (
			hasZoneProperty(player, ZoneProperty.ADVENTURE_MODE)
				&& !isInPlot(player)
		) {
			return GameMode.ADVENTURE;
		}

		return GameMode.SURVIVAL;
	}

	public static void setExpectedGameMode(Player player) {
		player.setGameMode(expectedGameMode(player));
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
		return mat == Material.SPONGE || (mat.equals(Material.WET_SPONGE) && loc.getY() < 53);
	}

	public static boolean playerCanMineBlock(Player player, Block block) {
		return playerCanMineBlock(player, block.getLocation());
	}

	// Check that the player can break/place blocks
	public static boolean playerCanMineBlock(Player player, Location loc) {
		GameMode gameMode = player.getGameMode();
		if (gameMode == GameMode.CREATIVE) {
			return true;
		}
		if (gameMode.equals(GameMode.ADVENTURE) || gameMode.equals(GameMode.SPECTATOR)) {
			return false;
		}
		return isMineable(loc);
	}

	/**
	 * Checks whether the block at the given location can potentially be mined by a player (i.e. is in a survival mode area)
	 */
	public static boolean isMineable(Location loc) {
		return !hasZoneProperty(loc, ZoneProperty.ADVENTURE_MODE) || isInPlot(loc);
	}

	public static boolean hasZoneProperty(Entity entity, ZoneProperty property) {
		return hasZoneProperty(entity.getLocation(), property);
	}

	public static boolean hasZoneProperty(Entity entity, ZoneProperty property, String namespace) {
		return hasZoneProperty(entity.getLocation(), property, namespace);
	}

	public static boolean hasZoneProperty(Location loc, ZoneProperty property) {
		return hasZoneProperty(loc, property, "default");
	}

	public static boolean hasZoneProperty(Location loc, ZoneProperty property, String namespace) {
		com.playmonumenta.scriptedquests.Plugin scriptedQuestsPlugin;
		scriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin) Bukkit.getPluginManager().getPlugin("ScriptedQuests");

		return scriptedQuestsPlugin.mZoneManager.hasProperty(loc, namespace, property.getPropertyName());
	}

	public static Optional<Zone> getZone(Location loc) {
		return getZone(loc, "default");
	}

	public static Optional<Zone> getZone(Location loc, String namespace) {
		com.playmonumenta.scriptedquests.Plugin scriptedQuestsPlugin;
		scriptedQuestsPlugin = (com.playmonumenta.scriptedquests.Plugin) Bukkit.getPluginManager().getPlugin("ScriptedQuests");

		if (scriptedQuestsPlugin == null || scriptedQuestsPlugin.mZoneManager == null) {
			return Optional.empty();
		}

		@Nullable Zone zone = scriptedQuestsPlugin.mZoneManager.getZone(loc, namespace);
		if (zone == null) {
			return Optional.empty();
		} else {
			return Optional.of(zone);
		}
	}
}
