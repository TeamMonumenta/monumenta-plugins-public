package com.playmonumenta.bungeecord.packets;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.network.ClientSocket;
import com.playmonumenta.bungeecord.network.SocketManager;

public class ShardTransferPlayerDataPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Shard.TransferPlayerData";

	public ShardTransferPlayerDataPacket(String newServer, String playerName, UUID playerUUID, String playerContent) {
		super(newServer, PacketOperation, new JsonObject());
		mData.addProperty("newServer", newServer);
		mData.addProperty("playerName", playerName);
		mData.addProperty("playerUUID", playerUUID.toString());
		mData.addProperty("playerContent", playerContent);
	}

	public static void handlePacket(SocketManager manager, ClientSocket client, BasePacket packet) throws Exception {
		throw new Exception("ShardTransferPlayerDataPacket cannot be handled by bungee");
	}
}
