package com.playmonumenta.plugins.fishing;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public abstract class FishingMinigame {
	private boolean mHasBegun = false;
	private boolean mTwentyTicksPassed = false;
	private boolean mBlockInput = false;

	private @Nullable String mForcedLootTablePath = null;

	public void setForcedLootTable(String lootTablePath) {
		mForcedLootTablePath = lootTablePath;
	}

	public @Nullable String getForcedLootTable() {
		return mForcedLootTablePath;
	}

	protected final void beginTracking() {
		mHasBegun = true;
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mTwentyTicksPassed = true, 20);
	}

	protected final boolean minigameUnstarted() {
		return !mHasBegun;
	}

	protected final boolean twentyTicksPassed() {
		return mTwentyTicksPassed;
	}

	protected final void onLeftClickInternal() {
		if (mBlockInput) {
			return;
		}
		blockInputThreeTicks();
		onLeftClick();
	}

	protected final void onRightClickInternal() {
		if (mBlockInput) {
			return;
		}
		blockInputThreeTicks();
		onRightClick();
	}

	protected final void blockInputThreeTicks() {
		mBlockInput = true;
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mBlockInput = false, 3);
	}

	protected abstract void beginMinigame(FishingManager fishingManager, Player player, Location centre);

	protected abstract void cancelMinigame();

	protected abstract void previewMinigame(Player player, Location centre);

	protected abstract void onLeftClick();

	protected abstract void onRightClick();
}
