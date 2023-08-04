package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;

public class StatTrackConvert implements Infusion {
	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_CONVERT;
	}

	@Override
	public String getName() {
		return "Items Converted";
	}

	// Increment happens in commands\experiencinator\ExperiencinatorUtils
}
