package com.playmonumenta.plugins.packets;

import org.bukkit.Bukkit;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;

public class BroadcastCommandPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Broadcast.Command";

	public BroadcastCommandPacket(String command) {
		super("*", PacketOperation, new JsonObject());
		mData.addProperty("command", command);
	}

	public static void handlePacket(Plugin plugin, BasePacket packet) throws Exception {
		if (!packet.hasData() ||
		    !packet.getData().has("command") ||
		    !packet.getData().get("command").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("command").isString()) {
			throw new Exception("CommandPacket failed to parse required string field 'command'");
		}
		String command = packet.getData().get("command").getAsString();

		if (ServerProperties.getBroadcastCommandEnabled() == true
		    || command.startsWith("say")
		    || command.startsWith("msg")
		    || command.startsWith("tell")
		    || command.startsWith("tellraw")) {

			plugin.getLogger().fine("Executing broadcast received command '" + command + "'");

			/* Call this on the main thread */
			Bukkit.getScheduler().callSyncMethod(plugin, () -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command));
		}
	}
}
