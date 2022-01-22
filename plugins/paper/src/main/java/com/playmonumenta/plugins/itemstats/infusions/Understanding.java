package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public class Understanding implements Infusion {

	public static final double POINTS_PER_LEVEL = 0.25;

	@Override
	public String getName() {
		return "Understanding";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.UNDERSTANDING;
	}
}
