package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;

public class Owned implements Infusion {

	@Override
	public String getName() {
		return "Owned";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.OWNED;
	}

}
