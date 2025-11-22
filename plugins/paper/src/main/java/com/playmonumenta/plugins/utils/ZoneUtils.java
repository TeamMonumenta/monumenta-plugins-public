package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.integrations.luckperms.GuildPlotUtils;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.scriptedquests.Plugin;
import com.playmonumenta.scriptedquests.zones.Zone;
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
		ABYSSAL_FORCED("Abyssal Forced"),
		ADVENTURE_MODE("Adventure Mode"),
		ANTI_SPEED("Anti Speed"),
		BIG_DOOR_DOWN_CCW("Big Door Down is CCW"),
		BIG_DOOR_DOWN_CW("Big Door Down is CW"),
		BLOCKBREAK_DISABLED("Blockbreak Disabled"),
		BONE_MEAL_DISABLED("Bone Meal Disabled"),
		BROOMSTICK_ENABLED("Broomstick Enabled"),
		CAN_DEPOSIT_INTO_POTS("Can Deposit Into Pots"),
		CAN_SHOOT_POTS("Can Shoot Pots"),
		CONDUIT_POWER_WATER("Conduit Power Water"),
		DISABLE_CUSTOM_TRADE_GUI("Disable Custom Trade GUI"),
		DISABLE_GRAVES("Disable Graves"),
		DISABLE_MAGIC_TESS("DisableMagicTess"),
		DISABLE_PURPLE_TESS("DisablePurpleTesseract"),
		DISABLE_REDSTONE_INTERACTIONS("Disable Redstone Interactions"),
		EXPLOSION_PROOF_CHESTS("Explosion Proof Chests"),
		FESTIVE_TESSERACT_DISABLED("Festive Tesseract Disabled"),
		FORCE_ENABLE_GRAPPLING_HOOK("Force Enable Grappling Hook"),
		ITEM_FRAMES_EDITABLE("Item Frames Editable"),
		LAND_BOAT_POSSIBLE("Land Boat Possible"),
		LOOTING_LIMITER_DISABLED("Looting Limiter Disabled"),
		LOOTROOM("Lootroom"),
		MASK_JUMP_BOOST("Mask Jump Boost"),
		MASK_SPEED("Mask Speed"),
		MASK_GEAR_PROJECTILE_SPEED("Mask Gear Projectile Speed"),
		MECHANICAL_ARMORY_DISABLED("MechanicalArmoryDisabled"),
		MONUMENT("Monument"),
		NO_BERRY_BUSH_CLICKS("No Berry Bush Clicks"),
		NO_BUFF_DURATION_LOSS_ON_DEATH("No Buff Duration Loss On Death"),
		NO_DOOR_CLICKS("No Door Clicks"),
		NO_EQUIPMENT_DAMAGE("No Equipment Damage"),
		NO_EXPLOSIONS("No Explosions"),
		NO_FALL_DAMAGE("No Fall Damage"),
		NO_GETTING_BOOK_FROM_LECTERN("No Getting Book From Lectern"),
		NO_MOBILITY_ABILITIES("No Mobility Abilities"),
		NO_NATURAL_SPAWNS("No Natural Spawns"),
		NO_PLACING_BOATS("No Placing Boats"),
		NO_PLACING_CONTAINERS("No Placing Containers"),
		NO_PLAYER_VEHICLES("No Player Vehicles"),
		NO_PORTABLE_STORAGE("No Portable Storage"),
		NO_POTIONS("No Potions"),
		NO_QUICK_BUILDING("NoQuickBuilding"),
		NO_SLEEPING("No Sleeping"),
		NO_SUMMONS("No Summons"),
		NO_SPECTATOR_ON_DEATH("No Spectator On Death"),
		NO_SPECTATOR_ON_RESPAWN("No Spectator On Respawn"),
		NO_TRAPDOOR_CLICKS("No Trapdoor Clicks"),
		NO_TRICKY_TRANSFORMATION("NoTrickyTransformation"),
		NO_VEHICLES("No Vehicles"),
		NO_VIRTUAL_INVENTORIES("No Virtual Inventories"),
		OVERWORLD_BLOCK_RESET("OverworldBlockReset"),
		PLOT("Plot"),
		PORTAL_GUN_ENABLED("Portal Gun Enabled"),
		PRECIOUS_BLOCK_DROPS_DISABLED("Precious Block Drops Disabled"),
		RAISE_GRAVE_ABOVE_ZONE("Raise Grave Above Zone"),
		RESIST_5("Resistance V"),
		RESTRICTED("Restricted"),
		SATURATION_2("Saturation II"),
		SHOPS_POSSIBLE("Shops Possible"),
		SPECTATE_AVAILABLE("Spectate Available"),
		SPEED_2("Speed II"),
		WINTER_SNOWBALLS_ONLY("Winter Snowballs Only"),
		;

		private final String mPropertyName;

		ZoneProperty(String propertyName) {
			mPropertyName = propertyName;
		}

		public String getPropertyName() {
			return mPropertyName;
		}
	}

	// Returns if the player is expected to be in Survival Mode or Adventure Mode for their given circumstances
	public static GameMode expectedGameMode(Player player) {
		GameMode guildPlotGameMode = GuildPlotUtils.guildPlotGameMode(player);
		if (guildPlotGameMode != null) {
			return guildPlotGameMode;
		}

		GameMode currentGameMode = player.getGameMode();
		Location location = player.getLocation();

		/*
		 * Checks for the plots shards, where plots may be present, but there isn't a zone for each plot.
		 * This depends on the presence of sponge at y=10 and meeting the requirements to own a plot.
		 * There's also an exception to allow building just outside a plot.
		 */
		if (hasZoneProperty(player, ZoneProperty.SHOPS_POSSIBLE)) {
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
		setExpectedGameMode(player, false);
	}

	public static void setExpectedGameMode(Player player, boolean onlyFromSurvivalAdventure) {
		GameMode currentGamemode = player.getGameMode();
		if (
			onlyFromSurvivalAdventure &&
				!GameMode.ADVENTURE.equals(currentGamemode)
				&& !GameMode.SURVIVAL.equals(currentGamemode)
		) {
			return;
		}
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
		if (GuildPlotUtils.isGuildPlot(loc)) {
			return true;
		}
		if (hasZoneProperty(loc, ZoneProperty.PLOT)) {
			return true;
		}
		if (!isTownWorld &&
			!hasZoneProperty(loc, ZoneProperty.SHOPS_POSSIBLE)) {
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
		Plugin scriptedQuestsPlugin;
		scriptedQuestsPlugin = (Plugin) Bukkit.getPluginManager().getPlugin("ScriptedQuests");

		return scriptedQuestsPlugin.mZoneManager.hasProperty(loc, namespace, property.getPropertyName());
	}

	public static Optional<Zone> getZone(Location loc) {
		return getZone(loc, "default");
	}

	public static Optional<Zone> getZone(Location loc, String namespace) {
		Plugin scriptedQuestsPlugin;
		scriptedQuestsPlugin = (Plugin) Bukkit.getPluginManager().getPlugin("ScriptedQuests");

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
