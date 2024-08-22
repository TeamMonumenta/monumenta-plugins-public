package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;

public class Hexed implements Infusion {

	@Override
	public String getName() {
		return "Hexed";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.HEXED;
	}
}
