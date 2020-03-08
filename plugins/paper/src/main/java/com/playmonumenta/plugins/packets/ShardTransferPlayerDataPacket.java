package com.playmonumenta.plugins.packets;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.NetworkUtils;
import com.rabbitmq.client.AMQP;

public class ShardTransferPlayerDataPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Shard.TransferPlayerData";

	public ShardTransferPlayerDataPacket(String newServer, String playerName, UUID playerUUID, String playerContent) {
		super(newServer, PacketOperation);
		getData().addProperty("playerName", playerName);
		getData().addProperty("playerUUID", playerUUID.toString());
		getData().addProperty("playerContent", playerContent);
	}

	@Override
	public AMQP.BasicProperties getProperties() {
		/* This packet type expires after 3s */
		return new AMQP.BasicProperties.Builder()
                                   .expiration("3000")
                                   .build();
	}

	public static void handlePacket(Plugin plugin, JsonObject data) throws Exception {
		if (!data.has("playerName") ||
		    !data.get("playerName").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("playerName").isString()) {
			throw new Exception("ShardTransferPlayerDataPacket missing required field 'playerName'");
		}
		if (!data.has("playerUUID") ||
		    !data.get("playerUUID").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("playerUUID").isString()) {
			throw new Exception("ShardTransferPlayerDataPacket missing required field 'playerUUID'");
		}
		if (!data.has("playerContent") ||
		    !data.get("playerContent").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("playerContent").isString()) {
			throw new Exception("ShardTransferPlayerDataPacket missing required field 'playerContent'");
		}

		String playerName = data.get("playerName").getAsString();
		UUID playerUUID = UUID.fromString(data.get("playerUUID").getAsString());
		String playerContent = data.get("playerContent").getAsString();

		// Save the player data so that when the player logs in they'll get it applied to them
		PlayerData.savePlayerData(plugin, playerUUID, playerContent);

		// Everything looks good - request bungeecord transfer the player to this server
		NetworkUtils.sendPlayer(plugin, playerName, playerUUID, ServerProperties.getShardName());
	}
}
