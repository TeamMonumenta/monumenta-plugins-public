package com.playmonumenta.plugins.seasonalevents.gui;

import com.playmonumenta.plugins.seasonalevents.PlayerProgress;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventManager;
import com.playmonumenta.plugins.seasonalevents.SeasonalPass;
import com.playmonumenta.plugins.seasonalevents.WeeklyMission;
import java.util.List;
import org.bukkit.entity.Player;

public class MissionsView extends View {
	public MissionsView(PassGui gui) {
		super(gui);
	}

	@Override
	public void setup(Player displayedPlayer) {
		SeasonalPass pass = mGui.mPass;
		PlayerProgress playerProgress = SeasonalEventManager.getPlayerProgress(displayedPlayer);

		int weeksPerRow = pass.mNumberOfWeeks / 2;
		int bonusWeeks = pass.mNumberOfWeeks % 2;

		for (int metaRow = 0; metaRow <= 1; metaRow++) {
			for (int column = 0; column < weeksPerRow + (metaRow == 1 ? bonusWeeks : 0); column++) {
				int x = column + 1;
				if (x >= 9) {
					// Outside of inventory, redesign required
					continue;
				}
				int week = weeksPerRow * metaRow + column;
				if (week > pass.mNumberOfWeeks) {
					// This should not happen, but just in case
					continue;
				}
				int displayWeek = week + 1;
				List<WeeklyMission> weekMissions = pass.getMissionsInWeek(displayWeek);

				for (int missionIndex = 0; missionIndex < 3; missionIndex++) {
					int y = 3 * metaRow + missionIndex;
					mGui.addMissionIcon(y, x, weekMissions, playerProgress, week, missionIndex);
				}
			}
		}
	}
}
