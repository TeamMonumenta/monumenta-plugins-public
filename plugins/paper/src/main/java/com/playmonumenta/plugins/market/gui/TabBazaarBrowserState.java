package com.playmonumenta.plugins.market.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

public class TabBazaarBrowserState {
	protected int mSelectedFilter = 0;
	protected String mQuicksearchValue = "";
	protected int mSelectedSortByIndex = 0;
	protected boolean mSortByDesc = false;

	public TabBazaarBrowserState(@Nullable JsonElement element) {
		if (!(element instanceof JsonObject data)) {
			return;
		}

		if (data.get("selectedFilter") instanceof JsonPrimitive selectedFilterPrimitive && selectedFilterPrimitive.isNumber()) {
			mSelectedFilter = selectedFilterPrimitive.getAsInt();
		}

		if (data.get("quickSearchValue") instanceof JsonPrimitive quickSearchValuePrimitive && quickSearchValuePrimitive.isString()) {
			mQuicksearchValue = quickSearchValuePrimitive.getAsString();
		}

		if (data.get("selectedSortByIndex") instanceof JsonPrimitive selectedSortByIndexPrimitive && selectedSortByIndexPrimitive.isNumber()) {
			mSelectedSortByIndex = selectedSortByIndexPrimitive.getAsInt();
		}

		if (data.get("sortByDesc") instanceof JsonPrimitive sortByDescPrimitive && sortByDescPrimitive.isBoolean()) {
			mSortByDesc = sortByDescPrimitive.getAsBoolean();
		}
	}

	public JsonObject toJson() {
		JsonObject data = new JsonObject();

		data.addProperty("selectedFilter", mSelectedFilter);
		data.addProperty("quickSearchValue", mQuicksearchValue);
		data.addProperty("selectedSortByIndex", mSelectedSortByIndex);
		data.addProperty("sortByDesc", mSortByDesc);

		return data;
	}
}
