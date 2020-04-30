package com.playmonumenta.plugins.packets;

import org.bukkit.Bukkit;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;

public class BroadcastCommandPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Broadcast.Command";

	public BroadcastCommandPacket(String command) {
		/* TODO: Some kind of timeout */
		super("*", PacketOperation);
		getData().addProperty("command", command);
	}

	public static void handlePacket(Plugin plugin, JsonObject data) throws Exception {
		if (!data.has("command") ||
		    !data.get("command").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("command").isString()) {
			throw new Exception("CommandPacket failed to parse required string field 'command'");
		}
		String command = data.get("command").getAsString();

		if (ServerProperties.getBroadcastCommandEnabled() == true
		    || command.startsWith("say")
		    || command.startsWith("msg")
		    || command.startsWith("tell")
		    || command.startsWith("restart-empty")
		    || command.startsWith("save-all")
		    || command.startsWith("tellraw")) {

			plugin.getLogger().fine("Executing broadcast received command '" + command + "'");

			/* Call this on the main thread */
			Bukkit.getScheduler().callSyncMethod(plugin, () -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command));
		}
	}
}
