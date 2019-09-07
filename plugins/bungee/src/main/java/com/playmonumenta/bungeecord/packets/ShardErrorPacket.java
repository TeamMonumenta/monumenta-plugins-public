package com.playmonumenta.bungeecord.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.network.ClientSocket;
import com.playmonumenta.bungeecord.network.SocketManager;

public class ShardErrorPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Shard.Error";

	public ShardErrorPacket(String targetShard, String error) {
		super(targetShard, PacketOperation, new JsonObject());
		mData.addProperty("error", error);
	}

	public static void handlePacket(SocketManager manager, ClientSocket client, BasePacket packet) throws Exception {
		throw new Exception("ShardErrorPacket cannot be handled by bungee");
	}
}
