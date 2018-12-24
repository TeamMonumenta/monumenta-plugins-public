package com.playmonumenta.plugins.network.packet;

import java.util.UUID;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.utils.NetworkUtils;
import com.playmonumenta.plugins.utils.PacketUtils;

public class TransferPlayerDataPacket implements Packet {
	public static final String StaticPacketChannel = "Monumenta.Bungee.Forward.TransferPlayerData";

	private String mNewServer;
	private String mPlayerName;
	private UUID mPlayerUUID;
	private String mPlayerContent;

	public TransferPlayerDataPacket(String server, String playerName, UUID playerUUID, String playerContent) {
		mNewServer = server;
		mPlayerName = playerName;
		mPlayerUUID = playerUUID;
		mPlayerContent = playerContent;
	}

	@Override
	public String getPacketChannel() {
		return StaticPacketChannel;
	}

	@Override
	public String getPacketData() throws Exception {
		String[] data = {mNewServer, mPlayerName, mPlayerUUID.toString(), mPlayerContent};
		return PacketUtils.encodeStrings(data);
	}

	public static void handlePacket(Plugin plugin, String data) throws Exception {
		String[] rcvStrings = PacketUtils.decodeStrings(data);
		if (rcvStrings == null || rcvStrings.length != 4) {
			throw new Exception("Received string data is null or invalid length");
		}

		String server = rcvStrings[0];
		String playerName = rcvStrings[1];
		UUID playerUUID = UUID.fromString(rcvStrings[2]);
		String playerContent = rcvStrings[3];

		// Save the player data so that when the player logs in they'll get it applied to them
		PlayerData.savePlayerData(plugin, playerUUID, playerContent);

		// Everything looks good - request bungeecord transfer the player to this server
		NetworkUtils.sendPlayer(plugin, playerName, playerUUID, server);
	}
}
