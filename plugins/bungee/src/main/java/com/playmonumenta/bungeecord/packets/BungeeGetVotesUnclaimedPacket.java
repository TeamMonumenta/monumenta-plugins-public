package com.playmonumenta.bungeecord.packets;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.Main;
import com.playmonumenta.bungeecord.voting.VoteManager;

public class BungeeGetVotesUnclaimedPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.GetVotesUnclaimed";

	public BungeeGetVotesUnclaimedPacket(String destination, UUID playerUUID, int count) {
		super(destination, PacketOperation);
		getData().addProperty("playerUUID", playerUUID.toString());
		getData().addProperty("votesUnclaimed", count);
	}

	public static void handlePacket(Main main, String source, JsonObject data) throws Exception {
		if (!data.has("playerUUID") ||
		    !data.get("playerUUID").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("playerUUID").isString()) {
			throw new Exception("CommandPacket failed to parse required string field 'playerUUID'");
		}

		if (!data.has("votesUnclaimed") ||
		    !data.get("votesUnclaimed").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("votesUnclaimed").isNumber()) {
			throw new Exception("CommandPacket failed to parse required int field 'votesUnclaimed'");
		}

		UUID uuid = UUID.fromString(data.get("playerUUID").getAsString());
		int count = data.get("votesUnclaimed").getAsInt();

		VoteManager.gotShardVoteCountRequest(source, uuid, count);
	}
}
