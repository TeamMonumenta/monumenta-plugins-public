package com.playmonumenta.bungeecord.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.network.ClientSocket;
import com.playmonumenta.bungeecord.network.SocketManager;

public class BroadcastCommandPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Broadcast.Command";

	public BroadcastCommandPacket(String command) {
		super("*", PacketOperation, new JsonObject());
		mData.addProperty("command", command);
	}

	public static void handlePacket(SocketManager manager, ClientSocket client, BasePacket packet) throws Exception {
		throw new Exception("BroadcastCommandPacket cannot be handled by bungee");
	}
}
