package com.playmonumenta.plugins.social;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.JsonObject;
import com.playmonumenta.networkchat.RemotePlayerListener;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.NetworkRelayMessageEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.redissync.RedisAPI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SocialManager implements Listener {
	//region <DECLARATIONS>
	// General
	private static final String AUDIT_LOG_PREFIX = "[Social Manager] ";
	private static final Map<UUID, PlayerSocialData> SOCIAL_DATA_MAP = new ConcurrentHashMap<>();

	// Friends
	private static final String REDIS_SOCIAL_TYPE_FRIEND = "listOfFriends";
	private static final String FRIEND_KEY = "friendsWith|";
	private static final String REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST = "listOfPendingFriendRequests";
	private static final String PENDING_FRIEND_REQUEST_KEY = "pendingFriendRequestFrom|";
	private static final long FRIEND_REQUEST_EXPIRATION_TIME = 600; // 10 minutes
	private static final Map<String, BukkitTask> PENDING_FRIEND_REQUESTS = new ConcurrentHashMap<>();

	// Blocking
	private static final String REDIS_SOCIAL_TYPE_BLOCKED = "listOfBlockedPlayers";
	private static final String BLOCKED_KEY = "hasBlocked|";
	private static final String BLOCK_EXEMPTION_PERMISSION = "monumenta.social.blockexemption";

	// RabbitMQ channels
	private static final String INCOMING_FRIEND_REQUEST_NOTIFICATION_CHANNEL = "incomingFriendRequestNotificationChannel";
	private static final String FRIEND_REQUEST_EXPIRATION_NOTIFICATION_CHANNEL = "friendRequestExpirationNotificationChannel";
	private static final String FRIEND_REQUEST_ACCEPTED_NOTIFICATION_CHANNEL = "friendRequestAcceptedNotificationChannel";
	private static final String MODERATOR_FORCE_ADDED_FRIENDS_NOTIFICATION_CHANNEL = "moderatorForceAddedFriendsNotificationChannel";
	private static final String REMOVED_FRIEND_NOTIFICATION_CHANNEL = "removedFriendNotificationChannel";
	private static final String MODERATOR_FORCE_REMOVED_FRIENDS_NOTIFICATION_CHANNEL = "moderatorForceRemovedFriendsNotificationChannel";
	//endregion

	//region <HELPER FUNCTIONS>
	static Component appendPrefix(Component message) {
		Component prefix = Component.empty().append(Component.text("Social > ", NamedTextColor.GOLD));

		return prefix.append(message);
	}

	static Component appendHeaderAndFooter(Component message) {
		Component header = Component.empty().append(Component.text("-----------------------------------------------------", NamedTextColor.BLUE, TextDecoration.STRIKETHROUGH));
		Component body = Component.empty().append(message);
		Component footer = Component.empty().append(Component.text("-----------------------------------------------------", NamedTextColor.BLUE, TextDecoration.STRIKETHROUGH));

		return
			header.append(Component.newline())
				.append(body).append(Component.newline())
				.append(footer);
	}

	static String resolveName(UUID uuid) {
		String cachedName = MonumentaRedisSyncIntegration.cachedUuidToName(uuid);
		return cachedName != null ? cachedName : String.valueOf(uuid);
	}

	private static String formatTimestamp(String isoTimestamp) {
		try {
			Instant instant = Instant.parse(isoTimestamp);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());
			return formatter.format(instant);
		} catch (DateTimeParseException e) {
			return "Unknown Date";
		}
	}

	private static String getRedisPath(String socialType, UUID uuid) {
		return "socialData:" + socialType + ":" + uuid;
	}

	private static void broadcastNotification(String channel, Map<String, String> jsonProperties, String logMessage) {
		JsonObject jsonObject = new JsonObject();

		for (Map.Entry<String, String> entry : jsonProperties.entrySet()) {
			jsonObject.addProperty(entry.getKey(), entry.getValue());
		}

		try {
			NetworkRelayAPI.sendBroadcastMessage(channel, jsonObject);
		} catch (Exception exception) {
			MMLog.warning(logMessage);
		}
	}
	//endregion

	public static class SocialInfo {
		public static class Friend {
			final UUID mFriendUuid;
			final String mFriendTimestamp;
			@Nullable String mFriendName = null;
			@Nullable PlayerProfile mFriendProfile = null;
			@Nullable ItemStack mFriendHead = null;

			Friend(UUID friendUuid, String friendTimestamp) {
				mFriendUuid = friendUuid;
				mFriendTimestamp = friendTimestamp;
			}
		}

		public static class BlockedPlayer {
			final UUID mBlockedUuid;
			final String mBlockedTimestamp;
			@Nullable String mBlockedName = null;
			@Nullable PlayerProfile mBlockedProfile = null;
			@Nullable ItemStack mBlockedHead = null;

			BlockedPlayer(UUID blockedUuid, String blockedTimestamp) {
				mBlockedUuid = blockedUuid;
				mBlockedTimestamp = blockedTimestamp;
			}
		}

		// UUID of the player
		final UUID mPlayerUuid;

		// Map of the player's friends
		final Map<UUID, Friend> mFriendMap;

		// Map of the player's blocked users
		final Map<UUID, BlockedPlayer> mBlockedMap;

		protected SocialInfo(UUID playerUuid, Map<UUID, Friend> friendMap, Map<UUID, BlockedPlayer> blockedMap) {
			mPlayerUuid = playerUuid;
			mFriendMap = friendMap;
			mBlockedMap = blockedMap;
		}

		// Call this to fetch player names and heads
		CompletableFuture<SocialInfo> populateNamesAndHeads() {
			CompletableFuture<SocialInfo> future = new CompletableFuture<>();

			// Populate all the names and profiles on the main thread
			for (Map.Entry<UUID, Friend> entry : mFriendMap.entrySet()) {
				UUID uuid = entry.getKey();
				Friend friend = entry.getValue();
				friend.mFriendName = MonumentaRedisSyncIntegration.cachedUuidToName(uuid);
				friend.mFriendProfile = Bukkit.getServer().createProfile(uuid);
			}

			for (Map.Entry<UUID, BlockedPlayer> entry : mBlockedMap.entrySet()) {
				UUID uuid = entry.getKey();
				BlockedPlayer blockedPlayer = entry.getValue();
				blockedPlayer.mBlockedName = MonumentaRedisSyncIntegration.cachedUuidToName(uuid);
				blockedPlayer.mBlockedProfile = Bukkit.getServer().createProfile(uuid);
			}

			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				// Complete all the profiles asynchronously
				for (Map.Entry<UUID, Friend> entry : mFriendMap.entrySet()) {
					PlayerProfile playerProfile = entry.getValue().mFriendProfile;

					if (playerProfile != null) {
						playerProfile.complete();
					}
				}

				for (Map.Entry<UUID, BlockedPlayer> entry : mBlockedMap.entrySet()) {
					PlayerProfile playerProfile = entry.getValue().mBlockedProfile;

					if (playerProfile != null) {
						playerProfile.complete();
					}
				}

				// Switch back to the main thread to finish assembling all the heads
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					for (Map.Entry<UUID, Friend> entry : mFriendMap.entrySet()) {
						Friend friend = entry.getValue();

						ItemStack friendHead = new ItemStack(Material.PLAYER_HEAD, 1);
						SkullMeta skullMeta = (SkullMeta) friendHead.getItemMeta();
						skullMeta.setPlayerProfile(friend.mFriendProfile);
						skullMeta.displayName(Component.text(friend.mFriendName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
						List<Component> loreBuilder = new ArrayList<>();
						loreBuilder.add(Component.text("Friends since:", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
						loreBuilder.add(Component.text(formatTimestamp(friend.mFriendTimestamp), NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
						skullMeta.lore(loreBuilder);
						skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
						friendHead.setItemMeta(skullMeta);

						friend.mFriendHead = friendHead;
					}

					for (Map.Entry<UUID, BlockedPlayer> entry : mBlockedMap.entrySet()) {
						BlockedPlayer blockedPlayer = entry.getValue();

						ItemStack blockedHead = new ItemStack(Material.PLAYER_HEAD, 1);
						SkullMeta skullMeta = (SkullMeta) blockedHead.getItemMeta();
						skullMeta.setPlayerProfile(blockedPlayer.mBlockedProfile);
						skullMeta.displayName(Component.text(blockedPlayer.mBlockedName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
						List<Component> loreBuilder = new ArrayList<>();
						loreBuilder.add(Component.text("Blocked since:", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
						loreBuilder.add(Component.text(formatTimestamp(blockedPlayer.mBlockedTimestamp), NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
						skullMeta.lore(loreBuilder);
						skullMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
						blockedHead.setItemMeta(skullMeta);

						blockedPlayer.mBlockedHead = blockedHead;
					}

					// Complete the future on the main thread
					future.complete(this);
				});
			});

			return future;
		}
	}

	public static class PlayerSocialData {
		private final UUID mPlayerUuid;
		private final Set<UUID> mFriends = ConcurrentHashMap.newKeySet();

		public PlayerSocialData(UUID playerUuid) {
			mPlayerUuid = playerUuid;
			loadFriends();
		}

		public UUID getPlayerUuid() {
			return mPlayerUuid;
		}

		public Set<UUID> getFriends() {
			return mFriends;
		}

		public void addFriend(UUID friendUuid) {
			String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
			mFriends.add(friendUuid);
			RedisAPI.getInstance().async().hset(
				getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, mPlayerUuid),
				FRIEND_KEY + friendUuid,
				friendUuid.toString() + "|" + timestamp
			);
		}

		public void removeFriend(UUID friendUuid) {
			mFriends.remove(friendUuid);
			RedisAPI.getInstance().async().hdel(
				getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, mPlayerUuid),
				FRIEND_KEY + friendUuid
			);
		}

		private void loadFriends() {
			RedisAPI.getInstance().async().hgetall(getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, mPlayerUuid)).thenAccept(friends -> {
				friends.forEach((key, value) -> {
					UUID friendUuid = UUID.fromString(value.split("\\|")[0]);
					mFriends.add(friendUuid);
				});
			});
		}
	}

	//region <FRIENDS>
	static void sendFriendRequest(UUID senderUuid, UUID receiverUuid) {
		Player sender = Bukkit.getPlayer(senderUuid);

		// Prevent players from adding themselves
		if (senderUuid.equals(receiverUuid)) {
			if (sender != null) {
				sender.sendMessage(appendPrefix(Component.text("You cannot add yourself as a friend!", NamedTextColor.RED)));
			}
			return;
		}

		// Check if the sender and the receiver are already friends
		areFriends(senderUuid, receiverUuid).thenAccept(alreadyFriends -> {
			if (alreadyFriends) {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("You are already friends with " + resolveName(receiverUuid) + "!", NamedTextColor.RED)));
				}
				return;
			}

			// Check if either player has blocked the other
			CompletableFuture<Boolean> senderBlockedReceiver = hasPlayerBlocked(senderUuid, receiverUuid);
			CompletableFuture<Boolean> receiverBlockedSender = hasPlayerBlocked(receiverUuid, senderUuid);

			CompletableFuture.allOf(senderBlockedReceiver, receiverBlockedSender).thenRun(() -> {
				if (senderBlockedReceiver.join() || receiverBlockedSender.join()) {
					if (sender != null) {
						sender.sendMessage(appendPrefix(Component.text("You cannot send a friend request to " + resolveName(receiverUuid) + "!", NamedTextColor.RED)));
					}
					return;
				}

				// Check if the sender already has a pending request from the receiver
				RedisAPI.getInstance().async().hget(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, senderUuid), PENDING_FRIEND_REQUEST_KEY + receiverUuid).thenAccept(existingRequest -> {
					if (existingRequest != null) {
						// Automatically accept the request if there is one pending
						addFriend(null, senderUuid, receiverUuid);
						return;
					}

					// Check if the receiver already has a pending request from the sender
					RedisAPI.getInstance().async().hget(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, receiverUuid), PENDING_FRIEND_REQUEST_KEY + senderUuid).thenAccept(pendingStatus -> {
						if (pendingStatus != null) {
							if (sender != null) {
								sender.sendMessage(appendPrefix(Component.text("You have already sent a friend request to " + resolveName(receiverUuid) + "!", NamedTextColor.YELLOW)));
							}
							return;
						}

						// Send and store the pending request with an expiration time
						long expirationTime = System.currentTimeMillis() / 1000 + FRIEND_REQUEST_EXPIRATION_TIME;
						RedisAPI.getInstance().async().hset(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, receiverUuid), PENDING_FRIEND_REQUEST_KEY + senderUuid, String.valueOf(expirationTime)).thenAccept(success -> {
							if (!success) {
								if (sender != null) {
									sender.sendMessage(appendPrefix(Component.text("An error occurred while sending your friend request. Please report this as a bug if this keeps happening.", NamedTextColor.RED)));
								}
								return;
							}

							// Notify the sender about the pending request
							if (sender != null) {
								sender.sendMessage(appendPrefix(Component.text("You have sent a friend request to " + resolveName(receiverUuid) + "! They have " + FRIEND_REQUEST_EXPIRATION_TIME / 60L + " minutes to accept before it expires.", NamedTextColor.YELLOW)));
							}

							// Notify the receiver about the pending request via RabbitMQ
							Map<String, String> jsonProperties = Map.of(
								"senderUuid", String.valueOf(senderUuid),
								"receiverUuid", String.valueOf(receiverUuid)
							);
							broadcastNotification(INCOMING_FRIEND_REQUEST_NOTIFICATION_CHANNEL, jsonProperties, "Failed to notify other shards of an incoming friend request via RabbitMQ.");

							// Schedule the request expiration task
							BukkitTask expirationTask = Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> expireFriendRequest(senderUuid, receiverUuid), FRIEND_REQUEST_EXPIRATION_TIME * 20L);

							// Add the request expiration task to the map for possible cancellation
							PENDING_FRIEND_REQUESTS.put(senderUuid + "_" + receiverUuid, expirationTask);
						});
					});
				});
			});
		});
	}

	private static void expireFriendRequest(UUID senderUuid, UUID receiverUuid) {
		RedisAPI.getInstance().async().hget(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, receiverUuid), PENDING_FRIEND_REQUEST_KEY + senderUuid).thenAccept(storedTime -> {
			if (storedTime != null && Long.parseLong(storedTime) <= System.currentTimeMillis() / 1000) {
				// Remove the pending request from the receiver
				RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, receiverUuid), PENDING_FRIEND_REQUEST_KEY + senderUuid);

				// Remove the request expiration task from the map
				PENDING_FRIEND_REQUESTS.remove(senderUuid + "_" + receiverUuid);

				// Notify the sender and the receiver about the request expiration via RabbitMQ
				Map<String, String> jsonProperties = Map.of(
					"senderUuid", String.valueOf(senderUuid),
					"receiverUuid", String.valueOf(receiverUuid)
				);
				broadcastNotification(FRIEND_REQUEST_EXPIRATION_NOTIFICATION_CHANNEL, jsonProperties, "Failed to notify other shards of an expired friend request via RabbitMQ.");
			}
		});
	}

	static void addFriend(@Nullable Player moderator, UUID senderUuid, UUID receiverUuid) {
		Player sender = Bukkit.getPlayer(senderUuid);

		// Prevent a player from having themselves be added as a friend
		if (senderUuid.equals(receiverUuid)) {
			if (moderator != null) {
				moderator.sendMessage(appendPrefix(Component.text("You cannot make a player be their own friend!", NamedTextColor.RED)));
			} else {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("You cannot add yourself as a friend!", NamedTextColor.RED)));
				}
			}
			return;
		}

		// Check if the sender and receiver are already friends
		areFriends(senderUuid, receiverUuid).thenAccept(alreadyFriends -> {
			if (alreadyFriends) {
				if (moderator != null) {
					moderator.sendMessage(appendPrefix(Component.text(resolveName(senderUuid) + " is already friends with " + resolveName(receiverUuid) + "!", NamedTextColor.RED)));
				} else {
					if (sender != null) {
						sender.sendMessage(appendPrefix(Component.text("You are already friends with " + resolveName(receiverUuid) + "!", NamedTextColor.RED)));
					}
				}
				return;
			}

			// Check if either player has blocked the other
			CompletableFuture<Boolean> senderBlockedReceiver = hasPlayerBlocked(senderUuid, receiverUuid);
			CompletableFuture<Boolean> receiverBlockedSender = hasPlayerBlocked(receiverUuid, senderUuid);

			CompletableFuture.allOf(senderBlockedReceiver, receiverBlockedSender).thenRun(() -> {
				if (senderBlockedReceiver.join() || receiverBlockedSender.join()) {
					if (moderator != null) {
						moderator.sendMessage(appendPrefix(Component.text("You cannot make these players be friends because one or both of them has the other player blocked!", NamedTextColor.RED)));
					} else {
						if (sender != null) {
							sender.sendMessage(appendPrefix(Component.text("You cannot add " + resolveName(receiverUuid) + " as a friend!", NamedTextColor.RED)));
						}
					}
					return;
				}

				// Cancel all pending friend request expiration tasks if applicable
				BukkitTask expirationTaskOne = PENDING_FRIEND_REQUESTS.remove(senderUuid + "_" + receiverUuid);
				if (expirationTaskOne != null) {
					expirationTaskOne.cancel();
				}

				BukkitTask expirationTaskTwo = PENDING_FRIEND_REQUESTS.remove(receiverUuid + "_" + senderUuid);
				if (expirationTaskTwo != null) {
					expirationTaskTwo.cancel();
				}

				// Get the current timestamp
				String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

				// Set the sender and the receiver as friends with the current timestamp
				CompletableFuture<Boolean> futureSender = RedisAPI.getInstance().async().hset(getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, senderUuid), FRIEND_KEY + receiverUuid, receiverUuid + "|" + timestamp).toCompletableFuture();
				CompletableFuture<Boolean> futureReceiver = RedisAPI.getInstance().async().hset(getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, receiverUuid), FRIEND_KEY + senderUuid, senderUuid + "|" + timestamp).toCompletableFuture();

				CompletableFuture.allOf(futureSender, futureReceiver).thenRun(() -> {
					if (futureSender.join() && futureReceiver.join()) {
						// Clean up all pending friend requests if applicable
						RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, senderUuid), PENDING_FRIEND_REQUEST_KEY + receiverUuid);
						RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, receiverUuid), PENDING_FRIEND_REQUEST_KEY + senderUuid);

						// Notify the sender and the receiver via RabbitMQ
						Map<String, String> jsonProperties = Map.of(
							"senderUuid", String.valueOf(senderUuid),
							"receiverUuid", String.valueOf(receiverUuid)
						);
						broadcastNotification(FRIEND_REQUEST_ACCEPTED_NOTIFICATION_CHANNEL, jsonProperties, "Failed to notify other shards of an accepted friend request via RabbitMQ.");

						if (moderator != null) {
							UUID moderatorUuid = moderator.getUniqueId();

							// Notify the sender and the receiver via RabbitMQ if a moderator forcefully designated them as friends or
							// Notify the sender/receiver via RabbitMQ if a moderator forcefully added them as their friend
							Map<String, String> moderatorJsonProperties = Map.of(
								"moderatorUuid", String.valueOf(moderatorUuid),
								"senderUuid", String.valueOf(senderUuid),
								"receiverUuid", String.valueOf(receiverUuid)
							);
							broadcastNotification(MODERATOR_FORCE_ADDED_FRIENDS_NOTIFICATION_CHANNEL, moderatorJsonProperties, "Failed to notify other shards of a friendship formed by a moderator via RabbitMQ.");

							moderator.sendMessage(appendPrefix(Component.text(resolveName(senderUuid) + " and " + resolveName(receiverUuid) + " are now friends with each other.", NamedTextColor.GREEN)));
							AuditListener.log(AUDIT_LOG_PREFIX + moderator.getName() + " forcefully made " + resolveName(senderUuid) + " and " + resolveName(receiverUuid) + " be friends.");
						}
					}
				});
			});
		});
	}

	static void removeFriend(@Nullable Player moderator, UUID senderUuid, UUID receiverUuid) {
		// Check if the sender and receiver are friends
		areFriends(senderUuid, receiverUuid).thenAccept(areFriends -> {
			if (!areFriends) {
				if (moderator != null) {
					moderator.sendMessage(appendPrefix(Component.text(resolveName(senderUuid) + " is not friends with " + resolveName(receiverUuid) + "!", NamedTextColor.RED)));
				} else {
					Player sender = Bukkit.getPlayer(senderUuid);
					if (sender != null) {
						sender.sendMessage(appendPrefix(Component.text("You are not friends with " + resolveName(receiverUuid) + "!", NamedTextColor.RED)));
					}
				}
				return;
			}

			// Remove the sender and the receiver as friends
			CompletableFuture<Boolean> futureSender = RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, senderUuid), FRIEND_KEY + receiverUuid).thenApply((val) -> val == 1L).toCompletableFuture();
			CompletableFuture<Boolean> futureReceiver = RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, receiverUuid), FRIEND_KEY + senderUuid).thenApply((val) -> val == 1L).toCompletableFuture();

			CompletableFuture.allOf(futureSender, futureReceiver).thenRun(() -> {
				if (futureSender.join() && futureReceiver.join()) {
					// Notify the sender and receiver about friend removal via RabbitMQ
					Map<String, String> jsonProperties = Map.of(
						"senderUuid", String.valueOf(senderUuid),
						"receiverUuid", String.valueOf(receiverUuid)
					);
					broadcastNotification(REMOVED_FRIEND_NOTIFICATION_CHANNEL, jsonProperties, "Failed to notify other shards of a friend being removed via RabbitMQ.");

					if (moderator != null) {
						UUID moderatorUuid = moderator.getUniqueId();

						// Notify the sender and the receiver via RabbitMQ if a moderator forcefully removed them as friends
						if (!moderatorUuid.equals(senderUuid) && !moderatorUuid.equals(receiverUuid)) {
							Map<String, String> moderatorJsonProperties = Map.of(
								"moderatorUuid", String.valueOf(moderatorUuid),
								"senderUuid", String.valueOf(senderUuid),
								"receiverUuid", String.valueOf(receiverUuid)
							);
							broadcastNotification(MODERATOR_FORCE_REMOVED_FRIENDS_NOTIFICATION_CHANNEL, moderatorJsonProperties, "Failed to notify other shards of a friendship ended by a moderator via RabbitMQ.");

							moderator.sendMessage(appendPrefix(Component.text(resolveName(senderUuid) + " and " + resolveName(receiverUuid) + " are no longer friends with each other.", NamedTextColor.GREEN)));
							AuditListener.log(AUDIT_LOG_PREFIX + moderator.getName() + " forcefully removed " + resolveName(senderUuid) + " and " + resolveName(receiverUuid) + " as friends.");
						}
					}
				}
			});
		});
	}
	//endregion

	//region <BLOCKING>
	static void blockPlayer(UUID senderUuid, UUID receiverUuid) {
		Player sender = Bukkit.getPlayer(senderUuid);

		// Prevent players from blocking themselves
		if (senderUuid.equals(receiverUuid)) {
			if (sender != null) {
				sender.sendMessage(appendPrefix(Component.text("You cannot block yourself!", NamedTextColor.RED)));
			}
			return;
		}

		// Check if the sender already has the receiver blocked
		hasPlayerBlocked(senderUuid, receiverUuid).thenAccept(alreadyBlocked -> {
			if (alreadyBlocked) {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("You already blocked " + resolveName(receiverUuid) + "!", NamedTextColor.RED)));
				}
				return;
			}

			// Check if the receiver has block exemption permission
			LuckPermsIntegration.loadUser(receiverUuid).thenAcceptAsync(receiver -> {
				if (receiver.getCachedData().getPermissionData().checkPermission(BLOCK_EXEMPTION_PERMISSION).asBoolean()) {
					if (sender != null) {
						sender.sendMessage(appendPrefix(Component.text("You cannot block " + resolveName(receiverUuid) + "!", NamedTextColor.RED)));
					}
					return;
				}

				// Get the current timestamp
				String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

				// Block the receiver and store the current timestamp
				RedisAPI.getInstance().async().hset(getRedisPath(REDIS_SOCIAL_TYPE_BLOCKED, senderUuid), BLOCKED_KEY + receiverUuid, receiverUuid + "|" + timestamp).thenRun(() -> {
					if (sender != null) {
						sender.sendMessage(appendPrefix(Component.text("You have blocked " + resolveName(receiverUuid) + ".", NamedTextColor.GREEN)));
					}

					// Cancel all pending friend request expiration tasks if applicable
					BukkitTask expirationTaskOne = PENDING_FRIEND_REQUESTS.remove(senderUuid + "_" + receiverUuid);
					if (expirationTaskOne != null) {
						expirationTaskOne.cancel();
					}

					BukkitTask expirationTaskTwo = PENDING_FRIEND_REQUESTS.remove(receiverUuid + "_" + senderUuid);
					if (expirationTaskTwo != null) {
						expirationTaskTwo.cancel();
					}

					// Remove all pending friend requests if applicable
					RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, senderUuid), PENDING_FRIEND_REQUEST_KEY + receiverUuid);
					RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, receiverUuid), PENDING_FRIEND_REQUEST_KEY + senderUuid);

					// Check if the sender and the receiver are friends
					areFriends(senderUuid, receiverUuid).thenAccept(areFriends -> {
						if (areFriends) {
							// Remove the receiver as a friend
							removeFriend(null, senderUuid, receiverUuid);
						}
					});
				});
			});
		});
	}

	static void unblockPlayer(UUID senderUuid, UUID receiverUuid) {
		Player sender = Bukkit.getPlayer(senderUuid);

		// Check if the receiver is blocked
		hasPlayerBlocked(senderUuid, receiverUuid).thenAccept(isBlocked -> {
			if (!isBlocked) {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("You do not have " + resolveName(receiverUuid) + " blocked!", NamedTextColor.RED)));
				}
				return;
			}

			// Remove the block on the receiver
			RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_BLOCKED, senderUuid), BLOCKED_KEY + receiverUuid).thenRun(() -> {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("You have unblocked " + resolveName(receiverUuid) + ".", NamedTextColor.GREEN)));
				}
			});
		});
	}
	//endregion

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onPlayerLogin(PlayerLoginEvent event) {
		UUID playerUuid = event.getPlayer().getUniqueId();
		long currentTime = System.currentTimeMillis() / 1000;

		// Check for expired friend requests when the player joins in case a shard went down before the corresponding expiration tasks could run
		RedisAPI.getInstance().async().hgetall(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, playerUuid)).thenAccept(data -> {
			data.forEach((key, value) -> {
				long storedTime = Long.parseLong(value);
				if (currentTime >= storedTime) {
					// Remove expired friend requests
					RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, playerUuid), key);
				}
			});
		});

		// Check the player's block list to see if any blocked players now have the block exemption permission
		RedisAPI.getInstance().async().hgetall(getRedisPath(REDIS_SOCIAL_TYPE_BLOCKED, playerUuid)).thenAccept(data -> {
			data.forEach((key, value) -> {
				// Split the "UUID|timestamp" string
				String[] parts = value.split("\\|");

				// Extract the UUID
				UUID blockedPlayerUuid = UUID.fromString(parts[0]);

				LuckPermsIntegration.loadUser(blockedPlayerUuid).thenAcceptAsync(blockedUser -> {
					if (blockedUser.getCachedData().getPermissionData().checkPermission(BLOCK_EXEMPTION_PERMISSION).asBoolean()) {
						// Automatically unblock the exempted player
						unblockPlayer(playerUuid, blockedPlayerUuid);
					}
				});
			});
		});

		if (getSocialData(playerUuid) == null) {
			SOCIAL_DATA_MAP.put(playerUuid, new PlayerSocialData(playerUuid));
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onPlayerLogout(PlayerQuitEvent event) {
		UUID playerUuid = event.getPlayer().getUniqueId();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (Bukkit.getPlayer(playerUuid) == null) {
				SOCIAL_DATA_MAP.remove(playerUuid);
			}
		}, 15 * 20L);
	}

	//region <RABBITMQ>
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void networkRelayMessageEvent(@NotNull NetworkRelayMessageEvent event) {
		JsonObject data = event.getData();
		switch (event.getChannel()) {
			case INCOMING_FRIEND_REQUEST_NOTIFICATION_CHANNEL -> handleIncomingFriendRequest(data);
			case FRIEND_REQUEST_EXPIRATION_NOTIFICATION_CHANNEL -> handleExpiredFriendRequest(data);
			case FRIEND_REQUEST_ACCEPTED_NOTIFICATION_CHANNEL -> handleAcceptedFriendRequest(data);
			case MODERATOR_FORCE_ADDED_FRIENDS_NOTIFICATION_CHANNEL -> handleModeratorForceAddedFriendship(data);
			case REMOVED_FRIEND_NOTIFICATION_CHANNEL -> handleRemovedFriend(data);
			case MODERATOR_FORCE_REMOVED_FRIENDS_NOTIFICATION_CHANNEL -> handleModeratorForceRemovedFriendship(data);
			default -> {
				// Do nothing
			}
		}
	}

	private void handleIncomingFriendRequest(JsonObject data) {
		if (data.has("senderUuid") && data.has("receiverUuid")) {
			UUID senderUuid = UUID.fromString(data.get("senderUuid").getAsString());
			UUID receiverUuid = UUID.fromString(data.get("receiverUuid").getAsString());

			Player receiver = Bukkit.getPlayer(receiverUuid);

			// Notify the receiver about the pending request
			if (receiver != null) {
				Component message = Component.text("Friend request from ", NamedTextColor.YELLOW);
				Component playerComponent = RemotePlayerListener.getPlayerComponent(senderUuid).clickEvent(null);

				Component acceptButton = Component.text("[ACCEPT]", NamedTextColor.GREEN, TextDecoration.BOLD)
					.hoverEvent(HoverEvent.showText(Component.text("Click here to accept " + resolveName(senderUuid) + "'s request!", NamedTextColor.GREEN)))
					.clickEvent(ClickEvent.runCommand("/friend add " + resolveName(senderUuid)));

				receiver.sendMessage(appendHeaderAndFooter(
					message.append(playerComponent).append(Component.newline())
						.append(acceptButton)
				));
			}
		}
	}

	private void handleExpiredFriendRequest(JsonObject data) {
		if (data.has("senderUuid") && data.has("receiverUuid")) {
			UUID senderUuid = UUID.fromString(data.get("senderUuid").getAsString());
			UUID receiverUuid = UUID.fromString(data.get("receiverUuid").getAsString());

			Player sender = Bukkit.getPlayer(senderUuid);
			Player receiver = Bukkit.getPlayer(receiverUuid);

			// Notify the sender and the receiver about the request expiration
			if (sender != null) {
				sender.sendMessage(appendPrefix(Component.text("Your friend request to " + resolveName(receiverUuid) + " has expired.", NamedTextColor.RED)));
			}

			if (receiver != null) {
				receiver.sendMessage(appendPrefix(Component.text("The friend request from " + resolveName(senderUuid) + " has expired.", NamedTextColor.RED)));
			}
		}
	}

	private void handleAcceptedFriendRequest(JsonObject data) {
		if (data.has("senderUuid") && data.has("receiverUuid")) {
			UUID senderUuid = UUID.fromString(data.get("senderUuid").getAsString());
			UUID receiverUuid = UUID.fromString(data.get("receiverUuid").getAsString());

			Player sender = Bukkit.getPlayer(senderUuid);
			Player receiver = Bukkit.getPlayer(receiverUuid);

			// Notify the sender and the receiver about the accepted request
			if (sender != null) {
				sender.sendMessage(appendPrefix(Component.text("You are now friends with " + resolveName(receiverUuid) + ".", NamedTextColor.GREEN)));
			}

			if (receiver != null) {
				receiver.sendMessage(appendPrefix(Component.text("You are now friends with " + resolveName(senderUuid) + ".", NamedTextColor.GREEN)));
			}

			PlayerSocialData senderData = getSocialData(senderUuid);
			PlayerSocialData receiverData = getSocialData(receiverUuid);
			if (senderData != null) {
				senderData.addFriend(receiverUuid);
			}
			if (receiverData != null) {
				receiverData.addFriend(senderUuid);
			}
		}
	}

	private void handleModeratorForceAddedFriendship(JsonObject data) {
		if (data.has("moderatorUuid") && data.has("senderUuid") && data.has("receiverUuid")) {
			UUID moderatorUuid = UUID.fromString(data.get("moderatorUuid").getAsString());
			UUID senderUuid = UUID.fromString(data.get("senderUuid").getAsString());
			UUID receiverUuid = UUID.fromString(data.get("receiverUuid").getAsString());

			Player sender = Bukkit.getPlayer(senderUuid);
			Player receiver = Bukkit.getPlayer(receiverUuid);

			if (!moderatorUuid.equals(senderUuid) && !moderatorUuid.equals(receiverUuid)) {
				// Notify the sender and the receiver if a moderator forcefully designated them as friends
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("A moderator forcefully made you and " + resolveName(receiverUuid) + " be friends with each other.", NamedTextColor.GREEN)));
				}

				if (receiver != null) {
					receiver.sendMessage(appendPrefix(Component.text("A moderator forcefully made you and " + resolveName(senderUuid) + " be friends with each other.", NamedTextColor.GREEN)));
				}
			} else if (moderatorUuid.equals(senderUuid) && receiver != null) {
				// Notify the receiver if a moderator forcefully added them as their friend
				receiver.sendMessage(appendPrefix(Component.text("A moderator forcefully made you and " + resolveName(senderUuid) + " be friends with each other.", NamedTextColor.GREEN)));
			} else if (moderatorUuid.equals(receiverUuid) && sender != null) {
				// Notify the sender if a moderator forcefully added them as their friend
				sender.sendMessage(appendPrefix(Component.text("A moderator forcefully made you and " + resolveName(receiverUuid) + " be friends with each other.", NamedTextColor.GREEN)));
			}

			PlayerSocialData senderData = getSocialData(senderUuid);
			PlayerSocialData receiverData = getSocialData(receiverUuid);
			if (senderData != null) {
				senderData.addFriend(receiverUuid);
			}
			if (receiverData != null) {
				receiverData.addFriend(senderUuid);
			}
		}
	}

	private void handleRemovedFriend(JsonObject data) {
		if (data.has("senderUuid") && data.has("receiverUuid")) {
			UUID senderUuid = UUID.fromString(data.get("senderUuid").getAsString());
			UUID receiverUuid = UUID.fromString(data.get("receiverUuid").getAsString());

			Player sender = Bukkit.getPlayer(senderUuid);
			Player receiver = Bukkit.getPlayer(receiverUuid);

			// Notify the sender and the receiver about friend removal
			if (sender != null) {
				sender.sendMessage(appendPrefix(Component.text("You are no longer friends with " + resolveName(receiverUuid) + "!", NamedTextColor.GREEN)));
			}

			if (receiver != null) {
				receiver.sendMessage(appendPrefix(Component.text(resolveName(senderUuid) + " removed you as a friend!", NamedTextColor.RED)));
			}

			PlayerSocialData senderData = getSocialData(senderUuid);
			PlayerSocialData receiverData = getSocialData(receiverUuid);
			if (senderData != null) {
				senderData.removeFriend(receiverUuid);
			}
			if (receiverData != null) {
				receiverData.removeFriend(senderUuid);
			}
		}
	}

	private void handleModeratorForceRemovedFriendship(JsonObject data) {
		if (data.has("senderUuid") && data.has("receiverUuid")) {
			UUID senderUuid = UUID.fromString(data.get("senderUuid").getAsString());
			UUID receiverUuid = UUID.fromString(data.get("receiverUuid").getAsString());

			Player sender = Bukkit.getPlayer(senderUuid);
			Player receiver = Bukkit.getPlayer(receiverUuid);

			// Notify the sender and the receiver if a moderator forcefully removed them as friends
			if (sender != null) {
				sender.sendMessage(appendPrefix(Component.text("A moderator forcefully removed you and " + resolveName(receiverUuid) + " as friends.", NamedTextColor.RED)));
			}

			if (receiver != null) {
				receiver.sendMessage(appendPrefix(Component.text("A moderator forcefully removed you and " + resolveName(senderUuid) + " as friends.", NamedTextColor.RED)));
			}

			PlayerSocialData senderData = getSocialData(senderUuid);
			PlayerSocialData receiverData = getSocialData(receiverUuid);
			if (senderData != null) {
				senderData.removeFriend(receiverUuid);
			}
			if (receiverData != null) {
				receiverData.removeFriend(senderUuid);
			}
		}
	}
	//endregion

	//region <GETTERS>
	static CompletableFuture<SocialInfo> getSocialInfo(UUID playerUuid) {
		CompletableFuture<SocialInfo> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				// Fetch friends and blocked players separately
				CompletableFuture<Map<String, String>> friendDataFuture = RedisAPI.getInstance().async().hgetall(getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, playerUuid)).toCompletableFuture();
				CompletableFuture<Map<String, String>> blockedDataFuture = RedisAPI.getInstance().async().hgetall(getRedisPath(REDIS_SOCIAL_TYPE_BLOCKED, playerUuid)).toCompletableFuture();

				// Combine both futures
				CompletableFuture.allOf(friendDataFuture, blockedDataFuture).thenRun(() -> {
					try {
						Map<String, String> friendData = friendDataFuture.get();
						Map<String, String> blockedData = blockedDataFuture.get();

						Map<UUID, SocialInfo.Friend> friendMap = new HashMap<>();
						Map<UUID, SocialInfo.BlockedPlayer> blockedPlayerMap = new HashMap<>();

						// Process friend data
						friendData.forEach((key, value) -> {
							// String format: UUID|timestamp
							// Split the string format
							String[] parts = value.split("\\|");

							UUID friendUuid = UUID.fromString(parts[0]);
							String friendTimestamp;
							if (parts.length >= 2) {
								friendTimestamp = parts[1];
							} else {
								friendTimestamp = DateTimeFormatter.ISO_INSTANT.format(
									DateUtils.localDateTime(2025, 3, 6)
										.toInstant(ZoneOffset.UTC));
								RedisAPI.getInstance().async().hset(
									getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, playerUuid),
									FRIEND_KEY + friendUuid,
									parts[0] + "|" + friendTimestamp
								);
							}

							friendMap.put(friendUuid, new SocialInfo.Friend(friendUuid, friendTimestamp));
						});

						// Process blocked data
						blockedData.forEach((key, value) -> {
							// String format: UUID|timestamp
							// Split the string format
							String[] parts = value.split("\\|");

							UUID blockedUuid = UUID.fromString(parts[0]);
							String blockedTimestamp = parts[1];

							blockedPlayerMap.put(blockedUuid, new SocialInfo.BlockedPlayer(blockedUuid, blockedTimestamp));
						});

						SocialInfo socialInfo = new SocialInfo(playerUuid, friendMap, blockedPlayerMap);

						// Complete the future on the main thread for easy use of .whenCompleted()
						Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> future.complete(socialInfo));
					} catch (Exception exception) {
						Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> future.completeExceptionally(exception));
					}
				});
			} catch (Exception exception) {
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> future.completeExceptionally(exception));
			}
		});

		return future;
	}

	public static CompletableFuture<List<UUID>> getFriends(UUID playerUuid) {
		return RedisAPI.getInstance().async().hgetall(getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, playerUuid)).toCompletableFuture().thenApply(data -> {
			if (data.isEmpty()) {
				return Collections.emptyList();
			}

			return data.values().stream()
				.map(value -> {
					// Split the "UUID|timestamp" string
					String[] parts = value.split("\\|");

					// Extract the UUID
					return UUID.fromString(parts[0]);
				})
				.toList();
		});
	}

	public static CompletableFuture<Boolean> areFriends(UUID playerOne, UUID playerTwo) {
		CompletableFuture<Boolean> checkOne = RedisAPI.getInstance().async().hget(getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, playerOne), FRIEND_KEY + playerTwo).toCompletableFuture().thenApply(Objects::nonNull);
		CompletableFuture<Boolean> checkTwo = RedisAPI.getInstance().async().hget(getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, playerTwo), FRIEND_KEY + playerOne).toCompletableFuture().thenApply(Objects::nonNull);
		return checkOne.thenCombine(checkTwo, (one, two) -> one && two);
	}

	public static CompletableFuture<List<UUID>> getBlockedPlayers(UUID playerUuid) {
		return RedisAPI.getInstance().async().hgetall(getRedisPath(REDIS_SOCIAL_TYPE_BLOCKED, playerUuid)).toCompletableFuture().thenApply(data -> {
			if (data.isEmpty()) {
				return Collections.emptyList();
			}

			return data.values().stream()
				.map(value -> {
					// Split the "UUID|timestamp" string
					String[] parts = value.split("\\|");

					// Extract the UUID
					return UUID.fromString(parts[0]);
				})
				.toList();
		});
	}

	public static CompletableFuture<Boolean> hasPlayerBlocked(UUID player, UUID blockedPlayer) {
		return RedisAPI.getInstance().async().hget(getRedisPath(REDIS_SOCIAL_TYPE_BLOCKED, player), BLOCKED_KEY + blockedPlayer).toCompletableFuture().thenApply(Objects::nonNull);
	}

	@Nullable
	public static PlayerSocialData getSocialData(UUID playerUuid) {
		return SOCIAL_DATA_MAP.get(playerUuid);
	}
	//endregion
}
