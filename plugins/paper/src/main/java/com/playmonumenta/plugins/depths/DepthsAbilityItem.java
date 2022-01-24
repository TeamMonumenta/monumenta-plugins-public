package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import org.bukkit.inventory.ItemStack;
import javax.annotation.Nullable;

public class DepthsAbilityItem {

	//Structure for item display of abilities

	public @Nullable ItemStack mItem;
	public @Nullable String mAbility;
	public int mRarity;
	public @Nullable DepthsTrigger mTrigger;

	public DepthsAbilityItem() {

	}
}
