package com.playmonumenta.plugins.packets;

import java.util.UUID;

import org.bukkit.Bukkit;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.RedeemVoteRewards;

/*
 * For Server -> Bungee:
 *   if votesUnclaimed == 0, request the count of eligible voting rewards
 *   if votesUnclaimed > 0, return these votes back to bungee for storage
 * For Bungee -> Server
 * 	 Decrement storage, and send votes as votesUnclaimed
 */
public class BungeeGetVotesUnclaimedPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.GetVotesUnclaimed";

	public BungeeGetVotesUnclaimedPacket(UUID playerUUID, int count) {
		super("bungee", PacketOperation);
		getData().addProperty("playerUUID", playerUUID.toString());
		getData().addProperty("votesUnclaimed", count);
	}

	public static void handlePacket(Plugin plugin, JsonObject data) throws Exception {
		if (!data.has("playerUUID") ||
		    !data.get("playerUUID").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("playerUUID").isString()) {
			throw new Exception("GetVotesUnclaimedPacket failed to parse required string field 'playerUUID'");
		}

		if (!data.has("votesUnclaimed") ||
		    !data.get("votesUnclaimed").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("votesUnclaimed").isNumber()) {
			throw new Exception("GetVotesUnclaimedPacket failed to parse required int field 'votesUnclaimed'");
		}

		UUID uuid = UUID.fromString(data.get("playerUUID").getAsString());
		int count = data.get("votesUnclaimed").getAsInt();

		/* Handle on the main thread */
		Bukkit.getScheduler().callSyncMethod(plugin, () -> {
			RedeemVoteRewards.gotVoteRewardMessage(plugin, uuid, count);
			return true;
		});
	}
}
