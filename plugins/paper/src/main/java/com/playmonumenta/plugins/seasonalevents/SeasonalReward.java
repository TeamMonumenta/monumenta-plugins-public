package com.playmonumenta.plugins.seasonalevents;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SeasonalReward {

	public SeasonalRewardType mType;

	// String data- name of title, or parrot unlock, or ability skin name
	public @Nullable String mData;

	// Int data- for amounts such as number of loot spins
	public int mAmount;

	// Fields for item display in GUI
	public @Nullable String mName;
	public @Nullable String mDescription;
	public @Nullable Material mDisplayItem;
	public @Nullable NamedTextColor mNameColor;
	public @Nullable NamedTextColor mDescriptionColor;
	public @Nullable ItemStack mLootTable;

	public SeasonalReward(SeasonalRewardType type, String data, int amount, String name, String description, Material displayItem) {
		mType = type;
		mData = data;
		mAmount = amount;
		mName = name;
		mDescription = description;
		mDisplayItem = displayItem;
	}

	public SeasonalReward(SeasonalRewardType type) {
		mType = type;
	}

}
