package com.playmonumenta.plugins.listeners;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;



public class ItemDropListener implements Listener {
	public static final String COMMAND = "disabledrop";
	public static final String ALIAS = "dd";

	private static final String LORE_TAG = "DisableDropLore";
	private static final String INTERESTING_TAG = "DisableDropInteresting";
	private static final String ALL_TAG = "DisableDropAll";
	private static final String HOLDING_TAG = "DisableDropHolding";

	private final Set<UUID> mLorePlayers = new HashSet<>();
	private final Set<UUID> mInterestingPlayers = new HashSet<>();
	private final Set<UUID> mAllPlayers = new HashSet<>();
	private final Set<UUID> mHoldingPlayers = new HashSet<>();

	public ItemDropListener() {
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.disabledrop");

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
		.withArguments(new LiteralArgument("none"))
		.executes((sender, args) -> {
			enable(sender);
		})
		.register();

		new CommandAPICommand(COMMAND)
		.withPermission(perms)
		.withAliases(ALIAS)
		.withArguments(new LiteralArgument("lore"))
		.executes((sender, args) -> {
			disableLore(sender);
		})
		.register();

		new CommandAPICommand(COMMAND)
		.withPermission(perms)
		.withAliases(ALIAS)
		.withArguments(new LiteralArgument("interesting"))
		.executes((sender, args) -> {
			disableInteresting(sender);
		})
		.register();

		new CommandAPICommand(COMMAND)
		.withPermission(perms)
		.withAliases(ALIAS)
		.withArguments(new LiteralArgument("all"))
		.executes((sender, args) -> {
			disableAll(sender);
		})
		.register();

		new CommandAPICommand(COMMAND)
		.withPermission(perms)
		.withAliases(ALIAS)
		.withArguments(new LiteralArgument("holding"))
		.executes((sender, args) -> {
			disableHolding(sender);
		})
		.register();
	}

	private void playerToggle(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);

		if (hasTag(player)) {
			enable(player);
		} else {
			disableLore(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void join(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Set<String> tags = player.getScoreboardTags();
		if (tags != null) {
			UUID uuid = player.getUniqueId();
			if (tags.contains(LORE_TAG)) {
				mLorePlayers.add(uuid);
			} else if (tags.contains(INTERESTING_TAG)) {
				mInterestingPlayers.add(uuid);
			} else if (tags.contains(ALL_TAG)) {
				mAllPlayers.add(uuid);
			} else if (tags.contains(HOLDING_TAG)) {
				mHoldingPlayers.add(uuid);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void quit(PlayerQuitEvent event) {
		removeFromSets(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerDropItemEvent(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		Item itemEntity = event.getItemDrop();
		if (player == null || itemEntity == null || player.getGameMode() == GameMode.CREATIVE) {
			return;
		}

		// If the item wouldn't fit back into the inventory we need to throw it in order to not delete it
		ItemStack item = itemEntity.getItemStack();
		PlayerInventory playerInventory = player.getInventory();
		if (!InventoryUtils.canFitInInventory(item, playerInventory)) {
			return;
		}

		UUID uuid = event.getPlayer().getUniqueId();
		if (mAllPlayers.contains(uuid)) {
			event.setCancelled(true);
		} else if (mInterestingPlayers.contains(uuid)) {
			if (ItemUtils.isInteresting(item)) {
				event.setCancelled(true);
			}
		} else if (mLorePlayers.contains(uuid)) {
			if (ItemUtils.hasLore(item)) {
				event.setCancelled(true);
			}
		} else if (mHoldingPlayers.contains(uuid)) {
			//Some bugs here that I have no idea how to fix
			//Prevents dropping any item from inventory if holding nothing in mainhand
			//Prevents dropping items from inventory if they are the same as the item you're holding

			ItemStack mainhand = playerInventory.getItemInMainHand();
			if (mainhand == null || mainhand.getType().isAir() || mainhand.isSimilar(item)) {
				event.setCancelled(true);
			}
		}
	}

	private void enable(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You can now drop all items.");
	}

	private void disableLore(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.addScoreboardTag(LORE_TAG);
		mLorePlayers.add(player.getUniqueId());
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You can no longer drop items with lore text.");
	}

	private void disableInteresting(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.addScoreboardTag(INTERESTING_TAG);
		mInterestingPlayers.add(player.getUniqueId());
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You can no longer drop interesting items.");
	}

	private void disableAll(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.addScoreboardTag(ALL_TAG);
		mAllPlayers.add(player.getUniqueId());
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You can no longer drop items.");
	}

	private void disableHolding(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);
		remove(player);
		player.addScoreboardTag(HOLDING_TAG);
		mHoldingPlayers.add(player.getUniqueId());
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You can no longer drop the item you are holding.");
	}

	private boolean hasTag(Player player) {
		Set<String> tags = player.getScoreboardTags();
		return tags.contains(LORE_TAG) || tags.contains(INTERESTING_TAG) || tags.contains(ALL_TAG) || tags.contains(HOLDING_TAG);
	}

	private void remove(Player player) {
		player.removeScoreboardTag(LORE_TAG);
		player.removeScoreboardTag(INTERESTING_TAG);
		player.removeScoreboardTag(ALL_TAG);
		player.removeScoreboardTag(HOLDING_TAG);
		removeFromSets(player);
	}

	private void removeFromSets(Player player) {
		UUID uuid = player.getUniqueId();
		mLorePlayers.remove(uuid);
		mInterestingPlayers.remove(uuid);
		mAllPlayers.remove(uuid);
		mHoldingPlayers.remove(uuid);
	}
}
