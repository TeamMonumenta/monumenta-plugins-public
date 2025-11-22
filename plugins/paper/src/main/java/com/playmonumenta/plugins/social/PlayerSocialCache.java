package com.playmonumenta.plugins.social;

import com.playmonumenta.redissync.RedisAPI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

/**
 * Caches a player's pending friend requests, friends, and blocked players.
 * <p>
 * Instances are normally loaded asynchronously from Redis via {@link #load(UUID)}.
 */
public class PlayerSocialCache {
	private final UUID mUuid;
	private final Map<UUID, Long> mPendingFriendRequests = new HashMap<>();
	private final Map<UUID, String> mFriends = new HashMap<>();
	private final Map<UUID, String> mBlockedPlayers = new HashMap<>();

	private PlayerSocialCache(UUID uuid) {
		mUuid = uuid;
	}

	static CompletableFuture<PlayerSocialCache> load(UUID uuid) {
		PlayerSocialCache socialCache = new PlayerSocialCache(uuid);

		// Fetch pending friend requests, friends, and blocked players
		CompletableFuture<Void> loadPendingFriendRequests = RedisAPI.getInstance().async()
			.hgetall(SocialManager.getRedisPath(SocialManager.REDIS_SOCIAL_TYPE_PENDING_FRIEND_REQUEST, uuid))
			.thenAccept(requestData ->
				requestData.forEach((key, value) ->
					socialCache.mPendingFriendRequests.put(UUID.fromString(key), Long.parseLong(value))))
			.toCompletableFuture();

		CompletableFuture<Void> loadFriends = RedisAPI.getInstance().async()
			.hgetall(SocialManager.getRedisPath(SocialManager.REDIS_SOCIAL_TYPE_FRIEND, uuid))
			.thenAccept(friendData ->
				friendData.forEach((key, value) ->
					socialCache.mFriends.put(UUID.fromString(key), value)))
			.toCompletableFuture();

		CompletableFuture<Void> loadBlockedPlayers = RedisAPI.getInstance().async()
			.hgetall(SocialManager.getRedisPath(SocialManager.REDIS_SOCIAL_TYPE_BLOCKED, uuid))
			.thenAccept(blockedData ->
				blockedData.forEach((key, value) ->
					socialCache.mBlockedPlayers.put(UUID.fromString(key), value)))
			.toCompletableFuture();

		return CompletableFuture.allOf(loadPendingFriendRequests, loadFriends, loadBlockedPlayers).thenApply(v -> socialCache);
	}

	/**
	 * Result of checking whether two players can become friends.
	 */
	public enum FriendshipCheckResult {
		OK,
		SELF,
		ALREADY_FRIENDS,
		BLOCKED
	}

	/**
	 * Result of checking whether a player can block another.
	 */
	public enum BlockCheckResult {
		OK,
		SELF,
		ALREADY_BLOCKED
	}

	//region <GETTERS>

	/**
	 * Gets the UUID of the player whose data this cache represents.
	 *
	 * @return the player's {@link UUID}
	 */
	public UUID getPlayerUuid() {
		return mUuid;
	}

	/**
	 * Evaluates whether this player can send a friend request to the player represented by {@code otherSocialCache}.
	 *
	 * @param otherSocialCache the other player’s social cache instance
	 * @return the reason the request is allowed or disallowed
	 */
	public FriendshipCheckResult canBecomeFriendsWith(PlayerSocialCache otherSocialCache) {
		UUID otherUuid = otherSocialCache.mUuid;

		// Check if the player is trying to be friends with themselves
		if (mUuid.equals(otherUuid)) {
			return FriendshipCheckResult.SELF;
		}

		// Check if the player is already friends with the other player
		if (isFriendsWith(otherUuid)) {
			return FriendshipCheckResult.ALREADY_FRIENDS;
		}

		// Check if either player has the other blocked
		if (isBlockedBetween(otherSocialCache)) {
			return FriendshipCheckResult.BLOCKED;
		}

		return FriendshipCheckResult.OK;
	}

	/**
	 * Returns an unmodifiable view of all pending friend requests.
	 * <p>
	 * The map key is the sender’s UUID; the value is the request-expiration time expressed as epoch seconds.
	 *
	 * @return immutable map of {@code senderUuid → expirationSeconds}
	 */
	public Map<UUID, Long> getPendingFriendRequests() {
		return Collections.unmodifiableMap(mPendingFriendRequests);
	}

	/**
	 * Checks whether the given UUID is already in the player's friends list.
	 *
	 * @param otherUuid UUID of the other player
	 * @return {@code true} if they are friends, otherwise {@code false}
	 */
	public boolean isFriendsWith(UUID otherUuid) {
		return mFriends.containsKey(otherUuid);
	}

	/**
	 * Retrieves the timestamp at which the friendship with the given player was created.
	 * <p>
	 * The timestamp is stored in ISO-8601 UTC format. For example:
	 * {@code 1970-01-01T00:00:00Z}.
	 *
	 * @param friendUuid UUID of the friend
	 * @return ISO-8601 timestamp string, or {@code null} if not friends
	 */
	@Nullable
	public String getFriendshipTimestamp(UUID friendUuid) {
		return mFriends.get(friendUuid);
	}

	/**
	 * Gets an immutable view of all friend UUIDs.
	 *
	 * @return set of friend UUIDs
	 */
	public Set<UUID> getFriends() {
		return mFriends.keySet();
	}

	/**
	 * Gets an immutable map of friend UUIDs to the timestamps at which each friendship was established.
	 * <p>
	 * Each timestamp is stored in ISO-8601 UTC format. For example:
	 * {@code 1970-01-01T00:00:00Z}.
	 *
	 * @return immutable map of {@code friendUuid → isoTimestamp}
	 */
	public Map<UUID, String> getFriendTimestamps() {
		return Collections.unmodifiableMap(mFriends);
	}

	/**
	 * Evaluates whether this player can block the player represented by {@code otherSocialCache}.
	 *
	 * @param otherSocialCache the other player’s social cache instance
	 * @return the reason the block is allowed or disallowed
	 */
	public BlockCheckResult canBlockPlayer(PlayerSocialCache otherSocialCache) {
		UUID otherUuid = otherSocialCache.mUuid;

		// Check if the player is trying to block themselves
		if (mUuid.equals(otherUuid)) {
			return BlockCheckResult.SELF;
		}

		// Check if the player already has the other player blocked
		if (hasBlocked(otherUuid)) {
			return BlockCheckResult.ALREADY_BLOCKED;
		}

		return BlockCheckResult.OK;
	}

	/**
	 * Checks whether this player currently has the specified UUID on their block list.
	 *
	 * @param otherUuid UUID of the player whose block status is being queried
	 * @return {@code true} if {@code otherUuid} is blocked, otherwise {@code false}
	 */
	public boolean hasBlocked(UUID otherUuid) {
		return mBlockedPlayers.containsKey(otherUuid);
	}

	/**
	 * Determines whether the two players is blocked in either direction.
	 *
	 * @param otherSocialCache the other player’s social cache
	 * @return {@code true} if a block exists in either direction, otherwise {@code false}
	 */
	public boolean isBlockedBetween(PlayerSocialCache otherSocialCache) {
		return mBlockedPlayers.containsKey(otherSocialCache.mUuid) || otherSocialCache.mBlockedPlayers.containsKey(mUuid);
	}

	/**
	 * Retrieves the timestamp at which {@code blockedUuid} was blocked by this player.
	 * <p>
	 * The timestamp is stored in ISO-8601 UTC format. For example:
	 * {@code 1970-01-01T00:00:00Z}.
	 *
	 * @param blockedUuid UUID of the blocked player
	 * @return ISO-8601 timestamp string, or {@code null} if the player is not blocked
	 */
	@Nullable
	public String getBlockTimestamp(UUID blockedUuid) {
		return mBlockedPlayers.get(blockedUuid);
	}

	/**
	 * Gets an immutable view of all blocked players' UUIDs.
	 *
	 * @return set of blocked UUIDs
	 */
	public Set<UUID> getBlockedPlayers() {
		return mBlockedPlayers.keySet();
	}

	/**
	 * Gets an immutable map of blocked players' UUIDs to the timestamps at which each block was placed.
	 * <p>
	 * Each timestamp is stored in ISO-8601 UTC format. For example:
	 * {@code 1970-01-01T00:00:00Z}.
	 *
	 * @return immutable map of {@code blockedUuid → isoTimestamp}
	 */
	public Map<UUID, String> getBlockedTimestamps() {
		return Collections.unmodifiableMap(mBlockedPlayers);
	}
	//endregion

	//region <SETTERS>
	void addPendingFriendRequest(UUID senderUuid, long expirationTime) {
		mPendingFriendRequests.put(senderUuid, expirationTime);
	}

	void removePendingFriendRequest(UUID senderUuid) {
		mPendingFriendRequests.remove(senderUuid);
	}

	void addFriend(UUID friendUuid, String timestamp) {
		mFriends.put(friendUuid, timestamp);
	}

	void removeFriend(UUID friendUuid) {
		mFriends.remove(friendUuid);
	}

	void blockPlayer(UUID blockedUuid, String timestamp) {
		mBlockedPlayers.put(blockedUuid, timestamp);
	}

	void unblockPlayer(UUID blockedUuid) {
		mBlockedPlayers.remove(blockedUuid);
	}
	//endregion
}
