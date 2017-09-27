package pe.project.utils;

import org.bukkit.entity.Player;

import pe.project.Main;
import pe.project.network.packet.TransferPlayerDataPacket;
import pe.project.network.packet.SendPlayerPacket;
import pe.project.playerdata.PlayerData;

public class NetworkUtils {

	public static void sendPlayer(Main main, Player player, String server) {
		SendPlayerPacket packet = new SendPlayerPacket();

		packet.mNewServer = server;
		packet.mPlayerName = player.getName();
		packet.mPlayerUUID = player.getUniqueId();

		PacketUtils.SendPacket(main, packet);
	}

	public static void transferPlayerData(Main main, Player player, String server) throws Exception {
		TransferPlayerDataPacket packet = new TransferPlayerDataPacket();

		packet.mNewServer = server;
		packet.mPlayerName = player.getName();
		packet.mPlayerUUID = player.getUniqueId();
		packet.mPlayerContent = PlayerData.convertToString(main, player);
		if (packet.mPlayerContent.isEmpty()) {
			main.getLogger().warning("Failed to get player data for " + player.getName());
			throw new Exception();
		}

		PacketUtils.SendPacket(main, packet);
	}
}
