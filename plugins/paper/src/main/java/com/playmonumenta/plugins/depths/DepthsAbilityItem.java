package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.GUIUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DepthsAbilityItem {

	//Structure for item display of abilities

	// This class is automatically serialized!
	// This is why we must be able to forget the item stack and regenerate it later if it is null
	public transient @Nullable ItemStack mItem;

	public final String mAbility;
	public final int mRarity;
	public final int mPreviousRarity; // 0 if not displaying previous rarity
	public final int mPreIncreaseRarity; // 0 if not applicable
	public final boolean mUseAbility;
	public final DepthsTrigger mTrigger;
	public final @Nullable DepthsTree mTree;

	public DepthsAbilityItem(ItemStack item, String ability, int rarity, int previousRarity, int preIncreaseRarity, boolean useAbility, DepthsTrigger trigger, @Nullable DepthsTree tree) {
		mItem = item;
		mAbility = ability;
		mRarity = rarity;
		mPreviousRarity = previousRarity;
		mPreIncreaseRarity = preIncreaseRarity;
		mTrigger = trigger;
		mTree = tree;
		mUseAbility = useAbility;
	}

	public ItemStack getItem(@Nullable Player player) {
		if (mItem == null) {
			DepthsAbilityInfo<?> info = DepthsManager.getInstance().getAbility(mAbility);
			if (info == null) {
				mItem = GUIUtils.createBasicItem(Material.BARRIER, "You should not be seeing this. Please report this bug.", NamedTextColor.RED);
			} else {
				mItem = info.createAbilityItem(mRarity, mPreviousRarity, mPreIncreaseRarity, player, mUseAbility);
			}
		}
		return mItem;
	}
}
