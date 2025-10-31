package com.playmonumenta.plugins.social;

import com.google.gson.JsonObject;
import com.playmonumenta.networkchat.RemotePlayerListener;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.NetworkRelayMessageEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.redissync.RedisAPI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SocialManager implements Listener {
	//region <DECLARATIONS>
	// General
	static final String LOG_PREFIX = "[Social Manager] ";
	private static final Map<UUID, PlayerSocialCache> SOCIAL_CACHE_MAP = new ConcurrentHashMap<>();

	// Friends
	static final String REDIS_SOCIAL_TYPE_FRIEND = "friends";
	static final String REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST = "pendingFriendRequests";
	private static final long FRIEND_REQUEST_EXPIRATION_TIME = 600; // 10 minutes
	private static final Map<String, BukkitTask> PENDING_FRIEND_REQUESTS_MAP = new ConcurrentHashMap<>();
	private static final Map<String, CompletableFuture<Void>> PENDING_FRIEND_REMOVALS_MAP = new ConcurrentHashMap<>();

	// Blocking
	static final String REDIS_SOCIAL_TYPE_BLOCKED = "blocked";
	private static final String BLOCK_EXEMPTION_PERMISSION = "monumenta.social.blockexemption";
	private static final Map<String, CompletableFuture<Void>> PENDING_UNBLOCKS_MAP = new ConcurrentHashMap<>();

	// RabbitMQ channels
	private static final String INCOMING_FRIEND_REQUEST_CHANNEL = "incomingFriendRequestChannel";
	private static final String FRIEND_REQUEST_EXPIRATION_CHANNEL = "friendRequestExpirationChannel";
	private static final String ADDED_FRIEND_CHANNEL = "addedFriendChannel";
	private static final String MODERATOR_FORCE_ADDED_FRIENDS_CHANNEL = "moderatorForceAddedFriendsChannel";
	private static final String REMOVED_FRIEND_CHANNEL = "removedFriendChannel";
	private static final String MODERATOR_FORCE_REMOVED_FRIENDS_CHANNEL = "moderatorForceRemovedFriendsChannel";
	private static final String BLOCKED_PLAYER_CHANNEL = "blockedPlayerChannel";
	private static final String UNBLOCKED_PLAYER_CHANNEL = "unblockedPlayerChannel";
	//endregion

	//region <FRIENDS>
	static void sendFriendRequest(UUID senderUuid, UUID receiverUuid) {
		Player sender = Bukkit.getPlayer(senderUuid);

		// Load the sender's and the receiver's social cache from memory or from Redis
		loadSocialCaches(senderUuid, receiverUuid).thenAccept(socialCaches -> {
			if (socialCaches == null) {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("An error occurred while trying to load player data. Please report this to server operators if this keep happening.", NamedTextColor.RED)));
				}

				MMLog.severe(LOG_PREFIX + "Failed to load social caches for " + senderUuid + " and/or " + receiverUuid + ". Is Redis down?");
				return;
			}

			// Fetch the sender's and the receiver's social cache
			PlayerSocialCache senderCache = socialCaches.get(senderUuid);
			PlayerSocialCache receiverCache = socialCaches.get(receiverUuid);

			// This shouldn't occur if loadSocialCaches didn't return null
			if (senderCache == null || receiverCache == null) {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("An error occurred while trying to load player data. Please report this to server operators if this keep happening.", NamedTextColor.RED)));
				}

				MMLog.severe(LOG_PREFIX + "Failed to load social caches for " + senderUuid + " and/or " + receiverUuid + ". Is Redis down?");
				return;
			}

			// Perform checks to see if the sender can be friends with the receiver
			PlayerSocialCache.FriendshipCheckResult result = senderCache.canBecomeFriendsWith(receiverCache);

			if (result != PlayerSocialCache.FriendshipCheckResult.OK) {
				Component errorMessage = switch (result) {
					case SELF -> Component.text("You cannot add yourself as a friend!", NamedTextColor.RED);
					case ALREADY_FRIENDS ->
						Component.text("You are already friends with " + resolveName(receiverUuid) + "!", NamedTextColor.RED);
					default ->
						Component.text("You cannot add " + resolveName(receiverUuid) + " as a friend!", NamedTextColor.RED);
				};

				if (sender != null) {
					sender.sendMessage(appendPrefix(errorMessage));
				}

				return;
			}

			// Check if the sender already has a pending friend request from the receiver
			if (senderCache.getPendingFriendRequests().containsKey(receiverUuid)) {
				// Automatically accept the request if there is one pending
				addFriend(null, senderUuid, receiverUuid);
				return;
			}

			// Check if the receiver already has a pending friend request from the sender
			if (receiverCache.getPendingFriendRequests().containsKey(senderUuid)) {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("You have already sent a friend request to " + resolveName(receiverUuid) + "!", NamedTextColor.YELLOW)));
				}
				return;
			}

			// Calculate the expiration time in seconds from now
			long expirationTime = System.currentTimeMillis() / 1000 + FRIEND_REQUEST_EXPIRATION_TIME;

			// Send the pending friend request to the receiver in Redis
			RedisAPI.getInstance().async().hset(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, receiverUuid), senderUuid.toString(), String.valueOf(expirationTime)).thenAccept(success -> {
				if (!success) {
					if (sender != null) {
						sender.sendMessage(appendPrefix(Component.text("An error occurred while sending your friend request. Please report this as a bug if this keeps happening.", NamedTextColor.RED)));
					}
					return;
				}

				// Notify the sender about the pending friend request
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("You have sent a friend request to " + resolveName(receiverUuid) + "! They have " + FRIEND_REQUEST_EXPIRATION_TIME / 60L + " minutes to accept before it expires.", NamedTextColor.YELLOW)));
				}

				// Broadcast to all shards via RabbitMQ to
				// add the pending friend request in memory,
				// to notify the receiver about the request,
				// and to schedule the friend request expiration task
				Map<String, String> jsonProperties = Map.of(
					"senderUuid", senderUuid.toString(),
					"receiverUuid", receiverUuid.toString(),
					"expirationTime", String.valueOf(expirationTime)
				);
				broadcastNotification(INCOMING_FRIEND_REQUEST_CHANNEL, jsonProperties, "Failed to notify other shards of an incoming friend request via RabbitMQ.");
			});
		});
	}

	private static void expireFriendRequest(UUID senderUuid, UUID receiverUuid) {
		RedisAPI.getInstance().async().hget(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, receiverUuid), senderUuid.toString()).thenAccept(storedTime -> {
			if (storedTime != null && Long.parseLong(storedTime) <= System.currentTimeMillis() / 1000) {
				// Remove the pending friend request from the receiver in Redis
				RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, receiverUuid), senderUuid.toString());

				// Broadcast to all shards via RabbitMQ to
				// discard the friend request expiration task
				// and notify the sender and the receiver about the friend request expiration
				Map<String, String> jsonProperties = Map.of(
					"senderUuid", senderUuid.toString(),
					"receiverUuid", receiverUuid.toString()
				);
				broadcastNotification(FRIEND_REQUEST_EXPIRATION_CHANNEL, jsonProperties, "Failed to notify other shards of an expired friend request via RabbitMQ.");
			}
		});
	}

	static void addFriend(@Nullable Player moderator, UUID senderUuid, UUID receiverUuid) {
		Player sender = Bukkit.getPlayer(senderUuid);

		// Load the sender's and the receiver's social cache from memory or from Redis
		loadSocialCaches(senderUuid, receiverUuid).thenAccept(socialCaches -> {
			if (socialCaches == null) {
				Component errorMessage = Component.text("An error occurred while trying to load player data. Please report this to server operators if this keep happening.", NamedTextColor.RED);

				if (moderator != null) {
					moderator.sendMessage(appendPrefix(errorMessage));
				} else if (sender != null) {
					sender.sendMessage(appendPrefix(errorMessage));
				}

				MMLog.severe(LOG_PREFIX + "Failed to load social caches for " + senderUuid + " and/or " + receiverUuid + ". Is Redis down?");
				return;
			}

			// Fetch the sender's and the receiver's social cache
			PlayerSocialCache senderCache = socialCaches.get(senderUuid);
			PlayerSocialCache receiverCache = socialCaches.get(receiverUuid);

			// This shouldn't occur if loadSocialCaches didn't return null
			if (senderCache == null || receiverCache == null) {
				Component errorMessage = Component.text("An error occurred while trying to load player data. Please report this to server operators if this keep happening.", NamedTextColor.RED);

				if (moderator != null) {
					moderator.sendMessage(appendPrefix(errorMessage));
				} else if (sender != null) {
					sender.sendMessage(appendPrefix(errorMessage));
				}

				MMLog.severe(LOG_PREFIX + "Failed to load social caches for " + senderUuid + " and/or " + receiverUuid + ". Is Redis down?");
				return;
			}

			// Perform checks to see if the sender can be friends with the receiver
			PlayerSocialCache.FriendshipCheckResult result = senderCache.canBecomeFriendsWith(receiverCache);

			if (result != PlayerSocialCache.FriendshipCheckResult.OK) {
				Component errorMessage = switch (result) {
					case SELF -> moderator != null
						? Component.text("You cannot make a player be their own friend!", NamedTextColor.RED)
						: Component.text("You cannot add yourself as a friend!", NamedTextColor.RED);

					case ALREADY_FRIENDS -> moderator != null
						? Component.text(resolveName(senderUuid) + " is already friends with " + resolveName(receiverUuid) + "!", NamedTextColor.RED)
						: Component.text("You are already friends with " + resolveName(receiverUuid) + "!", NamedTextColor.RED);

					case BLOCKED -> moderator != null
						? Component.text("You cannot make these players be friends because one or both has the other blocked!", NamedTextColor.RED)
						: Component.text("You cannot add " + resolveName(receiverUuid) + " as a friend!", NamedTextColor.RED);

					default -> moderator != null
						? Component.text("You cannot add " + resolveName(receiverUuid) + " as " + resolveName(senderUuid) + "'s friend!", NamedTextColor.RED)
						: Component.text("You cannot add " + resolveName(receiverUuid) + " as a friend!", NamedTextColor.RED);
				};

				if (moderator != null) {
					moderator.sendMessage(appendPrefix(errorMessage));
				} else if (sender != null) {
					sender.sendMessage(appendPrefix(errorMessage));
				}

				return;
			}

			// Fetch the current timestamp
			String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

			// Set the sender and the receiver as friends in Redis
			CompletableFuture<Boolean> senderRedisFuture = RedisAPI.getInstance().async().hset(getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, senderUuid), receiverUuid.toString(), timestamp).toCompletableFuture();
			CompletableFuture<Boolean> receiverRedisFuture = RedisAPI.getInstance().async().hset(getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, receiverUuid), senderUuid.toString(), timestamp).toCompletableFuture();

			CompletableFuture.allOf(senderRedisFuture, receiverRedisFuture).thenRun(() -> {
				if (senderRedisFuture.join() && receiverRedisFuture.join()) {
					// Clean up the pending friend requests in Redis
					RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, senderUuid), receiverUuid.toString());
					RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, receiverUuid), senderUuid.toString());

					// Broadcast to all shards via RabbitMQ to
					// set the sender and the receiver as friends in memory,
					// clean up the pending friend requests in memory,
					// cancel the friend request expiration task,
					// and to notify them
					Map<String, String> jsonProperties = Map.of(
						"senderUuid", senderUuid.toString(),
						"receiverUuid", receiverUuid.toString(),
						"timestamp", timestamp
					);
					broadcastNotification(ADDED_FRIEND_CHANNEL, jsonProperties, "Failed to notify other shards of a new friendship via RabbitMQ.");

					if (moderator != null) {
						UUID moderatorUuid = moderator.getUniqueId();

						// Notify the sender and the receiver via RabbitMQ if a moderator forcefully designated them as friends or
						// notify the sender/receiver via RabbitMQ if a moderator forcefully added them as their friend
						Map<String, String> moderatorJsonProperties = Map.of(
							"moderatorUuid", moderatorUuid.toString(),
							"senderUuid", senderUuid.toString(),
							"receiverUuid", receiverUuid.toString(),
							"timestamp", timestamp
						);
						broadcastNotification(MODERATOR_FORCE_ADDED_FRIENDS_CHANNEL, moderatorJsonProperties, "Failed to notify other shards of a new friendship formed by a moderator via RabbitMQ.");

						moderator.sendMessage(appendPrefix(Component.text(resolveName(senderUuid) + " and " + resolveName(receiverUuid) + " are now friends.", NamedTextColor.GREEN)));
						AuditListener.log(LOG_PREFIX + moderator.getName() + " forcefully made " + resolveName(senderUuid) + " and " + resolveName(receiverUuid) + " be friends.");
					}
				}
			});
		});
	}

	static CompletableFuture<Void> removeFriend(@Nullable Player moderator, UUID senderUuid, UUID receiverUuid) {
		Player sender = Bukkit.getPlayer(senderUuid);

		// Load the sender's social cache from memory or from Redis
		return loadSocialCaches(senderUuid).thenCompose(socialCaches -> {
			if (socialCaches == null) {
				Component errorMessage = Component.text("An error occurred while trying to load player data. Please report this to server operators if this keeps happening.", NamedTextColor.RED);

				if (moderator != null) {
					moderator.sendMessage(appendPrefix(errorMessage));
				} else if (sender != null) {
					sender.sendMessage(appendPrefix(errorMessage));
				}

				MMLog.severe(LOG_PREFIX + "Failed to load social cache for " + senderUuid + ". Is Redis down?");
				return CompletableFuture.failedFuture(new IllegalStateException(LOG_PREFIX + "Failed to load social cache for " + senderUuid + ". Is Redis down?"));
			}

			// Fetch the sender's social cache
			PlayerSocialCache senderCache = socialCaches.get(senderUuid);

			// This shouldn't occur if loadSocialCaches didn't return null
			if (senderCache == null) {
				Component errorMessage = Component.text("An error occurred while trying to load player data. Please report this to server operators if this keeps happening.", NamedTextColor.RED);

				if (moderator != null) {
					moderator.sendMessage(appendPrefix(errorMessage));
				} else if (sender != null) {
					sender.sendMessage(appendPrefix(errorMessage));
				}

				MMLog.severe(LOG_PREFIX + "Failed to load social cache for " + senderUuid + ". Is Redis down?");
				return CompletableFuture.failedFuture(new IllegalStateException(LOG_PREFIX + "Failed to load social cache for " + senderUuid + ". Is Redis down?"));
			}

			// Check if the sender and the receiver are friends
			if (!senderCache.isFriendsWith(receiverUuid)) {
				if (moderator != null) {
					moderator.sendMessage(appendPrefix(Component.text(resolveName(senderUuid) + " is not friends with " + resolveName(receiverUuid) + "!", NamedTextColor.RED)));
				} else if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("You are not friends with " + resolveName(receiverUuid) + "!", NamedTextColor.RED)));
				}

				return CompletableFuture.completedFuture(null);
			}

			// Remove the sender and the receiver as friends in Redis
			CompletableFuture<Boolean> senderRedisFuture = RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, senderUuid), receiverUuid.toString()).thenApply(val -> val == 1L).toCompletableFuture();
			CompletableFuture<Boolean> receiverRedisFuture = RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_FRIEND, receiverUuid), senderUuid.toString()).thenApply(val -> val == 1L).toCompletableFuture();

			// Create a pending future for the friend removal operation to be completed by RabbitMQ confirmation
			String friendKey = getSocialPairKey(senderUuid, receiverUuid);
			CompletableFuture<Void> syncFuture = new CompletableFuture<>();
			PENDING_FRIEND_REMOVALS_MAP.put(friendKey, syncFuture);

			return CompletableFuture.allOf(senderRedisFuture, receiverRedisFuture).thenRun(() -> {
				if (senderRedisFuture.join() && receiverRedisFuture.join()) {
					if (moderator != null) {
						UUID moderatorUuid = moderator.getUniqueId();

						// Broadcast to all shards via RabbitMQ to
						// remove the sender and the receiver as friends in memory and
						// to notify them if a moderator forcefully removed them as friends
						if (!moderatorUuid.equals(senderUuid) && !moderatorUuid.equals(receiverUuid)) {
							Map<String, String> moderatorJsonProperties = Map.of(
								"moderatorUuid", moderatorUuid.toString(),
								"senderUuid", senderUuid.toString(),
								"receiverUuid", receiverUuid.toString()
							);
							broadcastNotification(MODERATOR_FORCE_REMOVED_FRIENDS_CHANNEL, moderatorJsonProperties, "Failed to notify other shards of a friendship ended by a moderator via RabbitMQ.");

							moderator.sendMessage(appendPrefix(Component.text(resolveName(senderUuid) + " and " + resolveName(receiverUuid) + " are no longer friends with each other.", NamedTextColor.GREEN)));
							AuditListener.log(LOG_PREFIX + moderator.getName() + " forcefully removed " + resolveName(senderUuid) + " and " + resolveName(receiverUuid) + " as friends.");
						}
					} else {
						// Broadcast to all shards via RabbitMQ to
						// remove the sender and the receiver as friends in memory
						// and to notify them
						Map<String, String> jsonProperties = Map.of(
							"senderUuid", senderUuid.toString(),
							"receiverUuid", receiverUuid.toString()
						);
						broadcastNotification(REMOVED_FRIEND_CHANNEL, jsonProperties, "Failed to notify other shards of a friend being removed via RabbitMQ.");
					}
				}
			}).thenCompose(v -> syncFuture);
		});
	}
	//endregion

	//region <BLOCKING>
	static void blockPlayer(UUID senderUuid, UUID receiverUuid) {
		Player sender = Bukkit.getPlayer(senderUuid);

		// Load the sender's and the receiver's social cache from memory or from Redis
		loadSocialCaches(senderUuid, receiverUuid).thenAccept(socialCaches -> {
			if (socialCaches == null) {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("An error occurred while trying to load player data. Please report this to server operators if this keeps happening.", NamedTextColor.RED)));
				}

				MMLog.severe(LOG_PREFIX + "Failed to load social caches for " + senderUuid + " and/or " + receiverUuid + ". Is Redis down?");
				return;
			}

			PlayerSocialCache senderCache = socialCaches.get(senderUuid);
			PlayerSocialCache receiverCache = socialCaches.get(receiverUuid);

			// This shouldn't occur if loadSocialCaches didn't return null
			if (senderCache == null || receiverCache == null) {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("An error occurred while trying to load player data. Please report this to server operators if this keeps happening.", NamedTextColor.RED)));
				}

				MMLog.severe(LOG_PREFIX + "Failed to load social caches for " + senderUuid + " and/or " + receiverUuid + ". Is Redis down?");
				return;
			}

			// Perform synchronous checks to see if the sender can block the receiver
			PlayerSocialCache.BlockCheckResult result = senderCache.canBlockPlayer(receiverCache);

			if (result != PlayerSocialCache.BlockCheckResult.OK) {
				Component errorMessage = switch (result) {
					case SELF -> Component.text("You cannot block yourself!", NamedTextColor.RED);
					case ALREADY_BLOCKED ->
						Component.text("You already blocked " + resolveName(receiverUuid) + "!", NamedTextColor.RED);
					default ->
						Component.text("You cannot block " + resolveName(receiverUuid) + "!", NamedTextColor.RED);
				};

				if (sender != null) {
					sender.sendMessage(appendPrefix(errorMessage));
				}

				return;
			}

			// Check if the receiver has the block exemption permission
			LuckPermsIntegration.loadUser(receiverUuid).thenAcceptAsync(receiver -> {
				if (receiver.getCachedData().getPermissionData().checkPermission(BLOCK_EXEMPTION_PERMISSION).asBoolean()) {
					if (sender != null) {
						sender.sendMessage(appendPrefix(Component.text("You cannot block " + resolveName(receiverUuid) + "!", NamedTextColor.RED)));
					}
					return;
				}

				// Fetch the current timestamp
				String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());

				// Set the receiver as blocked in Redis
				RedisAPI.getInstance().async().hset(getRedisPath(REDIS_SOCIAL_TYPE_BLOCKED, senderUuid), receiverUuid.toString(), timestamp).thenRun(() -> {
					if (sender != null) {
						sender.sendMessage(appendPrefix(Component.text("You have blocked " + resolveName(receiverUuid) + ".", NamedTextColor.GREEN)));
					}

					// Remove all pending friend requests from Redis
					RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, senderUuid), receiverUuid.toString());
					RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, receiverUuid), senderUuid.toString());

					// Check if the sender and the receiver are friends
					if (senderCache.isFriendsWith(receiverUuid)) {
						// Remove the receiver as a friend
						removeFriend(null, senderUuid, receiverUuid);
					}

					// Broadcast to all shards via RabbitMQ to
					// set the receiver as blocked in memory,
					// to remove all pending friend requests from memory,
					// and to cancel all friend request expiration tasks
					Map<String, String> jsonProperties = Map.of(
						"senderUuid", senderUuid.toString(),
						"receiverUuid", receiverUuid.toString(),
						"timestamp", timestamp
					);
					broadcastNotification(BLOCKED_PLAYER_CHANNEL, jsonProperties, "Failed to notify other shards of a player being blocked via RabbitMQ.");
				});
			});
		});
	}

	static CompletableFuture<Void> unblockPlayer(UUID senderUuid, UUID receiverUuid) {
		Player sender = Bukkit.getPlayer(senderUuid);

		// Load the sender's social cache from memory or from Redis
		return loadSocialCaches(senderUuid).thenCompose(socialCaches -> {
			if (socialCaches == null) {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("An error occurred while trying to load player data. Please report this to server operators if this keeps happening.", NamedTextColor.RED)));
				}

				MMLog.severe(LOG_PREFIX + "Failed to load social cache for " + senderUuid + ". Is Redis down?");
				return CompletableFuture.failedFuture(new IllegalStateException(LOG_PREFIX + "Failed to load social cache for " + senderUuid + ". Is Redis down?"));
			}

			// Fetch the sender's social cache
			PlayerSocialCache senderCache = socialCaches.get(senderUuid);

			// This shouldn't occur if loadSocialCaches didn't return null
			if (senderCache == null) {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("An error occurred while trying to load player data. Please report this to server operators if this keeps happening.", NamedTextColor.RED)));
				}

				MMLog.severe(LOG_PREFIX + "Failed to load social cache for " + senderUuid + ". Is Redis down?");
				return CompletableFuture.failedFuture(new IllegalStateException(LOG_PREFIX + "Failed to load social cache for " + senderUuid + ". Is Redis down?"));
			}

			// Check if the receiver is blocked
			if (!senderCache.hasBlocked(receiverUuid)) {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("You do not have " + resolveName(receiverUuid) + " blocked!", NamedTextColor.RED)));
				}

				return CompletableFuture.completedFuture(null);
			}

			// Remove the block on the receiver in Redis
			CompletableFuture<Boolean> senderRedisFuture = RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_BLOCKED, senderUuid), receiverUuid.toString()).thenApply(val -> val == 1L).toCompletableFuture();

			// Create a pending future for the unblock operation to be completed by RabbitMQ confirmation
			String unblockKey = getSocialPairKey(senderUuid, receiverUuid);
			CompletableFuture<Void> syncFuture = new CompletableFuture<>();
			PENDING_UNBLOCKS_MAP.put(unblockKey, syncFuture);

			return senderRedisFuture.thenRun(() -> {
				if (sender != null) {
					sender.sendMessage(appendPrefix(Component.text("You have unblocked " + resolveName(receiverUuid) + ".", NamedTextColor.GREEN)));
				}

				// Broadcast to all shards via RabbitMQ to
				// remove the block on the receiver in memory
				Map<String, String> jsonProperties = Map.of(
					"senderUuid", senderUuid.toString(),
					"receiverUuid", receiverUuid.toString()
				);
				broadcastNotification(UNBLOCKED_PLAYER_CHANNEL, jsonProperties, "Failed to notify other shards of a player being unblocked via RabbitMQ.");
			}).thenCompose(v -> syncFuture);
		});
	}
	//endregion

	//region <EVENT HANDLERS>
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onPlayerLogin(PlayerLoginEvent event) {
		UUID playerUuid = event.getPlayer().getUniqueId();
		long currentTime = System.currentTimeMillis() / 1000;

		// Check for expired friend requests when the player joins in case a shard went down before the corresponding friend request expiration tasks could run
		CompletableFuture<Void> expiredFriendRequestsFuture = RedisAPI.getInstance().async()
			.hgetall(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, playerUuid))
			.thenAccept(data ->
				data.forEach((key, value) -> {
					long storedTime = Long.parseLong(value);
					if (currentTime >= storedTime) {
						// Remove expired friend requests
						RedisAPI.getInstance().async().hdel(getRedisPath(REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, playerUuid), key);
					}
				})
			)
			.toCompletableFuture();

		// Check the player's block list to see if any blocked players now have the block exemption permission
		CompletableFuture<Void> blockExemptedFuture = RedisAPI.getInstance().async()
			.hgetall(getRedisPath(REDIS_SOCIAL_TYPE_BLOCKED, playerUuid))
			.thenCompose(data -> {
				// Track all asynchronous LuckPerms tasks to run
				List<CompletableFuture<Void>> unblockFutures = new ArrayList<>();

				data.forEach((key, value) -> {
					UUID blockedPlayerUuid = UUID.fromString(key);
					CompletableFuture<Void> unblockFuture = LuckPermsIntegration.loadUser(blockedPlayerUuid).thenAcceptAsync(blockedUser -> {
						if (blockedUser.getCachedData().getPermissionData().checkPermission(BLOCK_EXEMPTION_PERMISSION).asBoolean()) {
							// Automatically unblock the exempted player
							unblockPlayer(playerUuid, blockedPlayerUuid);
						}
					});

					// Track the current LuckPerms task
					unblockFutures.add(unblockFuture);
				});

				// Wait for all asynchronous LuckPerms tasks to finish
				return CompletableFuture.allOf(unblockFutures.toArray(new CompletableFuture[0]));
			})
			.toCompletableFuture();

		// Load the player's social cache into memory after the previous checks complete
		CompletableFuture.allOf(expiredFriendRequestsFuture, blockExemptedFuture)
			.thenCompose(v -> PlayerSocialCache.load(playerUuid))
			.thenAccept(socialCache -> SOCIAL_CACHE_MAP.put(playerUuid, socialCache));
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onPlayerLogout(PlayerQuitEvent event) {
		UUID playerUuid = event.getPlayer().getUniqueId();
		PlayerSocialCache cachedAtLogout = SOCIAL_CACHE_MAP.get(playerUuid);

		// Cache cleanup task
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			Player player = Bukkit.getPlayer(playerUuid);
			boolean stillOffline = (player == null || !player.isOnline());
			boolean cacheUnchanged = SOCIAL_CACHE_MAP.get(playerUuid) == cachedAtLogout;

			// Check that the cache remains unchanged in case the player reconnects or returns to the same shard and gets a new cache before the cleanup task is run
			if (stillOffline && cacheUnchanged) {
				// Unload the player's social cache from memory if they are offline or in a different shard for 15 seconds
				SOCIAL_CACHE_MAP.remove(playerUuid);
			}
		}, 15 * 20L);
	}
	//endregion

	//region <RABBITMQ>
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void networkRelayMessageEvent(@NotNull NetworkRelayMessageEvent event) {
		JsonObject data = event.getData();
		switch (event.getChannel()) {
			case INCOMING_FRIEND_REQUEST_CHANNEL -> handleIncomingFriendRequest(data);
			case FRIEND_REQUEST_EXPIRATION_CHANNEL -> handleExpiredFriendRequest(data);
			case ADDED_FRIEND_CHANNEL -> handleAddedFriend(data);
			case MODERATOR_FORCE_ADDED_FRIENDS_CHANNEL -> handleModeratorForceAddedFriendship(data);
			case REMOVED_FRIEND_CHANNEL -> handleRemovedFriend(data);
			case MODERATOR_FORCE_REMOVED_FRIENDS_CHANNEL -> handleModeratorForceRemovedFriendship(data);
			case BLOCKED_PLAYER_CHANNEL -> handleBlockedPlayer(data);
			case UNBLOCKED_PLAYER_CHANNEL -> handleUnblockedPlayer(data);
			default -> {
				// Do nothing
			}
		}
	}

	private void handleIncomingFriendRequest(JsonObject data) {
		if (data.has("senderUuid") && data.has("receiverUuid") && data.has("expirationTime")) {
			UUID senderUuid = UUID.fromString(data.get("senderUuid").getAsString());
			UUID receiverUuid = UUID.fromString(data.get("receiverUuid").getAsString());
			long expirationTime = data.get("expirationTime").getAsLong();

			Player receiver = Bukkit.getPlayer(receiverUuid);
			PlayerSocialCache receiverCache = getSocialCache(receiverUuid);

			// Send the pending friend request to the receiver in memory
			if (receiverCache != null) {
				receiverCache.addPendingFriendRequest(senderUuid, expirationTime);
			}

			// Calculate how many seconds remain until the friend request expires
			long remainingSeconds = expirationTime - (System.currentTimeMillis() / 1000);

			if (remainingSeconds > 0) {
				// Schedule the friend request expiration task
				BukkitTask expirationTask = Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> expireFriendRequest(senderUuid, receiverUuid), remainingSeconds * 20L);

				// Track the friend request expiration task for possible cancellation
				PENDING_FRIEND_REQUESTS_MAP.put(senderUuid + "_" + receiverUuid, expirationTask);
			}

			// Notify the receiver about the pending friend request
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
			PlayerSocialCache receiverCache = getSocialCache(receiverUuid);

			// Remove the pending friend request from the receiver in memory
			if (receiverCache != null) {
				receiverCache.removePendingFriendRequest(senderUuid);
			}

			// Discard the friend request expiration task
			clearExpirationTasks(senderUuid, receiverUuid);

			// Notify the sender and the receiver about the friend request expiration
			if (sender != null) {
				sender.sendMessage(appendPrefix(Component.text("Your friend request to " + resolveName(receiverUuid) + " has expired.", NamedTextColor.RED)));
			}

			if (receiver != null) {
				receiver.sendMessage(appendPrefix(Component.text("The friend request from " + resolveName(senderUuid) + " has expired.", NamedTextColor.RED)));
			}
		}
	}

	private void handleAddedFriend(JsonObject data) {
		if (data.has("senderUuid") && data.has("receiverUuid") && data.has("timestamp")) {
			UUID senderUuid = UUID.fromString(data.get("senderUuid").getAsString());
			UUID receiverUuid = UUID.fromString(data.get("receiverUuid").getAsString());
			String timestamp = data.get("timestamp").getAsString();

			Player sender = Bukkit.getPlayer(senderUuid);
			Player receiver = Bukkit.getPlayer(receiverUuid);
			PlayerSocialCache senderCache = getSocialCache(senderUuid);
			PlayerSocialCache receiverCache = getSocialCache(receiverUuid);

			// Set the sender and the receiver as friends in memory and clean up the pending friend requests in memory
			if (senderCache != null) {
				senderCache.addFriend(receiverUuid, timestamp);
				senderCache.removePendingFriendRequest(receiverUuid);
			}

			if (receiverCache != null) {
				receiverCache.addFriend(senderUuid, timestamp);
				receiverCache.removePendingFriendRequest(senderUuid);
			}

			// Cancel the friend request expiration task
			clearExpirationTasks(senderUuid, receiverUuid);

			// Notify the sender and the receiver about their new friendship
			if (sender != null) {
				sender.sendMessage(appendPrefix(Component.text("You are now friends with " + resolveName(receiverUuid) + ".", NamedTextColor.GREEN)));
			}

			if (receiver != null) {
				receiver.sendMessage(appendPrefix(Component.text("You are now friends with " + resolveName(senderUuid) + ".", NamedTextColor.GREEN)));
			}
		}
	}

	private void handleModeratorForceAddedFriendship(JsonObject data) {
		if (data.has("moderatorUuid") && data.has("senderUuid") && data.has("receiverUuid") && data.has("timestamp")) {
			UUID moderatorUuid = UUID.fromString(data.get("moderatorUuid").getAsString());
			UUID senderUuid = UUID.fromString(data.get("senderUuid").getAsString());
			UUID receiverUuid = UUID.fromString(data.get("receiverUuid").getAsString());
			String timestamp = data.get("timestamp").getAsString();

			Player sender = Bukkit.getPlayer(senderUuid);
			Player receiver = Bukkit.getPlayer(receiverUuid);
			PlayerSocialCache senderCache = getSocialCache(senderUuid);
			PlayerSocialCache receiverCache = getSocialCache(receiverUuid);

			// Set the sender and the receiver as friends in memory and clean up the pending friend requests in memory
			if (senderCache != null) {
				senderCache.addFriend(receiverUuid, timestamp);
				senderCache.removePendingFriendRequest(receiverUuid);
			}

			if (receiverCache != null) {
				receiverCache.addFriend(senderUuid, timestamp);
				receiverCache.removePendingFriendRequest(senderUuid);
			}

			// Cancel the friend request expiration task
			clearExpirationTasks(senderUuid, receiverUuid);

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
		}
	}

	private void handleRemovedFriend(JsonObject data) {
		if (data.has("senderUuid") && data.has("receiverUuid")) {
			UUID senderUuid = UUID.fromString(data.get("senderUuid").getAsString());
			UUID receiverUuid = UUID.fromString(data.get("receiverUuid").getAsString());

			Player sender = Bukkit.getPlayer(senderUuid);
			Player receiver = Bukkit.getPlayer(receiverUuid);
			PlayerSocialCache senderCache = getSocialCache(senderUuid);
			PlayerSocialCache receiverCache = getSocialCache(receiverUuid);

			// Remove the sender and the receiver as friends in memory
			if (senderCache != null) {
				senderCache.removeFriend(receiverUuid);
			}

			if (receiverCache != null) {
				receiverCache.removeFriend(senderUuid);
			}

			// Notify the sender and the receiver about the friend removal
			if (sender != null) {
				sender.sendMessage(appendPrefix(Component.text("You are no longer friends with " + resolveName(receiverUuid) + "!", NamedTextColor.GREEN)));
			}

			if (receiver != null) {
				receiver.sendMessage(appendPrefix(Component.text(resolveName(senderUuid) + " removed you as a friend!", NamedTextColor.RED)));
			}

			// Complete the pending future associated with the friend removal
			String friendKey = getSocialPairKey(senderUuid, receiverUuid);
			CompletableFuture<Void> future = PENDING_FRIEND_REMOVALS_MAP.remove(friendKey);

			if (future != null) {
				// Mark the cache update as complete
				future.complete(null);
			}
		}
	}

	private void handleModeratorForceRemovedFriendship(JsonObject data) {
		if (data.has("senderUuid") && data.has("receiverUuid")) {
			UUID senderUuid = UUID.fromString(data.get("senderUuid").getAsString());
			UUID receiverUuid = UUID.fromString(data.get("receiverUuid").getAsString());

			Player sender = Bukkit.getPlayer(senderUuid);
			Player receiver = Bukkit.getPlayer(receiverUuid);
			PlayerSocialCache senderCache = getSocialCache(senderUuid);
			PlayerSocialCache receiverCache = getSocialCache(receiverUuid);

			// Remove the sender and the receiver as friends in memory
			if (senderCache != null) {
				senderCache.removeFriend(receiverUuid);
			}

			if (receiverCache != null) {
				receiverCache.removeFriend(senderUuid);
			}

			// Notify the sender and the receiver if a moderator forcefully removed them as friends
			if (sender != null) {
				sender.sendMessage(appendPrefix(Component.text("A moderator forcefully removed you and " + resolveName(receiverUuid) + " as friends.", NamedTextColor.RED)));
			}

			if (receiver != null) {
				receiver.sendMessage(appendPrefix(Component.text("A moderator forcefully removed you and " + resolveName(senderUuid) + " as friends.", NamedTextColor.RED)));
			}

			// Complete the pending future associated with the friend removal
			String friendKey = getSocialPairKey(senderUuid, receiverUuid);
			CompletableFuture<Void> future = PENDING_FRIEND_REMOVALS_MAP.remove(friendKey);

			if (future != null) {
				// Mark the cache update as complete
				future.complete(null);
			}
		}
	}

	private void handleBlockedPlayer(JsonObject data) {
		if (data.has("senderUuid") && data.has("receiverUuid") && data.has("timestamp")) {
			UUID senderUuid = UUID.fromString(data.get("senderUuid").getAsString());
			UUID receiverUuid = UUID.fromString(data.get("receiverUuid").getAsString());
			String timestamp = data.get("timestamp").getAsString();

			PlayerSocialCache senderCache = getSocialCache(senderUuid);
			PlayerSocialCache receiverCache = getSocialCache(receiverUuid);

			// Set the receiver as blocked in memory and remove all pending friend requests from memory
			if (senderCache != null) {
				senderCache.blockPlayer(receiverUuid, timestamp);
				senderCache.removePendingFriendRequest(receiverUuid);
			}

			if (receiverCache != null) {
				receiverCache.removePendingFriendRequest(senderUuid);
			}

			// Cancel all friend request expiration tasks
			clearExpirationTasks(senderUuid, receiverUuid);
		}
	}

	private void handleUnblockedPlayer(JsonObject data) {
		if (data.has("senderUuid") && data.has("receiverUuid")) {
			UUID senderUuid = UUID.fromString(data.get("senderUuid").getAsString());
			UUID receiverUuid = UUID.fromString(data.get("receiverUuid").getAsString());

			PlayerSocialCache senderCache = getSocialCache(senderUuid);

			// Remove the block on the receiver in memory
			if (senderCache != null) {
				senderCache.unblockPlayer(receiverUuid);
			}

			// Complete the pending future associated with the block removal
			String unblockKey = getSocialPairKey(senderUuid, receiverUuid);
			CompletableFuture<Void> future = PENDING_UNBLOCKS_MAP.remove(unblockKey);

			if (future != null) {
				// Mark the cache update as complete
				future.complete(null);
			}
		}
	}
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
		return cachedName != null ? cachedName : uuid.toString();
	}

	static String formatTimestamp(String isoTimestamp) {
		try {
			Instant instant = Instant.parse(isoTimestamp);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());
			return formatter.format(instant);
		} catch (DateTimeParseException exception) {
			return "Unknown Date";
		}
	}

	static CompletableFuture<@Nullable Map<UUID, PlayerSocialCache>> loadSocialCaches(UUID... uuids) {
		// Hold the final <UUID, PlayerSocialCache> pairs
		Map<UUID, PlayerSocialCache> result = new ConcurrentHashMap<>();

		// Track all asynchronous load tasks to run
		List<CompletableFuture<Boolean>> futures = new ArrayList<>();

		for (UUID uuid : uuids) {
			PlayerSocialCache cache = getSocialCache(uuid);

			if (cache != null) {
				// Use the existing cache if it's already loaded
				result.put(uuid, cache);
			} else {
				// Load the missing cache from Redis
				CompletableFuture<Boolean> future = PlayerSocialCache.load(uuid).thenApply(loaded -> {
					if (loaded != null) {
						// Use the newly loaded cache
						result.put(uuid, loaded);
						return true;
					}

					// Return false if the cache failed to load
					return false;
				});

				// Track the current loading task
				futures.add(future);
			}
		}

		// Wait for all asynchronous loading tasks to finish before checking if any failed to load
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> {
			boolean allSucceeded = futures.stream().allMatch(f -> f.getNow(false));

			// Return null if any tasks failed to load a cache
			return allSucceeded ? result : null;
		});
	}

	static CompletableFuture<PlayerSocialDisplayInfo> getSocialDisplayInfo(UUID playerUuid) {
		return loadSocialCaches(playerUuid).thenCompose(socialCaches -> {
			if (socialCaches == null) {
				MMLog.severe(LOG_PREFIX + "Failed to load social cache for " + playerUuid + ". Is Redis down?");
				return CompletableFuture.failedFuture(new IllegalStateException(LOG_PREFIX + "Failed to load social cache for " + playerUuid + ". Is Redis down?"));
			}

			// Fetch the player's social cache
			PlayerSocialCache playerCache = socialCaches.get(playerUuid);

			// This shouldn't occur if loadSocialCaches didn't return null
			if (playerCache == null) {
				MMLog.severe(LOG_PREFIX + "Failed to load social cache for " + playerUuid + ". Is Redis down?");
				return CompletableFuture.failedFuture(new IllegalStateException(LOG_PREFIX + "Failed to load social cache for " + playerUuid + ". Is Redis down?"));
			}

			return PlayerSocialDisplayInfo.getSocialInfoFromSocialCache(playerCache);
		});
	}

	private static void clearExpirationTasks(UUID firstUuid, UUID secondUuid) {
		BukkitTask firstTask = PENDING_FRIEND_REQUESTS_MAP.remove(firstUuid + "_" + secondUuid);
		if (firstTask != null) {
			firstTask.cancel();
		}

		BukkitTask secondTask = PENDING_FRIEND_REQUESTS_MAP.remove(secondUuid + "_" + firstUuid);
		if (secondTask != null) {
			secondTask.cancel();
		}
	}

	static String getRedisPath(String socialType, UUID uuid) {
		return "social:" + socialType + ":" + uuid;
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

	private static String getSocialPairKey(UUID uuid1, UUID uuid2) {
		// Generate a consistent key for a pair of UUIDs regardless of their order
		return uuid1.compareTo(uuid2) < 0 ? uuid1 + "-" + uuid2 : uuid2 + "-" + uuid1;
	}
	//endregion

	//region <GETTERS>

	/**
	 * Retrieves the in-memory {@code PlayerSocialCache} for the specified player if one has already been loaded.
	 *
	 * @param playerUuid the UUID of the player whose cache is requested (must not be {@code null})
	 * @return the cached {@code PlayerSocialCache}, or {@code null} if no cache is currently stored
	 */
	@Nullable
	public static PlayerSocialCache getSocialCache(UUID playerUuid) {
		return SOCIAL_CACHE_MAP.get(playerUuid);
	}
	//endregion
}
