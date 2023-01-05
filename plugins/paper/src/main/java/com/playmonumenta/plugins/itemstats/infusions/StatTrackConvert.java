package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils;

public class StatTrackConvert implements Infusion {
	@Override
	public ItemStatUtils.InfusionType getInfusionType() {
		return ItemStatUtils.InfusionType.STAT_TRACK_CONVERT;
	}

	@Override
	public String getName() {
		return "Items Converted";
	}

	// Increment happens in commands\experiencinator\ExperiencinatorUtils
}
