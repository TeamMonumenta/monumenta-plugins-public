package pe.project.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import pe.project.Main;
import pe.project.network.packet.TransferPlayerDataPacket;
import pe.project.network.packet.SendPlayerPacket;
import pe.project.playerdata.PlayerData;
import pe.project.point.AreaBounds;
import pe.project.point.Point;
import pe.project.utils.CommandUtils;
import pe.project.utils.NetworkUtils;
import pe.project.utils.ScoreboardUtils;

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

		if (arg3.length != 8 && arg3.length != 11) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			sender.sendMessage(ChatColor.RED + "Usage: " + command.getUsage());
			return false;
		}

		String server = arg3[0];

		// Default to sending equipment
		if (arg3[1].equals("False") || arg3[1].equals("false")) {
			sendPlayerStuff = false;
		}

		// Need to only match players with the given scoreName value
		if (arg3.length == 11) {
			try {
				scoreName = arg3[8];
				scoreMin = CommandUtils.parseIntFromString(sender, command, arg3[9]);
				scoreMax = CommandUtils.parseIntFromString(sender, command, arg3[10]);
			} catch (Exception e) {
				return false;
			}
		}

		AreaBounds bounds;
		try {
			bounds = CommandUtils.parseAreaFromString(sender, command, arg3[2], arg3[3],
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

				if (sendPlayerStuff == false) {
					SendPlayerPacket packet = new SendPlayerPacket();

					packet.mNewServer = server;
					packet.mPlayerName = player.getName();
					packet.mPlayerUUID = player.getUniqueId();

					sender.sendMessage("Transferring " + player.getName() + " to " + server);
					NetworkUtils.SendPacket(mMain, packet);
				} else {
					TransferPlayerDataPacket packet = new TransferPlayerDataPacket();

					packet.mNewServer = server;
					packet.mPlayerName = player.getName();
					packet.mPlayerUUID = player.getUniqueId();
					packet.mPlayerContent = PlayerData.convertToString(mMain, player);
					if (packet.mPlayerContent.isEmpty()) {
						sender.sendMessage(ChatColor.RED + "Failed to get player data for " + player.getName());
						continue;
					}

					sender.sendMessage("Transferring " + player.getName() + " with playerdata to " + server);
					NetworkUtils.SendPacket(mMain, packet);
				}

			}
		}

		return true;
	}

}
