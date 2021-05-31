package com.playmonumenta.plugins.listeners;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.playmonumenta.plugins.graves.GraveManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

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

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;



public class JunkItemListener implements Listener {
	private static final String NO_JUNK_ITEMS_TAG = "NoJunkItemsPickup";
	private static final String PICKUP_MIN_OBJ_NAME = "PickupMin";
	private static final int JUNK_ITEM_SIZE_THRESHOLD = 17;
	private static final int MAX_POSSIBLE_STACK = 64;
	private final Set<UUID> mPlayers = new HashSet<>();

	public JunkItemListener() {
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.pickup");

		new CommandAPICommand("pickup") // Only toggles
			.withPermission(perms)
			.withAliases("pu")
			.executes((sender, args) -> {
				playerToggle(sender);
			})
			.register();

		new CommandAPICommand("pickup") // Sets PickupMin, and always turns pickup on
		.withPermission(perms)
		.withArguments(new IntegerArgument("threshold"))
		.withAliases("pu")
		.executes((sender, args) -> {
			playerSetMin(sender, (int)args[0]);
		})
		.register();

	}

	private void playerToggle(CommandSender sender) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);

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

	private void playerSetMin(CommandSender sender, int newMin) throws WrapperCommandSyntaxException {
		Player player = CommandUtils.getPlayerFromSender(sender);

		ScoreboardUtils.setScoreboardValue(player, PICKUP_MIN_OBJ_NAME, newMin);

		Set<String> tags = player.getScoreboardTags();
		if (!tags.contains(NO_JUNK_ITEMS_TAG)) { // Only need to toggle if set to pick up all; does not call playerToggle to avoid redundant messages
			tags.add(NO_JUNK_ITEMS_TAG);
			mPlayers.add(player.getUniqueId());
		}

		if (newMin > MAX_POSSIBLE_STACK) {
			player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You will no longer pick up uninteresting items");
		} else {
			player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You will no longer pick up uninteresting items in stacks less than " + newMin);
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

			Player player = (Player)event.getEntity();
			PlayerInventory inv = player.getInventory();

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

			int minStack = ScoreboardUtils.getScoreboardValue(player, PICKUP_MIN_OBJ_NAME);
			if (minStack <= 0) { // Initializes PickupMin at JUNK_ITEM_SIZE_THRESHOLD; removes useless PickupMin values
				minStack = JUNK_ITEM_SIZE_THRESHOLD;
				ScoreboardUtils.setScoreboardValue(player, PICKUP_MIN_OBJ_NAME, minStack);
			}

			// Cancel pickup of non-interesting items that aren't on the player's hotbar
			if (!isInteresting(item, minStack)) {
				event.setCancelled(true);
			}
		}
	}

	private boolean isInteresting(ItemStack item, int minStack) {
		return item.getAmount() >= minStack
		       || ServerProperties.getAlwaysPickupMats().contains(item.getType())
		       || (item.hasItemMeta() && (item.getItemMeta().hasLore() ||
					                      (item.getItemMeta().hasDisplayName()
										   && ServerProperties.getNamedPickupMats().contains(item.getType()))));
	}
}
