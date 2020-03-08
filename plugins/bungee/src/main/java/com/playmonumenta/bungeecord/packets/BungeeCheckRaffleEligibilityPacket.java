package com.playmonumenta.bungeecord.packets;

import java.util.UUID;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.Main;
import com.playmonumenta.bungeecord.voting.VoteManager;

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

	public BungeeCheckRaffleEligibilityPacket(String destination, UUID playerUUID, boolean claimReward, boolean addBack) {
		super(destination, PacketOperation);
		getData().addProperty("playerUUID", playerUUID.toString());
		getData().addProperty("claimReward", claimReward);
		getData().addProperty("eligible", addBack);
	}

	public static void handlePacket(Main main, String source, JsonObject data) throws Exception {
		if (!data.has("playerUUID") ||
		    !data.get("playerUUID").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("playerUUID").isString()) {
			throw new Exception("CheckRaffleEligibilityPacket failed to parse required string field 'playerUUID'");
		}

		if (!data.has("claimReward") ||
		    !data.get("claimReward").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("claimReward").isBoolean()) {
			throw new Exception("CheckRaffleEligibilityPacket failed to parse required int field 'claimReward'");
		}

		if (!data.has("eligible") ||
		    !data.get("eligible").isJsonPrimitive() ||
		    !data.getAsJsonPrimitive("eligible").isBoolean()) {
			throw new Exception("CheckRaffleEligibilityPacket failed to parse required int field 'eligible'");
		}

		UUID uuid = UUID.fromString(data.get("playerUUID").getAsString());
		boolean claimReward = data.get("claimReward").getAsBoolean();
		boolean eligible = data.get("eligible").getAsBoolean();

		VoteManager.gotShardRaffleEligibilityRequest(source, uuid, claimReward, eligible);
	}
}
