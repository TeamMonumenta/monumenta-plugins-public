package com.playmonumenta.plugins.itemstats;

import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public interface Infusion extends ItemStat {

	/**
	 * A reference back to the associated InfusionType in ItemStatUtils.
	 *
	 * @returns the associated InfusionType
	 */
	InfusionType getInfusionType();

}
