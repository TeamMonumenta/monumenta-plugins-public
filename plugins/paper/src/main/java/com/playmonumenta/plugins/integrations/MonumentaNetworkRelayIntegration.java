package com.playmonumenta.plugins.integrations;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.GatherRemotePlayerDataEvent;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.RemotePlayerAPI;
import com.playmonumenta.networkrelay.RemotePlayerLoadedEvent;
import com.playmonumenta.networkrelay.RemotePlayerUnloadedEvent;
import com.playmonumenta.networkrelay.RemotePlayerUpdatedEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class MonumentaNetworkRelayIntegration implements Listener {
	public static final String AUDIT_LOG_CHANNEL = "Monumenta.Automation.AuditLog";
	public static final String AUDIT_LOG_SEVERE_CHANNEL = "Monumenta.Automation.AuditLogSevere";
	public static final String AUDIT_LOG_CHAT_MOD_CHANNEL = "Monumenta.Automation.ChatModAuditLog";
	public static final String AUDIT_LOG_DEATH_CHANNEL = "Monumenta.Automation.DeathAuditLog";
	public static final String AUDIT_LOG_PLAYERS_CHANNEL = "Monumenta.Automation.PlayerAuditLog";
	public static final String AUDIT_LOG_MAIL_CHANNEL = "Monumenta.Automation.MailAuditLog";
	public static final String AUDIT_LOG_MARKET_CHANNEL = "Monumenta.Automation.MarketAuditLog";
	public static final String AUDIT_LOG_REPORT_CHANNEL = "Monumenta.Automation.ReportAuditLog";
	public static final String ADMIN_ALERT_CHANNEL = "Monumenta.Automation.AdminNotification";
	private static final int CHARS_PER_MESSAGE_GROUP = 1950;

	private final Logger mLogger;
	private static @Nullable MonumentaNetworkRelayIntegration INSTANCE = null;
	private Map<String, StringBuilder> mLogBuffer = new HashMap<>();
	private @Nullable BukkitRunnable mLogBufferRunnable = null;

	public MonumentaNetworkRelayIntegration(Logger logger) {
		logger.info("Enabling MonumentaNetworkRelay integration");
		mLogger = logger;
		INSTANCE = this;
	}

	public static void disable() {
		MonumentaNetworkRelayIntegration instance = INSTANCE;
		if (instance == null) {
			return;
		}

		instance.mLogger.info("Disabling MonumentaNetworkRelay integration");

		if (instance.mLogBufferRunnable != null) {
			instance.mLogBufferRunnable.cancel();
			instance.mLogBufferRunnable = null;
		}

		INSTANCE = null;
	}

	private static void sendAuditLogMessage(String message, String channel) {
		MonumentaNetworkRelayIntegration instance = INSTANCE;
		if (instance != null) {
			// Ensure we're running sync
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				StringBuilder builder = instance.mLogBuffer.computeIfAbsent(channel, k -> new StringBuilder());

				if (builder.length() + message.length() + 2 >= CHARS_PER_MESSAGE_GROUP) {
					sendAuditLogMessageImmediate(builder.toString(), channel);
					builder.setLength(0);
				}

				if (!builder.isEmpty()) {
					builder.append("\n");
				}
				builder.append(message);

				if (instance.mLogBufferRunnable == null) {
					instance.mLogBufferRunnable = new BukkitRunnable() {
						@Override
						public void run() {
							instance.processBuffer();
						}
					};
					instance.mLogBufferRunnable.runTaskTimer(Plugin.getInstance(), 1L, 1L);
				}
			});
		}
	}

	private void processBuffer() {
		MonumentaNetworkRelayIntegration instance = INSTANCE;
		if (instance == null) {
			return;
		}

		if (mLogBufferRunnable != null) {
			mLogBufferRunnable.cancel();
			mLogBufferRunnable = null;
		}
		Map<String, StringBuilder> logBuffer = mLogBuffer;
		mLogBuffer = new HashMap<>();

		for (Map.Entry<String, StringBuilder> entry : logBuffer.entrySet()) {
			String channel = entry.getKey();
			String message = entry.getValue().toString();
			if (message.isEmpty()) {
				continue;
			}

			sendAuditLogMessageImmediate(message, channel);
		}
	}

	private static void sendAuditLogMessageImmediate(String message, String channel) {
		MonumentaNetworkRelayIntegration instance = INSTANCE;
		if (instance != null) {
			JsonObject data = new JsonObject();
			data.addProperty("message", message);
			try {
				NetworkRelayAPI.sendMessage("automation-bot", channel, data);
			} catch (Exception ex) {
				instance.mLogger.severe("Failed to send audit log message: " + ex.getMessage());
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
			}
		}
	}

	public static void sendDeathAuditLogMessage(String message) {
		sendAuditLogMessage(message, AUDIT_LOG_DEATH_CHANNEL);
	}

	public static void sendMailAuditLogMessage(String message) {
		sendAuditLogMessage(message, AUDIT_LOG_MAIL_CHANNEL);
	}

	public static void sendMarketAuditLogMessage(String message) {
		sendAuditLogMessage(message, AUDIT_LOG_MARKET_CHANNEL);
	}

	public static void sendPlayerAuditLogMessage(String message) {
		sendAuditLogMessage(message, AUDIT_LOG_PLAYERS_CHANNEL);
	}

	public static void sendAuditLogSevereMessage(String message) {
		sendAuditLogMessage(message, AUDIT_LOG_SEVERE_CHANNEL);
	}

	public static void sendAuditLogChatModMessage(String message) {
		sendAuditLogMessage(message, AUDIT_LOG_CHAT_MOD_CHANNEL);
	}

	public static void sendModAuditLogMessage(String message) {
		sendAuditLogMessage(message, AUDIT_LOG_CHANNEL);
	}

	public static void broadcastCommand(String command) {
		MonumentaNetworkRelayIntegration instance = INSTANCE;
		if (instance != null) {
			try {
				NetworkRelayAPI.sendBroadcastCommand(command);
			} catch (Exception ex) {
				instance.mLogger.severe("Failed to send broadcast message: " + ex.getMessage());
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
			}
		}
	}

	public static void sendAdminMessage(String message) {
		MonumentaNetworkRelayIntegration instance = INSTANCE;
		if (instance != null) {
			JsonObject data = new JsonObject();
			data.addProperty("message", message);
			try {
				NetworkRelayAPI.sendMessage("automation-bot", ADMIN_ALERT_CHANNEL, data);
			} catch (Exception ex) {
				instance.mLogger.severe("Failed to send admin alert message: " + ex.getMessage());
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
			}
		}
	}

	public static void sendReportAuditLogMessage(String message) {
		sendAuditLogMessage(message, AUDIT_LOG_REPORT_CHANNEL);
	}

	// TAB stuff
	@EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
	public void gatherPluginDataEvent(GatherRemotePlayerDataEvent event) {
		JsonObject data = LuckPermsIntegration.getPluginData(event.mRemotePlayer.getUuid());
		if (data != null) {
			event.setPluginData("monumenta", data);
		}
	}

	// Updates RemotePlayer information for other shards
	public static void refreshPlayer(Player player) {
		RemotePlayerAPI.refreshPlayer(player.getUniqueId());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void remotePlayerLoad(RemotePlayerLoadedEvent event) {
		TABIntegration.loadRemotePlayer(event.mRemotePlayer);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void remotePlayerUnload(RemotePlayerUnloadedEvent event) {
		TABIntegration.unloadRemotePlayer(event.mRemotePlayer);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void remotePlayerUpdated(RemotePlayerUpdatedEvent event) {
		TABIntegration.loadRemotePlayer(event.mRemotePlayer);
	}
}
