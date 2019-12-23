package com.playmonumenta.plugins.packets;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;

public class BungeeErrorPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.Error";

	public BungeeErrorPacket(String targetShard, String error) {
		super(targetShard, PacketOperation, new JsonObject());
		mData.addProperty("error", error);
	}

	public static void handlePacket(Plugin plugin, BasePacket packet) throws Exception {
		String op = null;
		JsonObject data = null;
		if (packet.hasData() &&
		    packet.getData().has("op") &&
		    packet.getData().get("op").isJsonPrimitive() &&
		    packet.getData().getAsJsonPrimitive("op").isString()) {
			op = packet.getData().get("op").getAsString();
		}
		if (packet.hasData() &&
		    packet.getData().has("data") &&
		    packet.getData().get("data").isJsonObject()) {
			data = packet.getData().getAsJsonObject("data");
		}

		if (op.equals(ShardTransferPlayerDataPacket.PacketOperation)) {
			ShardTransferPlayerDataPacket.handleError(plugin, new BasePacket(null, op, data));
		}
	}
}