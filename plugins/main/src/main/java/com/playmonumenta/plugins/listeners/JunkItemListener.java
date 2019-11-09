package com.playmonumenta.plugins.listeners;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

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
import org.bukkit.scheduler.BukkitRunnable;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;

public class JunkItemListener implements Listener {
	private static final String NO_JUNK_ITEMS_TAG = "NoJunkItemsPickup";
	private static final int JUNK_ITEM_SIZE_THRESHOLD = 16;
	private final Plugin mPlugin;
	private final Set<Player> mPlayers = new HashSet<Player>();

	public JunkItemListener(Plugin plugin) {
		mPlugin = plugin;
		final String command = "pickup";
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.pickup");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  playerToggle(sender);
		                                  }
		);
	}

	private void playerToggle(CommandSender sender) throws CommandSyntaxException {
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
		/* TODO: This is a gross hack to work around player data transferring... */
		new BukkitRunnable() {
			@Override
			public void run() {
				Player player = event.getPlayer();
				if (player.getScoreboardTags() != null && player.getScoreboardTags().contains(NO_JUNK_ITEMS_TAG)) {
					mPlayers.add(player);
				}
			}
		}.runTaskLater(mPlugin, 30);
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
		       || mPlugin.mServerProperties.getAlwaysPickupMats().contains(item.getType())
		       || (item.hasItemMeta() && (item.getItemMeta().hasLore() ||
					                      (item.getItemMeta().hasDisplayName()
										   && mPlugin.mServerProperties.getNamedPickupMats().contains(item.getType()))));
	}
}
