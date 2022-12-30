package com.playmonumenta.plugins.infinitytower.guis;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class TowerGuiItem {
	protected final ItemStack mItemStack;
	protected final @Nullable Conditions mConds;
	protected final @Nullable PostClick mPostClick;


	public TowerGuiItem(ItemStack item) {
		this(item, null);
	}

	public TowerGuiItem(ItemStack item, @Nullable Conditions cond) {
		this(item, cond, null);
	}

	public TowerGuiItem(ItemStack item, @Nullable Conditions cond, @Nullable PostClick post) {
		mItemStack = item;
		mConds = cond;
		mPostClick = post;
	}


	public @Nullable ItemStack getItem(Player player) {
		if (mConds == null) {
			return mItemStack;
		}

		return mConds.apply(player) ? mItemStack : null;

	}

	@FunctionalInterface
	public interface Conditions {
		boolean apply(Player p);
	}

	@FunctionalInterface
	public interface PostClick {
		boolean apply(Player p, int slot);
	}
}
