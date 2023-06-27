package com.playmonumenta.plugins.seasonalevents.gui;

import com.playmonumenta.plugins.seasonalevents.PlayerProgress;
import com.playmonumenta.plugins.seasonalevents.PlayerProgress.PassProgress;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventManager;
import com.playmonumenta.plugins.seasonalevents.SeasonalPass;
import com.playmonumenta.plugins.seasonalevents.WeeklyMission;
import com.playmonumenta.plugins.utils.DateUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class WeekView extends View {
	protected final int REWARDS_PAGE_DELTA = 4;
	protected int mRewardsStartIndex;

	public WeekView(PassGui gui) {
		super(gui);

		PassProgress passProgress = gui.getPassProgress();
		int level = gui.getLevel(passProgress);

		mRewardsStartIndex = level - (PassGui.MAX_X / 2);
	}

	@Override
	public void setup(Player displayedPlayer) {
		ItemStack item;
		ItemMeta meta;

		SeasonalPass pass = mGui.mPass;
		int displayWeek = pass.getWeekOfPass(DateUtils.localDateTime(7 * mGui.mDisplayedEpochWeek));
		int week = displayWeek - 1;
		if (week < 0) {
			week = 0;
		}
		if (week >= pass.mNumberOfWeeks) {
			week = pass.mNumberOfWeeks - 1;
		}

		/* Missions */

		if (week > 0) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Previous Missions", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			mGui.setItem(0, 1, item).onClick((InventoryClickEvent event) -> prevMissionsPage());
		}

		long nextWeek = mGui.mDisplayedEpochWeek + 1;
		if (week < pass.mNumberOfWeeks - 1 && !mGui.isFutureEpochWeek(nextWeek)) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Next Missions", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			mGui.setItem(0, 8, item).onClick((InventoryClickEvent event) -> nextMissionsPage());
		}

		// In case this is the fallback pass with 0 weeks
		if (week >= 0) {
			item = new ItemStack(Material.CLOCK, displayWeek);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Week " + displayWeek, NamedTextColor.GOLD, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			mGui.setItem(0, 2, item);

			List<WeeklyMission> weekMissions = pass.getMissionsInWeek(displayWeek);
			PlayerProgress playerProgress = SeasonalEventManager.getPlayerProgress(displayedPlayer);

			int numMissions = weekMissions.size();
			for (int missionIndex = 0; missionIndex < numMissions; missionIndex++) {
				mGui.addMissionIcon(0, 3 + missionIndex, weekMissions, playerProgress, week, missionIndex);
			}
		}

		/* Rewards */

		// Page bounds check
		mRewardsStartIndex = Math.max(0, Math.min(mRewardsStartIndex, pass.mRewards.size() - PassGui.MAX_X - 1));

		if (mRewardsStartIndex > 0) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Previous Rewards", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			mGui.setItem(3, 1, item).onClick((InventoryClickEvent event) -> prevRewardsPage());
		}

		if (mRewardsStartIndex < pass.mRewards.size() - PassGui.MAX_X - 1) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Next Rewards", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			mGui.setItem(3, PassGui.MAX_X, item).onClick((InventoryClickEvent event) -> nextRewardsPage());
		}

		for (int x = 0; x <= PassGui.MAX_X; x++) {
			int rewardIndex = mRewardsStartIndex + x;
			mGui.addRewardIndicatorIcon(4, x, displayedPlayer, rewardIndex);
			mGui.addRewardItem(5, x, displayedPlayer, rewardIndex);
		}
	}

	public void prevMissionsPage() {
		mGui.mDisplayedEpochWeek--;
		mGui.updateWithPageSound();
	}

	public void nextMissionsPage() {
		mGui.mDisplayedEpochWeek++;
		mGui.updateWithPageSound();
	}

	public void prevRewardsPage() {
		mRewardsStartIndex -= REWARDS_PAGE_DELTA;
		mGui.updateWithPageSound();
	}

	public void nextRewardsPage() {
		mRewardsStartIndex += REWARDS_PAGE_DELTA;
		mGui.updateWithPageSound();
	}
}
