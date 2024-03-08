package com.playmonumenta.plugins.market;

import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.redissync.ConfigAPI;
import com.playmonumenta.redissync.RedisAPI;
import java.util.HashMap;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class MarketItemDatabase {
	static HashMap<Long, ItemStack> mLocalCacheIDToItem = new HashMap<>();
	static HashMap<ItemStack, Long> mLocalCacheItemToID = new HashMap<>();

	private static final String mPathIDBCurrentID = ConfigAPI.getServerDomain() + ":market:ItemDBCurrentID";
	private static final String mPathIDBItemToID = ConfigAPI.getServerDomain() + ":market:ItemDBItemToID";
	private static final String mPathIDBIDToItem = ConfigAPI.getServerDomain() + ":market:ItemDBIDToItem";

	static long maxCacheAgeTimestamp = 0;

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
		MMLog.info("GET ID FROM ITEM " + ItemUtils.getPlainName(item));
		resetCacheIfTooOld();

		// attempt to fetch from cache
		Long id = mLocalCacheItemToID.get(item.asOne());
		if (id != null) {
			return id;
		}

		// attempt to fetch from redis
		id = fetchIDFromRedis(item.asOne());
		if (id != null) {
			return id;
		}

		// create a new entry in redis
		id = createItemEntryInRedis(item.asOne());
		return id;
	}

	public static ItemStack getItemStackFromID(long id) {
		MMLog.info("GET ITEM FROM ID " + id);
		resetCacheIfTooOld();

		// attempt to fetch from cache
		ItemStack item = mLocalCacheIDToItem.get(id);
		if (item != null) {
			return item.asOne();
		}

		// attempt to fetch from redis
		item = fetchItemFromRedis(id);
		if (item == null) {
			AuditListener.logMarket("PAAAAAAAANIC; no item found in database for id. ping @ray and tell him his code sucks" + id);
			return new ItemStack(Material.STONE, 1);
		}
		return item.asOne();
	}

	private static @Nullable Long fetchIDFromRedis(ItemStack item) {
		MMLog.info("FETCH ID FROM REDIS " + ItemUtils.getPlainName(item));
		String idStr = RedisAPI.getInstance().sync().hget(mPathIDBItemToID, ItemUtils.serializeItemStack(item));
		if (idStr == null || idStr.isEmpty()) {
			return null;
		}
		long id = Long.parseLong(idStr);
		saveToCache(id, item);
		return id;
	}

	private static @Nullable ItemStack fetchItemFromRedis(long id) {
		MMLog.info("FETCH ITEM FROM REDIS " + id);
		String mojangson = RedisAPI.getInstance().sync().hget(mPathIDBIDToItem, String.valueOf(id));
		if (mojangson == null || mojangson.isEmpty()) {
			return null;
		}
		ItemStack item = ItemUtils.parseItemStack(mojangson);
		saveToCache(id, item);
		return item;
	}

	private static void saveToCache(long id, ItemStack item) {
		MMLog.info("SAVING " + id + " - " + ItemUtils.getPlainName(item));
		mLocalCacheIDToItem.put(id, item.asOne());
		mLocalCacheItemToID.put(item.asOne(), id);
	}

	private static void resetCacheIfTooOld() {
		if (System.currentTimeMillis() > maxCacheAgeTimestamp) {
			MMLog.info("Reseting Market Item Index Cache");
			maxCacheAgeTimestamp = System.currentTimeMillis() + 1000 * 60 * 60; /*reset cache in 1h*/
			mLocalCacheIDToItem = new HashMap<>();
			mLocalCacheItemToID = new HashMap<>();
		}
	}
}
