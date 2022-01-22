package com.playmonumenta.plugins.integrations;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.NetworkRelayMessageEvent;
import com.playmonumenta.plugins.commands.ClaimRaffle;
import com.playmonumenta.plugins.commands.RedeemVoteRewards;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;
import java.util.logging.Logger;

public class MonumentaNetworkRelayIntegration implements Listener {
	public static final String AUDIT_LOG_CHANNEL = "Monumenta.Automation.AuditLog";
	public static final String AUDIT_LOG_SEVERE_CHANNEL = "Monumenta.Automation.AuditLogSevere";
	public static final String GET_VOTES_UNCLAIMED_CHANNEL = "Monumenta.Bungee.GetVotesUnclaimed";
	public static final String CHECK_RAFFLE_ELIGIBILITY_CHANNEL = "Monumenta.Bungee.CheckRaffleEligibility";
	public static final String ADMIN_ALERT_CHANNEL = "Monumenta.Automation.AdminNotification";

	private final Logger mLogger;
	private static @Nullable MonumentaNetworkRelayIntegration INSTANCE = null;

	public MonumentaNetworkRelayIntegration(Logger logger) {
		logger.info("Enabling MonumentaNetworkRelay integration");
		mLogger = logger;
		INSTANCE = this;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void networkRelayMessageEvent(NetworkRelayMessageEvent event) {
		switch (event.getChannel()) {
		case GET_VOTES_UNCLAIMED_CHANNEL: {
			JsonObject data = event.getData();
			if (!data.has("playerUUID") ||
				!data.get("playerUUID").isJsonPrimitive() ||
				!data.getAsJsonPrimitive("playerUUID").isString()) {
				mLogger.severe("GetVotesUnclaimedPacket failed to parse required string field 'playerUUID'");
				return;
			}

			if (!data.has("votesUnclaimed") ||
				!data.get("votesUnclaimed").isJsonPrimitive() ||
				!data.getAsJsonPrimitive("votesUnclaimed").isNumber()) {
				mLogger.severe("GetVotesUnclaimedPacket failed to parse required int field 'votesUnclaimed'");
				return;
			}

			UUID uuid = UUID.fromString(data.get("playerUUID").getAsString());
			int count = data.get("votesUnclaimed").getAsInt();

			RedeemVoteRewards.gotVoteRewardMessage(mLogger, uuid, count);
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

			ClaimRaffle.queryResponseReceived(uuid, claimReward, eligible);
			break;
		}
		default:
			break;
		}
	}

	public static void sendAuditLogSevereMessage(String message) {
		if (INSTANCE != null) {
			JsonObject data = new JsonObject();
			data.addProperty("message", message);
			try {
				NetworkRelayAPI.sendMessage("automation-bot", AUDIT_LOG_SEVERE_CHANNEL, data);
			} catch (Exception ex) {
				INSTANCE.mLogger.severe("Failed to send audit log message: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	public static void sendAuditLogMessage(String message) {
		if (INSTANCE != null) {
			JsonObject data = new JsonObject();
			data.addProperty("message", message);
			try {
				NetworkRelayAPI.sendMessage("automation-bot", AUDIT_LOG_CHANNEL, data);
			} catch (Exception ex) {
				INSTANCE.mLogger.severe("Failed to send audit log message: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	/*
	 * Server -> Bungee
	 *  claimReward = false -> This is just a test to see if the player is eligible (raffle rewards > 0)
	 *  claimReward = true -> Bungee should subtract 1 from the player's raffle rewards score
	 *  eligible = true -> Player previously tried to claim a raffle but item wasn't eligible - add back the raffle point
	 *  eligible = false -> Regular request
	 *
	 *  eligible == true && claimReward == true -> Undefined
	 *
	 * Bungee -> Server
	 *  claimReward same as request
	 *  eligible = Is the player eligible to claim a raffle reward.
	 *  If claimReward and eligible, attempt to grant raffle reward
	 */
	public static void sendCheckRaffleEligibilityPacket(UUID playerUUID, boolean claimReward, boolean addBack) {
		if (INSTANCE != null) {
			JsonObject data = new JsonObject();
			data.addProperty("playerUUID", playerUUID.toString());
			data.addProperty("claimReward", claimReward);
			data.addProperty("eligible", addBack);
			try {
				NetworkRelayAPI.sendMessage("bungee", CHECK_RAFFLE_ELIGIBILITY_CHANNEL, data);
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
	public static void sendGetVotesUnclaimedPacket(UUID playerUUID, int count) {
		if (INSTANCE != null) {
			JsonObject data = new JsonObject();
			data.addProperty("playerUUID", playerUUID.toString());
			data.addProperty("votesUnclaimed", count);
			try {
				NetworkRelayAPI.sendMessage("bungee", GET_VOTES_UNCLAIMED_CHANNEL, data);
			} catch (Exception ex) {
				INSTANCE.mLogger.severe("Failed to send get votes unclaimed message: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	public static void broadcastCommand(String command) {
		if (INSTANCE != null) {
			try {
				NetworkRelayAPI.sendBroadcastCommand(command);
			} catch (Exception ex) {
				INSTANCE.mLogger.severe("Failed to send broadcast message: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	public static void sendAdminMessage(String message) {
		if (INSTANCE != null) {
			JsonObject data = new JsonObject();
			data.addProperty("message", message);
			try {
				NetworkRelayAPI.sendMessage("automation-bot", ADMIN_ALERT_CHANNEL, data);
			} catch (Exception ex) {
				INSTANCE.mLogger.severe("Failed to send admin alert message: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}
}
