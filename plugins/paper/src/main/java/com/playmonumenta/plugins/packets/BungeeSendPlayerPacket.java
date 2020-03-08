package com.playmonumenta.plugins.packets;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

public class BungeeSendPlayerPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.SendPlayer";

	public BungeeSendPlayerPacket(String newServer, String playerName, UUID playerUUID) {
		super("bungee", PacketOperation);
		getData().addProperty("newServer", newServer);
		getData().addProperty("playerName", playerName);
		getData().addProperty("playerUUID", playerUUID.toString());
	}

	public static void handlePacket(Plugin plugin, JsonObject data) throws Exception {
		throw new Exception("Got message from which should only be received by bungeecord");
	}
}
