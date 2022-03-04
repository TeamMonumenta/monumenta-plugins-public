package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.graves.GraveManager;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;



public final class JunkItemListener implements Listener {
	public static final String COMMAND = "pickup";
	public static final String ALIAS = "pu";

	private static final String LORE_TAG = "OnlyLoredItemsPickup";
	private static final String INTERESTING_TAG = "NoJunkItemsPickup";

	private static final String PICKUP_MIN_OBJ_NAME = "PickupMin";
	private static final int JUNK_ITEM_SIZE_THRESHOLD = 17;
	private static final int MAX_POSSIBLE_STACK = 64;

	private final Set<UUID> mLorePlayers = new HashSet<>();
	private final Set<UUID> mInterestingPlayers = new HashSet<>();

	public JunkItemListener() {
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.pickup");

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.executes((sender, args) -> {
				playerToggle(sender);
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.withArguments(new LiteralArgument("lore"))
			.executes((sender, args) -> {
				pickupLore(sender);
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.withArguments(new LiteralArgument("interesting"))
			.executes((sender, args) -> {
				pickupInteresting(sender);
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withAliases(ALIAS)
			.withArguments(new LiteralArgument("all"))
			.executes((sender, args) -> {
				pickupAll(sender);
			})
			.register();

		new CommandAPICommand(COMMAND) // Sets PickupMin, but does not change pickup status
			.withPermission(perms)
			.withAliases(ALIAS)
			.withArguments(new LiteralArgument("threshold"), new IntegerArgument("count"))
			.executes((sender, args) -> {
				playerSetMin(sender, (int) args[0]);
			})
			.register();

	}

	private void playerToggle(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);

		if (hasTag(player)) {
			pickupAll(player);
		} else {
			pickupInteresting(player);
		}
	}

	private void playerSetMin(CommandSender sender, int newMin) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);

		newMin = Math.max(1, Math.min(newMin, MAX_POSSIBLE_STACK + 1));
		ScoreboardUtils.setScoreboardValue(player, PICKUP_MIN_OBJ_NAME, newMin);
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Threshold to pick up uninteresting items set to " + newMin + ".");
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void join(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Set<String> tags = player.getScoreboardTags();
		if (tags != null) {
			UUID uuid = player.getUniqueId();
			if (tags.contains(LORE_TAG)) {
				mLorePlayers.add(uuid);
			} else if (tags.contains(INTERESTING_TAG)) {
				mInterestingPlayers.add(uuid);
			}
		}
	}

	private void pickupAll(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You will now pick up all items.");
	}

	private void pickupLore(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.addScoreboardTag(LORE_TAG);
		mLorePlayers.add(player.getUniqueId());
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You will now only pick up items with lore text.");
	}

	private void pickupInteresting(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.addScoreboardTag(INTERESTING_TAG);
		mInterestingPlayers.add(player.getUniqueId());
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You will no longer pick up uninteresting items.");
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void quit(PlayerQuitEvent event) {
		removeFromSets(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void pickupItem(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player) {
			Item entity = event.getItem();
			ItemStack item = entity.getItemStack();
			if (item == null || item.getType().isAir()) {
				return;
			}
			Player player = (Player) event.getEntity();
			UUID uuid = player.getUniqueId();
			PlayerInventory inv = player.getInventory();

			// If they're in none of the groups, quit immediately without doing a bunch of checks
			if (!(mInterestingPlayers.contains(uuid) || mLorePlayers.contains(uuid))) {
				return;
			}

			// Allow collection of any items on the hotbar
			for (int i = 0; i <= 8; i++) {
				ItemStack hotbarItem = inv.getItem(i);
				if (hotbarItem != null && hotbarItem.isSimilar(item)) {
					// This is the same as something on the player's hotbar, definitely don't want to cancel pickup
					return;
				}
			}

			// Allow collection of death piles and valuable player-dropped items
			if (GraveManager.isGraveItem(entity) || GraveManager.isThrownItem(entity)) {
				return;
			}

			int minStack = ScoreboardUtils.getScoreboardValue(player, PICKUP_MIN_OBJ_NAME).orElse(0);
			if (minStack <= 0) { // Initializes PickupMin at JUNK_ITEM_SIZE_THRESHOLD; removes useless PickupMin values
				minStack = JUNK_ITEM_SIZE_THRESHOLD;
				ScoreboardUtils.setScoreboardValue(player, PICKUP_MIN_OBJ_NAME, minStack);
			}

			// If the stack size is at least the specified size, bypass restrictions
			if (item.getAmount() >= minStack) {
				return;
			}

			if (mLorePlayers.contains(uuid)) {
				if (!ItemUtils.hasLore(item)) {
					event.setCancelled(true);
				}
			} else if (mInterestingPlayers.contains(uuid)) {
				if (!ItemUtils.isInteresting(item)) {
					event.setCancelled(true);
				}
			}
		}
	}

	private boolean hasTag(Player player) {
		Set<String> tags = player.getScoreboardTags();
		return tags.contains(INTERESTING_TAG) || tags.contains(LORE_TAG);
	}

	private void remove(Player player) {
		player.removeScoreboardTag(LORE_TAG);
		player.removeScoreboardTag(INTERESTING_TAG);
		removeFromSets(player);
	}

	private void removeFromSets(Player player) {
		UUID uuid = player.getUniqueId();
		mLorePlayers.remove(uuid);
		mInterestingPlayers.remove(uuid);
	}
}
