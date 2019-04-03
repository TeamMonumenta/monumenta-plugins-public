package com.playmonumenta.plugins.utils;

import com.google.gson.Gson;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.packets.BasePacket;
import com.playmonumenta.plugins.packets.BroadcastCommandPacket;
import com.playmonumenta.plugins.packets.ForwardErrorPacket;
import com.playmonumenta.plugins.packets.GetServerListPacket;
import com.playmonumenta.plugins.packets.HeartbeatPacket;
import com.playmonumenta.plugins.packets.SendPlayerPacket;
import com.playmonumenta.plugins.packets.TransferPlayerDataPacket;

public class PacketUtils {
	public static void SendPacket(Plugin plugin, BasePacket packet) throws Exception {
		// Serialize the packet data into a string that can be sent
		String data = packet.getPacketData();

		// Send that string to the bungeecord proxy via the opened socket
		plugin.mSocketClient.writeJSON(packet.getPacketChannel(), data);
	}

	public static void ProcessPacket(Plugin plugin, String channel, String data) throws Exception {
		if (channel.equals(TransferPlayerDataPacket.StaticPacketChannel)) {
			TransferPlayerDataPacket.handlePacket(plugin, data);
		} else if (channel.equals(SendPlayerPacket.StaticPacketChannel)) {
			SendPlayerPacket.handlePacket(plugin, data);
		} else if (channel.equals(HeartbeatPacket.StaticPacketChannel)) {
			HeartbeatPacket.handlePacket(plugin, data);
		} else if (channel.equals(GetServerListPacket.StaticPacketChannel)) {
			GetServerListPacket.handlePacket(plugin, data);
		} else if (channel.equals(BroadcastCommandPacket.StaticPacketChannel)) {
			BroadcastCommandPacket.handlePacket(plugin, data);
		} else if (channel.equals(ForwardErrorPacket.StaticPacketChannel)) {
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
