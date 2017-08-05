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

//	/transferserver <server name> <x1> <y1> <z1> <x2> <y2> <z2>

public class TransferServer implements CommandExecutor {
	Main mMain;
	
	public TransferServer(Main main) {
		mMain = main;
	}
	
	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		if (arg3.length < 7) {
    		arg0.sendMessage(ChatColor.RED + "Too few parameters!");
    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
    		return false;
    	} else if (arg3.length > 7) {
    		arg0.sendMessage(ChatColor.RED + "Too many parameters!");
    		arg0.sendMessage(ChatColor.RED + "Usage: " + arg1.getUsage());
    		return false;
    	}
		
		String server = arg3[0];
		
		Point pos1;
		try {
			pos1 = CommandUtils.parsePointFromString(arg0, arg1, arg3[1], arg3[2], arg3[3]);
		} catch (Exception e) {
			return false;
		}
		
		Point pos2;
		try {
			pos2 = CommandUtils.parsePointFromString(arg0, arg1, arg3[4], arg3[5], arg3[6]);
		} catch (Exception e) {
			return false;
		}
		
		AreaBounds bounds = new AreaBounds("", pos1, pos2);
		
		for (Player player : mMain.getServer().getOnlinePlayers()) {
			if (bounds.within(player.getLocation())) {
				TransferPlayerPacket packet = new TransferPlayerPacket();
				
				packet.mNewServer = server;
				packet.mPlayerName = player.getName();
				packet.mPlayerContent = PlayerData.SerializePlayerData(mMain, player);
				
				NetworkUtils.SendPacket(mMain, packet);
			}
		}
		
		return true;
	}

}
