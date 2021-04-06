package com.playmonumenta.plugins.listeners;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
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

import com.playmonumenta.plugins.graves.GraveManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class JunkItemListener implements Listener {
	private static final String NO_JUNK_ITEMS_TAG = "NoJunkItemsPickup";
	private static final int JUNK_ITEM_SIZE_THRESHOLD = 17;
	private final Set<UUID> mPlayers = new HashSet<>();

	public JunkItemListener() {
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.pickup");

		new CommandAPICommand("pickup")
			.withPermission(perms)
			.withAliases("pu")
			.executes((sender, args) -> {
				playerToggle(sender);
			})
			.register();
	}

	private void playerToggle(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = null;

		if (sender instanceof ProxiedCommandSender) {
			sender = ((ProxiedCommandSender)sender).getCallee();
		}

		if (sender instanceof Player) {
			player = (Player)sender;
		} else {
			CommandAPI.fail("This command must be run by/as a player!");
		}

		Set<String> tags = player.getScoreboardTags();
		if (tags.contains(NO_JUNK_ITEMS_TAG)) {
			tags.remove(NO_JUNK_ITEMS_TAG);
			mPlayers.remove(player.getUniqueId());
			player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You will now pick up all items");
		} else {
			tags.add(NO_JUNK_ITEMS_TAG);
			mPlayers.add(player.getUniqueId());
			player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You will no longer pick up uninteresting items");
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void join(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (player.getScoreboardTags() != null && player.getScoreboardTags().contains(NO_JUNK_ITEMS_TAG)) {
			mPlayers.add(player.getUniqueId());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void quit(PlayerQuitEvent event) {
		mPlayers.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void pickupItem(EntityPickupItemEvent event) {
		if (!event.isCancelled() && (event.getEntity() instanceof Player)) {
			Item entity = event.getItem();
			ItemStack item = entity.getItemStack();

			if (!mPlayers.contains(event.getEntity().getUniqueId()) || item.getType().isAir()) {
				return;
			}

			PlayerInventory inv = ((Player)event.getEntity()).getInventory();

			// Allow collection of any items on the hotbar
			for (int i = 0; i <= 8; i++) {
				ItemStack hotbarItem = inv.getItem(i);
				if (hotbarItem != null && hotbarItem.getType().equals(item.getType()) && hotbarItem.isSimilar(item)) {
					// This is the same as something on the player's hotbar, definitely don't want to cancel pickup
					return;
				}
			}

			// Allow collection of death piles and valuable player-dropped items
			if (GraveManager.isGraveItem(entity) || GraveManager.isThrownItem(entity)) {
				return;
			}

			// Cancel pickup of non-interesting items that aren't on the player's hotbar
			if (!isInteresting(item)) {
				event.setCancelled(true);
			}
		}
	}

	private boolean isInteresting(ItemStack item) {
		return item.getAmount() >= JUNK_ITEM_SIZE_THRESHOLD
		       || ServerProperties.getAlwaysPickupMats().contains(item.getType())
		       || (item.hasItemMeta() && (item.getItemMeta().hasLore() ||
					                      (item.getItemMeta().hasDisplayName()
										   && ServerProperties.getNamedPickupMats().contains(item.getType()))));
	}
}
