package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.integrations.luckperms.guildgui.GuildGui;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.worlds.paper.MonumentaWorldManagementAPI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Nameable;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LoomInventory;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public class GuildPlotUtils {
	public static Pattern RE_GUILD_PLOT_NAME = Pattern.compile("^guildplot(\\d+)$");
	public static final String LAST_GUILD_WORLD_OBJECTIVE = "Guild";
	public static final String LAST_GUILD_WORLD_TYPE_OBJECTIVE = "GuildPlotType";
	public static final String SHARD_NAME = "guildplots";
	public static final Component FALLBACK_WORLD_COMPONENT
		= Component.text("Guild Hub", NamedTextColor.LIGHT_PURPLE)
		.decoration(TextDecoration.ITALIC, false);
	public static final Component NO_TRAVEL_ANCHOR_ACCESS_COMPONENT
		= Component.text("You cannot access this guild's travel anchors.", NamedTextColor.RED);
	public static final List<Map<ItemStack, Integer>> PLOT_COSTS = new ArrayList<>();

	public static void initialize(Location loc) {
		ItemStack hyperExperience
			= InventoryUtils.getItemFromLootTableOrWarn(
			loc,
			NamespacedKeyUtils.fromString("epic:r1/items/currency/hyper_experience")
		);
		ItemStack royalCrystal
			= InventoryUtils.getItemFromLootTableOrWarn(
			loc,
			NamespacedKeyUtils.fromString("epic:r1/items/currency/royal_crystal")
		);
		ItemStack hyperCrystalineShard
			= InventoryUtils.getItemFromLootTableOrWarn(
			loc,
			NamespacedKeyUtils.fromString("epic:r2/items/currency/hyper_crystalline_shard")
		);
		ItemStack gleamingSeashell
			= InventoryUtils.getItemFromLootTableOrWarn(
			loc,
			NamespacedKeyUtils.fromString("epic:r2/items/gleaming_seashell")
		);
		ItemStack hyperArchosRing
			= InventoryUtils.getItemFromLootTableOrWarn(
			loc,
			NamespacedKeyUtils.fromString("epic:r3/items/currency/hyperchromatic_archos_ring")
		);
		ItemStack godtreeCarving
			= InventoryUtils.getItemFromLootTableOrWarn(
			loc,
			NamespacedKeyUtils.fromString("epic:r3/items/currency/godtree_carving")
		);

		// LinkedHashMap iterates in insertion order (used so there's no surprises in the order they appear)

		if (hyperExperience != null && royalCrystal != null) {
			Map<ItemStack, Integer> altCost = new LinkedHashMap<>();
			altCost.put(hyperExperience, 12);
			altCost.put(royalCrystal, 6);
			PLOT_COSTS.add(altCost);
		}

		if (hyperCrystalineShard != null && gleamingSeashell != null) {
			Map<ItemStack, Integer> altCost = new LinkedHashMap<>();
			altCost.put(hyperCrystalineShard, 12);
			altCost.put(gleamingSeashell, 6);
			PLOT_COSTS.add(altCost);
		}

		if (hyperArchosRing != null && godtreeCarving != null) {
			Map<ItemStack, Integer> altCost = new LinkedHashMap<>();
			altCost.put(hyperArchosRing, 12);
			altCost.put(godtreeCarving, 6);
			PLOT_COSTS.add(altCost);
		}
	}

	public static long getLastGuildPlotScore(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, LAST_GUILD_WORLD_OBJECTIVE).orElse(0);
	}

	public static void setLastGuildPlotScore(Player player, @Nullable Group guild) {
		if (guild == null) {
			ScoreboardUtils.setScoreboardValue(player, LAST_GUILD_WORLD_OBJECTIVE, 0);
			return;
		}

		Long guildPlotId = LuckPermsIntegration.getGuildPlotId(guild);
		if (guildPlotId == null) {
			player.sendMessage(Component.text("", NamedTextColor.RED)
				.append(LuckPermsIntegration.getGuildFullComponent(guild))
				.append(Component.text("That guild does not have its numeric guild ID set. Contact a moderator."))
			);
			return;
		}
		int guildPlotType = LuckPermsIntegration.getPlotTypeId(guild);
		if (guildPlotId <= 0 || guildPlotId > Integer.MAX_VALUE) {
			player.sendMessage(Component.text("", NamedTextColor.RED)
				.append(LuckPermsIntegration.getGuildFullComponent(guild))
				.append(Component.text("That guild somehow has an invalid numeric guild ID set. Contact a moderator."))
			);
			return;
		}

		ScoreboardUtils.setScoreboardValue(player, LAST_GUILD_WORLD_OBJECTIVE, guildPlotId.intValue());
		ScoreboardUtils.setScoreboardValue(player, LAST_GUILD_WORLD_TYPE_OBJECTIVE, guildPlotType);
	}

	public static void sendToGuildPlotsShard(Player player) {
		try {
			MonumentaRedisSyncAPI.sendPlayer(player, SHARD_NAME);
		} catch (Exception ex) {
			player.sendMessage(Component.text(
				"Failed to send you to the guildplots shard:", NamedTextColor.RED));
			MessagingUtils.sendStackTrace(player, ex);
		}
	}

	public static void sendGuildPlotHub(Player player, boolean transferFromOtherShard) {
		ScoreboardUtils.setScoreboardValue(player, LAST_GUILD_WORLD_OBJECTIVE, 0);
		if (ServerProperties.getShardName().startsWith(SHARD_NAME)) {
			try {
				MonumentaWorldManagementAPI.sortWorld(player);
			} catch (Exception ex) {
				player.sendMessage(Component.text(
					"Failed to send you to the guild plot fallback world:", NamedTextColor.RED));
				MessagingUtils.sendStackTrace(player, ex);
			}
		} else if (transferFromOtherShard) {
			sendToGuildPlotsShard(player);
		}
	}

	public static void sendGuildPlotWorld(Player player, @Nullable Group guild) {
		if (guild == null) {
			sendGuildPlotHub(player, true);
			return;
		}

		setLastGuildPlotScore(player, guild);
		if (ServerProperties.getShardName().startsWith(SHARD_NAME)) {
			try {
				MonumentaWorldManagementAPI.sortWorld(player);
			} catch (Exception ex) {
				player.sendMessage(Component.text("Failed to send you to the guild plot for ", NamedTextColor.RED)
					.append(LuckPermsIntegration.getGuildFullComponent(guild))
				);
				MessagingUtils.sendStackTrace(player, ex);
			}
		} else {
			sendToGuildPlotsShard(player);
		}
	}

	public static boolean isGuildPlot(@Nullable Location location) {
		if (location == null) {
			return false;
		}
		return isGuildPlot(location.getWorld());
	}

	public static boolean isGuildPlot(@Nullable World world) {
		if (world == null) {
			return false;
		}
		return isGuildPlot(world.getName());
	}

	public static boolean isGuildPlot(@Nullable String worldName) {
		if (worldName == null) {
			return false;
		}

		Matcher matcher = RE_GUILD_PLOT_NAME.matcher(worldName);
		return matcher.matches();
	}

	public static @Nullable Long getGuildPlotNumber(@Nullable Location location) {
		if (location == null) {
			return null;
		}
		return getGuildPlotNumber(location.getWorld());
	}

	public static @Nullable Long getGuildPlotNumber(@Nullable World world) {
		if (world == null) {
			return null;
		}
		return getGuildPlotNumber(world.getName());
	}

	public static @Nullable Long getGuildPlotNumber(@Nullable String worldName) {
		if (worldName == null) {
			return null;
		}

		Matcher matcher = RE_GUILD_PLOT_NAME.matcher(worldName);
		if (!matcher.matches()) {
			return null;
		}

		try {
			return Long.decode(matcher.group(1));
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	public static String guildPlotName(long plotNumber) {
		return "guildplot" + plotNumber;
	}

	/**
	 * Checks if the player should be in adventure mode or survival mode in a given plot
	 *
	 * @param player The player to be checked at their current location
	 * @return null if the player is not in a guild plot,
	 * survival if they have access to their current plot,
	 * otherwise adventure mode
	 */
	public static @Nullable GameMode guildPlotGameMode(Player player) {
		Long guildPlotNumber = getGuildPlotNumber(player.getWorld());
		if (guildPlotNumber == null) {
			// Not a guild plot
			return null;
		}

		if (!ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.PLOT)) {
			// You want to break free? Too bad.
			return GameMode.ADVENTURE;
		}

		Group guild = LuckPermsIntegration.getLoadedGuildByPlotId(guildPlotNumber);
		if (guild == null) {
			return GameMode.ADVENTURE;
		}

		boolean hasPerm = GuildPermission.SURVIVAL.hasAccess(guild, player);
		return hasPerm ? GameMode.SURVIVAL : GameMode.ADVENTURE;
	}

	/**
	 * Kick players who should not be on a guild plot
	 *
	 * @param player The player to be checked
	 */
	public static void guildPlotAccessCheckAndKick(Player player) {
		Location loc = player.getLocation();
		if (!isGuildPlot(loc)) {
			// Not a guild plot
			return;
		}

		if (player.hasPermission(GuildGui.MOD_GUI_PERMISSION)) {
			// Moderators bypass this check
			return;
		}

		Long guildPlotId = getGuildPlotNumber(loc);
		Group guild = LuckPermsIntegration.getLoadedGuildByPlotId(guildPlotId);
		if (guild != null) {
			if (
				!LuckPermsIntegration.isLocked(guild)
					&& GuildPermission.VISIT.hasAccess(guild, player)
			) {
				return;
			}
		} // else the guild can't be found, probably deleted?

		// Checks failed, kick them!
		player.teleport(loc.getWorld().getSpawnLocation());
		sendGuildPlotHub(player, false);
	}

	/**
	 * Check if the current guild plot prevents viewing inventory access
	 *
	 * @param player to check access for
	 * @return True if not allowed to view inventories, otherwise false
	 */
	public static boolean guildPlotInventoryViewBlocked(Player player) {
		if (player.getGameMode().equals(GameMode.CREATIVE)) {
			return false;
		}

		Long guildPlotNumber = getGuildPlotNumber(player.getWorld());
		if (guildPlotNumber == null) {
			// Not a guild plot
			return false;
		}

		Group guild = LuckPermsIntegration.getLoadedGuildByPlotId(guildPlotNumber);
		if (guild == null) {
			return true;
		}

		return !GuildPermission.VIEW_ITEMS.hasAccess(guild, player);
	}

	public static boolean guildPlotInventoryViewBlocked(Player player, Inventory inventory) {
		if (player.getGameMode().equals(GameMode.CREATIVE)) {
			return false;
		}

		if (!guildPlotInventoryViewBlocked(player)) {
			return false;
		}

		return inventoryIsNotAlwaysAllowed(player, inventory);
	}

	/**
	 * Check if the current guild plot prevents modifying inventory access
	 *
	 * @param player to check access for
	 * @return True if not allowed to modify inventories, otherwise false
	 */
	public static boolean guildPlotInventoryModificationBlocked(Player player) {
		if (player.getGameMode().equals(GameMode.CREATIVE)) {
			return false;
		}

		Long guildPlotNumber = getGuildPlotNumber(player.getWorld());
		if (guildPlotNumber == null) {
			// Not a guild plot
			return false;
		}

		Group guild = LuckPermsIntegration.getLoadedGuildByPlotId(guildPlotNumber);
		if (guild == null) {
			return true;
		}

		if (guildPlotInventoryViewBlocked(player)) {
			return true;
		}

		return !GuildPermission.MOVE_ITEMS.hasAccess(guild, player);
	}

	public static boolean guildPlotInventoryModificationBlocked(Player player, Inventory inventory) {
		if (player.getGameMode().equals(GameMode.CREATIVE)) {
			return false;
		}

		if (!guildPlotInventoryModificationBlocked(player)) {
			return false;
		}

		return inventoryIsNotAlwaysAllowed(player, inventory);
	}

	public static boolean inventoryIsNotAlwaysAllowed(Player player, Inventory inventory) {
		InventoryHolder holder = inventory.getHolder();
		if (player.equals(holder)) {
			return false;
		}

		if (
			holder instanceof DoubleChest doubleChest &&
				// These checks could be null for virtual inventories, or non-chests for plugin/mod shenanigans
				doubleChest.getLeftSide() instanceof Chest left &&
				doubleChest.getRightSide() instanceof Chest right
		) {
			Component customName;
			// In future versions, Northwest (negative-most coordinate) chest's name is used if set, otherwise Southeast if set
			// In this version, though, left always wins first.
			customName = left.customName();
			if (customName == null) {
				// Not set, use right
				customName = right.customName();
			}

			if (
				customName != null
					&& MessagingUtils.plainText(customName).equals("Community Chest")
			) {
				return false;
			}
		}

		if (holder instanceof Nameable nameable) {
			Component customName = nameable.customName();

			if (
				customName != null
					&& holder instanceof Barrel
					&& MessagingUtils.plainText(customName).equals("Sharrel")
			) {
				return false;
			}

			if (
				customName != null
					&& holder instanceof Chest
					&& MessagingUtils.plainText(customName).equals("Community Chest")
			) {
				return false;
			}
		}

		if (inventory.equals(player.getEnderChest())) {
			return false;
		}

		return !(
			inventory instanceof CraftingInventory
				|| inventory instanceof LoomInventory
				|| inventory instanceof PlayerInventory
		);
	}

	public static boolean guildPlotChangeVaultOwnerBlocked(Player player) {
		if (player.getGameMode().equals(GameMode.CREATIVE)) {
			return false;
		}

		Long guildPlotNumber = getGuildPlotNumber(player.getWorld());
		if (guildPlotNumber == null) {
			// Not a guild plot
			return false;
		}

		Group guild = LuckPermsIntegration.getLoadedGuildByPlotId(guildPlotNumber);
		if (guild == null) {
			return true;
		}

		return !GuildPermission.EDIT_VAULT_OWNERSHIP.hasAccess(guild, player);
	}

	public static boolean guildPlotUseVaultBlocked(Player player) {
		if (player.getGameMode().equals(GameMode.CREATIVE)) {
			return false;
		}

		Long guildPlotNumber = getGuildPlotNumber(player.getWorld());
		if (guildPlotNumber == null) {
			// Not a guild plot
			return false;
		}

		Group guild = LuckPermsIntegration.getLoadedGuildByPlotId(guildPlotNumber);
		if (guild == null) {
			return true;
		}

		return !GuildPermission.USE_VAULT.hasAccess(guild, player);
	}

	public static boolean guildPlotEditTravelAnchorBlocked(Player player) {
		if (player.getGameMode().equals(GameMode.CREATIVE)) {
			return false;
		}

		Long guildPlotNumber = getGuildPlotNumber(player.getWorld());
		if (guildPlotNumber == null) {
			// Not a guild plot
			return false;
		}

		Group guild = LuckPermsIntegration.getLoadedGuildByPlotId(guildPlotNumber);
		if (guild == null) {
			return true;
		}

		return !GuildPermission.EDIT_TRAVEL_ANCHOR.hasAccess(guild, player);
	}

	public static boolean guildPlotUseTravelAnchorBlocked(Player player) {
		if (player.getGameMode().equals(GameMode.CREATIVE)) {
			return false;
		}

		Long guildPlotNumber = getGuildPlotNumber(player.getWorld());
		if (guildPlotNumber == null) {
			// Not a guild plot
			return false;
		}

		Group guild = LuckPermsIntegration.getLoadedGuildByPlotId(guildPlotNumber);
		if (guild == null) {
			return true;
		}

		return !GuildPermission.USE_TRAVEL_ANCHOR.hasAccess(guild, player);
	}

	public static boolean guildPlotUseEggsBlocked(Player player) {
		if (player.getGameMode().equals(GameMode.CREATIVE)) {
			return false;
		}

		Long guildPlotNumber = getGuildPlotNumber(player.getWorld());
		if (guildPlotNumber == null) {
			// Not a guild plot
			return false;
		}

		Group guild = LuckPermsIntegration.getLoadedGuildByPlotId(guildPlotNumber);
		if (guild == null) {
			return true;
		}

		return !GuildPermission.EGGS.hasAccess(guild, player);
	}
}
