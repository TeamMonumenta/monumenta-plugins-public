package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public class Debarking implements Infusion {

	@Override
	public String getName() {
		return "Debarking";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.DEBARKING;
	}

}
