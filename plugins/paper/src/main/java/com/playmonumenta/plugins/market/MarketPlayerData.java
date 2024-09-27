package com.playmonumenta.plugins.market;

import com.playmonumenta.plugins.market.filters.MarketFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class MarketPlayerData {
	private final HashSet<Long> mOwnedListingIDList;
	private List<MarketFilter> mPlayerFilters;
	private @Nullable MarketPlayerOptions mPlayerOptions = null;

	public MarketPlayerData() {
		this.mOwnedListingIDList = new HashSet<>();
		this.mPlayerFilters = getDefaultFiltersList();
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
}
