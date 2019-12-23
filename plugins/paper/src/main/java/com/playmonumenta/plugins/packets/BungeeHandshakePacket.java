package com.playmonumenta.plugins.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

public class BungeeHandshakePacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.Handshake";

	public BungeeHandshakePacket(String name) {
		super(null, PacketOperation, new JsonObject());
		mData.addProperty("name", name);
	}

	public static void handlePacket(Plugin plugin, BasePacket packet) throws Exception {
		plugin.getLogger().info("Socket Handshake Successful");
	}
}