package com.playmonumenta.plugins.seasonalevents;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class SeasonalReward {

	public SeasonalRewardType mType;

	// String data- name of title, or parrot unlock, or ability skin name
	public String mData;

	// Int data- for amounts such as number of loot spins
	public int mAmount;

	// Fields for item display in GUI
	public String mName;
	public String mDescription;
	public Material mDisplayItem;
	public NamedTextColor mNameColor;
	public NamedTextColor mDescriptionColor;
	public ItemStack mLootTable;

	public SeasonalReward(SeasonalRewardType type, String data, int amount, String name, String description, Material displayItem) {
		mType = type;
		mData = data;
		mAmount = amount;
		mName = name;
		mDescription = description;
		mDisplayItem = displayItem;
	}

	public SeasonalReward() {

	}

}
