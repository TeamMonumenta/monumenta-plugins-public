package com.playmonumenta.plugins.market;

import com.google.gson.Gson;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.market.filters.Comparator;
import com.playmonumenta.plugins.market.filters.FilterComponent;
import com.playmonumenta.plugins.market.filters.MarketFilter;
import com.playmonumenta.redissync.ConfigAPI;
import com.playmonumenta.redissync.RedisAPI;
import io.lettuce.core.KeyValue;
import io.lettuce.core.ScriptOutputType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class MarketRedisManager {

	private static final String pathListingCurrentID = ConfigAPI.getServerDomain() + ":market:listingCurrentID";
	private static final String pathListingHashMap = ConfigAPI.getServerDomain() + ":market:listings";

	public static String getListingCurrentIDRedisPath() {
		return pathListingCurrentID;
	}

	public static String getListingsRedisPath() {
		return pathListingHashMap;
	}

	// creates a new listing in redis
	// any validity checks should be done before the call to this method
	// returns a null listing if the creation failed
	@Nullable
	public static MarketListing createAndAddNewListing(Player player, ItemStack itemToSell, int itemsPerTrade, int amountOfTrades, int pricePerTrade, ItemStack currencyItemStack) {

		// increment the unique listingId in redis, and get it
		long listingID = getNextListingID();
		long itemToSellDatabaseID = RedisItemDatabase.getIDFromItemStack(itemToSell);
		long currencyDatabaseID = RedisItemDatabase.getIDFromItemStack(currencyItemStack);

		// build the listing
		MarketListing listing = new MarketListing(listingID, MarketListingType.BAZAAR, itemToSellDatabaseID, amountOfTrades, itemsPerTrade, pricePerTrade, currencyDatabaseID, player);

		// push the listing in redis
		boolean updateOk = createListing(listing);
		if (!updateOk) {
			return null;
		}

		return listing;
	}

	private static boolean createListing(MarketListing listing) {
		boolean updateOk = updateListing(listing);
		if (!updateOk) {
			return false;
		}

		// the index updates do not need to be in sync with the trading,
		// as such, to make the creation/update of listing faster, we do index update later
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			for (MarketListingIndex index : MarketListingIndex.values()) {
				index.addListingToIndexIfMatching(listing);
			}
		});

		return true;
	}

	public static Long getNextListingID() {
		return RedisAPI.getInstance().sync().incr(getListingCurrentIDRedisPath());
	}

	/**
	 * Updates (or creates) a listing in redis. Does not check if the listing is currently locked!
	 */
	private static boolean updateListing(MarketListing listing) {

		String json = listing.toJsonString();
		String id = String.valueOf(listing.getId());
		RedisAPI.getInstance().sync().hset(pathListingHashMap, id, json);

		return true;
	}

	private static @Nullable String mAtomicUpdateScriptHash = null;

	/**
	 * Performs an atomic compare-and-swap operation on a listing.
	 *
	 * @param expectedListing The raw value of the listing before any changes that is expected to be present in Redis
	 * @param newListing      The new value of the listing with changes
	 * @return Whether the swap was successful
	 */
	public static boolean atomicCompareAndSwapListing(String expectedListing, MarketListing newListing) {
		if (mAtomicUpdateScriptHash == null) {
			mAtomicUpdateScriptHash = RedisAPI.getInstance().sync().scriptLoad("""
				local map = KEYS[1];
				local id = ARGV[1];
				local expectedJson = ARGV[2];
				local newJson = ARGV[3];
				if redis.call('HGET', map, id) == expectedJson then
				    redis.call('HSET', map, id, newJson);
				    return true;
				end;
				return false;
				""");
		}

		String newJson = newListing.toJsonString();
		String id = String.valueOf(newListing.getId());

		return RedisAPI.getInstance().sync().evalsha(mAtomicUpdateScriptHash, ScriptOutputType.BOOLEAN, new String[] {pathListingHashMap}, id, expectedListing, newJson);

	}

	/**
	 * Atomically edits a listing and returns it. Waits up to a second for an existing lock to be released.
	 *
	 * @return The updated listing or {@code null} if the listing could not be edited
	 */
	public static @Nullable MarketListing atomicEditListing(long id, Consumer<MarketListing> updater) {
		EditedListing editedListing = atomicEditListing2(id, updater);
		return editedListing != null ? editedListing.mAfterEdit : null;
	}

	public record EditedListing(MarketListing mBeforeEdit, MarketListing mAfterEdit) {
	}

	/**
	 * Same as {@link #atomicEditListing(long, Consumer)}, but returns both the listing from just before and after editing it.
	 */
	public static @Nullable EditedListing atomicEditListing2(long id, Consumer<MarketListing> updater) {

		// try up to 5 times to edit the listing
		for (int i = 0; i < 5; i++) {
			String expectedListing = getListingRaw(id);
			if (expectedListing == null) {
				return null;
			}
			MarketListing newListing = parseListing(expectedListing);
			if (newListing.getEditLocked() != null) {
				// locked by someone else, try again shortly, but a maximum of twice
				if (i >= 2) {
					return null;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					return null;
				}
				continue;
			}
			updater.accept(newListing);
			if (atomicCompareAndSwapListing(expectedListing, newListing)) {
				return new EditedListing(parseListing(expectedListing), newListing);
			}
		}

		return null;
	}

	public static void removeEditLockAndUpdateListing(MarketListing listing) {
		listing.setEditLocked(null);
		updateListing(listing);
	}

	public static MarketListing getListing(long id) {
		return parseListing(getListingRaw(id));
	}

	public static MarketListing parseListing(String json) {
		return new Gson().fromJson(json, MarketListing.class);
	}

	public static String getListingRaw(long id) {
		return RedisAPI.getInstance().sync().hget(pathListingHashMap, String.valueOf(id));
	}

	// proxy for getListings(String... ids)
	public static List<MarketListing> getListings(Long... ids) {
		String[] idsStr = new String[ids.length];
		int i = 0;
		for (Long id : ids) {
			idsStr[i++] = String.valueOf(id);
		}
		List<MarketListing> out = getListings(idsStr);
		return out == null ? new ArrayList<>() : out;
	}

	public static @Nullable List<MarketListing> getListings(String... ids) {
		Gson gson = new Gson();
		List<KeyValue<String, String>> jsons = RedisAPI.getInstance().sync().hmget(pathListingHashMap, ids);
		ArrayList<MarketListing> out = new ArrayList<>();
		if (jsons == null) {
			return null;
		}
		for (KeyValue<String, String> entry : jsons) {
			String json = entry.getValueOrElse("");
			if (json != null) {
				MarketListing listing = gson.fromJson(json, MarketListing.class);
				if (listing != null && listing.isNotUsable()) {
					listing = new MarketListing(Long.parseLong(entry.getKey()));
				}
				out.add(listing);
			}
		}
		return out;
	}

	public static List<Long> getAllListingsIds(boolean sorted) {
		List<Long> out = new ArrayList<>();
		List<String> lst = RedisAPI.getInstance().sync().hkeys(pathListingHashMap);
		for (String l : lst) {
			out.add(Long.parseLong(l));
		}
		if (sorted) {
			Collections.sort(out);
		}
		return out;
	}

	public static void deleteListing(MarketListing listing) {
		RedisAPI.getInstance().sync().hdel(pathListingHashMap, String.valueOf(listing.getId()));

		// the index updates do not need to be in sync with the trading,
		// as such, to make the creation/update of listing faster, we do index update later
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			for (MarketListingIndex index : MarketListingIndex.values()) {
				index.removeListingFromIndex(listing);
			}
		});
	}

	public static List<Long> getAllListingsIdsMatchingFilter(MarketFilter filter) {

		List<Long> out;
		if (filter.startWithActiveOnly()) {
			out = MarketListingIndex.ACTIVE_LISTINGS.getListingsFromIndex(true);
		} else {
			out = getAllListingsIds(true);
		}

		Set<MarketListingIndex> usedIndexes = filter.getUsedIndexes();
		HashMap<MarketListingIndex, Map<String, List<Long>>> indexDatas = new HashMap<>();
		for (MarketListingIndex index : usedIndexes) {
			indexDatas.put(index, index.getListingsMapFromIndex(false));
		}

		for (FilterComponent comp : filter.getAllOptimisedComponents()) {
			if (out.isEmpty()) {
				break;
			}

			// find ids matching components
			Map<String, List<Long>> indexData = indexDatas.getOrDefault(comp.getTargetIndex(), new TreeMap<>());
			TreeSet<Long> idsToFilter = new TreeSet<>();
			for (String value : comp.mValuesList) {
				String regex = value.toLowerCase(Locale.ROOT).replace("*", ".*");
				List<String> matchingValues = new ArrayList<>();
				for (String indexValue : indexData.keySet()) {
					if (indexValue.toLowerCase(Locale.ROOT).matches(regex)) {
						matchingValues.add(indexValue);
					}
				}
				for (String matchingValue : matchingValues) {
					idsToFilter.addAll(indexData.getOrDefault(matchingValue, Collections.emptyList()));
				}
			}

			List<Long> newOut = new ArrayList<>();

			// do the actual filtering
			if (comp.mComparator.equals(Comparator.BLACKLIST)) {
				for (Long id : out) {
					if (!idsToFilter.contains(id)) {
						newOut.add(id);
					}
				}
			} else if (comp.mComparator.equals(Comparator.WHITELIST)) {
				for (Long id : out) {
					if (idsToFilter.contains(id)) {
						newOut.add(id);
					}
				}
			}

			out = newOut;
		}

		// order by

		// special faster algorithm for ACTIVE_LISTING index
		if (filter.getSorter().getIndexField() == MarketListingIndex.ACTIVE_LISTINGS) {
			Collections.sort(out);
			if (filter.getSorter().isdescendingOrder()) {
				Collections.reverse(out);
			}
		} else {
			// get the ordering index values
			Map<String, List<Long>> idxValues = indexDatas.getOrDefault(filter.getSorter().getIndexField(), new HashMap<>());
			// build an id->idxvalue map
			Map<Long, String> idValues = new HashMap<>();
			for (Map.Entry<String, List<Long>> entry : idxValues.entrySet()) {
				String key = entry.getKey();
				for (Long id : entry.getValue()) {
					idValues.put(id, key);
				}
			}
			// sort 'out' according to the id->idxvalue map, and the order
			if (filter.getSorter().isdescendingOrder()) {
				out.sort((id1, id2) -> {
					return idValues.getOrDefault(id1, "").compareTo(idValues.getOrDefault(id2, "")) * -1;
				});
			} else {
				out.sort(java.util.Comparator.comparing(id -> idValues.getOrDefault(id, "")));
			}
		}

		return out;
	}
}
