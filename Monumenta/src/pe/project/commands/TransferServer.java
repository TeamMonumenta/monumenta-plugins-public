package pe.project.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import org.bukkit.ChatColor;
import pe.project.Main;
import pe.project.point.AreaBounds;
import pe.project.utils.CommandUtils;
import pe.project.utils.ScoreboardUtils;
import pe.project.utils.NetworkUtils;

//	/transferserver <server name> <x1> <y1> <z1> <x2> <y2> <z2>

public class TransferServer implements CommandExecutor {
	Main mMain;

	public TransferServer(Main main) {
		mMain = main;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		String scoreName = null;
		int scoreMin = 0;
		int scoreMax = 0;
		boolean sendPlayerStuff = true;

		if (arg3.length != 1 && arg3.length != 8 && arg3.length != 11) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			return false;
		}

		String server = arg3[0];

		if (arg3.length == 1) {
			if (sender instanceof Player) {
				// Sender is requesting transfer to destination server with equipment
				try {
					sender.sendMessage("Transferring with playerdata to " + server);
					NetworkUtils.transferPlayerData(mMain, (Player)sender, server);
					return true;
				} catch (Exception e) {
					sender.sendMessage("Caught exception when transferring players");
					return false;
				}
			} else if (sender instanceof ProxiedCommandSender) {
				CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
				CommandSender caller = ((ProxiedCommandSender)sender).getCaller();
				if (callee instanceof Player) {
					// Sender is an /execute command targeting a player
					try {
						caller.sendMessage("Transferring " + callee.getName() + " with playerdata to " + server);
						NetworkUtils.transferPlayerData(mMain, (Player)callee, server);
						return true;
					} catch (Exception e) {
						caller.sendMessage("Caught exception when transferring players");
						return false;
					}
				}
			} else {
				// Only players can be sent!
				sender.sendMessage(ChatColor.RED + "Invalid number of parameters for non-player sender!");
				return false;
			}

		}

		// Default to sending equipment
		if (arg3[1].equals("False") || arg3[1].equals("false")) {
			sendPlayerStuff = false;
		}

		// Need to only match players with the given scoreName value
		if (arg3.length == 11) {
			try {
				scoreName = arg3[8];
				scoreMin = CommandUtils.parseIntFromString(sender, arg3[9]);
				scoreMax = CommandUtils.parseIntFromString(sender, arg3[10]);
			} catch (Exception e) {
				return false;
			}
		}

		AreaBounds bounds;
		try {
			bounds = CommandUtils.parseAreaFromString(sender, arg3[2], arg3[3],
			                                          arg3[4], arg3[5], arg3[6], arg3[7]);
		} catch (Exception e) {
			return false;
		}

		for (Player player : mMain.getServer().getOnlinePlayers()) {
			if (bounds.within(player.getLocation())) {
				if (scoreName != null) {
					int score = ScoreboardUtils.getScoreboardValue(player, scoreName);

					// Ignore this player if score was specified and it didn't match
					if (score == -1 || score < scoreMin || score > scoreMax) {
						continue;
					}
				}

				try {
					if (sendPlayerStuff == false) {
						sender.sendMessage("Transferring " + player.getName() + " to " + server);
						NetworkUtils.sendPlayer(mMain, player, server);
					} else {
						sender.sendMessage("Transferring " + player.getName() + " with playerdata to " + server);
						NetworkUtils.transferPlayerData(mMain, player, server);
					}
				} catch (Exception e) {
					sender.sendMessage("Caught exception when transferring players");
					e.printStackTrace();
					return false;
				}
			}
		}

		return true;
	}

}
