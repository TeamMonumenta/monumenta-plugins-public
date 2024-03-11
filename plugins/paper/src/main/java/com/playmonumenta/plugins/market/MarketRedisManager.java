package com.playmonumenta.plugins.market;

import com.google.gson.Gson;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.redissync.ConfigAPI;
import com.playmonumenta.redissync.RedisAPI;
import io.lettuce.core.KeyValue;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
	public static MarketListing createAndAddNewListing(Player player, ItemStack itemToSell, int amountToSell, int pricePerItemAmount, ItemStack currencyItemStack) {

		// increment the unique listingId in redis, and get it
		long listingID = getNextListingID();
		long itemToSellDatabaseID = MarketItemDatabase.getIDFromItemStack(itemToSell);
		long currencyDatabaseID = MarketItemDatabase.getIDFromItemStack(currencyItemStack);

		// build the listing
		MarketListing listing = new MarketListing(listingID, itemToSellDatabaseID, amountToSell, pricePerItemAmount, currencyDatabaseID, player);

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

	// updates (or creates) a listing in redis
	public static boolean updateListing(MarketListing listing) {

		String json = listing.toJsonString();
		String id = String.valueOf(listing.getId());
		RedisAPI.getInstance().sync().hset(pathListingHashMap, id, json);

		return true;
	}

	public static boolean updateListingSafe(Player player, MarketListing oldListing, MarketListing newListing) {
		long startTimestamp = System.currentTimeMillis();
		String editLockedString = player.getName() + "-" + startTimestamp;
		// fetch the listing
		MarketListing tempListing = getListing(oldListing.getId());
		// we expect the fetched listing to exactly equal the listing before update, and that its not editlocked
		if (!tempListing.isSimilar(oldListing)) {
			player.sendMessage(Component.text("Update was not possible: listing is different than expected", NamedTextColor.RED));
			return false;
		}
		if (tempListing.getEditLocked() != null) {
			player.sendMessage(Component.text("Update was not possible: listing is editlocked by: " + tempListing.getEditLocked(), NamedTextColor.RED));
			return false;
		}
		// editlock the listing
		tempListing.setEditLocked(editLockedString);
		updateListing(tempListing);
		// refetch, to verify that It's still editlocked by the same player
		MarketListing tempListing2 = getListing(oldListing.getId());
		if (!tempListing2.isSimilar(tempListing)) {
			player.sendMessage(Component.text("Update was not possible: value post-locking differs: " + tempListing2.getEditLocked(), NamedTextColor.RED));
			return false;
		}
		// push the new listings, with the same editlocked
		MarketListing tempListing3 = new MarketListing(newListing);
		tempListing3.setEditLocked(editLockedString);
		updateListing(tempListing3);
		// refetch, to verify that It's still editlocked by the same player
		MarketListing tempListing4 = getListing(oldListing.getId());
		if (!tempListing4.isSimilar(tempListing3)) {
			player.sendMessage(Component.text("Update was not possible: listing post-update is different than expected: " + tempListing2.getEditLocked(), NamedTextColor.RED));
			return false;
		}
		// push the new listing, without the editlock
		updateListing(newListing);
		return true;
	}

	public static MarketListing getListing(long id) {
		String json = getListingRaw(id);
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

	// proxy for getListings(String... ids)
	public static List<MarketListing> getListings(List<Long> ids) {
		return getListings(ids.toArray(new Long[0]));
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

	public static List<Long> getAllListingsIds() {
		List<Long> out = new ArrayList<>();
		List<String> lst = RedisAPI.getInstance().sync().hkeys(pathListingHashMap);
		for (String l : lst) {
			out.add(Long.parseLong(l));
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
}
