package com.playmonumenta.plugins.market;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.market.filters.MarketFilter;
import com.playmonumenta.plugins.market.gui.TabBazaarBrowserState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class MarketPlayerData {
	private final HashSet<Long> mOwnedListingIDList;
	private List<MarketFilter> mPlayerFilters;
	private @Nullable MarketPlayerOptions mPlayerOptions = null;
	private TabBazaarBrowserState mTabBazaarBrowserState;

	public MarketPlayerData() {
		this.mOwnedListingIDList = new HashSet<>();
		this.mPlayerFilters = getDefaultFiltersList();
		this.mTabBazaarBrowserState = new TabBazaarBrowserState(null);
	}

	public static MarketPlayerData fromJson(@Nullable JsonObject data) {
		MarketPlayerData marketPlayerData = new MarketPlayerData();
		if (data == null) {
			return marketPlayerData;
		}

		Gson gson = new Gson();

		// OWNERSHIP
		JsonArray ownershipArray = data.getAsJsonArray("playerListings");
		for (JsonElement elem : ownershipArray) {
			marketPlayerData.addListingIDToPlayer(elem.getAsString());
		}

		// FILTERS
		JsonArray filtersArray = data.getAsJsonArray("playerFilters");
		if (filtersArray == null) {
			marketPlayerData.resetPlayerFiltersList();
		} else {
			ArrayList<MarketFilter> filters = new ArrayList<>();
			for (JsonElement filterObj : filtersArray.asList()) {
				MarketFilter filter = gson.fromJson(filterObj, MarketFilter.class);
				filters.add(filter);
			}
			marketPlayerData.setPlayerFiltersList(filters);
		}

		// OPTIONS
		JsonElement optionsElement = data.get("playerOptions");
		if (optionsElement == null) {
			marketPlayerData.setPlayerOptions(new MarketPlayerOptions());
		} else {
			marketPlayerData.setPlayerOptions(gson.fromJson(optionsElement, MarketPlayerOptions.class));
		}

		// TAB STATE
		marketPlayerData.mTabBazaarBrowserState = new TabBazaarBrowserState(data.get("tabBazaarBrowserState"));

		return marketPlayerData;
	}

	public JsonObject toJson() {
		JsonObject data = new JsonObject();
		Gson gson = new Gson();

		// OWNERSHIP
		JsonArray ownershipArray = new JsonArray();
		for (Long id : getOwnedListingsIDList()) {
			if (id != null) {
				ownershipArray.add(String.valueOf(id));
			}
		}
		data.add("playerListings", ownershipArray);

		// FILTERS
		JsonArray filtersArray = new JsonArray();
		for (MarketFilter filter : getPlayerFiltersList()) {
			JsonElement elem = gson.toJsonTree(filter);
			filtersArray.add(elem);
		}
		data.add("playerFilters", filtersArray);

		// OPTIONS
		JsonElement optionsElement = gson.toJsonTree(getPlayerOptions());
		data.add("playerOptions", optionsElement);

		// TAB STATE
		data.add("tabBazaarBrowserState", this.mTabBazaarBrowserState.toJson());

		return data;
	}

	public void addListingIDToPlayer(String idStr) {
		addListingIDToPlayer(Long.parseLong(idStr));
	}

	public void addListingIDToPlayer(long id) {
		mOwnedListingIDList.add(id);
	}

	public List<Long> getOwnedListingsIDList() {
		List<Long> list = new ArrayList<>(mOwnedListingIDList);
		Collections.sort(list);
		return list;
	}

	public void removeListingIDFromPlayer(long listingID) {
		mOwnedListingIDList.remove(listingID);
	}

	public List<MarketFilter> getPlayerFiltersList() {
		return new ArrayList<>(this.mPlayerFilters);
	}

	public void setPlayerFiltersList(List<MarketFilter> playerFilters) {
		this.mPlayerFilters = new ArrayList<>(playerFilters);
	}

	public void resetPlayerFiltersList() {
		this.mPlayerFilters = getDefaultFiltersList();
	}

	private List<MarketFilter> getDefaultFiltersList() {
		return new ArrayList<>(List.of(
			MarketFilter.PREMADE_R1_ONLY,
			MarketFilter.PREMADE_R2_ONLY,
			MarketFilter.PREMADE_R3_ONLY,
			MarketFilter.PREMADE_REGIONLESS,
			MarketFilter.PREMADE_TIERED,
			MarketFilter.PREMADE_UNTIERED,
			MarketFilter.PREMADE_EQUIPABLE,
			MarketFilter.PREMADE_NOTEQUIPABLE
		));
	}

	public @Nullable MarketPlayerOptions getPlayerOptions() {
		return mPlayerOptions;
	}

	public void setPlayerOptions(MarketPlayerOptions playerOptions) {
		this.mPlayerOptions = playerOptions;
	}

	public TabBazaarBrowserState getTabBazaarBrowserState() {
		return mTabBazaarBrowserState;
	}
}
