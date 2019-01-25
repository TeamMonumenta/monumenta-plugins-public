package com.playmonumenta.plugins.packets;

import java.util.UUID;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PacketUtils;

public class SendPlayerPacket implements BasePacket {
	public static final String StaticPacketChannel = "Monumenta.Bungee.SendPlayer";

	private String mNewServer;
	private String mPlayerName;
	private UUID mPlayerUUID;

	public SendPlayerPacket(String server, String playerName, UUID playerUUID) {
		mNewServer = server;
		mPlayerName = playerName;
		mPlayerUUID = playerUUID;
	}

	@Override
	public String getPacketChannel() {
		return StaticPacketChannel;
	}

	@Override
	public String getPacketData() throws Exception {
		String[] data = {mNewServer, mPlayerName, mPlayerUUID.toString()};
		return PacketUtils.encodeStrings(data);
	}

	public static void handlePacket(Plugin plugin, String data) throws Exception {
		throw new Exception("Got message from which should only be received by bungeecord");
	}
}
