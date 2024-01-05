package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;

public class Revelation implements Infusion {

	@Override
	public String getName() {
		return "Revelation";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.REVELATION;
	}
}
