package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public class Locked implements Infusion {

	@Override
	public String getName() {
		return "Locked";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.LOCKED;
	}

}
