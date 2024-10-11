package com.playmonumenta.plugins.market;

import com.google.gson.Gson;
import com.playmonumenta.plugins.market.filters.ComponentConfig;
import com.playmonumenta.redissync.ConfigAPI;
import com.playmonumenta.redissync.RedisAPI;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import org.apache.commons.lang3.ArrayUtils;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public enum MarketListingIndex {

	// use this class if we ever want to add filters or sorting

	ACTIVE_LISTINGS("ActiveListings",
		true,
		(MarketListing l) -> String.valueOf(l.getId()),
		(MarketListing l) -> !l.getPurchasableStatus(1).isError(),
		false,
		Material.BLAZE_POWDER,
		null
	),

	LISTING_TYPE("ListingType",
		false,
		(MarketListing l) -> String.valueOf(l.getListingType()),
		(MarketListing l) -> l.getListingType() != null,
		false,
		Material.BOOK,
		null
	),

	REGION("Region",
		false,
		(MarketListing l) -> String.valueOf(l.getRegion()),
		(MarketListing l) -> l.getRegion() != null,
		true,
		Material.GRASS_BLOCK,
		ComponentConfig.REGION_CONFIG
	),

	LOCATION("Location",
		false,
		(MarketListing l) -> String.valueOf(l.getLocation()),
		(MarketListing l) -> l.getLocation() != null,
		true,
		Material.COMPASS,
		ComponentConfig.LOCATION_CONFIG
	),

	NAME("Name",
		false,
		(MarketListing l) -> l.getItemName(),
		(MarketListing l) -> l.getItemName() != null,
		true,
		Material.OAK_SIGN,
		null
	),

	CURRENCY("Currency",
		false,
		(MarketListing l) -> l.getCurrencyName(),
		(MarketListing l) -> !l.getCurrencyName().equals("ERROR"),
		true,
		Material.EXPERIENCE_BOTTLE,
		ComponentConfig.CURRENCY_CONFIG
	),

	TYPE("Type",
		false,
		(MarketListing l) -> String.valueOf(l.getItemType()),
		(MarketListing l) -> l.getItemType() != null,
		true,
		Material.FLOWER_BANNER_PATTERN,
		ComponentConfig.TYPE_CONFIG
	),

	TIER("Tier",
		false,
		(MarketListing l) -> String.valueOf(l.getItemTier()),
		(MarketListing l) -> l.getItemTier() != null,
		true,
		Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
		ComponentConfig.TIER_CONFIG
	),

	;
	final String mRedisPath;
	final boolean mSortedFetch;
	final Function<MarketListing, String> mGetKeyMethod;
	final Function<MarketListing, Boolean> mMatchMethod;
	final boolean mPlayerSelectable;
	final Material mDisplayIconBaseMaterial;
	final @Nullable Map<String, ComponentConfig.ComponentConfigObject> mComponentConfig;

	MarketListingIndex(String redisID, boolean sortedFetch, Function<MarketListing, String> getKeyMethod, Function<MarketListing, Boolean> matchMethod, boolean playerSelectable, Material displayIconBaseMaterial, @Nullable Map<String, ComponentConfig.ComponentConfigObject> componentConfig) {
		this.mRedisPath = ConfigAPI.getServerDomain() + ":market:index:" + redisID;
		this.mSortedFetch = sortedFetch;
		this.mGetKeyMethod = getKeyMethod;
		this.mMatchMethod = matchMethod;
		this.mPlayerSelectable = playerSelectable;
		this.mDisplayIconBaseMaterial = displayIconBaseMaterial;
		this.mComponentConfig = componentConfig;
	}

	public void removeListingFromIndex(MarketListing listing) {
		// Special case for ACTIVE_LISTINGS, which is just a simple list, and not a hashmap
		// a simpler, but unique algorithm needs to be used
		if (this == ACTIVE_LISTINGS) {
			RedisAPI.getInstance().sync().lrem(mRedisPath, 0, String.valueOf(listing.getId()));
			return;
		}

		String key = this.mGetKeyMethod.apply(listing);

		// get the current values of the index, at listing key
		String listingIdList = RedisAPI.getInstance().sync().hget(mRedisPath, key);
		// remove the listing ID to the list
		listingIdList = listingIdList.replace(String.valueOf(listing.getId()), "").replace(",,", ",");
		// push the new value
		RedisAPI.getInstance().sync().hset(mRedisPath, key, listingIdList);
	}

	public List<Long> getListingsFromIndex(boolean descOrder) {
		// Special case for ACTIVE_LISTINGS, which is just a simple list, and not a hashmap
		// a simpler, but unique algorithm needs to be used
		if (this == ACTIVE_LISTINGS) {
			return getActiveListings(descOrder);
		}

		return new ArrayList<>();
	}

	public Map<String, List<Long>> getListingsMapFromIndex(boolean descOrder) {

		TreeMap<String, List<Long>> out = new TreeMap<>();

		// Special case for ACTIVE_LISTINGS, which is just a simple list, and not a hashmap
		// a simpler, but unique algorithm needs to be used
		if (this == ACTIVE_LISTINGS) {
			out.put("ACTIVE_LISTINGS", getActiveListings(descOrder));
			return out;
		}

		Map<String, String> indexContents = RedisAPI.getInstance().sync().hgetall(mRedisPath);
		for (Map.Entry<String, String> entry : indexContents.entrySet()) {
			List<Long> list = new ArrayList<>();
			String[] values = entry.getValue().split(",");
			for (String value : values) {
				if (value != null && !value.isEmpty() && value.matches("[0-9]*")) {
					list.add(Long.parseLong(value));
				}
			}
			list.sort(descOrder ? Comparator.reverseOrder() : Comparator.naturalOrder());
			out.put(entry.getKey(), list);
		}

		return out;
	}

	public List<String> getListingsKeysFromIndex(boolean descOrder) {
		List<String> out = new ArrayList<>();

		// Special case for ACTIVE_LISTINGS, which is just a simple list, and not a hashmap
		// a simpler, but unique algorithm needs to be used
		if (this == ACTIVE_LISTINGS) {
			out.add("ACTIVE_LISTINGS");
			return out;
		}

		out = RedisAPI.getInstance().sync().hkeys(mRedisPath);
		out.sort(descOrder ? Comparator.reverseOrder() : Comparator.naturalOrder());
		return out;
	}

	private List<Long> getActiveListings(boolean descOrder) {
		List<String> activeUnfilteredListings = RedisAPI.getInstance().sync().lrange(mRedisPath, 0, -1);
		List<Long> activeFilteredListings = new ArrayList<>();
		for (String listingID : activeUnfilteredListings) {
			activeFilteredListings.add(Long.parseLong(listingID));
		}
		if (mSortedFetch) {
			Collections.sort(activeFilteredListings);
		}
		if (descOrder) {
			Collections.reverse(activeFilteredListings); // not gonna touch this one because idk how the logic of this method works
		}
		return activeFilteredListings;
	}


	public void addListingToIndexIfMatching(MarketListing listing) {

		// only add to listing if said listing matches the index conditions
		if (this.mMatchMethod.apply(listing)) {
			this.addListingToIndex(listing);
		}

	}

	public void addListingToIndex(MarketListing listing) {
		String key = this.mGetKeyMethod.apply(listing);

		// Special case for ACTIVE_LISTINGS, which is just a simple list, and not a hashmap
		// a simpler, but unique algorithm needs to be used
		if (this == ACTIVE_LISTINGS) {
			RedisAPI.getInstance().sync().lpush(mRedisPath, key);
			return;
		}

		// get the current values of the index, at listing key
		String listingIdList = RedisAPI.getInstance().sync().hget(mRedisPath, key);
		// add the new listing ID to the list
		if (listingIdList != null && !listingIdList.isEmpty()) {
			listingIdList += "," + listing.getId();
		} else {
			listingIdList = "" + listing.getId();
		}
		// push the new value
		RedisAPI.getInstance().sync().hset(mRedisPath, key, listingIdList);
	}


	public static String dumpAllListingsContents() {

		StringBuilder sb = new StringBuilder();

		for (MarketListingIndex index : values()) {
			sb.append("For ").append(index.toString()).append(":\n");
			sb.append(index.dumpIndexContents());
		}

		return sb.toString();
	}

	private String dumpIndexContents() {
		StringBuilder sb = new StringBuilder();
		if (this == ACTIVE_LISTINGS) {
			List<String> lst = RedisAPI.getInstance().sync().lrange(mRedisPath, 0, -1);
			sb.append(Arrays.toString(lst.toArray())).append("\n");
			return sb.toString();
		}

		Map<String, String> indexContents = RedisAPI.getInstance().sync().hgetall(mRedisPath);
		for (Map.Entry<String, String> entry : indexContents.entrySet()) {
			sb.append("  ").append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
		}
		return sb.toString();

	}

	public static void resyncAllIndexes() {

		// init
		HashMap<MarketListingIndex, HashMap<String, ArrayList<Long>>> indexValuesMap = new HashMap<>();
		for (MarketListingIndex index : values()) {
			indexValuesMap.put(index, new HashMap<>());
		}


		// fetch and store data
		ScanCursor cursor = ScanCursor.INITIAL;
		while (!cursor.isFinished()) {
			// get redis data
			MapScanCursor<String, String> hscanResult = RedisAPI.getInstance().sync().hscan(MarketRedisManager.getListingsRedisPath(), cursor, new ScanArgs().limit(50));
			cursor = ScanCursor.of(hscanResult.getCursor());
			cursor.setFinished(hscanResult.isFinished());

			// convert to listings
			Map<String, String> map = hscanResult.getMap();
			for (String json : map.values()) {
				MarketListing listing = new Gson().fromJson(json, MarketListing.class);
				// maybe some indexed fields changed their definition. recalculate, and update on the redis side if something got changed
				if (listing.getEditLocked() == null) {
					MarketListing updatedListing = new MarketListing(listing);
					updatedListing.recalculateListingIndexValues();
					if (!listing.isSimilar(updatedListing)) {
						if (MarketRedisManager.atomicCompareAndSwapListing(json, updatedListing)) {
							listing = updatedListing;
						}
					}
				}

				// store index data from listing
				for (MarketListingIndex index : values()) {

					// skip if listing does not match index
					if (!index.mMatchMethod.apply(listing)) {
						continue;
					}

					HashMap<String, ArrayList<Long>> indexMap = indexValuesMap.computeIfAbsent(index, k -> new HashMap<>());

					String key = index.mGetKeyMethod.apply(listing);
					if (index == ACTIVE_LISTINGS) {
						key = "ALL";
					}
					ArrayList<Long> lst = indexMap.getOrDefault(key, new ArrayList<>());
					lst.add(listing.getId());
					indexMap.put(key, lst);
				}
			}

			// now, every new index values should be in local memory
			// push it to redis

			for (MarketListingIndex index : values()) {
				// delete the old values
				RedisAPI.getInstance().sync().del(index.mRedisPath);

				// push the new values
				HashMap<String, ArrayList<Long>> indexValues = indexValuesMap.getOrDefault(index, new HashMap<>());

				// special case for active_listings
				if (index == ACTIVE_LISTINGS) {
					ArrayList<Long> values = indexValues.get("ALL");
					if (values != null) {
						values.sort(Collections.reverseOrder());
						String[] array = new String[values.size()];
						for (int i = 0; i < values.size(); i++) {
							array[i] = String.valueOf(values.get(i));
						}
						RedisAPI.getInstance().sync().lpush(index.mRedisPath, array);
					}
					continue;
				}

				for (String key : indexValues.keySet()) {
					ArrayList<Long> values = indexValues.get(key);
					if (values != null) {
						values.sort(Collections.reverseOrder());
						String valuesStr = ArrayUtils.toString(values).replace(" ", "");
						valuesStr = valuesStr.substring(1, valuesStr.length() - 1);
						RedisAPI.getInstance().sync().hset(index.mRedisPath, key, valuesStr);
					}
				}

			}

		}
	}

	public Material getDisplayIconMaterial() {
		return this.mDisplayIconBaseMaterial;
	}

	public boolean isPlayerSelectable() {
		return mPlayerSelectable;
	}

	public static List<MarketListingIndex> getAllPlayerSelectable() {
		List<MarketListingIndex> out = new ArrayList<>();

		for (MarketListingIndex idx : values()) {
			if (idx.isPlayerSelectable()) {
				out.add(idx);
			}
		}

		return out;
	}


	public static List<MarketListingIndex> getAllPlayerSortable() {
		List<MarketListingIndex> out = new ArrayList<>();
		out.add(ACTIVE_LISTINGS);
		out.addAll(getAllPlayerSelectable());
		return out;
	}

	public @Nullable Map<String, ComponentConfig.ComponentConfigObject> getComponentConfig() {
		return mComponentConfig;
	}
}
