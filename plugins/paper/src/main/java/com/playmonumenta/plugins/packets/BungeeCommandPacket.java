package com.playmonumenta.plugins.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

/**
 * BungeeCommandPacket sends a command to bungee.
 */
public class BungeeCommandPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.Command";
	/**
	 * Create a new packet containing a command.
	 * This command will be sent only to bungee.
	 * @param command
	 */
	public BungeeCommandPacket(String command) {
		super(null, PacketOperation, new JsonObject());
		mData.addProperty("command", command);
	}

	public static void handlePacket(Plugin plugin, BasePacket packet) throws Exception {
		throw new Exception("BungeeCommandPacket cannot be handled by shards.");
	}
}