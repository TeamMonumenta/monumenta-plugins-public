package com.playmonumenta.plugins.market.filters;

import com.playmonumenta.plugins.market.MarketListingIndex;

public class Sorter {

	public static final Sorter DEFAULT_SORTER = new Sorter(MarketListingIndex.ACTIVE_LISTINGS, true);

	boolean mDescendingOrder;

	MarketListingIndex mIndexField;

	public Sorter(MarketListingIndex idx, boolean isDesc) {
		mDescendingOrder = isDesc;
		mIndexField = idx;
	}

	public boolean isdescendingOrder() {
		return mDescendingOrder;
	}

	public void setdescendingOrder(boolean mDescendingOrder) {
		this.mDescendingOrder = mDescendingOrder;
	}

	public MarketListingIndex getIndexField() {
		return mIndexField;
	}

	public void setIndexField(MarketListingIndex mIndexField) {
		this.mIndexField = mIndexField;
	}
}
