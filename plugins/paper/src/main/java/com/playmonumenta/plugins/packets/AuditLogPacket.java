package com.playmonumenta.plugins.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

public class AuditLogPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Automation.AuditLog";

	public AuditLogPacket(String message) {
		super("automation-bot", PacketOperation);
		getData().addProperty("message", message);
	}

	public static void handlePacket(Plugin plugin, JsonObject data) throws Exception {
		throw new Exception("Got message from which should only be received by bungeecord");
	}
}
