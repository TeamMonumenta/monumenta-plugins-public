package com.playmonumenta.plugins.market.filters;

import com.playmonumenta.plugins.market.MarketListingIndex;
import java.util.List;

public class FilterComponent {

	public MarketListingIndex mField;
	public Comparator mComparator;
	public List<String> mValuesList;

	public FilterComponent(MarketListingIndex field, Comparator comparator, List<String> values) {
		this.mField = field;
		this.mComparator = comparator;
		this.mValuesList = values;
	}

	public MarketListingIndex getTargetIndex() {
		return this.mField;
	}
}
