package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;

public class StatTrackRepair implements Infusion {
	@Override
	public InfusionType getInfusionType() {
		return InfusionType.STAT_TRACK_REPAIR;
	}

	@Override
	public String getName() {
		return "Times Repaired";
	}

	// Increment happens in inventories\AnvilFixInInventory
}
