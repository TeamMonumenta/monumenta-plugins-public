package com.playmonumenta.plugins.packets;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

public class BungeeGetServerListPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.GetServerList";

	public BungeeGetServerListPacket(String playerName, UUID playerUUID) {
		super("bungee", PacketOperation);
		getData().addProperty("playerName", playerName);
		getData().addProperty("playerUUID", playerUUID.toString());
	}

	public static void handlePacket(Plugin plugin, JsonObject data) throws Exception {
		throw new Exception("BungeeGetServerListPacket cannot be handled by shards");
	}
}
