package com.playmonumenta.plugins.seasonalevents;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class WeeklyMission extends Mission {
	// Which week of the pass the mission is active
	public int mWeek;

	public WeeklyMission(CommandSender sender,
	                     String startDateStr,
						 String passName,
	                     JsonObject missionObject,
	                     boolean showWarnings) {
		super(sender, startDateStr, passName, missionObject, showWarnings);
		mWeek = missionObject.get("week").getAsInt();
		if (mWeek <= 0 && showWarnings) {
			sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
					+ passName + ": Mission week is <= 0: " + mWeek, NamedTextColor.RED)
				.hoverEvent(Component.text(missionObject.toString(), NamedTextColor.RED)));
		}
	}

	@Override
	public int firstWeek() {
		return mWeek;
	}

	@Override
	public int lastWeek() {
		return mWeek;
	}

}
