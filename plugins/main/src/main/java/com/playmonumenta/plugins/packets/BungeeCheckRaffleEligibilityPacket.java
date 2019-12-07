package com.playmonumenta.plugins.packets;

import java.util.UUID;

import org.bukkit.Bukkit;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.commands.ClaimRaffle;

/*
 * Server -> Bungee
 * 	claimReward = false -> This is just a test to see if the player is eligible (raffle rewards > 0)
 * 	claimReward = true -> Bungee should subtract 1 from the player's raffle rewards score
 * 	eligible = true -> Player previously tried to claim a raffle but item wasn't eligible - add back the raffle point
 * 	eligible = false -> Regular request
 *
 * 	eligible == true && claimReward == true -> Undefined
 *
 * Bungee -> Server
 *  claimReward same as request
 *  eligible = Is the player eligible to claim a raffle reward.
 *  If claimReward and eligible, attempt to grant raffle reward
 */
public class BungeeCheckRaffleEligibilityPacket extends BasePacket {
	public static final String PacketOperation = "Monumenta.Bungee.CheckRaffleEligibility";

	public BungeeCheckRaffleEligibilityPacket(UUID playerUUID, boolean claimReward, boolean add_back) {
		super(null, PacketOperation, new JsonObject());
		mData.addProperty("playerUUID", playerUUID.toString());
		mData.addProperty("claimReward", claimReward);
		mData.addProperty("eligible", add_back);
	}

	public static void handlePacket(Plugin plugin, BasePacket packet) throws Exception {
		if (!packet.hasData() ||
		    !packet.getData().has("playerUUID") ||
		    !packet.getData().get("playerUUID").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("playerUUID").isString()) {
			throw new Exception("CheckRaffleEligibilityPacket failed to parse required string field 'playerUUID'");
		}

		if (!packet.hasData() ||
		    !packet.getData().has("claimReward") ||
		    !packet.getData().get("claimReward").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("claimReward").isBoolean()) {
			throw new Exception("CheckRaffleEligibilityPacket failed to parse required int field 'claimReward'");
		}

		if (!packet.hasData() ||
		    !packet.getData().has("eligible") ||
		    !packet.getData().get("eligible").isJsonPrimitive() ||
		    !packet.getData().getAsJsonPrimitive("eligible").isBoolean()) {
			throw new Exception("CheckRaffleEligibilityPacket failed to parse required int field 'eligible'");
		}

		UUID uuid = UUID.fromString(packet.getData().get("playerUUID").getAsString());
		boolean claimReward = packet.getData().get("claimReward").getAsBoolean();
		boolean eligible = packet.getData().get("eligible").getAsBoolean();

		/* Handle on the main thread */
		Bukkit.getScheduler().callSyncMethod(plugin, () -> {
			ClaimRaffle.queryResponseReceived(plugin, uuid, claimReward, eligible);
			return true;
		});
	}
}
