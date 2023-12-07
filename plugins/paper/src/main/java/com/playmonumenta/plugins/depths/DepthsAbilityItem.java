package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import org.bukkit.inventory.ItemStack;

public class DepthsAbilityItem {

	//Structure for item display of abilities

	public ItemStack mItem;
	public String mAbility;
	public int mRarity;
	public DepthsTrigger mTrigger;

	public DepthsAbilityItem(ItemStack item, String ability, int rarity, DepthsTrigger trigger) {
		mItem = item;
		mAbility = ability;
		mRarity = rarity;
		mTrigger = trigger;
	}
}
