package pe.project.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import org.bukkit.ChatColor;
import pe.project.Main;
import pe.project.utils.NetworkUtils;

//	/transferserver <server name> <x1> <y1> <z1> <x2> <y2> <z2>

public class TransferServer implements CommandExecutor {
	Main mMain;

	public TransferServer(Main main) {
		mMain = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {

		if (arg3.length != 1 && arg3.length != 2) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			return false;
		}

		String server = arg3[0];

		// Default to sending equipment
		boolean sendPlayerStuff = true;

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
		try {
			if (sendPlayerStuff == true) {
				player.sendMessage(ChatColor.GOLD + "Transferring you to " + server);
				NetworkUtils.transferPlayerData(mMain, player, server);
			} else {
				player.sendMessage(ChatColor.GOLD + "Transferring you " + ChatColor.RED  + "without playerdata" + ChatColor.GOLD + " to " + server);
				NetworkUtils.sendPlayer(mMain, player, server);
			}
		} catch (Exception e) {
			sender.sendMessage("Caught exception when transferring players");
			return false;
		}

		return true;
	}

}
