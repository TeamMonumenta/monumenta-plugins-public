package pe.project.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.Main;
import pe.project.network.packet.TransferPlayerPacket;
import pe.project.playerdata.PlayerData;
import pe.project.utils.NetworkUtils;

public class BungeeTest implements CommandExecutor {
	Main mMain;
	
	public BungeeTest(Main main) {
		mMain = main;
	}
	
	@Override
	public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
		TransferPlayerPacket packet = new TransferPlayerPacket();
		
		Player player = mMain.getServer().getPlayer(arg3[0]);
		
		packet.mNewServer = arg3[1];
		packet.mPlayerName = player.getName();
		packet.mPlayerContent = PlayerData.SerializePlayerData(mMain, player);

		NetworkUtils.SendPacket(mMain, packet);
		
		return true;
	}
}
