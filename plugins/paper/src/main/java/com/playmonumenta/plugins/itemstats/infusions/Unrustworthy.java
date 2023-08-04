package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;

public class Unrustworthy implements Infusion {

	@Override
	public String getName() {
		return "Unrustworthy";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.UNRUSTWORTHY;
	}

}
