package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public class Soulbound implements Infusion {

	@Override
	public String getName() {
		return "Soulbound";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.SOULBOUND;
	}

}
