package com.playmonumenta.plugins.market.filters;

import com.playmonumenta.plugins.itemstats.enums.ItemType;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.market.MarketListingIndex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jetbrains.annotations.Nullable;

public class MarketFilter {

	public static MarketFilter EMPTY_FILTER = new MarketFilter("Default: No filter", null);
	public static MarketFilter PREMADE_R1_ONLY = new MarketFilter("Premade: King's Valley", List.of(new FilterComponent(MarketListingIndex.REGION, Comparator.WHITELIST, List.of(Region.VALLEY.toString()))));

	public static MarketFilter PREMADE_R2_ONLY = new MarketFilter("Premade: Celsian Isles", List.of(new FilterComponent(MarketListingIndex.REGION, Comparator.WHITELIST, List.of(Region.ISLES.toString()))));

	public static MarketFilter PREMADE_R3_ONLY = new MarketFilter("Premade: Architect's Ring", List.of(new FilterComponent(MarketListingIndex.REGION, Comparator.WHITELIST, List.of(Region.RING.toString()))));

	public static MarketFilter PREMADE_REGIONLESS = new MarketFilter("Premade: No region", List.of(new FilterComponent(MarketListingIndex.REGION, Comparator.BLACKLIST, List.of(Region.VALLEY.toString(), Region.ISLES.toString(), Region.RING.toString()))));

	public static MarketFilter PREMADE_TIERED = new MarketFilter("Premade: Tiered only", List.of(new FilterComponent(MarketListingIndex.TIER, Comparator.BLACKLIST, List.of(Tier.NONE.toString()))));

	public static MarketFilter PREMADE_UNTIERED = new MarketFilter("Premade: Untiered only", List.of(new FilterComponent(MarketListingIndex.TIER, Comparator.WHITELIST, List.of(Tier.NONE.toString()))));

	public static MarketFilter PREMADE_EQUIPABLE = new MarketFilter("Premade: Equipable (armor+offhand)", List.of(new FilterComponent(MarketListingIndex.TYPE, Comparator.WHITELIST, List.of(ItemType.HELMET.toString(), ItemType.CHESTPLATE.toString(), ItemType.LEGGINGS.toString(), ItemType.BOOTS.toString(), ItemType.OFFHAND.toString(), ItemType.SHIELD.toString()))));

	public static MarketFilter PREMADE_NOTEQUIPABLE = new MarketFilter("Premade: Not Equipable", List.of(new FilterComponent(MarketListingIndex.TYPE, Comparator.BLACKLIST, List.of(ItemType.HELMET.toString(), ItemType.CHESTPLATE.toString(), ItemType.LEGGINGS.toString(), ItemType.BOOTS.toString(), ItemType.OFFHAND.toString(), ItemType.SHIELD.toString()))));

	public MarketFilter() {
		mComponents = new ArrayList<>();
		mSorter = Sorter.DEFAULT_SORTER;
	}

	public MarketFilter(@Nullable String displayName, @Nullable List<FilterComponent> components) {
		if (components != null) {
			mComponents = new ArrayList<>(components);
		}
		mDisplayName = displayName;
		mSorter = Sorter.DEFAULT_SORTER;
	}

	public MarketFilter(@Nullable String displayName, @Nullable List<FilterComponent> components, @Nullable Sorter sorter) {
		if (components != null) {
			mComponents = new ArrayList<>(components);
		}
		mDisplayName = displayName;
		if (sorter == null) {
			mSorter = Sorter.DEFAULT_SORTER;
		} else {
			mSorter = sorter;
		}

	}

	boolean mStartWithActiveOnly = false;
	@Nullable List<FilterComponent> mComponents = null;
	@Nullable String mDisplayName = null;

	Sorter mSorter;

	public static MarketFilter mergeOf(MarketFilter filterA, MarketFilter filterB) {
		return new MarketFilter()
			.startWithActiveOnly(filterA.startWithActiveOnly() || filterB.startWithActiveOnly())
			.setDisplayName(filterB.getDisplayName())
			.addComponents(filterA.getComponents())
			.addComponents(filterB.getComponents())
			.setSorter(filterB.getSorter());
	}

	public MarketFilter startWithActiveOnly(boolean b) {
		mStartWithActiveOnly = b;
		return this;
	}

	public boolean startWithActiveOnly() {
		return mStartWithActiveOnly;
	}

	public Set<MarketListingIndex> getUsedIndexes() {
		TreeSet<MarketListingIndex> out = new TreeSet<>();
		if (mComponents != null) {
			for (FilterComponent comp : mComponents) {
				out.add(comp.mField);
			}
		}
		out.add(this.getSorter().mIndexField);
		return out;
	}

	public MarketFilter addComponents(List<FilterComponent> components) {
		if (this.mComponents == null) {
			this.mComponents = new ArrayList<>();
		}
		this.mComponents.addAll(components);
		return this;
	}

	public List<FilterComponent> getComponents() {
		if (this.mComponents == null) {
			return new ArrayList<>();
		}
		return mComponents;
	}

	public List<FilterComponent> getAllOptimisedComponents() {

		// group components together
		HashMap<MarketListingIndex, HashMap<Comparator, ArrayList<String>>> groupedComponents = new HashMap<>();
		if (mComponents != null) {
			for (FilterComponent comp : mComponents) {
				HashMap<Comparator, ArrayList<String>> indexData = groupedComponents.getOrDefault(comp.mField, new HashMap<>());
				ArrayList<String> comparatorData = indexData.getOrDefault(comp.mComparator, new ArrayList<>());
				if (comp.mValuesList != null && !comp.mValuesList.isEmpty()) {
					comparatorData.addAll(comp.mValuesList);
				}
				indexData.put(comp.mComparator, comparatorData);
				groupedComponents.put(comp.mField, indexData);
			}
		}

		// build new components, separate whitelist and blacklist, sorting by size of values checked
		TreeMap<Integer, ArrayList<FilterComponent>> whitelist = new TreeMap<>();
		TreeMap<Integer, ArrayList<FilterComponent>> blacklist = new TreeMap<>();
		for (Map.Entry<MarketListingIndex, HashMap<Comparator, ArrayList<String>>> entries : groupedComponents.entrySet()) {
			MarketListingIndex field = entries.getKey();
			for (Map.Entry<Comparator, ArrayList<String>> entry : entries.getValue().entrySet()) {
				Comparator comparator = entry.getKey();
				ArrayList<String> values = entry.getValue();
				FilterComponent component = new FilterComponent(field, comparator, values);
				if (comparator == Comparator.WHITELIST) {
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

		// build the final list with order whitelist first to blacklist second, with the smallest size of values first
		List<FilterComponent> out = new ArrayList<>();
		for (ArrayList<FilterComponent> list : whitelist.values()) {
			out.addAll(list);
		}
		for (ArrayList<FilterComponent> list : blacklist.values()) {
			out.addAll(list);
		}

		return out;
	}

	public @Nullable String getDisplayName() {
		return mDisplayName;
	}

	public MarketFilter setDisplayName(@Nullable String displayName) {
		this.mDisplayName = displayName;
		return this;
	}

	public void addComponent(FilterComponent comp) {
		if (mComponents == null) {
			mComponents = new ArrayList<>();
		}
		this.mComponents.add(comp);
	}

	public Sorter getSorter() {
		return mSorter;
	}

	public MarketFilter setSorter(Sorter mSorter) {
		this.mSorter = mSorter;
		return this;
	}
}
