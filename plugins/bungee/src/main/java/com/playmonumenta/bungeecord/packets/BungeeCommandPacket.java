package com.playmonumenta.bungeecord.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.network.ClientSocket;
import com.playmonumenta.bungeecord.network.SocketManager;

import net.md_5.bungee.api.ProxyServer;

public class BungeeCommandPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.Command";

	public BungeeCommandPacket(String command) {
		super(null, PacketOperation, new JsonObject());
		mData.addProperty("command", command);
	}

	public static void handlePacket(SocketManager manager, ClientSocket client, BasePacket packet) throws Exception {
		if (!packet.hasData() ||
		!packet.getData().has("command") ||
		!packet.getData().get("command").isJsonPrimitive() ||
		!packet.getData().getAsJsonPrimitive("command").isString()) {
			throw new Exception("BungeeCommandPacket data is missing required string field 'command'");
		}
		ProxyServer proxy = ProxyServer.getInstance();
		proxy.getPluginManager().dispatchCommand(proxy.getConsole(), packet.getData().get("command").getAsString());
	}
}
