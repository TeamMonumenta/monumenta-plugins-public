package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public class Rustworthy implements Infusion {

	@Override
	public String getName() {
		return "Rustworthy";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.RUSTWORTHY;
	}

}
