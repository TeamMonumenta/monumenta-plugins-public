package com.playmonumenta.plugins.market;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class MarketFilter {

	public MarketFilter() {
		mHiddenComponents = new ArrayList<>();
		mComponents = new ArrayList<>();
	}

	boolean mStartWithActiveOnly = false;

	ArrayList<FilterComponent> mHiddenComponents;
	ArrayList<FilterComponent> mComponents;

	public MarketFilter startWithActiveOnly(boolean b) {
		mStartWithActiveOnly = b;
		return this;
	}

	public boolean startWithActiveOnly() {
		return mStartWithActiveOnly;
	}

	public Set<MarketListingIndex> getUsedIndexes() {
		TreeSet<MarketListingIndex> out = new TreeSet<>();
		for (FilterComponent comp : mHiddenComponents) {
			out.add(comp.mIndex);
		}
		for (FilterComponent comp : mComponents) {
			out.add(comp.mIndex);
		}
		return out;
	}

	public MarketFilter addComponent(Type type, MarketListingIndex index, String... values) {
		mComponents.add(new FilterComponent(type, index, values));
		return this;
	}

	public List<FilterComponent> getAllOptimisedComponents() {
		HashMap<MarketListingIndex, HashMap<Type, ArrayList<String>>> groupedComponents = new HashMap<>();
		ArrayList<FilterComponent> allComponents = new ArrayList<>();
		allComponents.addAll(mHiddenComponents);
		allComponents.addAll(mComponents);

		// group components together
		for (FilterComponent comp : allComponents) {
			HashMap<Type, ArrayList<String>> indexData = groupedComponents.getOrDefault(comp.mIndex, new HashMap<>());
			ArrayList<String> typeData = indexData.getOrDefault(comp.mType, new ArrayList<>());
			typeData.addAll(List.of(comp.mValues));
			indexData.put(comp.mType, typeData);
			groupedComponents.put(comp.mIndex, indexData);
		}

		// build new components, separate whitelist and blacklist, sorting by size of values checked
		TreeMap<Integer, ArrayList<FilterComponent>> whitelist = new TreeMap<>();
		TreeMap<Integer, ArrayList<FilterComponent>> blacklist = new TreeMap<>();
		for (Map.Entry<MarketListingIndex, HashMap<Type, ArrayList<String>>> entries : groupedComponents.entrySet()) {
			MarketListingIndex index = entries.getKey();
			for (Map.Entry<Type, ArrayList<String>> entry : entries.getValue().entrySet()) {
				Type type = entry.getKey();
				ArrayList<String> values = entry.getValue();
				FilterComponent component = new FilterComponent(type, index, values.toArray(new String[0]));
				if (type == Type.WHITELIST) {
					ArrayList<FilterComponent> list = whitelist.getOrDefault(values.size(), new ArrayList<>());
					list.add(component);
					whitelist.put(values.size(), list);
				} else {
					ArrayList<FilterComponent> list = blacklist.getOrDefault(values.size(), new ArrayList<>());
					list.add(component);
					blacklist.put(values.size(), list);
				}
			}
		}

		// build the final list with order whitelist first to blacklist second, with smallest size of values first
		List<FilterComponent> out = new ArrayList<>();
		for (ArrayList<FilterComponent> list : whitelist.values()) {
			out.addAll(list);
		}
		for (ArrayList<FilterComponent> list : blacklist.values()) {
			out.addAll(list);
		}

		return out;
	}

	public enum Type {
		BLACKLIST,
		WHITELIST
	}

	public static class FilterComponent {

		Type mType;
		MarketListingIndex mIndex;
		String[] mValues;

		public FilterComponent(Type type, MarketListingIndex index, String[] values) {
			this.mType = type;
			this.mIndex = index;
			this.mValues = values;
		}
	}

}
