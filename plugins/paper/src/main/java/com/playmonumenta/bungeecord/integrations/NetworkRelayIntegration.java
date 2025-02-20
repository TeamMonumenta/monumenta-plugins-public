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
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public class NetworkRelayIntegration implements Listener {
	public static final String VOTE_NOTIFY_CHANNEL = "Monumenta.Bungee.VoteNotify";

	private final Logger mLogger;
	private static @MonotonicNonNull NetworkRelayIntegration INSTANCE = null;

	public NetworkRelayIntegration(Logger logger) {
		logger.info("Enabling MonumentaNetworkRelay integration");
		mLogger = logger;
		INSTANCE = this;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void networkRelayMessageEventBungee(NetworkRelayMessageEventBungee event) {
		if (event.getChannel().equals(VOTE_NOTIFY_CHANNEL)) {
			JsonObject data = event.getData();
			if (!data.has("playerUUID") ||
				!data.get("playerUUID").isJsonPrimitive() ||
				!data.getAsJsonPrimitive("playerUUID").isString()) {
				mLogger.severe("VOTE_NOTIFY_CHANNEL failed to parse required string field 'playerUUID'");
				return;
			}

			if (!data.has("matchingSite") ||
				!data.get("matchingSite").isJsonPrimitive() ||
				!data.getAsJsonPrimitive("matchingSite").isString()) {
				mLogger.severe("VOTE_NOTIFY_CHANNEL failed to parse required int field 'matchingSite'");
				return;
			}

			if (!data.has("cooldownMinutes") ||
					!data.get("cooldownMinutes").isJsonPrimitive() ||
					!data.getAsJsonPrimitive("cooldownMinutes").isNumber()) {
				mLogger.severe("VOTE_NOTIFY_CHANNEL failed to parse required int field 'cooldownMinutes'");
				return;
			}

			UUID uuid = UUID.fromString(data.get("playerUUID").getAsString());
			String matchingSite = data.get("matchingSite").getAsString();
			long cooldownMinutes = data.get("cooldownMinutes").getAsLong();

			VoteManager.gotVoteNotifyMessage(uuid, matchingSite, cooldownMinutes);
		}
	}

	public static void sendVoteNotifyPacket(UUID playerUUID, String matchingSite, long cooldownMinutes) {
		if (INSTANCE != null) {
			JsonObject data = new JsonObject();
			data.addProperty("playerUUID", playerUUID.toString());
			data.addProperty("matchingSite", matchingSite);
			data.addProperty("cooldownMinutes", cooldownMinutes);
			try {
				/* Send this message to whatever this shard is called (likely "bungee") */
				NetworkRelayAPI.sendBroadcastMessage(VOTE_NOTIFY_CHANNEL, data);
			} catch (Exception ex) {
				INSTANCE.mLogger.severe("Failed to send vote notify message: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	public static String getProxyName() {
		if (INSTANCE != null) {
			@Nullable String shardName;
			try {
				shardName = NetworkRelayAPI.getShardName();
			} catch (Exception exception) {
				shardName = null;
			}
			if (shardName == null) {
				return "Unknown";
			}
			return shardName;
		}
		return "No NetworkRelay integration";
	}
}
