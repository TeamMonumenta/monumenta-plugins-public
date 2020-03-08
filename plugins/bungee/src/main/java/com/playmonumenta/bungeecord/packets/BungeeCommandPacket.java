package com.playmonumenta.bungeecord.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.Main;

import net.md_5.bungee.api.ProxyServer;

public class BungeeCommandPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.Command";

	public BungeeCommandPacket() throws Exception {
		super();
	}

	public static void handlePacket(Main main, String source, JsonObject data) throws Exception {
		if (!data.has("command") ||
		    !data.get("command").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("command").isString()) {
			throw new Exception("BungeeCommandPacket data is missing required string field 'command'");
		}
		ProxyServer proxy = ProxyServer.getInstance();
		proxy.getPluginManager().dispatchCommand(proxy.getConsole(), data.get("command").getAsString());
	}
}
