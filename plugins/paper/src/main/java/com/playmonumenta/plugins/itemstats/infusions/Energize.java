package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;

public class Energize implements Infusion {

	@Override
	public String getName() {
		return "Energized";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ENERGIZE;
	}

}
