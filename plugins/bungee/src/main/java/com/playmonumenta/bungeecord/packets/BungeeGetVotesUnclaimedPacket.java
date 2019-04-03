package com.playmonumenta.bungeecord.packets;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.network.ClientSocket;
import com.playmonumenta.bungeecord.network.SocketManager;
import com.playmonumenta.bungeecord.voting.VoteManager;

public class BungeeGetVotesUnclaimedPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.GetVotesUnclaimed";

	public BungeeGetVotesUnclaimedPacket(UUID playerUUID, int count) {
		super(null, PacketOperation, new JsonObject());
		mData.addProperty("playerUUID", playerUUID.toString());
		mData.addProperty("votesUnclaimed", count);
	}

	public static void handlePacket(SocketManager manager, ClientSocket client, BasePacket packet) throws Exception {
		if (!packet.hasData() ||
		    !packet.getData().has("playerUUID") ||
		    !packet.getData().get("playerUUID").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("playerUUID").isString()) {
			throw new Exception("CommandPacket failed to parse required string field 'playerUUID'");
		}

		if (!packet.hasData() ||
		    !packet.getData().has("votesUnclaimed") ||
		    !packet.getData().get("votesUnclaimed").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("votesUnclaimed").isNumber()) {
			throw new Exception("CommandPacket failed to parse required int field 'votesUnclaimed'");
		}

		UUID uuid = UUID.fromString(packet.getData().get("playerUUID").getAsString());
		int count = packet.getData().get("votesUnclaimed").getAsInt();

		VoteManager.gotShardVoteCountRequest(client, uuid, count);
	}
}
