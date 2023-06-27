package com.playmonumenta.plugins.seasonalevents.gui;

import org.bukkit.entity.Player;

public class RewardsView extends View {
	public RewardsView(PassGui gui) {
		super(gui);
	}

	@Override
	public void setup(Player displayedPlayer) {
		int x;
		int rewardIndex;

		for (int i = 0; i < 8; i++) {
			x = i + 1;
			rewardIndex = i;
			mGui.addRewardIndicatorIcon(0, x, displayedPlayer, rewardIndex);
			mGui.addRewardItem(1, x, displayedPlayer, rewardIndex);
		}

		for (int i = 0; i < 8; i++) {
			x = i + 1;
			rewardIndex = i + 8;
			mGui.addRewardIndicatorIcon(2, x, displayedPlayer, rewardIndex);
			mGui.addRewardItem(3, x, displayedPlayer, rewardIndex);
		}

		for (int i = 0; i < 9; i++) {
			x = i;
			rewardIndex = i + 16;
			mGui.addRewardIndicatorIcon(4, x, displayedPlayer, rewardIndex);
			mGui.addRewardItem(5, x, displayedPlayer, rewardIndex);
		}
	}
}
