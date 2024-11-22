package com.playmonumenta.plugins.mail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.NetworkRelayMessageEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.mail.recipient.GuildRecipient;
import com.playmonumenta.plugins.mail.recipient.MailDirection;
import com.playmonumenta.plugins.mail.recipient.PlayerRecipient;
import com.playmonumenta.plugins.mail.recipient.Recipient;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import dev.jorel.commandapi.CommandAPICommand;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MailMan implements Listener {
	private static final String MAIL_SLOT_UPDATE_CHANNEL
		= "com.playmonumenta.plugins.mail.MailMan.broadcastMailSlotChange";
	private static final String BLOCK_ALLOW_LIST_UPDATE_CHANNEL
		= "com.playmonumenta.plugins.mail.MailMan.broadcastBlockAllowListChange";
	private static final String MAIL_KEY = "com.playmonumenta.plugins.mail.MailMan";

	// Configurable constants:
	public static final int INTERACTIONS_PER_TIME_LIMIT = 14;
	public static final int INTERACTION_TIME_LIMIT_MINUTES = 5;

	private static @Nullable MailMan INSTANCE = null;
	private static final ConcurrentSkipListMap<Recipient, MailCache> mRecipientMailCaches = new ConcurrentSkipListMap<>();
	private static final ConcurrentSkipListSet<MailGui> mOpenMailGuis = new ConcurrentSkipListSet<>();
	private static final HashMap<UUID, HashSet<String>> mPlayerActiveTransactions = new HashMap<>();
	private static final HashMap<UUID, List<LocalDateTime>> mPlayerInteractionCooldowns = new HashMap<>();
	private static final TreeSet<Integer> mScheduledCleanupTicks = new TreeSet<>();
	private static @Nullable BukkitRunnable mCleanupRunnable = null;

	private MailMan() {
		INSTANCE = this;
	}

	public static MailMan getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new MailMan();
		}
		return INSTANCE;
	}

	/**
	 * Register mailbox-related commands
	 */
	public static void registerCommands() {
		if (ServerProperties.getShardName().startsWith("tutorial")) {
			return;
		}

		CommandAPICommand root = new CommandAPICommand("mail");
		CommandAPICommand mod = new CommandAPICommand("mod");

		BlockAllowListCommand.attach(root, mod);
		MailboxGui.attach(root, mod);
		SendGui.attach(root);

		root.withSubcommand(mod);
		root.register();
	}

	public static void onDisable() {
		if (mCleanupRunnable != null) {
			mCleanupRunnable.cancel();
			mCleanupRunnable = null;
		}
	}

	public static MailCache recipientMailCache(Recipient recipient) {
		return mRecipientMailCaches.computeIfAbsent(recipient, MailCache::new);
	}

	/**
	 * Registers a mailbox, returning the already registered mailbox instead if it exists
	 * @param mailbox A mailbox to register
	 * @return The stored mailbox, whether new or old
	 */
	public static Mailbox getOrRegister(Mailbox mailbox) {
		Mailbox result = getOrRegister(mailbox, false);
		if (result == null) {
			throw new RuntimeException("This will never happen. There used to be a contract saying so. ErrorProne is stupid.");
		}
		return result;
	}

	public static @Nullable Mailbox getOrRegister(Mailbox mailbox, boolean requireLoaded) {
		boolean foundRecipient = false;

		for (Recipient participant : mailbox.participants()) {
			MailCache mailCache = mRecipientMailCaches.get(participant);
			if (mailCache != null) {
				foundRecipient = true;
				mailbox = mailCache.registerMailbox(mailbox);
			}
		}

		return (!requireLoaded || foundRecipient) ? mailbox : null;
	}

	protected static void registerMailGui(MailGui mailGui) {
		mOpenMailGuis.add(mailGui);
	}

	protected static void unregisterMailGui(MailGui mailGui) {
		mOpenMailGuis.remove(mailGui);
		scheduleCleanupTask();
	}

	public static void broadcastMailSlotChange(Mailbox mailbox, int slot) {
		if (INSTANCE == null) {
			return;
		}

		JsonObject changeJson = new JsonObject();
		changeJson.add("mailbox", mailbox.toJson());
		changeJson.addProperty("slot", slot);
		try {
			NetworkRelayAPI.sendBroadcastMessage(MAIL_SLOT_UPDATE_CHANNEL, changeJson);
		} catch (Exception e) {
			MMLog.warning("[Mailbox] Failed to notify other shards of an updated slot through RabbitMQ; this won't affect items, but RabbitMQ needs fixing");
		}
	}

	public static void broadcastBlockAllowListChange(
		Recipient listOwner,
		BlockAllowListType listType,
		Recipient target,
		boolean isAdded
	) {
		if (INSTANCE == null) {
			return;
		}

		JsonObject changeJson = new JsonObject();
		changeJson.add("listOwner", listOwner.toJson());
		changeJson.addProperty("listType", listType.argument());
		changeJson.add("target", target.toJson());
		changeJson.addProperty("isAdded", isAdded);
		try {
			NetworkRelayAPI.sendBroadcastMessage(BLOCK_ALLOW_LIST_UPDATE_CHANNEL, changeJson);
		} catch (Exception e) {
			MMLog.warning("[Mailbox] Failed to notify other shards of an updated block/allow list through RabbitMQ; this is non-critical, but RabbitMQ needs fixing");
		}
	}

	public void handleRemoteMailSlotChange(JsonObject data) {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				Mailbox remoteMailbox = Mailbox.fromJson(data.getAsJsonObject("mailbox")).join();
				MMLog.finer(() -> "[Mailbox] Got remote mail slot update message for mailbox: "
					+ MessagingUtils.plainText(remoteMailbox.friendlyName()));

				MailCache receiverCache = mRecipientMailCaches.get(remoteMailbox.receiver());
				int receivedMail = receiverCache == null ? -1 : receiverCache.totalMailboxesReceived();

				Mailbox mailbox = getOrRegister(remoteMailbox, true);
				if (mailbox == null) {
					MMLog.finer(() -> "[Mailbox] No locally cached mailbox for remote mail slot update; ignoring");
					return;
				}

				int slot = data.getAsJsonPrimitive("slot").getAsInt();
				MMLog.finer(() -> "[Mailbox] Preparing to update slot " + slot);
				mailbox.refreshSlot(slot).join();
				MMLog.finer(() -> "[Mailbox] Updating any GUIs affected by remote mail slot update");
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					for (MailGui mailGui : mOpenMailGuis) {
						mailGui.refreshMailbox(mailbox);
					}
				});

				if (receiverCache != null) {
					if (receivedMail == 0) {
						receiverCache.showGotMailMessage();
					}
				}
			} catch (Throwable throwable) {
				MMLog.warning("[Mailbox] Got invalid remote mail slot update message:");
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), throwable);
			}
		});
	}

	public void handleBlockAllowListChange(JsonObject data) {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				Recipient listOwner;
				if (data.get("listOwner") instanceof JsonObject listOwnerJson) {
					listOwner = Recipient.fromJson(listOwnerJson).join();
				} else {
					MMLog.warning("Got block/allow list update message with no listOwner");
					return;
				}

				MailCache mailCache = mRecipientMailCaches.get(listOwner);
				if (mailCache == null) {
					return;
				}

				BlockAllowListType listType;
				if (data.get("listType") instanceof JsonPrimitive listTypePrimitive && listTypePrimitive.isString()) {
					listType = BlockAllowListType.byArgument(listTypePrimitive.getAsString());
				} else {
					MMLog.warning("Got block/allow list update message with no listType");
					return;
				}
				if (listType == null) {
					MMLog.warning("Got block/allow list update message with invalid listType");
					return;
				}

				Recipient target;
				if (data.get("target") instanceof JsonObject targetJson) {
					target = Recipient.fromJson(targetJson).join();
				} else {
					MMLog.warning("Got block/allow list update message with no target");
					return;
				}

				boolean isAdded;
				if (data.get("isAdded") instanceof JsonPrimitive isAddedPrimitive && isAddedPrimitive.isBoolean()) {
					isAdded = isAddedPrimitive.getAsBoolean();
				} else {
					MMLog.warning("Got block/allow list update message didn't specify isAdded (or removed)");
					return;
				}

				if (isAdded) {
					mailCache.blockAllowListAdd(listType, target, false).join();
				} else {
					mailCache.blockAllowListRemove(listType, target, false).join();
				}
			} catch (Throwable throwable) {
				MMLog.warning("[Mailbox] Got invalid remote block/allow list update message:");
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), throwable);
			}
		});
	}

	/**
	 * Attempts to add a cooldown on mail interactions.
	 * @param player The player whose mail is put on cooldown
	 * @return time until the next one expires is returned as a string if too many are active, otherwise null
	 */
	protected static @Nullable String addInteractionCooldown(Player player) {
		UUID playerId = player.getUniqueId();
		LocalDateTime now = DateUtils.trueUtcDateTime();
		clearOldInteractionCooldowns(playerId, now);

		List<LocalDateTime> cooldowns = mPlayerInteractionCooldowns.computeIfAbsent(playerId, k -> new ArrayList<>());
		if (cooldowns.size() >= INTERACTIONS_PER_TIME_LIMIT) {
			return StringUtils.intToMinuteAndSeconds((int) now.until(cooldowns.get(0), ChronoUnit.SECONDS));
		}

		cooldowns.add(now.plusMinutes(INTERACTION_TIME_LIMIT_MINUTES));
		return null;
	}

	private static void clearOldInteractionCooldowns(UUID playerId, LocalDateTime now) {
		List<LocalDateTime> cooldowns = mPlayerInteractionCooldowns.computeIfAbsent(playerId, k -> new ArrayList<>());

		while (!cooldowns.isEmpty() && now.isAfter(cooldowns.get(0))) {
			cooldowns.remove(0);
		}
	}

	protected static void interactionChange(Player player, String slotKey, boolean isInteracting) {
		UUID playerId = player.getUniqueId();
		Set<String> interactionList
			= mPlayerActiveTransactions.computeIfAbsent(playerId, k -> new HashSet<>());
		if (isInteracting) {
			interactionList.add(slotKey);
		} else {
			interactionList.remove(slotKey);
			if (interactionList.isEmpty()) {
				mPlayerActiveTransactions.remove(playerId);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		UUID playerId = player.getUniqueId();
		Recipient playerRecipient = new PlayerRecipient(playerId);
		mRecipientMailCaches.computeIfAbsent(playerRecipient, MailCache::new);

		Group guild = LuckPermsIntegration.getGuildRoot(LuckPermsIntegration.getGuild(player));
		if (guild != null) {
			Long guildId = LuckPermsIntegration.getGuildPlotId(guild);
			if (guildId == null) {
				MMLog.warning("Guild " + guild.getFriendlyName() + " has no plot ID");
			} else {
				Recipient recipient = new GuildRecipient(guildId, guild);
				mRecipientMailCaches.computeIfAbsent(recipient, MailCache::new);
			}
		}

		JsonObject mailData = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), MAIL_KEY);
		if (mailData != null) {
			if (mailData.get("cooldowns") instanceof JsonArray cooldownsJson) {
				List<LocalDateTime> cooldowns = mPlayerInteractionCooldowns
					.computeIfAbsent(playerId, k -> new ArrayList<>());
				for (JsonElement cooldownJson : cooldownsJson) {
					if (cooldownJson instanceof JsonPrimitive cooldownPrimitive && cooldownPrimitive.isNumber()) {
						long cooldownSecond = cooldownPrimitive.getAsLong();
						cooldowns.add(LocalDateTime.ofEpochSecond(cooldownSecond, 0, ZoneOffset.UTC));
					}
				}
			}

			JsonElement midTransactionJson = mailData.get("mid_transaction");
			if (
				midTransactionJson instanceof JsonPrimitive midTransactionPrimitive
					&& midTransactionPrimitive.isBoolean()
					&& midTransactionPrimitive.getAsBoolean()
			) {
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(),
					() -> player.sendMessage(
						Component.text("Your mail may have dropped on the ground", NamedTextColor.YELLOW)
					), 1L);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerSaveEvent(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		UUID playerId = player.getUniqueId();

		JsonObject mailData = new JsonObject();

		JsonArray cooldownsJson = new JsonArray();
		clearOldInteractionCooldowns(playerId, DateUtils.trueUtcDateTime());
		for (LocalDateTime cooldown : mPlayerInteractionCooldowns.getOrDefault(playerId, new ArrayList<>())) {
			cooldownsJson.add(cooldown.toEpochSecond(ZoneOffset.UTC));
		}
		if (!cooldownsJson.isEmpty()) {
			mailData.add("cooldowns", cooldownsJson);
		}

		mailData.addProperty("mid_transaction",
			!mPlayerActiveTransactions.getOrDefault(playerId, new HashSet<>()).isEmpty());

		// TODO JsonObject.isEmpty() is not available until gson 10.1, which is not available in Paper 1.19.4
		if (!mailData.entrySet().isEmpty()) {
			event.setPluginData(MAIL_KEY, mailData);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		scheduleCleanupTask();
	}

	public static void playerGuildChange(Player player) {
		Group guild = LuckPermsIntegration.getGuildRoot(LuckPermsIntegration.getGuild(player));
		if (guild != null) {
			Long guildId = LuckPermsIntegration.getGuildPlotId(guild);
			if (guildId == null) {
				MMLog.warning("Guild " + guild.getFriendlyName() + " has no plot ID");
			} else {
				Recipient recipient = new GuildRecipient(guildId, guild);
				mRecipientMailCaches.computeIfAbsent(recipient, MailCache::new);
			}
		}
		scheduleCleanupTask();
	}

	private static void scheduleCleanupTask() {
		int targetTick = Bukkit.getCurrentTick() + 20;
		mScheduledCleanupTicks.add(targetTick);

		if (mCleanupRunnable != null) {
			return;
		}
		mCleanupRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				removeUnusedMailCaches();

				int currentTick = Bukkit.getCurrentTick();
				while (!mScheduledCleanupTicks.isEmpty()) {
					int testTick = mScheduledCleanupTicks.first();
					if (testTick <= currentTick) {
						mScheduledCleanupTicks.remove(testTick);
						continue;
					}
					return;
				}

				mCleanupRunnable = null;
				cancel();
			}
		};
		mCleanupRunnable.runTaskTimer(Plugin.getInstance(), 20, 20);
	}

	private static void removeUnusedMailCaches() {
		MMLog.finer(() -> "[MailMan] Clearing unused mail caches...");

		// Don't remove caches for online players
		Set<Recipient> toRemove = new TreeSet<>(mRecipientMailCaches.keySet());
		MMLog.finer(() -> "[MailMan] Caches: " + recipientCollectionToString(toRemove));
		toRemove.removeIf(e -> {
			if (!(e instanceof PlayerRecipient playerRecipient)) {
				return true;
			}

			UUID playerId = playerRecipient.getPlayerId();
			OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);

			if (player.isOnline()) {
				return true;
			}
			mPlayerInteractionCooldowns.remove(playerId);
			mPlayerActiveTransactions.remove(playerId);
			return false;
		});
		MMLog.finer(() -> "[MailMan] Offline players: " + recipientCollectionToString(toRemove));

		// Don't remove any caches for guilds that have players online
		Set<Recipient> guildRecipientsToRemove = new TreeSet<>(mRecipientMailCaches.keySet());
		guildRecipientsToRemove.removeIf(e -> !(e instanceof GuildRecipient));
		for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
			Group guild = LuckPermsIntegration.getGuildRoot(LuckPermsIntegration.getGuild(onlinePlayer));
			if (guild == null) {
				continue;
			}

			Long guildId = LuckPermsIntegration.getGuildPlotId(guild);
			if (guildId == null) {
				MMLog.warning("Guild " + guild.getFriendlyName() + " has no plot ID");
				continue;
			}

			Recipient recipient = new GuildRecipient(guildId, guild);
			guildRecipientsToRemove.remove(recipient);
		}
		MMLog.finer(() -> "[MailMan] Guilds with no online players: " + recipientCollectionToString(guildRecipientsToRemove));
		toRemove.addAll(guildRecipientsToRemove);

		// Don't remove any caches currently in use by GUIs
		Set<Recipient> guiRecipientsToKeep = new TreeSet<>();
		for (MailGui mailGui : mOpenMailGuis) {
			for (MailCache mailCache : mailGui.getRecipientCaches()) {
				guiRecipientsToKeep.add(mailCache.recipient());
			}
		}
		MMLog.finer(() -> "[MailMan] Caches in use by GUIs; to keep: " + recipientCollectionToString(guiRecipientsToKeep));
		toRemove.removeAll(guiRecipientsToKeep);

		for (Recipient recipient : toRemove) {
			MMLog.finer(() -> "[Mailbox] Unloaded mail cache for " + recipient.friendlyStr(MailDirection.DEFAULT));
			mRecipientMailCaches.remove(recipient);
		}
	}

	private static String recipientCollectionToString(Collection<Recipient> recipients) {
		StringBuilder builder = new StringBuilder();
		boolean notFirst = false;
		for (Recipient recipient : recipients) {
			if (notFirst) {
				builder.append(", ");
			} else {
				notFirst = true;
			}
			builder.append(recipient.friendlyStr(MailDirection.DEFAULT));
		}
		return builder.toString();
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void networkRelayMessageEvent(@NotNull NetworkRelayMessageEvent event) {
		JsonObject data = event.getData();
		switch (event.getChannel()) {
			case MAIL_SLOT_UPDATE_CHANNEL -> handleRemoteMailSlotChange(data);
			case BLOCK_ALLOW_LIST_UPDATE_CHANNEL -> handleBlockAllowListChange(data);
			default -> {
			}
		}
	}
}
