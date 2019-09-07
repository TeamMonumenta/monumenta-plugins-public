package com.playmonumenta.bungeecord.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.network.ClientSocket;
import com.playmonumenta.bungeecord.network.SocketManager;

public class BungeeErrorPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.Error";

	public BungeeErrorPacket(String error, String sender, String dest, String op, JsonObject data) {
		super(sender, PacketOperation, new JsonObject());
		mData.addProperty("error", error);
		mData.addProperty("dest", dest);
		mData.addProperty("op", op);
		mData.add("data", data);
	}

	public static void handlePacket(SocketManager manager, ClientSocket client, BasePacket packet) throws Exception {
		// TODO: Allow shards to send an error message to bungee
	}

}
