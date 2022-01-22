package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public class StatTrack implements Infusion {

	@Override
	public String getName() {
		return "Stat Track";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK;
	}

}
