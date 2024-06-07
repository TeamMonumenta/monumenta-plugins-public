package com.playmonumenta.plugins.seasonalevents.gui;

import com.playmonumenta.plugins.seasonalevents.LongMission;
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

		List<LongMission> longMissions = pass.getLongMissions();
		int longMissionColumns = longMissions.size();
		longMissionColumns = longMissionColumns / 3 + ((longMissionColumns % 3 == 0) ? 0 : 1);
		int columns = pass.mNumberOfWeeks + longMissionColumns;

		int columnsPerRow = columns / 2;
		int leftoverColumns = columns % 2;

		for (int metaRow = 0; metaRow <= 1; metaRow++) {
			for (int column = 0; column < columnsPerRow + (metaRow == 1 ? leftoverColumns : 0); column++) {
				int x = column + 1;
				if (x >= 9) {
					// Outside of inventory, redesign required
					continue;
				}
				int week = columnsPerRow * metaRow + column;
				if (week >= pass.mNumberOfWeeks) {
					// Long mission entries go here
					int longMissionColumn = week - pass.mNumberOfWeeks;
					for (int missionSubIndex = 0; missionSubIndex < 3; missionSubIndex++) {
						int missionIndex = longMissionColumn * 3 + missionSubIndex;
						if (missionIndex > longMissions.size()) {
							break;
						}

						int y = 3 * metaRow + missionSubIndex;
						mGui.addLongMissionIcon(y, x, longMissions, playerProgress, missionIndex);
					}
					continue;
				}

				// Weekly mission entries go here
				int displayWeek = week + 1;
				List<WeeklyMission> weekMissions = pass.getActiveWeeklyMissions(displayWeek);

				for (int missionIndex = 0; missionIndex < 3; missionIndex++) {
					int y = 3 * metaRow + missionIndex;
					mGui.addWeeklyMissionIcon(y, x, weekMissions, playerProgress, week, missionIndex);
				}
			}
		}
	}
}
