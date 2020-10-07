package com.playmonumenta.plugins.listeners;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import com.playmonumenta.plugins.server.properties.ServerProperties;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class JunkItemListener implements Listener {
	private static final String NO_JUNK_ITEMS_TAG = "NoJunkItemsPickup";
	private static final int JUNK_ITEM_SIZE_THRESHOLD = 17;
	private final Set<Player> mPlayers = new HashSet<Player>();

	public JunkItemListener() {
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.pickup");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		new CommandAPICommand("pickup")
			.withPermission(perms)
			.withArguments(arguments)
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
			mPlayers.remove(player);
			player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You will now pick up all items");
		} else {
			tags.add(NO_JUNK_ITEMS_TAG);
			mPlayers.add(player);
			player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You will no longer pick up uninteresting items");
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void join(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (player.getScoreboardTags() != null && player.getScoreboardTags().contains(NO_JUNK_ITEMS_TAG)) {
			mPlayers.add(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void quit(PlayerQuitEvent event) {
		mPlayers.remove(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void pickupItem(EntityPickupItemEvent event) {
		if (!event.isCancelled() && (event.getEntity() instanceof Player)) {
			ItemStack item = event.getItem().getItemStack();
			if (mPlayers.contains((Player)event.getEntity()) && !isInteresting(item)) {
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
