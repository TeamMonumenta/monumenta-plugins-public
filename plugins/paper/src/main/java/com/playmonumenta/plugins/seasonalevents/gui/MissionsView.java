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

		for (int metaColumn = 0; metaColumn <= 1; metaColumn++) {
			for (int y = 0; y < 6; y++) {
				int x = 4 * metaColumn + 1;
				int week = 6 * metaColumn + y;
				int displayWeek = week + 1;

				List<WeeklyMission> weekMissions = pass.getMissionsInWeek(displayWeek);

				mGui.addWeekIcon(y, x, displayWeek);
				for (int missionIndex = 0; missionIndex < 3; missionIndex++) {
					mGui.addMissionIcon(y, x + missionIndex + 1, weekMissions, playerProgress, week, missionIndex);
				}
			}
		}
	}
}
