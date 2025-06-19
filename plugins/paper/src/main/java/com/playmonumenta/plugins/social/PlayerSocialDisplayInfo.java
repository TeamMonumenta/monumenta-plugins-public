package com.playmonumenta.plugins.social;

import com.playmonumenta.plugins.utils.MMLog;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerSocialDisplayInfo {
	final UUID mPlayerUuid;
	final Map<UUID, Friend> mFriendMap;
	final Map<UUID, BlockedPlayer> mBlockedMap;

	private PlayerSocialDisplayInfo(UUID playerUuid, Map<UUID, Friend> friendMap, Map<UUID, BlockedPlayer> blockedMap) {
		mPlayerUuid = playerUuid;
		mFriendMap = friendMap;
		mBlockedMap = blockedMap;
	}

	public static class Friend {
		final String mFriendName;
		final UUID mFriendUuid;
		final String mFriendTimestamp;

		Friend(String friendName, UUID friendUuid, String friendTimestamp) {
			mFriendName = friendName;
			mFriendUuid = friendUuid;
			mFriendTimestamp = friendTimestamp;
		}
	}

	public static class BlockedPlayer {
		final String mBlockedName;
		final UUID mBlockedUuid;
		final String mBlockedTimestamp;

		BlockedPlayer(String blockedName, UUID blockedUuid, String blockedTimestamp) {
			mBlockedName = blockedName;
			mBlockedUuid = blockedUuid;
			mBlockedTimestamp = blockedTimestamp;
		}
	}

	static CompletableFuture<PlayerSocialDisplayInfo> getSocialInfoFromSocialCache(PlayerSocialCache cache) {
		UUID playerUuid = cache.getPlayerUuid();

		// Process friend data
		Map<UUID, Friend> friendMap = new HashMap<>();
		for (UUID friendUuid : cache.getFriends()) {
			String friendName = SocialManager.resolveName(friendUuid);
			String friendTimestamp = cache.getFriendTimestamps().getOrDefault(friendUuid, "Unknown Date");

			if (friendTimestamp.equals("Unknown Date")) {
				MMLog.warning("[Social Manager] Failed to get friend timestamp between " + friendUuid + " and " + playerUuid + ".");
			}

			friendMap.put(friendUuid, new Friend(friendName, friendUuid, friendTimestamp));
		}

		// Process blocked data
		Map<UUID, BlockedPlayer> blockedMap = new HashMap<>();
		for (UUID blockedUuid : cache.getBlockedPlayers()) {
			String blockedName = SocialManager.resolveName(blockedUuid);
			String blockedTimestamp = cache.getBlockedTimestamps().getOrDefault(blockedUuid, "Unknown Date");

			if (blockedTimestamp.equals("Unknown Date")) {
				MMLog.warning("[Social Manager] Failed to get blocked timestamp for " + blockedUuid + " from " + playerUuid + "'s block list.");
			}

			blockedMap.put(blockedUuid, new BlockedPlayer(blockedName, blockedUuid, blockedTimestamp));
		}

		return CompletableFuture.completedFuture(new PlayerSocialDisplayInfo(playerUuid, friendMap, blockedMap));
	}
}
