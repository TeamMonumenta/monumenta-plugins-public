package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;

public class Ammunition implements Infusion {

	@Override
	public String getName() {
		return "Ammunition";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.AMMUNITION;
	}
}
