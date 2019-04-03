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
		super(null, PacketOperation, new JsonObject());
		mData.addProperty("playerUUID", playerUUID.toString());
		mData.addProperty("votesUnclaimed", count);
	}

	public static void handlePacket(Plugin plugin, BasePacket packet) throws Exception {
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

		/* Handle on the main thread */
		Bukkit.getScheduler().callSyncMethod(plugin, () -> {
			RedeemVoteRewards.gotVoteRewardMessage(plugin, uuid, count);
			return true;
		});
	}
}
