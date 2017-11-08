package pe.project.utils;

import com.google.gson.Gson;

import pe.project.Main;
import pe.project.network.packet.*;

public class PacketUtils {
	public static void SendPacket(Main main, Packet packet) throws Exception {
		// Serialize the packet data into a string that can be sent
		String data = packet.getPacketData();

		// Send that string to the bungeecord proxy via the opened socket
		main.mSocketClient.writeJSON(packet.getPacketChannel(), data);
	}

	public static void ProcessPacket(Main main, String channel, String data) throws Exception {
		if (channel.equals(TransferPlayerDataPacket.getStaticPacketChannel())) {
			TransferPlayerDataPacket.handlePacket(main, data);
		} else if (channel.equals(SendPlayerPacket.getStaticPacketChannel())) {
			SendPlayerPacket.handlePacket(main, data);
		} else if (channel.equals(HeartbeatPacket.getStaticPacketChannel())) {
			HeartbeatPacket.handlePacket(main, data);
		} else if (channel.equals(GetServerListPacket.getStaticPacketChannel())) {
			GetServerListPacket.handlePacket(main, data);
		} else if (channel.equals(BroadcastCommandPacket.getStaticPacketChannel())) {
			BroadcastCommandPacket.handlePacket(main, data);
		}
	}

	/**
	 * Encodes string array to a single string
	 */
	public static String encodeStrings(String in[]) throws Exception {
		Gson gs = new Gson();
		return gs.toJson(in);
	}

	/**
	 * Decodes a stringified array of strings
	 */
	public static String[] decodeStrings(String encodedStr) throws Exception {
		Gson gs = new Gson();
		return gs.fromJson(encodedStr, String[].class);
	}
}
