package com.playmonumenta.plugins.market;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.redissync.ConfigAPI;
import com.playmonumenta.redissync.RedisAPI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class RedisItemDatabase {
	private static final int MAX_CACHE_ENTRIES = 10240;
	private static final int CACHE_EXPIRY_MINUTES = 30;

	static ScheduledThreadPoolExecutor mRealTimePool = new ScheduledThreadPoolExecutor(1);
	private static @Nullable TimerTask mRealTimeRunnable = null;

	static ConcurrentHashMap<Long, LocalDateTime> mLocalCacheIdToExpiry = new ConcurrentHashMap<>();
	static ConcurrentSkipListMap<LocalDateTime, Long> mLocalCacheExpiryToId = new ConcurrentSkipListMap<>();
	static ConcurrentHashMap<Long, ItemStack> mLocalCacheIDToItem = new ConcurrentHashMap<>();
	static ConcurrentHashMap<ItemStack, Long> mLocalCacheItemToID = new ConcurrentHashMap<>();

	private static final String mPathIDBCurrentID = ConfigAPI.getServerDomain() + ":market:ItemDBCurrentID";
	private static final String mPathIDBItemToID = ConfigAPI.getServerDomain() + ":market:ItemDBItemToID";
	private static final String mPathIDBIDToItem = ConfigAPI.getServerDomain() + ":market:ItemDBIDToItem";

	private static long createItemEntryInRedis(ItemStack item) {
		long id = getNextItemID();
		saveToCache(id, item);
		String mojangson = ItemUtils.serializeItemStack(item);
		String idStr = String.valueOf(id);
		RedisAPI.getInstance().sync().hset(mPathIDBItemToID, mojangson, idStr);
		RedisAPI.getInstance().sync().hset(mPathIDBIDToItem, idStr, mojangson);
		return id;
	}

	private static long getNextItemID() {
		return RedisAPI.getInstance().sync().incr(mPathIDBCurrentID);
	}

	public static long getIDFromItemStack(ItemStack item) {
		// attempt to fetch from cache
		Long id = mLocalCacheItemToID.get(item.asOne());
		if (id != null) {
			touchCacheTimestamp(id);
			return id;
		}

		// attempt to fetch from redis
		id = fetchIDFromRedis(item.asOne());
		if (id != null) {
			touchCacheTimestamp(id);
			return id;
		}

		// create a new entry in redis
		id = createItemEntryInRedis(item.asOne());
		touchCacheTimestamp(id);
		return id;
	}

	public static ItemStack getItemStackFromID(long id) {
		// attempt to fetch from cache
		ItemStack item = mLocalCacheIDToItem.get(id);
		if (item != null) {
			touchCacheTimestamp(id);
			return item.asOne();
		}

		// attempt to fetch from redis
		item = fetchItemFromRedis(id);
		if (item == null) {
			AuditListener.logMarket("PAAAAAAAANIC; no item found in database for id " + id + ". ping @ray and tell him his code sucks");
			return new ItemStack(Material.STONE, 1);
		}
		return item.asOne();
	}

	private static @Nullable Long fetchIDFromRedis(ItemStack item) {
		String idStr = RedisAPI.getInstance().sync().hget(mPathIDBItemToID, ItemUtils.serializeItemStack(item));
		if (idStr == null || idStr.isEmpty()) {
			return null;
		}
		long id = Long.parseLong(idStr);
		saveToCache(id, item);
		return id;
	}

	private static @Nullable ItemStack fetchItemFromRedis(long id) {
		String mojangson = RedisAPI.getInstance().sync().hget(mPathIDBIDToItem, String.valueOf(id));
		if (mojangson == null || mojangson.isEmpty()) {
			return null;
		}
		ItemStack item = ItemUtils.parseItemStack(mojangson);
		saveToCache(id, item);
		return item;
	}

	private static void saveToCache(long id, ItemStack item) {
		mLocalCacheIDToItem.put(id, item.asOne());
		mLocalCacheItemToID.put(item.asOne(), id);
		touchCacheTimestamp(id);
	}

	private static void touchCacheTimestamp(long id) {
		LocalDateTime expiry = mLocalCacheIdToExpiry.get(id);
		if (expiry != null) {
			mLocalCacheExpiryToId.remove(expiry);
		}

		expiry = DateUtils.trueUtcDateTime().plusMinutes(CACHE_EXPIRY_MINUTES);
		mLocalCacheIdToExpiry.put(id, expiry);
		mLocalCacheExpiryToId.put(expiry, id);

		if (mLocalCacheExpiryToId.size() > MAX_CACHE_ENTRIES) {
			removeOldestEntry();
		}

		removeExpiredEntries();
	}

	private static void removeOldestEntry() {
		Map.Entry<LocalDateTime, Long> entry = mLocalCacheExpiryToId.firstEntry();
		if (entry == null) {
			return;
		}
		LocalDateTime expiry = entry.getKey();
		Long id = entry.getValue();

		mLocalCacheExpiryToId.remove(expiry);
		mLocalCacheIdToExpiry.remove(id);
		ItemStack item = mLocalCacheIDToItem.get(id);
		if (item != null) {
			mLocalCacheItemToID.remove(item);
		}
	}

	private static void removeExpiredEntries() {
		LocalDateTime now = DateUtils.trueUtcDateTime();
		while (true) {
			Map.Entry<LocalDateTime, Long> entry = mLocalCacheExpiryToId.firstEntry();
			if (entry == null) {
				return;
			}
			LocalDateTime expiry = entry.getKey();
			if (now.isBefore(expiry)) {
				break;
			}
			Long id = entry.getValue();

			mLocalCacheExpiryToId.remove(expiry);
			mLocalCacheIdToExpiry.remove(id);
			ItemStack item = mLocalCacheIDToItem.get(id);
			if (item != null) {
				mLocalCacheItemToID.remove(item);
			}
		}

		Map.Entry<LocalDateTime, Long> entry = mLocalCacheExpiryToId.firstEntry();
		LocalDateTime expiry = entry == null ? null : entry.getKey();
		if (mRealTimeRunnable == null && expiry != null) {
			mRealTimeRunnable = new TimerTask() {
				@Override
				public void run() {
					Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
						mRealTimeRunnable = null;
						removeExpiredEntries();
					});
				}
			};
			long remainingSeconds = Math.max(now.until(expiry, ChronoUnit.SECONDS), 0) + 1;
			mRealTimePool.schedule(mRealTimeRunnable, remainingSeconds, TimeUnit.SECONDS);
		}
	}
}
