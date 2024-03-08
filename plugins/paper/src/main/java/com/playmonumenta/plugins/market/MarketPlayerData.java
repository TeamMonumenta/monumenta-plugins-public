package com.playmonumenta.plugins.market;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class MarketPlayerData {
	private final HashSet<Long> mOwnedListingIDList;

	public MarketPlayerData() {
		this.mOwnedListingIDList = new HashSet<>();
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
}
