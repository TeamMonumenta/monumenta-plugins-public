package pe.project.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import org.bukkit.ChatColor;

import pe.project.Constants;
import pe.project.Plugin;
import pe.project.utils.NetworkUtils;
import pe.project.utils.InventoryUtils;

//	/transferserver <server name> <x1> <y1> <z1> <x2> <y2> <z2>

public class TransferServer implements CommandExecutor {
	Plugin mPlugin;

	public TransferServer(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length > 2 || (arg3.length == 0 && !(sender instanceof Player))) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			return false;
		}

		if (arg3.length == 0 && sender instanceof Player) {
			// No arguments - print usage and request list of available servers
			sender.sendMessage(ChatColor.RED + "Usage: " + command.getUsage());
			try {
				NetworkUtils.getServerList(mPlugin, (Player)sender);
			} catch (Exception e) {
				sender.sendMessage("Requesting server list from bungee failed");
			}
			return true;
		}

		String server = arg3[0];

		// Error if target server is now allowed
		if (!(mPlugin.mServerProperties.mAllowedTransferTargets.isEmpty()
			  || mPlugin.mServerProperties.mAllowedTransferTargets.contains(server))) {
			sender.sendMessage(ChatColor.RED + "You may not transfer to that server from here!");
			sender.sendMessage(ChatColor.RED + "Allowed servers are: " +
			                   mPlugin.mServerProperties.mAllowedTransferTargets.toString());
			return false;
		}

		// Default to server properties - if properties says false, no way to set to true
		boolean sendPlayerStuff = mPlugin.mServerProperties.getTransferDataEnabled();

		if (arg3.length == 2) {
			if (arg3[1].equals("False") || arg3[1].equals("false") || arg3[1].equals("f") || arg3[1].equals("F")) {
				sendPlayerStuff = false;
			}
		}

		if (sender instanceof Player) {
			// Sender is requesting transfer to destination server with equipment
			return _transferServer(sender, (Player)sender, sendPlayerStuff, server);
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
			CommandSender caller = ((ProxiedCommandSender)sender).getCaller();
			if (callee instanceof Player) {
				// Sender is an /execute command targeting a player
				caller.sendMessage("Transferring " + callee.getName() + " with playerdata to " + server);
				return _transferServer(caller, (Player)callee, sendPlayerStuff, server);
			} else {
				sender.sendMessage(ChatColor.RED + "Execute command detected with non-player target!");
				return false;
			}
		} else {
			// Only players can be sent!
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters for non-player sender!");
			return false;
		}
	}

	private boolean _transferServer(CommandSender sender, Player player, boolean sendPlayerStuff, String server) {
		/* Don't let the player transfer again if their inventory is still locked */
		if (player.hasMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY)) {
			sender.sendMessage(ChatColor.RED + "Player attempted to transfer with locked inventory!");
			sender.sendMessage(ChatColor.YELLOW + "You can fix this by logging out and back in.");
			return false;
		}

		try {
			if (sendPlayerStuff == true) {
				player.sendMessage(ChatColor.GOLD + "Transferring you to " + server);

				/* Mark this player as inventory locked */
				player.setMetadata(Constants.PLAYER_ITEMS_LOCKED_METAKEY, new FixedMetadataValue(mPlugin, true));

				InventoryUtils.removeSpecialItems(player);

				NetworkUtils.transferPlayerData(mPlugin, player, server);
			} else {
				player.sendMessage(ChatColor.GOLD + "Transferring you " + ChatColor.RED  + "without playerdata" + ChatColor.GOLD + " to " + server);
				NetworkUtils.sendPlayer(mPlugin, player, server);
			}
		} catch (Exception e) {
			sender.sendMessage("Caught exception when transferring players");
			return false;
		}

		return true;
	}
}
