package com.playmonumenta.plugins.packets;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

public class BungeeSendPlayerPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.SendPlayer";

	public BungeeSendPlayerPacket(String newServer, String playerName, UUID playerUUID) {
		super(null, PacketOperation, new JsonObject());
		mData.addProperty("newServer", newServer);
		mData.addProperty("playerName", playerName);
		mData.addProperty("playerUUID", playerUUID.toString());
	}

	public static void handlePacket(Plugin plugin, BasePacket packet) throws Exception {
		throw new Exception("Got message from which should only be received by bungeecord");
	}
}
