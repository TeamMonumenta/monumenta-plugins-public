package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;

public class HuntTrack implements Infusion {

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.HUNT_TRACK;
	}

	@Override
	public String getName() {
		return "HuntTrack";
	}

}
