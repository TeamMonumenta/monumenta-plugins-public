package com.playmonumenta.plugins.network.packet;

import java.util.UUID;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PacketUtils;

public class GetServerListPacket implements Packet {
	public static final String StaticPacketChannel = "Monumenta.Bungee.GetServerList";

	private String mPlayerName;
	private UUID mPlayerUUID;

	public GetServerListPacket(String playerName, UUID playerUUID) {
		mPlayerName = playerName;
		mPlayerUUID = playerUUID;
	}

	@Override
	public String getPacketChannel() {
		return StaticPacketChannel;
	}

	@Override
	public String getPacketData() throws Exception {
		String[] data = {mPlayerName, mPlayerUUID.toString()};
		return PacketUtils.encodeStrings(data);
	}

	public static void handlePacket(Plugin plugin, String data) throws Exception {
		throw new Exception("Got message from which should only be received by bungeecord");
	}
}
