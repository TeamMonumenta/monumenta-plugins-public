package pe.project.utils;

import java.util.UUID;

import org.bukkit.entity.Player;

import pe.project.Main;
import pe.project.network.packet.TransferPlayerDataPacket;
import pe.project.network.packet.SendPlayerPacket;
import pe.project.network.packet.GetServerListPacket;
import pe.project.network.packet.BroadcastCommandPacket;
import pe.project.playerdata.PlayerData;

public class NetworkUtils {

	public static void sendPlayer(Main plugin, Player player, String server) throws Exception {
		sendPlayer(plugin, player.getName(), player.getUniqueId(), server);
	}

	public static void sendPlayer(Main plugin, String playerName, UUID playerUUID, String server) throws Exception {
		PacketUtils.SendPacket(plugin, new SendPlayerPacket(server,
															playerName,
															playerUUID));

		// Success, print transfer message request to log
		plugin.getLogger().info("Requested bungeecord transfer " + playerName + " to " + server);
	}

	public static void transferPlayerData(Main plugin, Player player, String server) throws Exception {
		PacketUtils.SendPacket(plugin, new TransferPlayerDataPacket(server,
																    player.getName(),
																    player.getUniqueId(),
																    PlayerData.convertToString(plugin, player)));
	}

	public static void getServerList(Main plugin, Player player) throws Exception {
		PacketUtils.SendPacket(plugin, new GetServerListPacket(player.getName(), player.getUniqueId()));

		// Success, print transfer message request to log
		plugin.getLogger().info("Requested server list for " + player.getName());
	}

	public static void broadcastCommand(Main plugin, String command) throws Exception {
		PacketUtils.SendPacket(plugin, new BroadcastCommandPacket(command));

		// Success, print transfer message request to log
		plugin.getLogger().info("Requested broadcast of command '" + command + "'");
	}
}
