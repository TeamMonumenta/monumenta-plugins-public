package com.playmonumenta.plugins.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

public class AuditLogPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Automation.AuditLog";

	public AuditLogPacket(String message) {
		super("automation_bot", PacketOperation, new JsonObject());
		mData.addProperty("message", message);
	}

	public static void handlePacket(Plugin plugin, BasePacket packet) throws Exception {
		throw new Exception("Got message from which should only be received by bungeecord");
	}
}
