package com.playmonumenta.plugins.seasonalevents;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class LongMission extends Mission {
	// First week of the pass the mission is active
	public int mFirstWeek;
	// Last week of the pass the mission is active
	public int mLastWeek;

	public LongMission(CommandSender sender,
	                   String startDateStr,
	                   String passName,
	                   JsonObject missionObject,
	                   boolean showWarnings) {
		super(sender, startDateStr, passName, missionObject, showWarnings);
		mFirstWeek = missionObject.get("firstweek").getAsInt();
		if (mFirstWeek <= 0 && showWarnings) {
			sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
					+ passName + ": Long mission week starts at week <= 0: " + mFirstWeek, NamedTextColor.RED)
				.hoverEvent(Component.text(missionObject.toString(), NamedTextColor.RED)));
		}
		mLastWeek = missionObject.get("lastweek").getAsInt();
		if (mLastWeek <= mFirstWeek && showWarnings) {
			sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
					+ passName + ": Long mission last week is <= first week: "
					+ mFirstWeek + " <= " + mLastWeek, NamedTextColor.RED)
				.hoverEvent(Component.text(missionObject.toString(), NamedTextColor.RED)));
		}
	}

	@Override
	public int firstWeek() {
		return mFirstWeek;
	}

	@Override
	public int lastWeek() {
		return mLastWeek;
	}

}
