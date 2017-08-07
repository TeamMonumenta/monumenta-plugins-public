package pe.project.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import pe.project.Main;
import pe.project.network.packet.TransferPlayerPacket;
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
		String score_name = null;
		int score_min = 0;
		int score_max = 0;
		int send_player_stuff = 1;

		if (arg3.length != 8 && arg3.length != 11) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			sender.sendMessage(ChatColor.RED + "Usage: " + command.getUsage());
			return false;
		}

		String server = arg3[0];

		// Default to sending equipment
		if (arg3[1].equals("False") || arg3[1].equals("false")) {
			send_player_stuff = 0;
		}

		// Need to only match players with the given score_name value
		if (arg3.length == 11) {
			try {
				score_name = arg3[8];
				score_min = CommandUtils.parseIntFromString(sender, command, arg3[9]);
				score_max = CommandUtils.parseIntFromString(sender, command, arg3[10]);
			} catch (Exception e) {
				return false;
			}
		}

		Point pos1;
		Point pos2;
		try {
			pos1 = CommandUtils.parsePointFromString(sender, command, arg3[2], arg3[3], arg3[4]);
			pos2 = CommandUtils.parsePointFromString(sender, command, arg3[5], arg3[6], arg3[7]);
		} catch (Exception e) {
			return false;
		}

		AreaBounds bounds = new AreaBounds("", pos1, pos2);


		for (Player player : mMain.getServer().getOnlinePlayers()) {
			if (bounds.within(player.getLocation())) {
				if (score_name != null) {
					int score = ScoreboardUtils.getScoreboardValue(player, score_name);

					// Ignore this player if score was specified and it didn't match
					if (score == -1 || score < score_min || score > score_max) {
						continue;
					}
				}

				TransferPlayerPacket packet = new TransferPlayerPacket();

				packet.mNewServer = server;
				packet.mPlayerName = player.getName();

				if (send_player_stuff != 0) {
					packet.mPlayerContent = PlayerData.SerializePlayerData(mMain, player);
				} else {
					packet.mPlayerContent = null;
				}

				NetworkUtils.SendPacket(mMain, packet);
			}
		}

		return true;
	}

}
