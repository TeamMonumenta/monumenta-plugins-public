package com.playmonumenta.plugins.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NetworkUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.BooleanArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

public class TransferServer extends GenericCommand {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		String command = "transferserver";
		CommandPermission perms = CommandPermission.fromString("monumenta.command.transferserver");

		/* No-argument variant to get server list */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      if (sender instanceof Player) {
		                                          getServerList(plugin, sender, (Player)sender);
		                                      } else {
		                                          error(sender, "No-argument variant can only be run by players");
		                                      }
		                                  }
		);

		/* Single player argument to get server list */
		arguments = new LinkedHashMap<>();
		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      for (Player player : (Collection<Player>)args[0]) {
		                                          getServerList(plugin, sender, player);
		                                      }
		                                  }
		);

		/* Transfer with data by default */
		arguments = new LinkedHashMap<>();
		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("server", new StringArgument());
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      sendPlayer(plugin, (Collection<Player>)args[0],
		                                                 (String)args[1], true);
		                                  }
		);

		/* Transfer specifying data true/false */
		arguments = new LinkedHashMap<>();
		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("server", new StringArgument());
		arguments.put("sendData", new BooleanArgument());
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      sendPlayer(plugin, (Collection<Player>)args[0],
		                                                 (String)args[1], (Boolean)args[2]);
		                                  }
		);
	}

	private static void getServerList(Plugin plugin, CommandSender sender, Player player) {
		try {
			NetworkUtils.getServerList(plugin, player);
		} catch (Exception e) {
			error(sender, "An error occurred while requesting the server list");
		}
	}

	private static void sendPlayer(Plugin plugin, Collection<Player> players, String server, boolean sendPlayerStuff) {
		for (Player player : players) {
			/* Error if target server is not allowed */
			if (!(ServerProperties.getAllowedTransferTargets().isEmpty()
				  || ServerProperties.getAllowedTransferTargets().contains(server))) {
				error(player, "You may not transfer to that server from here!");
				error(player, "Allowed servers are: " + ServerProperties.getAllowedTransferTargets().toString());
				continue;
			}

			/* Can only send stuff if requested AND server properties allows it */
			sendPlayerStuff = sendPlayerStuff && ServerProperties.getTransferDataEnabled();

			/* Don't let the player transfer again if their inventory is still locked */
			if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
				error(player, "Player attempted to transfer with locked inventory!");
				error(player, "You can fix this by logging out and back in.");
				continue;
			}

			if (server.equalsIgnoreCase(ServerProperties.getShardName())) {
				error(player, "Can not transfer to the same server you are already on");
				continue;
			}

			try {
				if (sendPlayerStuff) {
					player.sendMessage(ChatColor.GOLD + "Transferring you to " + server);

					/* Mark this player as inventory locked */
					player.setMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY, new FixedMetadataValue(plugin, true));

					int dropped = InventoryUtils.removeSpecialItems(player, false);
					if (dropped == 1) {
						player.sendMessage(ChatColor.RED + "The dungeon key you were carrying was dropped!");
					} else if (dropped > 1) {
						player.sendMessage(ChatColor.RED + "The dungeon keys you were carrying were dropped!");
					}

					//Update custom enchants after removal of items
					plugin.mTrackingManager.mPlayers.updateEquipmentProperties(player, null);

					if (!NetworkUtils.transferPlayerData(plugin, player, server)) {
						// Failed to send, unlock the player's inventory and let them know to retry later
						player.removeMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY, plugin);
						player.sendMessage(ChatColor.RED + "Transfer failed! Please try again.");
						player.sendMessage(ChatColor.RED + "If this issue persists, please ask a moderator for assistance.");
					}

					/* TODO: Some kind of timeout to unlock player when transfer doesn't go through */
				} else {
					player.sendMessage(ChatColor.GOLD + "Transferring you " + ChatColor.RED + "without playerdata" +
					                   ChatColor.GOLD + " to " + server);
					NetworkUtils.sendPlayer(plugin, player, server);
				}
			} catch (Exception e) {
				error(player, "Caught exception when transferring players");
				plugin.getLogger().severe("Caught exception when transferring players" + e);
				e.printStackTrace();
				continue;
			}
		}
	}
}

