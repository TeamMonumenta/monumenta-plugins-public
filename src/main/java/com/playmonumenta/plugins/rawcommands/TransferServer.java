package com.playmonumenta.plugins.rawcommands;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NetworkUtils;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.BooleanArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.StringArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

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
		                                      sendPlayer(plugin, sender, (Collection<Player>)args[0],
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
		                                      sendPlayer(plugin, sender, (Collection<Player>)args[0],
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

	private static void sendPlayer(Plugin plugin, CommandSender sender, Collection<Player>players,
	                               String server, boolean sendPlayerStuff) {
		for (Player player : players) {
			/* Error if target server is not allowed */
			if (!(plugin.mServerProperties.mAllowedTransferTargets.isEmpty()
				  || plugin.mServerProperties.mAllowedTransferTargets.contains(server))) {
				error(player, "You may not transfer to that server from here!");
				error(player, "Allowed servers are: " + plugin.mServerProperties.mAllowedTransferTargets.toString());
				continue;
			}

			/* Can only send stuff if requested AND server properties allows it */
			sendPlayerStuff = sendPlayerStuff && plugin.mServerProperties.getTransferDataEnabled();

			/* Don't let the player transfer again if their inventory is still locked */
			if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
				error(player, "Player attempted to transfer with locked inventory!");
				error(player, "You can fix this by logging out and back in.");
				continue;
			}

			try {
				if (sendPlayerStuff) {
					player.sendMessage(ChatColor.GOLD + "Transferring you to " + server);

					/* Mark this player as inventory locked */
					player.setMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY, new FixedMetadataValue(plugin, true));

					InventoryUtils.removeSpecialItems(player);

					NetworkUtils.transferPlayerData(plugin, player, server);
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

