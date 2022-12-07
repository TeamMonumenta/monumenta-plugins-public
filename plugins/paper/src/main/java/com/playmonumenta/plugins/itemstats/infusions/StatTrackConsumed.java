package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public class StatTrackConsumed implements Infusion {

	@Override
	public String getName() {
		return "Times Consumed";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_CONSUMED;
	}

}
