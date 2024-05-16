package com.playmonumenta.plugins.market.filters;

import com.playmonumenta.plugins.market.MarketListingIndex;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class FilterComponent {

	public MarketListingIndex mField;
	public Comparator mComparator;
	public List<String> mValuesList;

	public FilterComponent(MarketListingIndex field, Comparator comparator, @Nullable List<String> values) {
		this.mField = field;
		this.mComparator = comparator;
		this.mValuesList = new ArrayList<>();
		if (values != null) {
			this.mValuesList.addAll(values);
		}
	}

	public MarketListingIndex getTargetIndex() {
		return this.mField;
	}

	public void addValue(String value) {
		if (this.mValuesList == null) {
			this.mValuesList = new ArrayList<>();
		}
		this.mValuesList.add(value);
	}

	public void removeValue(String value) {
		if (this.mValuesList == null) {
			this.mValuesList = new ArrayList<>();
		}
		this.mValuesList.remove(value);
	}
}
