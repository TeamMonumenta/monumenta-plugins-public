package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;

public class StatTrackBlocks implements Infusion {

	@Override
	public String getName() {
		return "Blocks Placed";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_BLOCKS;
	}
}
