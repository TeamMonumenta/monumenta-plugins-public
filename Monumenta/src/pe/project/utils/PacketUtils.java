package pe.project.utils;

import com.google.gson.Gson;

import pe.project.Plugin;
import pe.project.network.packet.*;

public class PacketUtils {
	public static void SendPacket(Plugin plugin, Packet packet) throws Exception {
		// Serialize the packet data into a string that can be sent
		String data = packet.getPacketData();

		// Send that string to the bungeecord proxy via the opened socket
		plugin.mSocketClient.writeJSON(packet.getPacketChannel(), data);
	}

	public static void ProcessPacket(Plugin plugin, String channel, String data) throws Exception {
		if (channel.equals(SendPlayerPacket.getStaticPacketChannel())) {
			SendPlayerPacket.handlePacket(plugin, data);
		} else if (channel.equals(HeartbeatPacket.getStaticPacketChannel())) {
			HeartbeatPacket.handlePacket(plugin, data);
		} else if (channel.equals(GetServerListPacket.getStaticPacketChannel())) {
			GetServerListPacket.handlePacket(plugin, data);
		} else if (channel.equals(BroadcastCommandPacket.getStaticPacketChannel())) {
			BroadcastCommandPacket.handlePacket(plugin, data);
		} else if (channel.equals(ForwardErrorPacket.getStaticPacketChannel())) {
			ForwardErrorPacket.handlePacket(plugin, data);
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
