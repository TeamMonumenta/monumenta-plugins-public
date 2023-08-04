package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;

public class WaxOff implements Infusion {

	@Override
	public String getName() {
		return "Wax Off";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.WAX_OFF;
	}

}
