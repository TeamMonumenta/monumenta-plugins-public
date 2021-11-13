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

import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;



public class ItemDropListener implements Listener {
	public static final String COMMAND = "disabledrop";

	private static final String LORE_TAG = "DisableDropLore";
	private static final String INTERESTING_TAG = "DisableDropInteresting";
	private static final String ALL_TAG = "DisableDropAll";

	private final Set<UUID> mLorePlayers = new HashSet<>();
	private final Set<UUID> mInterestingPlayers = new HashSet<>();
	private final Set<UUID> mAllPlayers = new HashSet<>();

	public ItemDropListener() {
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.disabledrop");

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.executes((sender, args) -> {
				playerToggle(sender);
			})
			.register();

		new CommandAPICommand(COMMAND)
		.withPermission(perms)
		.withArguments(new LiteralArgument("none"))
		.executes((sender, args) -> {
			enable(sender);
		})
		.register();

		new CommandAPICommand(COMMAND)
		.withPermission(perms)
		.withArguments(new LiteralArgument("lore"))
		.executes((sender, args) -> {
			disableLore(sender);
		})
		.register();

		new CommandAPICommand(COMMAND)
		.withPermission(perms)
		.withArguments(new LiteralArgument("interesting"))
		.executes((sender, args) -> {
			disableInteresting(sender);
		})
		.register();

		new CommandAPICommand(COMMAND)
		.withPermission(perms)
		.withArguments(new LiteralArgument("all"))
		.executes((sender, args) -> {
			disableAll(sender);
		})
		.register();

	}

	private void playerToggle(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);

		if (hasTag(player)) {
			enable(player);
		} else {
			disableAll(player);
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

		ItemStack item = itemEntity.getItemStack();
		if (!InventoryUtils.canFitInInventory(item, player.getInventory())) {
			return;
		}

		UUID uuid = event.getPlayer().getUniqueId();
		if (mAllPlayers.contains(uuid)) {
			event.setCancelled(true);
		} else if (mInterestingPlayers.contains(uuid)) {
			if (isInteresting(item)) {
				event.setCancelled(true);
			}
		} else if (mLorePlayers.contains(uuid)) {
			if (isLored(item)) {
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

	private boolean hasTag(Player player) {
		Set<String> tags = player.getScoreboardTags();
		return tags.contains(LORE_TAG) || tags.contains(INTERESTING_TAG) || tags.contains(ALL_TAG);
	}

	private void remove(Player player) {
		player.removeScoreboardTag(LORE_TAG);
		player.removeScoreboardTag(INTERESTING_TAG);
		player.removeScoreboardTag(ALL_TAG);
		removeFromSets(player);
	}

	private void removeFromSets(Player player) {
		UUID uuid = player.getUniqueId();
		mLorePlayers.remove(uuid);
		mInterestingPlayers.remove(uuid);
		mAllPlayers.remove(uuid);
	}

	private boolean isLored(ItemStack item) {
		return (item.hasItemMeta() && (item.getItemMeta().hasLore() || (item.getItemMeta().hasDisplayName() && ServerProperties.getNamedPickupMats().contains(item.getType()))));
	}

	private boolean isInteresting(ItemStack item) {
		return ServerProperties.getAlwaysPickupMats().contains(item.getType()) || isLored(item);
	}
}
