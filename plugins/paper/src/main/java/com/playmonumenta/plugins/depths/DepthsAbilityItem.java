package com.playmonumenta.plugins.depths;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;

public class DepthsAbilityItem {

	//Structure for item display of abilities

	public @Nullable ItemStack mItem;
	public @Nullable String mAbility;
	public int mRarity;
	public @Nullable DepthsTrigger mTrigger;

	public DepthsAbilityItem() {

	}
}
