package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;

public class PotionRadius implements Attribute {
	@Override
	public String getName() {
		return "Potion Radius";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.POTION_RADIUS;
	}

	@Override
	public double getPriorityAmount() {
		return 2;
	}
}
