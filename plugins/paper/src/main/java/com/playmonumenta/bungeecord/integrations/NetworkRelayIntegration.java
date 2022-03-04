package com.playmonumenta.bungeecord.integrations;

import com.google.gson.JsonObject;
import com.playmonumenta.bungeecord.voting.VoteManager;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.NetworkRelayMessageEventBungee;
import java.util.UUID;
import java.util.logging.Logger;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class NetworkRelayIntegration implements Listener {
	public static final String GET_VOTES_UNCLAIMED_CHANNEL = "Monumenta.Bungee.GetVotesUnclaimed";
	public static final String CHECK_RAFFLE_ELIGIBILITY_CHANNEL = "Monumenta.Bungee.CheckRaffleEligibility";

	private final Logger mLogger;
	private static NetworkRelayIntegration INSTANCE = null;

	public NetworkRelayIntegration(Logger logger) {
		logger.info("Enabling MonumentaNetworkRelay integration");
		mLogger = logger;
		INSTANCE = this;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void networkRelayMessageEventBungee(NetworkRelayMessageEventBungee event) {
		switch (event.getChannel()) {
		case GET_VOTES_UNCLAIMED_CHANNEL: {
			JsonObject data = event.getData();
			if (!data.has("playerUUID") ||
				!data.get("playerUUID").isJsonPrimitive() ||
				!data.getAsJsonPrimitive("playerUUID").isString()) {
				mLogger.severe("CommandPacket failed to parse required string field 'playerUUID'");
				return;
			}

			if (!data.has("votesUnclaimed") ||
				!data.get("votesUnclaimed").isJsonPrimitive() ||
				!data.getAsJsonPrimitive("votesUnclaimed").isNumber()) {
				mLogger.severe("CommandPacket failed to parse required int field 'votesUnclaimed'");
				return;
			}

			UUID uuid = UUID.fromString(data.get("playerUUID").getAsString());
			int count = data.get("votesUnclaimed").getAsInt();

			VoteManager.gotShardVoteCountRequest(event.getSource(), uuid, count);
			break;
		}
		case CHECK_RAFFLE_ELIGIBILITY_CHANNEL: {
			JsonObject data = event.getData();
			if (!data.has("playerUUID") ||
				!data.get("playerUUID").isJsonPrimitive() ||
				!data.getAsJsonPrimitive("playerUUID").isString()) {
				mLogger.severe("CheckRaffleEligibilityPacket failed to parse required string field 'playerUUID'");
				return;
			}

			if (!data.has("claimReward") ||
				!data.get("claimReward").isJsonPrimitive() ||
				!data.getAsJsonPrimitive("claimReward").isBoolean()) {
				mLogger.severe("CheckRaffleEligibilityPacket failed to parse required int field 'claimReward'");
				return;
			}

			if (!data.has("eligible") ||
				!data.get("eligible").isJsonPrimitive() ||
				!data.getAsJsonPrimitive("eligible").isBoolean()) {
				mLogger.severe("CheckRaffleEligibilityPacket failed to parse required int field 'eligible'");
				return;
			}

			UUID uuid = UUID.fromString(data.get("playerUUID").getAsString());
			boolean claimReward = data.get("claimReward").getAsBoolean();
			boolean eligible = data.get("eligible").getAsBoolean();

			VoteManager.gotShardRaffleEligibilityRequest(event.getSource(), uuid, claimReward, eligible);
			break;
		}
		default:
			break;
		}
	}

	/*
	 * Server -> Bungee
	 *   claimReward = false -> This is just a test to see if the player is eligible (raffle rewards > 0)
	 *   claimReward = true -> Bungee should subtract 1 from the player's raffle rewards score
	 *   eligible = true -> Player previously tried to claim a raffle but item wasn't eligible - add back the raffle point
	 *   eligible = false -> Regular request
	 *
	 *   eligible == true && claimReward == true -> Undefined
	 *
	 * Bungee -> Server
	 *   claimReward same as request
	 *   eligible = Is the player eligible to claim a raffle reward.
	 *   If claimReward and eligible, attempt to grant raffle reward
	 */
	public static void sendCheckRaffleEligibilityPacket(String destination, UUID playerUUID, boolean claimReward, boolean addBack) {
		if (INSTANCE != null) {
			JsonObject data = new JsonObject();
			data.addProperty("playerUUID", playerUUID.toString());
			data.addProperty("claimReward", claimReward);
			data.addProperty("eligible", addBack);
			try {
				NetworkRelayAPI.sendMessage(destination, CHECK_RAFFLE_ELIGIBILITY_CHANNEL, data);
			} catch (Exception ex) {
				INSTANCE.mLogger.severe("Failed to send check raffle eligibility message: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	/*
	 * For Server -> Bungee:
	 *   if votesUnclaimed == 0, request the count of eligible voting rewards
	 *   if votesUnclaimed > 0, return these votes back to bungee for storage
	 * For Bungee -> Server
	 *   Decrement storage, and send votes as votesUnclaimed
	 */
	public static void sendGetVotesUnclaimedPacket(String destination, UUID playerUUID, int count) {
		if (INSTANCE != null) {
			JsonObject data = new JsonObject();
			data.addProperty("playerUUID", playerUUID.toString());
			data.addProperty("votesUnclaimed", count);
			try {
				NetworkRelayAPI.sendMessage(destination, GET_VOTES_UNCLAIMED_CHANNEL, data);
			} catch (Exception ex) {
				INSTANCE.mLogger.severe("Failed to send get votes unclaimed message: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
}
