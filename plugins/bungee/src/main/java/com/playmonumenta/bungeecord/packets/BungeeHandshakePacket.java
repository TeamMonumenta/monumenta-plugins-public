package com.playmonumenta.bungeecord.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.network.ClientSocket;
import com.playmonumenta.bungeecord.network.SocketManager;

public class BungeeHandshakePacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.Handshake";

	public BungeeHandshakePacket(String name) {
		super(null, PacketOperation, new JsonObject());
		mData.addProperty("name", name);
	}

	public static void handlePacket(SocketManager manager, ClientSocket client, BasePacket packet) throws Exception {
		if (!packet.hasData() ||
		    !packet.getData().has("name") ||
		    !packet.getData().get("name").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("name").isString()) {
			throw new Exception("BungeeHelloPacket missing required string field 'name'");
		}
		if (client.getName() != null) {
			throw new Exception("BungeeHelloPacket received but client already said hello");
		}
		manager.clientHello(client, packet.getData().getAsJsonPrimitive("name").getAsString());
	}
}
