package pe.project.utils;

import org.bukkit.Bukkit;

import pe.project.Main;
import pe.project.network.packet.Packet;
import pe.project.network.packet.TransferPlayerDataPacket;
import pe.project.network.packet.SendPlayerPacket;
import pe.project.network.packet.HeartbeatPacket;

public class NetworkUtils {
	public static void SendPacket(Main main, Packet packet) {
		// Serialize the packet data into a string that can be sent
		String data = packet.getPacketData();

		// Send that string to the bungeecord proxy via the opened socket
		try {
			main.mSocketClient.writeJSON(packet.getPacketChannel(), data);
		} catch (Exception e) {
			main.getLogger().warning("Failed to send packet on channel '" + packet.getPacketChannel() + "' to bungeecord");
		}
	}

	public static void ProcessPacket(Main main, String channel, String data) {
		if (channel.equals(TransferPlayerDataPacket.getStaticPacketChannel())) {
			TransferPlayerDataPacket packet = new TransferPlayerDataPacket();
			packet.handlePacket(main, data);
		} else if (channel.equals(SendPlayerPacket.getStaticPacketChannel())) {
			SendPlayerPacket packet = new SendPlayerPacket();
			packet.handlePacket(main, data);
		} else if (channel.equals(HeartbeatPacket.getStaticPacketChannel())) {
			HeartbeatPacket packet = new HeartbeatPacket();
			packet.handlePacket(main, data);
		}
	}
}
